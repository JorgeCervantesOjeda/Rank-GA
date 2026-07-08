// C:/Users/usuario/ownCloud2/RankGA/src/Problems/SimpleCircularRacingBackend.java
// Deterministic kinematic racing backend on a circular track for development and tests.
package Problems;

import java.util.Random;

public final class SimpleCircularRacingBackend
  implements RacingBackend {

  private final SimpleCircularTrack track;
  private final double timeStepSeconds;
  private final double steeringRateGain;
  private final double throttleAcceleration;
  private final double brakeAcceleration;
  private final double dragCoefficient;
  private final double maxSpeed;
  private RacingCarState currentState;
  private double lastTrackAngle;
  private double progressMeters;

  public SimpleCircularRacingBackend( SimpleCircularTrack track,
                                      double timeStepSeconds,
                                      double steeringRateGain,
                                      double throttleAcceleration,
                                      double brakeAcceleration,
                                      double dragCoefficient,
                                      double maxSpeed ) {
    if( track == null ) {
      throw new IllegalArgumentException( "track must not be null" );
    }
    validatePositive( "timeStepSeconds",
                      timeStepSeconds );
    validatePositive( "steeringRateGain",
                      steeringRateGain );
    validatePositive( "throttleAcceleration",
                      throttleAcceleration );
    validatePositive( "brakeAcceleration",
                      brakeAcceleration );
    validateNonNegative( "dragCoefficient",
                         dragCoefficient );
    validatePositive( "maxSpeed",
                      maxSpeed );
    this.track = track;
    this.timeStepSeconds = timeStepSeconds;
    this.steeringRateGain = steeringRateGain;
    this.throttleAcceleration = throttleAcceleration;
    this.brakeAcceleration = brakeAcceleration;
    this.dragCoefficient = dragCoefficient;
    this.maxSpeed = maxSpeed;
  }

  @Override
  public String getTrackName() {
    return track.getTrackName();
  }

  @Override
  public RacingStartState sampleStartState( Random random ) {
    if( random == null ) {
      throw new IllegalArgumentException( "random must not be null" );
    }
    double angle = 2.0 * Math.PI * random.nextDouble();
    double radius = track.getInnerRadius()
                    + ( track.getOuterRadius() - track.getInnerRadius() )
                    * random.nextDouble();
    double x = track.getCenterX() + radius * Math.cos( angle );
    double y = track.getCenterY() + radius * Math.sin( angle );
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
    currentState = new RacingCarState( startState.getX(),
                                       startState.getY(),
                                       startState.getSpeed(),
                                       wrapToPi( startState.getHeading() ),
                                       0.0 );
    lastTrackAngle = track.computeAngle( currentState.getX(),
                                         currentState.getY() );
    progressMeters = 0.0;
    return currentState;
  }

  @Override
  public RacingStepResult step( RacingBackendAction backendAction ) {
    ensureInitialized();
    validateAction( backendAction );

    double nextSpeed = computeNextSpeed( backendAction );
    double averageSpeed = 0.5 * ( currentState.getSpeed() + nextSpeed );
    double nextHeading = backendAction.hasHeadingTarget()
                         ? wrapToPi( backendAction.getHeadingTarget() )
                         : wrapToPi(
                           currentState.getHeading()
                           + steeringRateGain
                             * backendAction.getSteeringCommand()
                             * timeStepSeconds );
    double nextX = currentState.getX()
                   + averageSpeed * Math.cos( nextHeading ) * timeStepSeconds;
    double nextY = currentState.getY()
                   + averageSpeed * Math.sin( nextHeading ) * timeStepSeconds;
    double nextTimeSeconds = currentState.getTimeSeconds() + timeStepSeconds;
    currentState = new RacingCarState( nextX,
                                       nextY,
                                       nextSpeed,
                                       nextHeading,
                                       nextTimeSeconds );

    double nextTrackAngle = track.computeAngle( nextX,
                                                nextY );
    double deltaAngle = wrapToPi( nextTrackAngle - lastTrackAngle );
    progressMeters += track.getRadiusOfCenterline() * deltaAngle;
    lastTrackAngle = nextTrackAngle;

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
    return track.getOuterRadius();
  }

  @Override
  public double getPolicyHalfRangeY() {
    return track.getOuterRadius();
  }

  @Override
  public double getPolicySpeedScale() {
    return maxSpeed;
  }

  private double computeNextSpeed( RacingBackendAction backendAction ) {
    double acceleration = throttleAcceleration * backendAction.getThrottleCommand()
                          - brakeAcceleration * backendAction.getBrakeCommand()
                          - dragCoefficient * currentState.getSpeed();
    double rawSpeed = currentState.getSpeed() + acceleration * timeStepSeconds;
    if( rawSpeed < 0.0 ) {
      return 0.0;
    }
    if( rawSpeed > maxSpeed ) {
      return maxSpeed;
    }
    return rawSpeed;
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
    if( Math.abs( backendAction.getSteeringCommand() ) > 1.0 ) {
      throw new IllegalArgumentException( "steeringCommand must lie in [-1, 1]" );
    }
    if( backendAction.getThrottleCommand() > 1.0 ) {
      throw new IllegalArgumentException( "throttleCommand must lie in [0, 1]" );
    }
    if( backendAction.getBrakeCommand() > 1.0 ) {
      throw new IllegalArgumentException( "brakeCommand must lie in [0, 1]" );
    }
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
