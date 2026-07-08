// C:/Users/usuario/ownCloud2/RankGA/src/rankga/RunRacingOvalExperiment.java
// Standalone runner for a full RankGA log on the simple oval racing backend.
package rankga;

import Problems.ProblemRacing;
import Problems.RacingStartState;
import Problems.RacingStartStateProvider;
import Problems.SimpleOvalRacingBackend;
import Problems.SimpleOvalRarsRacingBackend;
import Problems.SimpleOvalTrack;
import Problems.SimpleTargetRacingActionAdapter;

public final class RunRacingOvalExperiment {

  private static final long DEFAULT_SEED = 1234L;
  private static final int DEFAULT_COUNT_OF_INDIVIDUALS = 24;
  private static final int DEFAULT_COUNT_OF_ANCHORS = 50;
  private static final int DEFAULT_REPETITIONS = 1;
  private static final int COUNT_OF_RACING_RUNS = 7;
  private static final long DEFAULT_PATIENCE_MILLIS = 1000L;
  private static final double DEFAULT_INITIAL_TIME_LIMIT_SECONDS = 1.0;
  private static final double DEFAULT_TIME_LIMIT_GROWTH_FACTOR = 1.01;
  private static final int DEFAULT_TIME_LIMIT_PATIENCE_GENERATIONS = 100;
  private static final double DEFAULT_GOAL_LAPS = 0.0;
  private static final double DEFAULT_GOAL_SPEED_REWARD_WEIGHT = 0.0;
  private static final boolean DEFAULT_SHOULD_STOP_AT_GOAL = true;
  private static final double DEFAULT_LOCAL_SEARCH_INTENSITY = 0.01;
  private static final double DEFAULT_GLOBAL_SEARCH_INTENSITY = 0.1;
  private static final String BACKEND_KINEMATIC = "kinematic";
  private static final String BACKEND_RARS = "rars";
  private static final String FITNESS_MODE_ROBUST = "robust";
  private static final String FITNESS_MODE_DISTANCE_ONLY = "distance-only";
  private static final String FITNESS_MODE_TARGET_SPEED = "target-speed";

  private RunRacingOvalExperiment() {
  }

  public static void main( String[] args ) {
    long seed = DEFAULT_SEED;
    int countOfIndividuals = DEFAULT_COUNT_OF_INDIVIDUALS;
    int countOfAnchors = DEFAULT_COUNT_OF_ANCHORS;
    int repetitions = DEFAULT_REPETITIONS;
    long patienceMillis = DEFAULT_PATIENCE_MILLIS;
    double initialTimeLimitSeconds = DEFAULT_INITIAL_TIME_LIMIT_SECONDS;
    double timeLimitGrowthFactor = DEFAULT_TIME_LIMIT_GROWTH_FACTOR;
    int timeLimitPatienceGenerations = DEFAULT_TIME_LIMIT_PATIENCE_GENERATIONS;
    double goalLaps = DEFAULT_GOAL_LAPS;
    double goalSpeedRewardWeight = DEFAULT_GOAL_SPEED_REWARD_WEIGHT;
    boolean shouldStopAtGoal = DEFAULT_SHOULD_STOP_AT_GOAL;
    double globalSearchIntensity = Double.NaN;
    double localSearchIntensity = DEFAULT_LOCAL_SEARCH_INTENSITY;
    String fitnessMode = FITNESS_MODE_ROBUST;
    String backendName = BACKEND_KINEMATIC;

    for( String arg : args ) {
      if( arg.startsWith( "--seed=" ) ) {
        seed = Long.parseLong( arg.substring( "--seed=".length() ) );
      } else if( arg.startsWith( "--population=" ) ) {
        countOfIndividuals = Integer.parseInt(
          arg.substring( "--population=".length() ) );
      } else if( arg.startsWith( "--anchors=" ) ) {
        countOfAnchors = Integer.parseInt( arg.substring( "--anchors=".length() ) );
      } else if( arg.startsWith( "--repetitions=" ) ) {
        repetitions = Integer.parseInt( arg.substring( "--repetitions=".length() ) );
      } else if( arg.startsWith( "--patience-ms=" ) ) {
        patienceMillis = Long.parseLong(
          arg.substring( "--patience-ms=".length() ) );
      } else if( arg.startsWith( "--initial-t=" ) ) {
        initialTimeLimitSeconds = Double.parseDouble(
          arg.substring( "--initial-t=".length() ) );
      } else if( arg.startsWith( "--time-growth-factor=" ) ) {
        timeLimitGrowthFactor = Double.parseDouble(
          arg.substring( "--time-growth-factor=".length() ) );
      } else if( arg.startsWith( "--time-patience-generations=" ) ) {
        timeLimitPatienceGenerations = Integer.parseInt(
          arg.substring( "--time-patience-generations=".length() ) );
      } else if( arg.startsWith( "--goal-laps=" ) ) {
        goalLaps = Double.parseDouble(
          arg.substring( "--goal-laps=".length() ) );
      } else if( arg.startsWith( "--goal-speed-reward=" ) ) {
        goalSpeedRewardWeight = Double.parseDouble(
          arg.substring( "--goal-speed-reward=".length() ) );
      } else if( arg.startsWith( "--stop-at-goal=" ) ) {
        shouldStopAtGoal = parseBoolean(
          arg.substring( "--stop-at-goal=".length() ) );
      } else if( arg.startsWith( "--global-search-intensity=" ) ) {
        globalSearchIntensity = Double.parseDouble(
          arg.substring( "--global-search-intensity=".length() ) );
      } else if( arg.startsWith( "--local-search-intensity=" ) ) {
        localSearchIntensity = Double.parseDouble(
          arg.substring( "--local-search-intensity=".length() ) );
      } else if( arg.startsWith( "--fitness-mode=" ) ) {
        fitnessMode = arg.substring( "--fitness-mode=".length() );
      } else if( arg.startsWith( "--backend=" ) ) {
        backendName = arg.substring( "--backend=".length() );
      } else {
        throw new IllegalArgumentException( "Unknown argument: " + arg );
      }
    }

    validateNonNegative( "goalLaps",
                         goalLaps );
    validateNonNegative( "goalSpeedRewardWeight",
                         goalSpeedRewardWeight );
    validateFitnessMode( fitnessMode );
    validateBackendName( backendName );
    SimpleOvalTrack track = buildTrack();
    if( Double.isNaN( globalSearchIntensity ) ) {
      globalSearchIntensity = computeNormalizedGlobalSearchIntensity();
    }
    validatePositive( "globalSearchIntensity",
                      globalSearchIntensity );
    validatePositive( "localSearchIntensity",
                      localSearchIntensity );
    if( globalSearchIntensity <= localSearchIntensity ) {
      throw new IllegalArgumentException(
        "globalSearchIntensity must be greater than localSearchIntensity" );
    }
    double goalDistanceMeters = computeGoalDistanceMeters( track,
                                                           goalLaps );
    double rankGoalFitness = shouldStopAtGoal
                             ? ProblemRacing.computeAllRunsGoalFitnessThreshold(
                               COUNT_OF_RACING_RUNS )
                             : Double.MAX_VALUE;
    ProblemRacing problem = buildProblem( track,
                                          countOfAnchors,
                                          initialTimeLimitSeconds,
                                          timeLimitGrowthFactor,
                                          timeLimitPatienceGenerations,
                                          goalDistanceMeters,
                                          rankGoalFitness,
                                          goalSpeedRewardWeight,
                                          globalSearchIntensity,
                                          localSearchIntensity,
                                          fitnessMode,
                                          backendName );
    RankGA.run( problem,
                countOfIndividuals,
                repetitions,
                seed,
                "mode=oval-full-output;anchors=" + countOfAnchors
                + ";genotype=normalized-policy"
                + ";initialT=" + initialTimeLimitSeconds
                + ";timeGrowthFactor=" + timeLimitGrowthFactor
                + ";timePatienceGenerations=" + timeLimitPatienceGenerations
                + ";goalLaps=" + goalLaps
                + ";goalSpeedReward=" + goalSpeedRewardWeight
                + ";stopAtGoal=" + shouldStopAtGoal
                + ";lapLength=" + track.getLapLength()
                + ";goalDistanceMeters=" + goalDistanceMeters
                + ";rankGoalFitness=" + rankGoalFitness
                + ";globalSearchIntensity=" + globalSearchIntensity
                + ";localSearchIntensity=" + localSearchIntensity
                + ";fitnessMode=" + fitnessMode
                + ";backend=" + backendName
                + ";racingEvaluator=" + describeRacingEvaluator(
                  backendName )
                + ";racingPopulationParallel="
                + Boolean.getBoolean( "rankga.racing.populationParallel" ),
                patienceMillis,
                RankGA.IncumbentUpdatePolicy.STRICT,
                RankGA.PatienceResetPolicy.FITNESS );
  }

  private static String describeRacingEvaluator( String backendName ) {
    if( !BACKEND_KINEMATIC.equals( backendName ) ) {
      return "mutable";
    }
    if( "false".equalsIgnoreCase( System.getProperty(
                                  "rankga.racing.simpleOvalPure",
                                  "true" ) ) ) {
      return "mutable";
    }
    if( Boolean.getBoolean( "rankga.racing.populationParallel" ) ) {
      return "cpu_population_parallel";
    }
    return "cpu_serial";
  }

  private static ProblemRacing buildProblem( SimpleOvalTrack track,
                                             int countOfAnchors,
                                             double initialTimeLimitSeconds,
                                             double timeLimitGrowthFactor,
                                             int timeLimitPatienceGenerations,
                                             double goalDistanceMeters,
                                             double rankGoalFitness,
                                             double goalSpeedRewardWeight,
                                             double globalSearchIntensity,
                                             double localSearchIntensity,
                                             String fitnessMode,
                                             String backendName ) {
    Problems.RacingBackend backend = buildBackend( track,
                                                   backendName );
    ProblemRacing problem = new ProblemRacing(
      backend,
      new SimpleTargetRacingActionAdapter(),
      new OvalFixedStartStateProvider( track ),
      countOfAnchors,
      COUNT_OF_RACING_RUNS,
      initialTimeLimitSeconds,
      timeLimitGrowthFactor,
      timeLimitPatienceGenerations,
      1.0,
      goalSpeedRewardWeight,
      2.0,
      localSearchIntensity,
      globalSearchIntensity,
      goalDistanceMeters,
      rankGoalFitness );
    if( FITNESS_MODE_DISTANCE_ONLY.equals( fitnessMode ) ) {
      problem.useDistanceOnlyFitness();
    } else if( FITNESS_MODE_TARGET_SPEED.equals( fitnessMode ) ) {
      problem.useTargetDistanceSpeedFitness();
    }
    return problem;
  }

  static double computeNormalizedGlobalSearchIntensity() {
    return DEFAULT_GLOBAL_SEARCH_INTENSITY;
  }

  private static SimpleOvalTrack buildTrack() {
    return new SimpleOvalTrack( "simple_oval",
                                0.0,
                                0.0,
                                25.0,
                                30.0,
                                5.0 );
  }

  private static Problems.RacingBackend buildBackend( SimpleOvalTrack track,
                                                      String backendName ) {
    if( BACKEND_RARS.equals( backendName ) ) {
      return new SimpleOvalRarsRacingBackend( track,
                                              0.05,
                                              1100.0,
                                              135000.0,
                                              1.4,
                                              2.0,
                                              0.45,
                                              40.0 );
    }
    return new SimpleOvalRacingBackend( track,
                                        0.2,
                                        1.0,
                                        12.0,
                                        18.0,
                                        0.05,
                                        40.0 );
  }

  private static double computeGoalDistanceMeters( SimpleOvalTrack track,
                                                   double goalLaps ) {
    if( goalLaps == 0.0 ) {
      return Double.MAX_VALUE;
    }
    return track.getLapLength() * goalLaps;
  }

  private static boolean parseBoolean( String value ) {
    if( "true".equalsIgnoreCase( value ) ) {
      return true;
    }
    if( "false".equalsIgnoreCase( value ) ) {
      return false;
    }
    throw new IllegalArgumentException(
      "Boolean options must be true or false: " + value );
  }

  private static void validateFitnessMode( String fitnessMode ) {
    if( FITNESS_MODE_ROBUST.equals( fitnessMode )
        || FITNESS_MODE_DISTANCE_ONLY.equals( fitnessMode )
        || FITNESS_MODE_TARGET_SPEED.equals( fitnessMode ) ) {
      return;
    }
    throw new IllegalArgumentException(
      "fitness-mode must be robust, distance-only, or target-speed: "
      + fitnessMode );
  }

  private static void validateBackendName( String backendName ) {
    if( BACKEND_KINEMATIC.equals( backendName )
        || BACKEND_RARS.equals( backendName ) ) {
      return;
    }
    throw new IllegalArgumentException(
      "backend must be kinematic or rars: " + backendName );
  }

  private static void validateNonNegative( String label,
                                           double value ) {
    if( Double.isNaN( value ) || Double.isInfinite( value ) ) {
      throw new IllegalArgumentException( label + " must be finite" );
    }
    if( value < 0.0 ) {
      throw new IllegalArgumentException( label + " must be non-negative" );
    }
  }

  private static void validatePositive( String label,
                                        double value ) {
    if( Double.isNaN( value ) || Double.isInfinite( value ) ) {
      throw new IllegalArgumentException( label + " must be finite" );
    }
    if( value <= 0.0 ) {
      throw new IllegalArgumentException( label + " must be positive" );
    }
  }

  private static final class OvalFixedStartStateProvider
    implements RacingStartStateProvider {

    private final RacingStartState[] states;

    private OvalFixedStartStateProvider( SimpleOvalTrack track ) {
      this.states = new RacingStartState[] {
        buildState( track,
                    10.0,
                    0.0,
                    0.0 ),
        buildState( track,
                    50.0,
                    1.0,
                    0.12 ),
        buildState( track,
                    80.0,
                    -0.8,
                    -0.18 ),
        buildState( track,
                    140.0,
                    0.7,
                    0.10 ),
        buildState( track,
                    190.0,
                    -0.6,
                    -0.10 ),
        buildState( track,
                    225.0,
                    0.5,
                    0.16 ),
        buildState( track,
                    260.0,
                    -0.4,
                    -0.08 )
      };
    }

    @Override
    public void refreshBatch() {
      // Fixed states keep the experiment reproducible.
    }

    @Override
    public RacingStartState getStartState( int runIndex ) {
      return states[ runIndex ];
    }

    @Override
    public int countOfStates() {
      return states.length;
    }

    private static RacingStartState buildState( SimpleOvalTrack track,
                                                double progressMeters,
                                                double lateralOffsetMeters,
                                                double headingOffsetRadians ) {
      SimpleOvalTrack.CenterlinePose pose = track.computeCenterlinePose(
        progressMeters );
      return new RacingStartState( pose.getX()
                                   + lateralOffsetMeters * pose.getNormalX(),
                                   pose.getY()
                                   + lateralOffsetMeters * pose.getNormalY(),
                                   0.0,
                                   pose.getHeading() + headingOffsetRadians );
    }
  }
}
