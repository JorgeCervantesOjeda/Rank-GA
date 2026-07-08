// C:/Users/usuario/ownCloud2/RankGA/src/Problems/SimpleOvalRacingBatchEvaluator.java
// Pure CPU batch evaluator for ProblemRacing with SimpleOvalRacingBackend.
package Problems;

import rankga.Individual;

final class SimpleOvalRacingBatchEvaluator {

  private static final int COUNT_OF_FIELDS_PER_ANCHOR = 4;
  private static final int INDEX_OF_X = 0;
  private static final int INDEX_OF_Y = 1;
  private static final int INDEX_OF_SPEED_TARGET = 2;
  private static final int INDEX_OF_DIRECTION_TARGET = 3;
  private static final double EPSILON = 1.0e-9;
  private static final double HALF_PI = Math.PI / 2.0;

  private final SimpleOvalTrack track;
  private final int countOfAnchors;
  private final double currentTimeLimitSeconds;
  private final double weightOfPosition;
  private final double inverseDistancePower;
  private final double policyCenterX;
  private final double policyCenterY;
  private final double policyHalfRangeX;
  private final double policyHalfRangeY;
  private final double policySpeedScale;
  private final double goalDistanceMeters;
  private final boolean hasFiniteGoalDistance;
  private final double timeStepSeconds;
  private final double steeringRateGain;
  private final double throttleAcceleration;
  private final double brakeAcceleration;
  private final double dragCoefficient;
  private final double maxSpeed;
  private final double directionGain;
  private final double throttleGain;
  private final double brakeGain;

  SimpleOvalRacingBatchEvaluator( SimpleOvalRacingBackend backend,
                                  SimpleTargetRacingActionAdapter actionAdapter,
                                  int countOfAnchors,
                                  double currentTimeLimitSeconds,
                                  double weightOfPosition,
                                  double inverseDistancePower,
                                  double goalDistanceMeters,
                                  boolean hasFiniteGoalDistance ) {
    this.track = backend.getTrack();
    this.countOfAnchors = countOfAnchors;
    this.currentTimeLimitSeconds = currentTimeLimitSeconds;
    this.weightOfPosition = weightOfPosition;
    this.inverseDistancePower = inverseDistancePower;
    this.policyCenterX = backend.getPolicyCenterX();
    this.policyCenterY = backend.getPolicyCenterY();
    this.policyHalfRangeX = backend.getPolicyHalfRangeX();
    this.policyHalfRangeY = backend.getPolicyHalfRangeY();
    this.policySpeedScale = backend.getPolicySpeedScale();
    this.goalDistanceMeters = goalDistanceMeters;
    this.hasFiniteGoalDistance = hasFiniteGoalDistance;
    this.timeStepSeconds = backend.getTimeStepSeconds();
    this.steeringRateGain = backend.getSteeringRateGain();
    this.throttleAcceleration = backend.getThrottleAcceleration();
    this.brakeAcceleration = backend.getBrakeAcceleration();
    this.dragCoefficient = backend.getDragCoefficient();
    this.maxSpeed = backend.getMaxSpeed();
    this.directionGain = actionAdapter.getDirectionGain();
    this.throttleGain = actionAdapter.getThrottleGain();
    this.brakeGain = actionAdapter.getBrakeGain();
  }

  RacingEvaluationAggregate evaluate( Individual individual,
                                      RacingStartState[] startStates ) {
    RunMetrics[] metrics = new RunMetrics[ startStates.length ];
    for( int runIndex = 0;
         runIndex < startStates.length;
         runIndex++ ) {
      metrics[ runIndex ] = evaluateRun( individual,
                                         startStates[ runIndex ] );
    }

    int countOfSafeRuns = 0;
    int countOfGoalRuns = 0;
    double sumOfDistanceMeters = 0.0;
    double sumOfAverageSpeedMetersPerSecond = 0.0;
    double sumOfGoalTimeSeconds = 0.0;
    for( RunMetrics runMetrics : metrics ) {
      if( !runMetrics.crashLike ) {
        countOfSafeRuns++;
      }
      if( runMetrics.reachedGoal ) {
        countOfGoalRuns++;
        sumOfGoalTimeSeconds += runMetrics.elapsedSeconds;
      }
      sumOfDistanceMeters += capDistanceForAggregate(
        runMetrics.distanceMeters );
      sumOfAverageSpeedMetersPerSecond += runMetrics.averageSpeedMetersPerSecond;
    }

    double averageDistanceMeters = sumOfDistanceMeters / startStates.length;
    double averageSpeedMetersPerSecond =
            sumOfAverageSpeedMetersPerSecond / startStates.length;
    double averageGoalTimeSeconds = countOfGoalRuns > 0
                                    ? sumOfGoalTimeSeconds / countOfGoalRuns
                                    : currentTimeLimitSeconds;
    return new RacingEvaluationAggregate(
      countOfSafeRuns,
      countOfGoalRuns,
      averageDistanceMeters,
      averageSpeedMetersPerSecond,
      averageGoalTimeSeconds );
  }

  private RunMetrics evaluateRun( Individual individual,
                                  RacingStartState startState ) {
    PolicyAction startAction = interpolatePolicy(
      individual,
      startState.getX(),
      startState.getY(),
      startState.getSpeed(),
      startState.getHeading() );
    double x = startState.getX();
    double y = startState.getY();
    double speed = clamp( startAction.speedTarget,
                          0.0,
                          policySpeedScale );
    double heading = wrapToPi( startState.getHeading() );
    double timeSeconds = 0.0;
    double lastProgressMetersOnLap = projectProgressMeters( x,
                                                            y );
    double progressMeters = 0.0;
    boolean offTrack = false;

    while( timeSeconds < currentTimeLimitSeconds ) {
      PolicyAction policyAction = interpolatePolicy( individual,
                                                     x,
                                                     y,
                                                     speed,
                                                     heading );
      double steeringCommand = clamp(
        directionGain * wrapToPi( policyAction.directionTarget - heading ),
        -1.0,
        1.0 );
      double speedError = policyAction.speedTarget - speed;
      double throttleCommand = speedError > 0.0
                               ? clamp( throttleGain * speedError,
                                        0.0,
                                        1.0 )
                               : 0.0;
      double brakeCommand = speedError < 0.0
                            ? clamp( brakeGain * -speedError,
                                     0.0,
                                     1.0 )
                            : 0.0;
      double acceleration = throttleAcceleration * throttleCommand
                            - brakeAcceleration * brakeCommand
                            - dragCoefficient * speed;
      double nextSpeed = speed + acceleration * timeStepSeconds;
      if( nextSpeed < 0.0 ) {
        nextSpeed = 0.0;
      }
      if( nextSpeed > maxSpeed ) {
        nextSpeed = maxSpeed;
      }
      double averageSpeed = 0.5 * ( speed + nextSpeed );
      double nextHeading = wrapToPi( policyAction.directionTarget );
      double nextX = x
                     + averageSpeed * Math.cos( nextHeading )
                       * timeStepSeconds;
      double nextY = y
                     + averageSpeed * Math.sin( nextHeading )
                       * timeStepSeconds;
      double nextTimeSeconds = timeSeconds + timeStepSeconds;
      double nextProgressMetersOnLap = projectProgressMeters( nextX,
                                                              nextY );
      progressMeters += wrapProgressDelta(
        nextProgressMetersOnLap - lastProgressMetersOnLap,
        track.getLapLength() );
      lastProgressMetersOnLap = nextProgressMetersOnLap;
      offTrack = !isInsideTrack( nextX,
                                 nextY );
      x = nextX;
      y = nextY;
      speed = nextSpeed;
      heading = nextHeading;
      timeSeconds = nextTimeSeconds;

      if( offTrack ) {
        break;
      }
      if( isZeroSpeed( speed ) ) {
        break;
      }
      if( progressMeters >= goalDistanceMeters ) {
        break;
      }
    }

    boolean endedAtZeroSpeed = isZeroSpeed( speed );
    boolean crashLike = offTrack || endedAtZeroSpeed;
    boolean reachedGoal = progressMeters >= goalDistanceMeters
                          && !crashLike;
    double lapAverageSpeed = timeSeconds <= 0.0
                             ? 0.0
                             : ( reachedGoal
                                 ? goalDistanceMeters
                                 : progressMeters )
                               / timeSeconds;
    return new RunMetrics( progressMeters,
                           lapAverageSpeed,
                           timeSeconds,
                           crashLike,
                           reachedGoal );
  }

  private PolicyAction interpolatePolicy( Individual individual,
                                          double carX,
                                          double carY,
                                          double carSpeed,
                                          double carHeading ) {
    double weightedSpeedTarget = 0.0;
    double weightedDirectionX = 0.0;
    double weightedDirectionY = 0.0;
    double sumOfWeights = 0.0;

    for( int anchorIndex = 0;
         anchorIndex < countOfAnchors;
         anchorIndex++ ) {
      int offset = anchorIndex * COUNT_OF_FIELDS_PER_ANCHOR;
      double anchorX = policyCenterX
                       + individual.getGene( offset + INDEX_OF_X )
                         .getValue() * policyHalfRangeX;
      double anchorY = policyCenterY
                       + individual.getGene( offset + INDEX_OF_Y )
                         .getValue() * policyHalfRangeY;
      double anchorSpeedTarget = individual.getGene(
        offset + INDEX_OF_SPEED_TARGET ).getValue() * policySpeedScale;
      double anchorDirectionTarget = wrapToPi(
        individual.getGene( offset + INDEX_OF_DIRECTION_TARGET ).getValue()
        * Math.PI );
      double deltaX = carX - anchorX;
      double deltaY = carY - anchorY;
      double distance = Math.sqrt( weightOfPosition
                                   * ( deltaX * deltaX + deltaY * deltaY ) );
      double weight = 1.0 / Math.pow( distance + EPSILON,
                                      inverseDistancePower );
      weightedSpeedTarget += weight * anchorSpeedTarget;
      weightedDirectionX += weight * Math.cos( anchorDirectionTarget );
      weightedDirectionY += weight * Math.sin( anchorDirectionTarget );
      sumOfWeights += weight;
    }
    if( sumOfWeights <= 0.0 ) {
      throw new IllegalStateException( "sumOfWeights must be positive" );
    }
    return new PolicyAction( weightedSpeedTarget / sumOfWeights,
                             Math.atan2( weightedDirectionY,
                                         weightedDirectionX ) );
  }

  private double capDistanceForAggregate( double progressMeters ) {
    double nonNegativeProgressMeters = Math.max( 0.0,
                                                progressMeters );
    if( !hasFiniteGoalDistance ) {
      return nonNegativeProgressMeters;
    }
    return Math.min( nonNegativeProgressMeters,
                     goalDistanceMeters );
  }

  private boolean isInsideTrack( double x,
                                 double y ) {
    return computeProjectionDistanceMeters( x,
                                            y ) <= track.getHalfWidth();
  }

  private double projectProgressMeters( double x,
                                        double y ) {
    Projection bestProjection = computeProjection( x,
                                                   y );
    return bestProjection.progressMeters;
  }

  private double computeProjectionDistanceMeters( double x,
                                                  double y ) {
    return computeProjection( x,
                              y ).distanceMeters;
  }

  private Projection computeProjection( double x,
                                        double y ) {
    double localX = x - track.getCenterX();
    double localY = y - track.getCenterY();
    Projection bestProjection = projectionOnTopStraight( localX,
                                                         localY );
    bestProjection = pickClosest( bestProjection,
                                  projectionOnRightTurn( localX,
                                                         localY ) );
    bestProjection = pickClosest( bestProjection,
                                  projectionOnBottomStraight( localX,
                                                              localY ) );
    return pickClosest( bestProjection,
                        projectionOnLeftTurn( localX,
                                              localY ) );
  }

  private Projection projectionOnTopStraight( double localX,
                                              double localY ) {
    double projectedX = clamp( localX,
                               -track.getHalfLengthOfStraight(),
                               track.getHalfLengthOfStraight() );
    double projectedY = track.getRadiusOfTurns();
    double progressMeters = projectedX + track.getHalfLengthOfStraight();
    return new Projection( progressMeters,
                           distanceMeters( localX,
                                           localY,
                                           projectedX,
                                           projectedY ) );
  }

  private Projection projectionOnRightTurn( double localX,
                                            double localY ) {
    double angle = clamp( Math.atan2( localY,
                                      localX
                                      - track.getHalfLengthOfStraight() ),
                          -HALF_PI,
                          HALF_PI );
    double projectedX = track.getHalfLengthOfStraight()
                        + track.getRadiusOfTurns() * Math.cos( angle );
    double projectedY = track.getRadiusOfTurns() * Math.sin( angle );
    double progressMeters = 2.0 * track.getHalfLengthOfStraight()
                            + ( HALF_PI - angle )
                              * track.getRadiusOfTurns();
    return new Projection( progressMeters,
                           distanceMeters( localX,
                                           localY,
                                           projectedX,
                                           projectedY ) );
  }

  private Projection projectionOnBottomStraight( double localX,
                                                 double localY ) {
    double projectedX = clamp( localX,
                               -track.getHalfLengthOfStraight(),
                               track.getHalfLengthOfStraight() );
    double projectedY = -track.getRadiusOfTurns();
    double progressMeters = 2.0 * track.getHalfLengthOfStraight()
                            + Math.PI * track.getRadiusOfTurns()
                            + ( track.getHalfLengthOfStraight()
                                - projectedX );
    return new Projection( progressMeters,
                           distanceMeters( localX,
                                           localY,
                                           projectedX,
                                           projectedY ) );
  }

  private Projection projectionOnLeftTurn( double localX,
                                           double localY ) {
    double angle = clamp( Math.atan2( localY,
                                      -( localX
                                         + track.getHalfLengthOfStraight() ) ),
                          -HALF_PI,
                          HALF_PI );
    double projectedX = -track.getHalfLengthOfStraight()
                        - track.getRadiusOfTurns() * Math.cos( angle );
    double projectedY = track.getRadiusOfTurns() * Math.sin( angle );
    double progressMeters = 4.0 * track.getHalfLengthOfStraight()
                            + Math.PI * track.getRadiusOfTurns()
                            + ( angle + HALF_PI )
                              * track.getRadiusOfTurns();
    return new Projection( progressMeters,
                           distanceMeters( localX,
                                           localY,
                                           projectedX,
                                           projectedY ) );
  }

  private static Projection pickClosest( Projection currentBest,
                                         Projection candidate ) {
    return candidate.distanceMeters < currentBest.distanceMeters
           ? candidate
           : currentBest;
  }

  private static double distanceMeters( double x,
                                        double y,
                                        double projectedX,
                                        double projectedY ) {
    double deltaX = x - projectedX;
    double deltaY = y - projectedY;
    return Math.sqrt( deltaX * deltaX + deltaY * deltaY );
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

  private static boolean isZeroSpeed( double speedMetersPerSecond ) {
    return Math.abs( speedMetersPerSecond ) <= EPSILON;
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

  private static final class RunMetrics {

    private final double distanceMeters;
    private final double averageSpeedMetersPerSecond;
    private final double elapsedSeconds;
    private final boolean crashLike;
    private final boolean reachedGoal;

    private RunMetrics( double distanceMeters,
                        double averageSpeedMetersPerSecond,
                        double elapsedSeconds,
                        boolean crashLike,
                        boolean reachedGoal ) {
      this.distanceMeters = distanceMeters;
      this.averageSpeedMetersPerSecond = averageSpeedMetersPerSecond;
      this.elapsedSeconds = elapsedSeconds;
      this.crashLike = crashLike;
      this.reachedGoal = reachedGoal;
    }
  }

  private static final class PolicyAction {

    private final double speedTarget;
    private final double directionTarget;

    private PolicyAction( double speedTarget,
                          double directionTarget ) {
      this.speedTarget = speedTarget;
      this.directionTarget = directionTarget;
    }
  }

  private static final class Projection {

    private final double progressMeters;
    private final double distanceMeters;

    private Projection( double progressMeters,
                        double distanceMeters ) {
      this.progressMeters = progressMeters;
      this.distanceMeters = distanceMeters;
    }
  }
}
