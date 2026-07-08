// C:/Users/usuario/ownCloud2/RankGA/src/Problems/ProblemRacing.java
// RankGA skeleton for racing policy optimization on a simulator-neutral backend.
package Problems;

import java.util.Locale;
import java.util.Random;
import java.util.stream.IntStream;
import rankga.AdaptiveProblem;
import rankga.BatchEvaluatableProblem;
import rankga.Gene;
import rankga.Individual;
import rankga.Problem;

public class ProblemRacing
  implements Problem,
             AdaptiveProblem,
             BatchEvaluatableProblem {

  private static final int COUNT_OF_FIELDS_PER_ANCHOR = 4;
  private static final int INDEX_OF_X = 0;
  private static final int INDEX_OF_Y = 1;
  private static final int INDEX_OF_SPEED_TARGET = 2;
  private static final int INDEX_OF_DIRECTION_TARGET = 3;
  private static final double DEFAULT_WEIGHT_OF_POSITION = 1.0;
  private static final double DEFAULT_GOAL_SPEED_REWARD_WEIGHT = 0.0;
  private static final double DEFAULT_INVERSE_DISTANCE_POWER = 2.0;
  private static final double DEFAULT_LOCAL_SEARCH_INTENSITY = 0.01;
  private static final double DEFAULT_GLOBAL_SEARCH_INTENSITY = 0.1;
  private static final double DEFAULT_GOAL_FITNESS = Double.MAX_VALUE;
  private static final double WEIGHT_OF_AVERAGE_DISTANCE = 0.5;
  private static final double WEIGHT_OF_GOAL_COUNT = 1.0e-6;
  private static final double WEIGHT_OF_GOAL_TIME = 1.0e-9;
  private static final double EPSILON = 1.0e-9;
  private static final double INITIAL_BORDER_OFFSET_RATIO = 0.95;
  private static final double INITIAL_INWARD_DIRECTION_BIAS = 0.35;
  private static final double INITIAL_TARGET_SPEED_METERS_PER_SECOND = 3.0;
  private static final double INITIAL_TARGET_SPEED_NOISE = 0.5;
  private static final double INITIAL_DIRECTION_NOISE_RADIANS = 0.08;
  private static final double INITIAL_PROGRESS_NOISE_RATIO = 0.25;
  private static final double INITIAL_LATERAL_NOISE_RATIO = 0.025;
  private static final String PURE_SIMPLE_OVAL_EVALUATION_PROPERTY =
          "rankga.racing.simpleOvalPure";
  private static final String POPULATION_PARALLEL_EVALUATION_PROPERTY =
          "rankga.racing.populationParallel";

  private enum FitnessMode {
    ROBUST_GOAL_ORDER,
    DISTANCE_ONLY,
    TARGET_DISTANCE_SPEED
  }

  private final RacingBackend backend;
  private final RacingActionAdapter actionAdapter;
  private final RacingStartStateProvider startStateProvider;
  private final int countOfAnchors;
  private final int countOfRuns;
  private final double growthFactorOfTimeLimit;
  private final int countOfPatienceGenerations;
  private final double weightOfPosition;
  private final double goalSpeedRewardWeight;
  private final double inverseDistancePower;
  private final double localSearchIntensity;
  private final double globalSearchIntensity;
  private final double policyCenterX;
  private final double policyCenterY;
  private final double policyHalfRangeX;
  private final double policyHalfRangeY;
  private final double policySpeedScale;
  private final double goalDistanceMeters;
  private final double rankGoalFitness;
  private final boolean canUseSimpleOvalPureEvaluator;
  private double currentTimeLimitSeconds;
  private double bestFitnessSeen;
  private boolean bestFitnessSeenWentOffTrack;
  private boolean bestFitnessSeenReachedGoal;
  private double bestEvaluatedFitnessSeen;
  private boolean bestEvaluatedFitnessSeenWentOffTrack;
  private boolean bestEvaluatedFitnessSeenReachedGoal;
  private int countOfStagnantGenerations;
  private FitnessMode fitnessMode;

  public ProblemRacing( RacingBackend backend,
                        RacingActionAdapter actionAdapter,
                        RacingStartStateProvider startStateProvider,
                        int countOfAnchors,
                        int countOfRuns,
                        double initialTimeLimitSeconds,
                        double growthFactorOfTimeLimit,
                        int countOfPatienceGenerations ) {
    this( backend,
          actionAdapter,
          startStateProvider,
          countOfAnchors,
          countOfRuns,
          initialTimeLimitSeconds,
          growthFactorOfTimeLimit,
          countOfPatienceGenerations,
          DEFAULT_WEIGHT_OF_POSITION,
          DEFAULT_GOAL_SPEED_REWARD_WEIGHT,
          DEFAULT_INVERSE_DISTANCE_POWER,
          DEFAULT_LOCAL_SEARCH_INTENSITY,
          DEFAULT_GLOBAL_SEARCH_INTENSITY );
  }

  public ProblemRacing( RacingBackend backend,
                        RacingActionAdapter actionAdapter,
                        RacingStartStateProvider startStateProvider,
                        int countOfAnchors,
                        int countOfRuns,
                        double initialTimeLimitSeconds,
                        double growthFactorOfTimeLimit,
                        int countOfPatienceGenerations,
                        double weightOfPosition,
                        double goalSpeedRewardWeight,
                        double inverseDistancePower,
                        double localSearchIntensity,
                        double globalSearchIntensity ) {
    this( backend,
          actionAdapter,
          startStateProvider,
          countOfAnchors,
          countOfRuns,
          initialTimeLimitSeconds,
          growthFactorOfTimeLimit,
          countOfPatienceGenerations,
          weightOfPosition,
          goalSpeedRewardWeight,
          inverseDistancePower,
          localSearchIntensity,
          globalSearchIntensity,
          DEFAULT_GOAL_FITNESS );
  }

  public ProblemRacing( RacingBackend backend,
                        RacingActionAdapter actionAdapter,
                        RacingStartStateProvider startStateProvider,
                        int countOfAnchors,
                        int countOfRuns,
                        double initialTimeLimitSeconds,
                        double growthFactorOfTimeLimit,
                        int countOfPatienceGenerations,
                        double weightOfPosition,
                        double goalSpeedRewardWeight,
                        double inverseDistancePower,
                        double localSearchIntensity,
                        double globalSearchIntensity,
                        double goalDistanceMeters ) {
    this( backend,
          actionAdapter,
          startStateProvider,
          countOfAnchors,
          countOfRuns,
          initialTimeLimitSeconds,
          growthFactorOfTimeLimit,
          countOfPatienceGenerations,
          weightOfPosition,
          goalSpeedRewardWeight,
          inverseDistancePower,
          localSearchIntensity,
          globalSearchIntensity,
          goalDistanceMeters,
          goalDistanceMeters );
  }

  public ProblemRacing( RacingBackend backend,
                        RacingActionAdapter actionAdapter,
                        RacingStartStateProvider startStateProvider,
                        int countOfAnchors,
                        int countOfRuns,
                        double initialTimeLimitSeconds,
                        double growthFactorOfTimeLimit,
                        int countOfPatienceGenerations,
                        double weightOfPosition,
                        double goalSpeedRewardWeight,
                        double inverseDistancePower,
                        double localSearchIntensity,
                        double globalSearchIntensity,
                        double goalDistanceMeters,
                        double rankGoalFitness ) {
    validateDependency( "backend",
                        backend );
    validateDependency( "actionAdapter",
                        actionAdapter );
    validateDependency( "startStateProvider",
                        startStateProvider );
    validatePositiveInt( "countOfAnchors",
                         countOfAnchors );
    validatePositiveInt( "countOfRuns",
                         countOfRuns );
    validatePositiveDouble( "initialTimeLimitSeconds",
                            initialTimeLimitSeconds );
    if( growthFactorOfTimeLimit <= 1.0 ) {
      throw new IllegalArgumentException( "growthFactorOfTimeLimit must be greater than 1.0" );
    }
    validatePositiveInt( "countOfPatienceGenerations",
                         countOfPatienceGenerations );
    validatePositiveDouble( "weightOfPosition",
                            weightOfPosition );
    validateNonNegativeDouble( "goalSpeedRewardWeight",
                               goalSpeedRewardWeight );
    validatePositiveDouble( "inverseDistancePower",
                            inverseDistancePower );
    validatePositiveDouble( "localSearchIntensity",
                            localSearchIntensity );
    validatePositiveDouble( "globalSearchIntensity",
                            globalSearchIntensity );
    validateFiniteDouble( "policyCenterX",
                          backend.getPolicyCenterX() );
    validateFiniteDouble( "policyCenterY",
                          backend.getPolicyCenterY() );
    validatePositiveDouble( "policyHalfRangeX",
                            backend.getPolicyHalfRangeX() );
    validatePositiveDouble( "policyHalfRangeY",
                            backend.getPolicyHalfRangeY() );
    validatePositiveDouble( "policySpeedScale",
                            backend.getPolicySpeedScale() );
    validatePositiveDouble( "goalDistanceMeters",
                            goalDistanceMeters );
    validatePositiveDouble( "rankGoalFitness",
                            rankGoalFitness );
    if( globalSearchIntensity <= localSearchIntensity ) {
      throw new IllegalArgumentException(
        "globalSearchIntensity must be greater than localSearchIntensity" );
    }
    if( startStateProvider.countOfStates() != countOfRuns ) {
      throw new IllegalArgumentException(
        "startStateProvider.countOfStates must match countOfRuns" );
    }
    this.backend = backend;
    this.actionAdapter = actionAdapter;
    this.startStateProvider = startStateProvider;
    this.countOfAnchors = countOfAnchors;
    this.countOfRuns = countOfRuns;
    this.currentTimeLimitSeconds = initialTimeLimitSeconds;
    this.growthFactorOfTimeLimit = growthFactorOfTimeLimit;
    this.countOfPatienceGenerations = countOfPatienceGenerations;
    this.weightOfPosition = weightOfPosition;
    this.goalSpeedRewardWeight = goalSpeedRewardWeight;
    this.inverseDistancePower = inverseDistancePower;
    this.localSearchIntensity = localSearchIntensity;
    this.globalSearchIntensity = globalSearchIntensity;
    this.policyCenterX = backend.getPolicyCenterX();
    this.policyCenterY = backend.getPolicyCenterY();
    this.policyHalfRangeX = backend.getPolicyHalfRangeX();
    this.policyHalfRangeY = backend.getPolicyHalfRangeY();
    this.policySpeedScale = backend.getPolicySpeedScale();
    this.goalDistanceMeters = goalDistanceMeters;
    this.rankGoalFitness = rankGoalFitness;
    this.canUseSimpleOvalPureEvaluator =
    !"false".equalsIgnoreCase(
      System.getProperty( PURE_SIMPLE_OVAL_EVALUATION_PROPERTY,
                          "true" ) )
    &&
    backend instanceof SimpleOvalRacingBackend
    && actionAdapter instanceof SimpleTargetRacingActionAdapter;
    this.bestFitnessSeen = Double.NEGATIVE_INFINITY;
    this.bestFitnessSeenWentOffTrack = true;
    this.bestFitnessSeenReachedGoal = false;
    this.bestEvaluatedFitnessSeen = Double.NEGATIVE_INFINITY;
    this.bestEvaluatedFitnessSeenWentOffTrack = true;
    this.bestEvaluatedFitnessSeenReachedGoal = false;
    this.countOfStagnantGenerations = 0;
    this.fitnessMode = FitnessMode.ROBUST_GOAL_ORDER;
    this.startStateProvider.refreshBatch();
  }

  public void useDistanceOnlyFitness() {
    this.fitnessMode = FitnessMode.DISTANCE_ONLY;
  }

  public void useTargetDistanceSpeedFitness() {
    this.fitnessMode = FitnessMode.TARGET_DISTANCE_SPEED;
  }

  @Override
  public void adapt( double bestFitness ) {
    validateFiniteDouble( "bestFitness",
                          bestFitness );
    adaptFromEvaluation( new RacingEvaluationSummary(
      bestFitness,
      bestEvaluatedFitnessSeenWentOffTrack,
      bestEvaluatedFitnessSeenReachedGoal ) );
    startStateProvider.refreshBatch();
  }

  @Override
  public void adapt( Individual bestIndividual ) {
    validateDependency( "bestIndividual",
                        bestIndividual );
    adapt( bestIndividual.getFitness() );
    startStateProvider.refreshBatch();
  }

  @Override
  public double fitness( Individual individual ) {
    validateDependency( "individual",
                        individual );
    if( canUseSimpleOvalPureEvaluator ) {
      return simpleOvalFitness( individual );
    }
    return backendFitness( individual );
  }

  @Override
  public void evaluateFitnessBatch( Individual[] individuals ) {
    validateDependency( "individuals",
                        individuals );
    if( !canUseSimpleOvalPureEvaluator
        || !Boolean.getBoolean( POPULATION_PARALLEL_EVALUATION_PROPERTY ) ) {
      for( Individual individual : individuals ) {
        individual.updateFitness();
      }
      return;
    }
    IntStream.range( 0,
                     individuals.length )
      .parallel()
                 .forEach( index -> {
        Individual individual = individuals[ index ];
        individual.setExtraString( new StringBuilder( "" ) );
        individual.setFitness( simpleOvalFitness( individual ) );
      } );
  }

  private double simpleOvalFitness( Individual individual ) {
    RacingStartState[] startStates = new RacingStartState[ countOfRuns ];
    for( int runIndex = 0;
         runIndex < countOfRuns;
         runIndex++ ) {
      startStates[ runIndex ] = startStateProvider.getStartState( runIndex );
    }
    SimpleOvalRacingBatchEvaluator evaluator =
            new SimpleOvalRacingBatchEvaluator(
              (SimpleOvalRacingBackend) backend,
              (SimpleTargetRacingActionAdapter) actionAdapter,
              countOfAnchors,
              currentTimeLimitSeconds,
              weightOfPosition,
              inverseDistancePower,
              goalDistanceMeters,
              hasFiniteGoalDistance() );
    RacingEvaluationAggregate aggregate = evaluator.evaluate(
      individual,
      startStates );
    return completeFitnessEvaluation( individual,
                                      aggregate );
  }

  private double backendFitness( Individual individual ) {
    int countOfSafeRuns = 0;
    int countOfGoalRuns = 0;
    double sumOfDistanceMeters = 0.0;
    double sumOfAverageSpeedMetersPerSecond = 0.0;
    double sumOfGoalTimeSeconds = 0.0;
    for( int runIndex = 0;
         runIndex < countOfRuns;
         runIndex++ ) {
      RunEvaluation runEvaluation = evaluateRun( individual,
                                                 startStateProvider.getStartState( runIndex ) );
      if( !runEvaluation.isCrashLike() ) {
        countOfSafeRuns++;
      }
      if( runEvaluation.hasReachedGoal() ) {
        countOfGoalRuns++;
        sumOfGoalTimeSeconds += runEvaluation.getElapsedSeconds();
      }
      sumOfDistanceMeters += capDistanceForAggregate(
        runEvaluation.getDistanceMeters() );
      sumOfAverageSpeedMetersPerSecond +=
        runEvaluation.getAverageSpeedMetersPerSecond();
    }
    double averageDistanceMeters = sumOfDistanceMeters / countOfRuns;
    double averageSpeedMetersPerSecond =
            sumOfAverageSpeedMetersPerSecond / countOfRuns;
    double averageGoalTimeSeconds = countOfGoalRuns > 0
                                    ? sumOfGoalTimeSeconds / countOfGoalRuns
                                    : currentTimeLimitSeconds;
    return completeFitnessEvaluation(
      individual,
      new RacingEvaluationAggregate(
        countOfSafeRuns,
        countOfGoalRuns,
        averageDistanceMeters,
        averageSpeedMetersPerSecond,
        averageGoalTimeSeconds ) );
  }

  private double completeFitnessEvaluation(
    Individual individual,
    RacingEvaluationAggregate aggregate ) {
    double aggregateFitness = computeAggregateFitness(
      aggregate.getCountOfSafeRuns(),
      aggregate.getAverageDistanceMeters(),
      aggregate.getCountOfGoalRuns(),
      aggregate.getAverageGoalTimeSeconds(),
      aggregate.getAverageSpeedMetersPerSecond() );
    boolean anyRunWentOffTrack = aggregate.getCountOfSafeRuns() < countOfRuns;
    boolean allRunsReachedGoal = aggregate.getCountOfGoalRuns() == countOfRuns;
    individual.appendExtraString(
      String.format( Locale.US,
                     "distance=%.6f offTrack=%s goalReached=%s avgSpeed=%.6f T=%.6f M=%d fitness=%.12f safeRuns=%d avgDistance=%.6f goalRuns=%d avgGoalTime=%.6f",
                     aggregate.getAverageDistanceMeters(),
                     Boolean.toString( anyRunWentOffTrack ),
                     Boolean.toString( allRunsReachedGoal ),
                     aggregate.getAverageSpeedMetersPerSecond(),
                     currentTimeLimitSeconds,
                     countOfRuns,
                     aggregateFitness,
                     aggregate.getCountOfSafeRuns(),
                     aggregate.getAverageDistanceMeters(),
                     aggregate.getCountOfGoalRuns(),
                     aggregate.getAverageGoalTimeSeconds() ) );
    RacingEvaluationSummary summary = new RacingEvaluationSummary(
      aggregateFitness,
      anyRunWentOffTrack,
      allRunsReachedGoal );
    registerBestEvaluatedRun( summary );
    return aggregateFitness;
  }

  @Override
  public double getGlobalSearchIntensity() {
    return globalSearchIntensity;
  }

  @Override
  public double getLocalSearchIntensity() {
    return localSearchIntensity;
  }

  @Override
  public String getProblemName() {
    return "Racing_" + backend.getTrackName() + "_" + countOfAnchors;
  }

  @Override
  public int getGenomeLength() {
    return countOfAnchors * COUNT_OF_FIELDS_PER_ANCHOR;
  }

  @Override
  public Gene getNewGene( boolean randomize,
                          Random random ) {
    validateDependency( "random",
                        random );
    return new GeneDoublePrecision( random,
                                    randomize
                                    ? globalSearchIntensity
                                    : 0.0 );
  }

  @Override
  public Gene getNewGene( Gene gene ) {
    return new GeneDoublePrecision( (GeneDoublePrecision) gene );
  }

  @Override
  public double getGoalFt() {
    return rankGoalFitness;
  }

  @Override
  public int getDisplayModulus() {
    return 1;
  }

  @Override
  public Individual getNewIndividual( boolean randomize,
                                      Random random ) {
    validateDependency( "random",
                        random );
    if( randomize && backend instanceof SimpleOvalRacingBackend ovalBackend ) {
      Individual individual = new Individual( this,
                                              false,
                                              random );
      initializeOvalBorderAnchors( individual,
                                   ovalBackend.getTrack(),
                                   random );
      return individual;
    }
    if( randomize && backend instanceof SimpleOvalRarsRacingBackend rarsBackend ) {
      Individual individual = new Individual( this,
                                              false,
                                              random );
      initializeOvalBorderAnchors( individual,
                                   rarsBackend.getTrack(),
                                   random );
      return individual;
    }
    return new Individual( this,
                           randomize,
                           random );
  }

  @Override
  public Individual getNewIndividual( Individual another ) {
    return new Individual( another );
  }

  public double getCurrentTimeLimitSeconds() {
    return currentTimeLimitSeconds;
  }

  public boolean hasBestFitnessSeenGoneOffTrack() {
    return bestFitnessSeenWentOffTrack;
  }

  public boolean hasBestFitnessSeenReachedGoal() {
    return bestFitnessSeenReachedGoal;
  }

  public int getCountOfStagnantGenerations() {
    return countOfStagnantGenerations;
  }

  public static double computeAllRunsGoalFitnessThreshold( int countOfRuns ) {
    validatePositiveInt( "countOfRuns",
                         countOfRuns );
    return countOfRuns
           + WEIGHT_OF_AVERAGE_DISTANCE
           + WEIGHT_OF_GOAL_COUNT;
  }

  private RunEvaluation evaluateRun( Individual individual,
                                     RacingStartState startState ) {
    RacingStartState policyStartState = buildPolicyStartState( individual,
                                                               startState );
    RacingCarState currentState = backend.resetEpisode( policyStartState );
    if( currentState == null ) {
      throw new IllegalStateException( "backend.resetEpisode must return a car state" );
    }
    double progressMeters = backend.measureProgress( currentState );
    RacingTerminationReason terminationReason = RacingTerminationReason.NONE;

    while( currentState.getTimeSeconds() < currentTimeLimitSeconds ) {
      RacingPolicyAction policyAction = interpolatePolicy( individual,
                                                           currentState );
      RacingBackendAction backendAction = actionAdapter.toBackendAction(
        policyAction,
        currentState );
      RacingStepResult stepResult = backend.step( backendAction );
      if( stepResult == null ) {
        throw new IllegalStateException( "backend.step must return a step result" );
      }
      currentState = stepResult.getCarState();
      progressMeters = stepResult.getProgressMeters();
      terminationReason = stepResult.getTerminationReason();
      if( stepResult.isTerminal() ) {
        break;
      }
      if( isZeroSpeed( currentState.getSpeed() ) ) {
        break;
      }
      if( progressMeters >= goalDistanceMeters ) {
        break;
      }
    }

    boolean endedAtZeroSpeed = isZeroSpeed( currentState.getSpeed() );
    boolean offTrack = terminationReason == RacingTerminationReason.OFF_TRACK
                       || endedAtZeroSpeed;
    boolean crashLike = terminationReason.isCrashLike()
                        || endedAtZeroSpeed;
    boolean reachedGoal = progressMeters >= goalDistanceMeters
                          && !crashLike;
    double lapAverageSpeed = computeLapAverageSpeed( progressMeters,
                                                     currentState.getTimeSeconds(),
                                                     reachedGoal );
    return new RunEvaluation( progressMeters,
                              lapAverageSpeed,
                              currentState.getTimeSeconds(),
                              offTrack,
                              crashLike,
                              reachedGoal );
  }

  private boolean isZeroSpeed( double speedMetersPerSecond ) {
    return Math.abs( speedMetersPerSecond ) <= EPSILON;
  }

  private RacingStartState buildPolicyStartState( Individual individual,
                                                  RacingStartState startState ) {
    RacingCarState queryState = new RacingCarState( startState.getX(),
                                                    startState.getY(),
                                                    startState.getSpeed(),
                                                    startState.getHeading(),
                                                    0.0 );
    RacingPolicyAction policyAction = interpolatePolicy( individual,
                                                         queryState );
    double startSpeedMetersPerSecond = clamp( policyAction.getSpeedTarget(),
                                              0.0,
                                              policySpeedScale );
    return new RacingStartState( startState.getX(),
                                 startState.getY(),
                                 startSpeedMetersPerSecond,
                                 startState.getHeading() );
  }

  private void initializeOvalBorderAnchors( Individual individual,
                                            SimpleOvalTrack track,
                                            Random random ) {
    double countOfProgressSlots = Math.max( 1.0,
                                            Math.ceil( countOfAnchors / 2.0 ) );
    double spacingMeters = track.getLapLength() / countOfProgressSlots;
    for( int anchorIndex = 0;
         anchorIndex < countOfAnchors;
         anchorIndex++ ) {
      int indexOfProgressSlot = anchorIndex / 2;
      double sideSign = anchorIndex % 2 == 0
                        ? 1.0
                        : -1.0;
      double progressMeters = ( indexOfProgressSlot + 0.5 ) * spacingMeters
                              + random.nextGaussian()
                                * INITIAL_PROGRESS_NOISE_RATIO
                                * spacingMeters;
      SimpleOvalTrack.CenterlinePose pose = track.computeCenterlinePose(
        progressMeters );
      double lateralOffsetMeters = sideSign
                                   * INITIAL_BORDER_OFFSET_RATIO
                                   * track.getHalfWidth()
                                   + random.nextGaussian()
                                     * INITIAL_LATERAL_NOISE_RATIO
                                     * track.getHalfWidth();
      lateralOffsetMeters = clamp( lateralOffsetMeters,
                                   -track.getHalfWidth() * 0.99,
                                   track.getHalfWidth() * 0.99 );
      double targetDirection = computeInitialTargetDirection( pose,
                                                              sideSign,
                                                              random );
      double targetSpeed = clamp( INITIAL_TARGET_SPEED_METERS_PER_SECOND
                                  + random.nextGaussian()
                                    * INITIAL_TARGET_SPEED_NOISE,
                                  1.0,
                                  5.0 );
      setDecodedAnchor( individual,
                        anchorIndex,
                        pose.getX() + lateralOffsetMeters * pose.getNormalX(),
                        pose.getY() + lateralOffsetMeters * pose.getNormalY(),
                        targetSpeed,
                        targetDirection );
    }
  }

  private double computeInitialTargetDirection( SimpleOvalTrack.CenterlinePose pose,
                                                double sideSign,
                                                Random random ) {
    double tangentX = Math.cos( pose.getHeading() );
    double tangentY = Math.sin( pose.getHeading() );
    double inwardX = -sideSign * pose.getNormalX();
    double inwardY = -sideSign * pose.getNormalY();
    return wrapToPi(
      Math.atan2( tangentY + INITIAL_INWARD_DIRECTION_BIAS * inwardY,
                  tangentX + INITIAL_INWARD_DIRECTION_BIAS * inwardX )
      + random.nextGaussian() * INITIAL_DIRECTION_NOISE_RADIANS );
  }

  private void setDecodedAnchor( Individual individual,
                                 int anchorIndex,
                                 double x,
                                 double y,
                                 double speedTarget,
                                 double directionTarget ) {
    int offset = anchorIndex * COUNT_OF_FIELDS_PER_ANCHOR;
    individual.getGene( offset + INDEX_OF_X )
      .setDoubleValue( encodeX( x ) );
    individual.getGene( offset + INDEX_OF_Y )
      .setDoubleValue( encodeY( y ) );
    individual.getGene( offset + INDEX_OF_SPEED_TARGET )
      .setDoubleValue( encodeSpeedTarget( speedTarget ) );
    individual.getGene( offset + INDEX_OF_DIRECTION_TARGET )
      .setDoubleValue( encodeDirectionTarget( directionTarget ) );
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

  private double encodeX( double x ) {
    return ( x - policyCenterX ) / policyHalfRangeX;
  }

  private double encodeY( double y ) {
    return ( y - policyCenterY ) / policyHalfRangeY;
  }

  private double encodeSpeedTarget( double speedTarget ) {
    return speedTarget / policySpeedScale;
  }

  private double encodeDirectionTarget( double directionTarget ) {
    return wrapToPi( directionTarget ) / Math.PI;
  }

  private double decodeX( double rawX ) {
    return policyCenterX + rawX * policyHalfRangeX;
  }

  private double decodeY( double rawY ) {
    return policyCenterY + rawY * policyHalfRangeY;
  }

  private double decodeSpeedTarget( double rawSpeedTarget ) {
    return rawSpeedTarget * policySpeedScale;
  }

  private double decodeDirectionTarget( double rawDirectionTarget ) {
    return wrapToPi( rawDirectionTarget * Math.PI );
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

  private double capDistanceForAggregate( double progressMeters ) {
    double nonNegativeProgressMeters = Math.max( 0.0,
                                                progressMeters );
    if( !hasFiniteGoalDistance() ) {
      return nonNegativeProgressMeters;
    }
    return Math.min( nonNegativeProgressMeters,
                     goalDistanceMeters );
  }

  private double computeAggregateFitness( int countOfSafeRuns,
                                          double averageDistanceMeters,
                                          int countOfGoalRuns,
                                          double averageGoalTimeSeconds,
                                          double averageSpeedMetersPerSecond ) {
    if( fitnessMode == FitnessMode.DISTANCE_ONLY ) {
      return averageDistanceMeters;
    }
    if( fitnessMode == FitnessMode.TARGET_DISTANCE_SPEED ) {
      return computeTargetDistanceSpeedFitness(
        averageDistanceMeters,
        countOfGoalRuns,
        averageGoalTimeSeconds,
        averageSpeedMetersPerSecond );
    }
    double normalizedAverageDistance = normalizeAverageDistance(
      averageDistanceMeters );
    double goalRatio = countOfGoalRuns / (double) countOfRuns;
    double goalTimeScore = countOfGoalRuns > 0
                           ? 1.0 / ( 1.0 + averageGoalTimeSeconds )
                           : 0.0;
    return countOfSafeRuns
           + WEIGHT_OF_AVERAGE_DISTANCE * normalizedAverageDistance
           + WEIGHT_OF_GOAL_COUNT * goalRatio
           + WEIGHT_OF_GOAL_TIME * ( 1.0 + goalSpeedRewardWeight )
             * goalTimeScore;
  }

  private double computeTargetDistanceSpeedFitness(
    double averageDistanceMeters,
    int countOfGoalRuns,
    double averageGoalTimeSeconds,
    double averageSpeedMetersPerSecond ) {
    if( !hasFiniteGoalDistance() ) {
      return averageDistanceMeters;
    }
    double goalRatio = countOfGoalRuns / (double) countOfRuns;
    double targetSpeedMetersPerSecond = countOfGoalRuns > 0
                                        ? goalDistanceMeters
                                          / averageGoalTimeSeconds
                                        : averageSpeedMetersPerSecond;
    return averageDistanceMeters
           + goalDistanceMeters * goalRatio
           + goalRatio * targetSpeedMetersPerSecond;
  }

  private double normalizeAverageDistance( double averageDistanceMeters ) {
    double nonNegativeAverageDistanceMeters = Math.max( 0.0,
                                                       averageDistanceMeters );
    if( hasFiniteGoalDistance() ) {
      return Math.min( 1.0,
                       nonNegativeAverageDistanceMeters / goalDistanceMeters );
    }
    return nonNegativeAverageDistanceMeters
           / ( 1.0 + nonNegativeAverageDistanceMeters );
  }

  private boolean hasFiniteGoalDistance() {
    return goalDistanceMeters != DEFAULT_GOAL_FITNESS;
  }

  private double computeLapAverageSpeed( double progressMeters,
                                         double elapsedSeconds,
                                         boolean reachedGoal ) {
    if( elapsedSeconds <= 0.0 ) {
      return 0.0;
    }
    double measuredDistanceMeters = reachedGoal
                                    ? goalDistanceMeters
                                    : progressMeters;
    return measuredDistanceMeters / elapsedSeconds;
  }

  private void adaptFromEvaluation( RacingEvaluationSummary summary ) {
    double bestFitness = summary.getFitness();
    validateFiniteDouble( "bestFitness",
                          bestFitness );
    if( bestFitness > bestFitnessSeen ) {
      bestFitnessSeen = bestFitness;
      bestFitnessSeenWentOffTrack = summary.hasGoneOffTrack();
      bestFitnessSeenReachedGoal = summary.hasReachedGoal();
      countOfStagnantGenerations = 0;
    } else if( summary.hasGoneOffTrack()
               || summary.hasReachedGoal() ) {
      countOfStagnantGenerations = 0;
    } else {
      countOfStagnantGenerations++;
      if( countOfStagnantGenerations >= countOfPatienceGenerations ) {
        currentTimeLimitSeconds *= growthFactorOfTimeLimit;
        countOfStagnantGenerations = 0;
      }
    }
  }

  private void registerBestEvaluatedRun( RacingEvaluationSummary summary ) {
    if( summary.getFitness() > bestEvaluatedFitnessSeen ) {
      bestEvaluatedFitnessSeen = summary.getFitness();
      bestEvaluatedFitnessSeenWentOffTrack = summary.hasGoneOffTrack();
      bestEvaluatedFitnessSeenReachedGoal = summary.hasReachedGoal();
    }
  }

  private RacingPolicyAction interpolatePolicy( Individual individual,
                                                RacingCarState carState ) {
    double weightedSpeedTarget = 0.0;
    double weightedDirectionX = 0.0;
    double weightedDirectionY = 0.0;
    double sumOfWeights = 0.0;

    for( int anchorIndex = 0;
         anchorIndex < countOfAnchors;
         anchorIndex++ ) {
      double anchorX = decodeX(
        getAnchorValue( individual,
                        anchorIndex,
                        INDEX_OF_X ) );
      double anchorY = decodeY(
        getAnchorValue( individual,
                        anchorIndex,
                        INDEX_OF_Y ) );
      double anchorSpeedTarget = decodeSpeedTarget(
        getAnchorValue( individual,
                        anchorIndex,
                        INDEX_OF_SPEED_TARGET ) );
      double anchorDirectionTarget = decodeDirectionTarget(
        getAnchorValue( individual,
                        anchorIndex,
                        INDEX_OF_DIRECTION_TARGET ) );
      double distance = computeStateDistance( carState,
                                              anchorX,
                                              anchorY );
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

    return new RacingPolicyAction( weightedSpeedTarget / sumOfWeights,
                                   Math.atan2( weightedDirectionY,
                                               weightedDirectionX ) );
  }

  private double computeStateDistance( RacingCarState carState,
                                       double anchorX,
                                       double anchorY ) {
    double deltaX = carState.getX() - anchorX;
    double deltaY = carState.getY() - anchorY;
    return Math.sqrt( weightOfPosition * ( deltaX * deltaX + deltaY * deltaY ) );
  }

  private double getAnchorValue( Individual individual,
                                 int anchorIndex,
                                 int fieldIndex ) {
    return individual.getGene( anchorIndex * COUNT_OF_FIELDS_PER_ANCHOR + fieldIndex )
      .getValue();
  }

  private static final class RunEvaluation {

    private final double distanceMeters;
    private final double averageSpeedMetersPerSecond;
    private final double elapsedSeconds;
    private final boolean offTrack;
    private final boolean crashLike;
    private final boolean reachedGoal;

    private RunEvaluation( double distanceMeters,
                           double averageSpeedMetersPerSecond,
                           double elapsedSeconds,
                           boolean offTrack,
                           boolean crashLike,
                           boolean reachedGoal ) {
      this.distanceMeters = distanceMeters;
      this.averageSpeedMetersPerSecond = averageSpeedMetersPerSecond;
      this.elapsedSeconds = elapsedSeconds;
      this.offTrack = offTrack;
      this.crashLike = crashLike;
      this.reachedGoal = reachedGoal;
    }

    public double getDistanceMeters() {
      return distanceMeters;
    }

    public double getAverageSpeedMetersPerSecond() {
      return averageSpeedMetersPerSecond;
    }

    public double getElapsedSeconds() {
      return elapsedSeconds;
    }

    public boolean isOffTrack() {
      return offTrack;
    }

    public boolean isCrashLike() {
      return crashLike;
    }

    public boolean hasReachedGoal() {
      return reachedGoal;
    }
  }

  private static final class RacingEvaluationSummary {

    private final double fitness;
    private final boolean offTrack;
    private final boolean reachedGoal;

    private RacingEvaluationSummary( double fitness,
                                     boolean offTrack,
                                     boolean reachedGoal ) {
      validateFiniteDouble( "fitness",
                            fitness );
      this.fitness = fitness;
      this.offTrack = offTrack;
      this.reachedGoal = reachedGoal;
    }

    public double getFitness() {
      return fitness;
    }

    public boolean hasGoneOffTrack() {
      return offTrack;
    }

    public boolean hasReachedGoal() {
      return reachedGoal;
    }
  }

  private static void validateDependency( String label,
                                          Object dependency ) {
    if( dependency == null ) {
      throw new IllegalArgumentException( label + " must not be null" );
    }
  }

  private static void validatePositiveInt( String label,
                                           int value ) {
    if( value <= 0 ) {
      throw new IllegalArgumentException( label + " must be positive" );
    }
  }

  private static void validatePositiveDouble( String label,
                                              double value ) {
    validateFiniteDouble( label,
                          value );
    if( value <= 0.0 ) {
      throw new IllegalArgumentException( label + " must be positive" );
    }
  }

  private static void validateNonNegativeDouble( String label,
                                                 double value ) {
    validateFiniteDouble( label,
                          value );
    if( value < 0.0 ) {
      throw new IllegalArgumentException( label + " must be non-negative" );
    }
  }

  private static void validateFiniteDouble( String label,
                                            double value ) {
    if( Double.isNaN( value ) || Double.isInfinite( value ) ) {
      throw new IllegalArgumentException( label + " must be finite" );
    }
  }
}
