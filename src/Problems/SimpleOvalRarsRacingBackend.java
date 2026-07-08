// C:/Users/usuario/ownCloud2/RankGA/src/Problems/SimpleOvalRarsRacingBackend.java
// RARS-inspired point-mass oval backend driven by commanded wheel direction and speed.
package Problems;

import java.util.Random;

public final class SimpleOvalRarsRacingBackend
  implements RacingBackend {

  private static final double GRAVITY_METERS_PER_SECOND_SQUARED = 9.81;
  private static final double EPSILON = 1.0e-9;

  private final SimpleOvalTrack track;
  private final double timeStepSeconds;
  private final double massKilograms;
  private final double maxPowerWatts;
  private final double maxFrictionCoefficient;
  private final double slipSpeedScaleMetersPerSecond;
  private final double dragCoefficient;
  private final double maxSpeed;
  private RacingCarState currentState;
  private double velocityX;
  private double velocityY;
  private double lastProgressMetersOnLap;
  private double progressMeters;

  public SimpleOvalRarsRacingBackend( SimpleOvalTrack track,
                                      double timeStepSeconds,
                                      double massKilograms,
                                      double maxPowerWatts,
                                      double maxFrictionCoefficient,
                                      double slipSpeedScaleMetersPerSecond,
                                      double dragCoefficient,
                                      double maxSpeed ) {
    if( track == null ) {
      throw new IllegalArgumentException( "track must not be null" );
    }
    validatePositive( "timeStepSeconds",
                      timeStepSeconds );
    validatePositive( "massKilograms",
                      massKilograms );
    validatePositive( "maxPowerWatts",
                      maxPowerWatts );
    validatePositive( "maxFrictionCoefficient",
                      maxFrictionCoefficient );
    validatePositive( "slipSpeedScaleMetersPerSecond",
                      slipSpeedScaleMetersPerSecond );
    validateNonNegative( "dragCoefficient",
                         dragCoefficient );
    validatePositive( "maxSpeed",
                      maxSpeed );
    this.track = track;
    this.timeStepSeconds = timeStepSeconds;
    this.massKilograms = massKilograms;
    this.maxPowerWatts = maxPowerWatts;
    this.maxFrictionCoefficient = maxFrictionCoefficient;
    this.slipSpeedScaleMetersPerSecond = slipSpeedScaleMetersPerSecond;
    this.dragCoefficient = dragCoefficient;
    this.maxSpeed = maxSpeed;
  }

  @Override
  public String getTrackName() {
    return track.getTrackName() + "_rars";
  }

  public SimpleOvalTrack getTrack() {
    return track;
  }

  @Override
  public RacingStartState sampleStartState( Random random ) {
    if( random == null ) {
      throw new IllegalArgumentException( "random must not be null" );
    }
    double progressMetersOnLap = track.getLapLength() * random.nextDouble();
    double lateralOffsetMeters = track.getHalfWidth()
                                 * ( 2.0 * random.nextDouble() - 1.0 );
    SimpleOvalTrack.CenterlinePose pose = track.computeCenterlinePose(
      progressMetersOnLap );
    double x = pose.getX() + lateralOffsetMeters * pose.getNormalX();
    double y = pose.getY() + lateralOffsetMeters * pose.getNormalY();
    double heading = 2.0 * Math.PI * random.nextDouble();
    return new RacingStartState( x,
                                 y,
                                 0.0,
                                 heading );
  }

  @Override
  public RacingCarState resetEpisode( RacingStartState startState ) {
    if( startState == null ) {
      throw new IllegalArgumentException( "startState must not be null" );
    }
    if( !track.isInsideTrack( startState.getX(),
                              startState.getY() ) ) {
      throw new IllegalArgumentException( "startState must lie inside the track" );
    }
    double startHeading = wrapToPi( startState.getHeading() );
    double startSpeed = clamp( startState.getSpeed(),
                               0.0,
                               maxSpeed );
    velocityX = startSpeed * Math.cos( startHeading );
    velocityY = startSpeed * Math.sin( startHeading );
    currentState = new RacingCarState( startState.getX(),
                                       startState.getY(),
                                       startSpeed,
                                       startHeading,
                                       0.0 );
    lastProgressMetersOnLap = track.projectProgressMeters( currentState.getX(),
                                                           currentState.getY() );
    progressMeters = 0.0;
    return currentState;
  }

  @Override
  public RacingStepResult step( RacingBackendAction backendAction ) {
    ensureInitialized();
    validateAction( backendAction );

    double speed = vecMag( velocityX,
                           velocityY );
    double velocityHeading = speed > EPSILON
                             ? Math.atan2( velocityY,
                                           velocityX )
                             : currentState.getHeading();
    double wheelDirection = wrapToPi( backendAction.getHeadingTarget() );
    double commandedWheelSpeed = clamp( backendAction.getSpeedTarget(),
                                        0.0,
                                        maxSpeed );
    double alpha = wrapToPi( wheelDirection - velocityHeading );
    double sine = Math.sin( alpha );
    double cosine = Math.cos( alpha );
    double slipNormal = -commandedWheelSpeed * sine;
    double slipTangential = speed - commandedWheelSpeed * cosine;
    double slipSpeed = vecMag( slipTangential,
                               slipNormal );
    double trackForce = massKilograms
                        * GRAVITY_METERS_PER_SECOND_SQUARED
                        * computeFrictionCoefficient( slipSpeed );
    double forceNormal;
    double forceTangential;
    if( slipSpeed <= EPSILON ) {
      forceNormal = 0.0;
      forceTangential = 0.0;
    } else {
      forceNormal = -trackForce * slipNormal / slipSpeed;
      forceTangential = -trackForce * slipTangential / slipSpeed;
    }

    double powerWatts = Math.abs( commandedWheelSpeed )
                        * ( forceTangential * cosine + forceNormal * sine );
    if( powerWatts > maxPowerWatts && powerWatts > EPSILON ) {
      double powerScale = maxPowerWatts / powerWatts;
      forceNormal *= powerScale;
      forceTangential *= powerScale;
    }

    double dragForce = dragCoefficient * speed * speed;
    double tangentialAcceleration = ( forceTangential - dragForce )
                                    / massKilograms;
    double normalAcceleration = forceNormal / massKilograms;
    double accelerationX = tangentialAcceleration * Math.cos( velocityHeading )
                           - normalAcceleration * Math.sin( velocityHeading );
    double accelerationY = normalAcceleration * Math.cos( velocityHeading )
                           + tangentialAcceleration * Math.sin( velocityHeading );

    double nextVelocityX = velocityX + accelerationX * timeStepSeconds;
    double nextVelocityY = velocityY + accelerationY * timeStepSeconds;
    double nextSpeed = vecMag( nextVelocityX,
                               nextVelocityY );
    if( nextSpeed > maxSpeed ) {
      double speedScale = maxSpeed / nextSpeed;
      nextVelocityX *= speedScale;
      nextVelocityY *= speedScale;
      nextSpeed = maxSpeed;
    }
    double nextX = currentState.getX()
                   + 0.5 * ( velocityX + nextVelocityX ) * timeStepSeconds;
    double nextY = currentState.getY()
                   + 0.5 * ( velocityY + nextVelocityY ) * timeStepSeconds;
    velocityX = nextVelocityX;
    velocityY = nextVelocityY;
    double nextHeading = nextSpeed > EPSILON
                         ? Math.atan2( velocityY,
                                       velocityX )
                         : velocityHeading;
    currentState = new RacingCarState( nextX,
                                       nextY,
                                       nextSpeed,
                                       wrapToPi( nextHeading ),
                                       currentState.getTimeSeconds()
                                       + timeStepSeconds );

    double nextProgressMetersOnLap = track.projectProgressMeters( nextX,
                                                                  nextY );
    progressMeters += wrapProgressDelta( nextProgressMetersOnLap
                                         - lastProgressMetersOnLap,
                                         track.getLapLength() );
    lastProgressMetersOnLap = nextProgressMetersOnLap;

    RacingTerminationReason terminationReason = track.isInsideTrack( nextX,
                                                                     nextY )
                                                ? RacingTerminationReason.NONE
                                                : RacingTerminationReason.OFF_TRACK;
    return new RacingStepResult( currentState,
                                 progressMeters,
                                 terminationReason );
  }

  @Override
  public double measureProgress( RacingCarState carState ) {
    ensureInitialized();
    if( carState == null ) {
      throw new IllegalArgumentException( "carState must not be null" );
    }
    return progressMeters;
  }

  @Override
  public boolean isInsideTrack( double x,
                                double y ) {
    return track.isInsideTrack( x,
                                y );
  }

  @Override
  public double getPolicyCenterX() {
    return track.getCenterX();
  }

  @Override
  public double getPolicyCenterY() {
    return track.getCenterY();
  }

  @Override
  public double getPolicyHalfRangeX() {
    return track.getHalfLengthOfStraight()
           + track.getRadiusOfTurns()
           + track.getHalfWidth();
  }

  @Override
  public double getPolicyHalfRangeY() {
    return track.getRadiusOfTurns() + track.getHalfWidth();
  }

  @Override
  public double getPolicySpeedScale() {
    return maxSpeed;
  }

  private double computeFrictionCoefficient( double slipSpeed ) {
    return maxFrictionCoefficient
           * ( 1.0 - Math.exp( -slipSpeed / slipSpeedScaleMetersPerSecond ) );
  }

  private static double wrapProgressDelta( double deltaProgressMeters,
                                           double lapLength ) {
    if( deltaProgressMeters > lapLength / 2.0 ) {
      return deltaProgressMeters - lapLength;
    }
    if( deltaProgressMeters < -lapLength / 2.0 ) {
      return deltaProgressMeters + lapLength;
    }
    return deltaProgressMeters;
  }

  private void ensureInitialized() {
    if( currentState == null ) {
      throw new IllegalStateException( "resetEpisode must be called before stepping the backend" );
    }
  }

  private static void validateAction( RacingBackendAction backendAction ) {
    if( backendAction == null ) {
      throw new IllegalArgumentException( "backendAction must not be null" );
    }
    if( !backendAction.hasHeadingTarget() ) {
      throw new IllegalArgumentException( "RARS backend requires headingTarget" );
    }
    if( !backendAction.hasSpeedTarget() ) {
      throw new IllegalArgumentException( "RARS backend requires speedTarget" );
    }
  }

  private static double vecMag( double x,
                                double y ) {
    return Math.sqrt( x * x + y * y );
  }

  private static double clamp( double value,
                               double minValue,
                               double maxValue ) {
    if( value < minValue ) {
      return minValue;
    }
    if( value > maxValue ) {
      return maxValue;
    }
    return value;
  }

  private static double wrapToPi( double angle ) {
    double wrappedAngle = angle;
    while( wrappedAngle <= -Math.PI ) {
      wrappedAngle += 2.0 * Math.PI;
    }
    while( wrappedAngle > Math.PI ) {
      wrappedAngle -= 2.0 * Math.PI;
    }
    return wrappedAngle;
  }

  private static void validatePositive( String label,
                                        double value ) {
    validateFinite( label,
                    value );
    if( value <= 0.0 ) {
      throw new IllegalArgumentException( label + " must be positive" );
    }
  }

  private static void validateNonNegative( String label,
                                           double value ) {
    validateFinite( label,
                    value );
    if( value < 0.0 ) {
      throw new IllegalArgumentException( label + " must be non-negative" );
    }
  }

  private static void validateFinite( String label,
                                      double value ) {
    if( Double.isNaN( value ) || Double.isInfinite( value ) ) {
      throw new IllegalArgumentException( label + " must be finite" );
    }
  }
}
