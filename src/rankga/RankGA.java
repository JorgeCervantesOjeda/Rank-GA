package rankga;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import static rankga.ConvertTime.convertMillisToTimeFormat;

/**
 * RankGA — Genetic Algorithm (GA) driver using rank-based selection, pairwise recombination, and rank-based mutation intensity.
 *
 * <h2>Overview</h2>
 * This class coordinates the full evolutionary loop:
 * <ol>
 * <li><b>Initialization</b>: create a population for a specific {@link Problem}.</li>
 * <li><b>Evaluation</b>: compute fitness and keep population sorted (descending).</li>
 * <li><b>Selection</b>: rank-based cloning with stochastic rounding to keep size exactly N.</li>
 * <li><b>Recombination</b>: pair sorted individuals (0,1), (2,3), … with uniform crossover.</li>
 * <li><b>Mutation</b>: per-individual <em>intensity</em> I<sub>i</sub> = G · (rank)<sup>β</sup>, β = ln(G/L)/ln(N−1).</li>
 * <li><b>Adaptation</b>: if the problem implements {@link AdaptiveProblem}, allow it to adjust its internal parameters based on the current best fitness.</li>
 * <li><b>Termination</b>: stop when (a) the configured patience policy has not accepted progress within the patience window, or (b) the problem’s goal fitness
 * is reached.</li>
 * </ol>
 *
 * <h2>Logging</h2>
 * The driver prints compact progress lines to stdout and appends two files per run:
 * <ul>
 * <li><code>&lt;problemRunName&gt;.txt</code>: chronological log of best-so-far snapshots.</li>
 * <li><code>&lt;problemRunName&gt;_&lt;rep&gt;.txt</code>: last full population dump for the repetition.</li>
 * </ul>
 *
 * <h2>Caveats / Assumptions</h2>
 * <ul>
 * <li>Fitness is maximized (higher is better).</li>
 * <li>{@link Population#evaluate()} must be called after operators to maintain sorting.</li>
 * <li>Patience is measured in wall-clock time and its reset semantics are configurable: by strict fitness improvement or by incumbent movement.</li>
 * <li>The default launch problem is selected via {@code --problem}; if absent, {@code one-max} is used.</li>
 * </ul>
 *
 * Author: Jorge Cervantes — Universidad Autónoma Metropolitana, Mexico City
 */
public class RankGA {

  /**
   * Max wall-clock time without improvement before stopping (milliseconds).
   */
  static final long DEFAULT_PATIENCE_MILLIS = 1 * 60L * 1000L;
  private static final String SNAPSHOT_HEADER = "t, ni, rep, g, s, ph, d, rank, p, fitness, extra, genes, DateTime, mil";
  private static final String POPULATION_HEADER = "rank, mutationIntensity, fitness, extra, genes";

  public enum IncumbentUpdatePolicy {
    STRICT,
    NEUTRAL;

    static IncumbentUpdatePolicy fromOption( String option ) {
      if( option == null ) {
        return STRICT;
      }
      switch( option.trim().toLowerCase( Locale.ROOT ) ) {
        case "strict":
          return STRICT;
        case "neutral":
          return NEUTRAL;
        default:
          throw new IllegalArgumentException(
            "Unknown incumbent update policy '" + option
            + "'. Allowed: strict, neutral" );
      }
    }

  }

  public enum PatienceResetPolicy {
    FITNESS,
    MOVEMENT;

    static PatienceResetPolicy fromOption( String option ) {
      if( option == null ) {
        return FITNESS;
      }
      switch( option.trim().toLowerCase( Locale.ROOT ) ) {
        case "fitness":
          return FITNESS;
        case "movement":
          return MOVEMENT;
        default:
          throw new IllegalArgumentException(
            "Unknown patience reset policy '" + option
            + "'. Allowed: fitness, movement" );
      }
    }

  }

  // --- GA state (static for a simple single-run driver)
  // ---------------------------------------
  /**
   * Current population instance for the active repetition.
   */
  static protected Population population;

  /**
   * Timestamps for run start and periodic progress.
   */
  static protected Date startTime;
  static protected final Date runTime = new Date(); // elapsed since start
  static protected final Date tryTime = new Date(); // elapsed since last improvement
  static protected Date notImproved = new Date(); // last time an improvement was recorded
  static protected Date lastDisplay = new Date(); // last time we printed a progress line
  static protected Date now; // current timestamp snapshot

  /**
   * Best-so-far individual (deep copy) used to detect genuine improvements and log snapshots.
   */
  static protected Individual lastBest;

  /**
   * Repetition counter (outer loop to assess robustness).
   */
  static protected int repetition;

  /**
   * Generation counter (increments after each operator phase).
   */
  static protected long generation;
  static protected long patienceMillis = DEFAULT_PATIENCE_MILLIS;
  static protected IncumbentUpdatePolicy incumbentUpdatePolicy = IncumbentUpdatePolicy.STRICT;
  static protected PatienceResetPolicy patienceResetPolicy = PatienceResetPolicy.FITNESS;

  // --------------------------------------------------------------------------------------------
  // Entry point
  // --------------------------------------------------------------------------------------------
  public static void main( String[] args ) {
    Map<String, String> options = ProblemFactory.parseArguments( args );
    String problemId = options.getOrDefault( "problem",
                                             "one-max" );
    long seed = ProblemFactory.readLongOption( options,
                                               "seed",
                                               System.currentTimeMillis() );

    if( options.containsKey( "help" )
        || "help".equals( problemId ) ) {
      printUsage();
      return;
    }

    int populationSize = ProblemFactory.readIntOption( options,
                                                       "population",
                                                       20 );
    int repetitions = ProblemFactory.readIntOption( options,
                                                    "repetitions",
                                                    10 );
    long selectedPatienceMillis = ProblemFactory.readLongOption(
         options,
         "patiencems",
         DEFAULT_PATIENCE_MILLIS );
    IncumbentUpdatePolicy selectedIncumbentUpdatePolicy = IncumbentUpdatePolicy
                          .fromOption( options.get( "incumbentupdate" ) );
    PatienceResetPolicy selectedPatienceResetPolicy = PatienceResetPolicy.fromOption( options.get( "patiencereset" ) );

    Problem problem = ProblemFactory.create( problemId,
                                             options,
                                             seed );
    String problemParameters = ProblemFactory.describeProblemParameters(
           problemId,
           options,
           problem );
    run( problem,
         populationSize,
         repetitions,
         seed,
         problemParameters,
         selectedPatienceMillis,
         selectedIncumbentUpdatePolicy,
         selectedPatienceResetPolicy );
  }

  /**
   * Execute a full Rank-GA run for a selected problem.
   *
   * @param problem        owning problem
   * @param populationSize population size N
   * @param repetitions    number of full runs
   * @param seed           base seed for the run
   */
  public static void run( Problem problem,
                          int populationSize,
                          int repetitions,
                          long seed ) {
    run( problem,
         populationSize,
         repetitions,
         seed,
         "",
         DEFAULT_PATIENCE_MILLIS,
         IncumbentUpdatePolicy.STRICT,
         PatienceResetPolicy.FITNESS );
  }

  /**
   * Execute a full Rank-GA run for a selected problem.
   *
   * @param problem           owning problem
   * @param populationSize    population size N
   * @param repetitions       number of full runs
   * @param seed              base seed for the run
   * @param problemParameters semicolon-separated description of the effective problem parameters used for this run
   */
  public static void run( Problem problem,
                          int populationSize,
                          int repetitions,
                          long seed,
                          String problemParameters ) {
    run( problem,
         populationSize,
         repetitions,
         seed,
         problemParameters,
         DEFAULT_PATIENCE_MILLIS,
         IncumbentUpdatePolicy.STRICT,
         PatienceResetPolicy.FITNESS );
  }

  /**
   * Execute a full Rank-GA run with explicit incumbency and patience policies.
   *
   * @param problem                       owning problem
   * @param populationSize                population size N
   * @param repetitions                   number of full runs
   * @param seed                          base seed for the run
   * @param problemParameters             semicolon-separated description of the effective problem parameters used for this run
   * @param patienceMillis                max wall-clock time without accepted progress
   * @param selectedIncumbentUpdatePolicy policy that decides whether neutral moves can replace the incumbent
   * @param selectedPatienceResetPolicy   policy that decides what resets patience
   */
  public static void run( Problem problem,
                          int populationSize,
                          int repetitions,
                          long seed,
                          String problemParameters,
                          long patienceMillis,
                          IncumbentUpdatePolicy selectedIncumbentUpdatePolicy,
                          PatienceResetPolicy selectedPatienceResetPolicy ) {
    if( patienceMillis <= 0L ) {
      throw new IllegalArgumentException(
        "patienceMillis must be positive" );
    }
    RankGA.patienceMillis = patienceMillis;
    RankGA.incumbentUpdatePolicy = selectedIncumbentUpdatePolicy;
    RankGA.patienceResetPolicy = selectedPatienceResetPolicy;

    // Unique name for logs (problem name + seed + timestamp).
    long runId = System.currentTimeMillis();
    String problemName = problem.getProblemName();
    Path outputDirectory = RunOutputPaths.ensureFamilyDirectory( problem );
    String problemRunName = outputDirectory.resolve(
           problemName + "_seed" + seed + "_" + runId ).toString();
    Path summaryFile = outputDirectory.resolve(
         problemName + "_seed" + seed + "_" + runId
         + "_summary.csv" );
    Path summaryMetadataFile = metadataFileFor( summaryFile );

    System.out.println( "Patience: " + convertMillisToTimeFormat( patienceMillis ) );
    System.out.println( "Seed: " + seed );
    System.out.println( "Problem: " + problemRunName );
    System.out.println( "Population: " + populationSize );
    System.out.println( "Repetitions: " + repetitions );
    System.out.println( "Incumbent update policy: "
                        + selectedIncumbentUpdatePolicy.name().toLowerCase(
        Locale.ROOT ) );
    System.out.println( "Patience reset policy: "
                        + selectedPatienceResetPolicy.name().toLowerCase(
        Locale.ROOT ) );

    writeStructuredSummaryMetadata( summaryMetadataFile,
                                    problem,
                                    problemName,
                                    problemParameters,
                                    seed,
                                    runId,
                                    populationSize,
                                    repetitions,
                                    problem.getGoalFt(),
                                    patienceMillis,
                                    selectedIncumbentUpdatePolicy,
                                    selectedPatienceResetPolicy,
                                    problemRunName );

    // Outer loop: repeat full runs to assess robustness / variance.
    for( repetition = 0; repetition < repetitions; repetition++ ) {

      // --- Reset run-level clocks/state
      // -------------------------------------------------------
      startTime = new Date();
      long repetitionSeed = seed + repetition;
      Random repetitionRandom = new Random( repetitionSeed );
      long evaluations = initializePopulation( problem,
                                               populationSize,
                                               repetitionRandom );
      lastBest = problem.getNewIndividual( population.getFittest() ); // deep copy of current best
      generation = 1;
      notImproved = new Date();
      lastDisplay = new Date();
      now = new Date();

      // --- Header for console logging
      // ---------------------------------------------------------
      System.out.println(
        "t\tni\trep\tg\ts\tph\td\trank\tp\tfitness\textra\tgenes\tDateTime\tmil" );

      // Initial snapshot “S” (Start).
      report( "S",
              problemRunName );

      // If the initial population already reaches the goal, stop without forcing
      // an extra generation. The summary should then report only the initial N
      // evaluations.
      while( ( now.getTime() - notImproved.getTime() ) < patienceMillis
             && population.getFittest().getFitness() < problem.getGoalFt() ) {
        evaluations += evolvePopulation( problemRunName,
                                         problem );
        now = new Date();

        // Emit periodic lightweight progress lines (not full snapshots).
        displayProgress();

        // Allow only adaptive problems to adjust internal parameters.
        if( problem instanceof AdaptiveProblem ) {
          ( (AdaptiveProblem) problem ).adapt( lastBest.getFitness() );
        }

      }

      String terminationReason = population.getFittest().getFitness() >= problem.getGoalFt()
                                 ? "goal"
                                 : "patience";

      // Final snapshot “L” (Last) after termination.
      report( "L",
              problemRunName );

      appendStructuredSummary( summaryFile,
                               repetition,
                               repetitionSeed,
                               evaluations,
                               population.getFittest().getFitness(),
                               now.getTime() - startTime.getTime(),
                               terminationReason );
    }

    generatePlots( summaryFile );
  }

  /**
   * Execute a full Rank-GA run for a selected problem.
   *
   * @param problem        owning problem
   * @param populationSize population size N
   * @param repetitions    number of full runs
   */
  public static void run( Problem problem,
                          int populationSize,
                          int repetitions ) {
    run( problem,
         populationSize,
         repetitions,
         System.currentTimeMillis() );
  }

  // --------------------------------------------------------------------------------------------
  // Initialization
  // --------------------------------------------------------------------------------------------
  /**
   * Creates a fresh population and evaluates it (so it starts sorted).
   * <p>
   * Population size is supplied by the launcher.
   * </p>
   *
   * @return number of fitness evaluations performed to initialize the population
   */
  private static long initializePopulation( Problem problem,
                                            int populationSize,
                                            Random randomizer ) {
    population = new Population( populationSize,
                                 problem,
                                 true, // randomize initial genomes
                                 randomizer // PRNG
  );
    population.evaluate(); // compute fitness and sort descending
    return population.getSize();
  }

  /**
   * Evaluate the current population after one operator phase and count the number of fitness computations performed.
   *
   * @param problemRunName log file prefix
   * @param problem        owning problem for deep-copy factories
   *
   * @return number of fitness evaluations performed in this epoch
   */
  private static long evolvePopulation( String problemRunName,
                                        Problem problem ) {
    long evaluations = 0L;

    // --- Selection (rank-based cloning)
    // -------------------------------------------------------
    population.select();

    // --- Recombination in adjacent pairs, then evaluate and check improvement
    // ----------------
    population.recombine();
    population.evaluate();
    evaluations += population.getSize();
    checkImprovement( "R",
                      problemRunName,
                      problem ); // “R” = post-Recombination snapshot if better

    // --- Mutation with rank-based intensity, then evaluate and check improvement
    // --------------
    population.mutate();
    population.evaluate();
    evaluations += population.getSize();
    checkImprovement( "M",
                      problemRunName,
                      problem ); // “M” = post-Mutation snapshot if better

    return evaluations;
  }

  // --------------------------------------------------------------------------------------------
  // Structured run summary
  // --------------------------------------------------------------------------------------------
  /**
   * Append one structured CSV row for a repetition.
   *
   * The per-row CSV keeps only repetition-varying data. Run-level metadata is written once to the companion {@code *_summary_meta.csv} file.
   */
  private static void appendStructuredSummary( Path summaryFile,
                                               int repetitionIndex,
                                               long repetitionSeed,
                                               long evaluations,
                                               double bestFitness,
                                               long elapsedMillis,
                                               String terminationReason ) {
    boolean writeHeader = !Files.exists( summaryFile );
    try( PrintWriter out = new PrintWriter(
                     new BufferedWriter( new FileWriter( summaryFile.toFile(),
                                                         true ) ) ) ) {
      if( writeHeader ) {
        out.println(
          "repetition,repetition_seed,evaluations,best_fitness,elapsed_ms,termination_reason" );
      }
      out.println(
        String.join(
          ",",
          Integer.toString( repetitionIndex ),
          Long.toString( repetitionSeed ),
          Long.toString( evaluations ),
          String.format( Locale.ROOT,
                         "%.17g",
                         bestFitness ),
          Long.toString( elapsedMillis ),
          csvField( terminationReason ) ) );
    } catch( IOException e ) {
      System.out.println( e );
    }
  }

  private static void writeStructuredSummaryMetadata(
    Path metadataFile,
    Problem problem,
    String problemName,
    String problemParameters,
    long seed,
    long runId,
    int populationSize,
    int repetitions,
    double goalFitness,
    long patienceMillis,
    IncumbentUpdatePolicy incumbentUpdatePolicy,
    PatienceResetPolicy patienceResetPolicy,
    String problemRunName ) {
    try( PrintWriter out = new PrintWriter(
                     new BufferedWriter( new FileWriter( metadataFile.toFile(),
                                                         false ) ) ) ) {
      out.println( "key,value" );
      out.println( csvField( "algorithm" ) + "," + csvField( "RankGA" ) );
      out.println( csvField( "problem_class" ) + ","
                   + csvField( problem.getClass().getSimpleName() ) );
      out.println( csvField( "problem_name" ) + "," + csvField( problemName ) );
      out.println( csvField( "problem_parameters" ) + ","
                   + csvField( problemParameters ) );
      out.println( csvField( "base_seed" ) + ","
                   + csvSpreadsheetTextField( Long.toString( seed ) ) );
      out.println( csvField( "run_id" ) + ","
                   + csvSpreadsheetTextField( Long.toString( runId ) ) );
      out.println( csvField( "population_size" ) + ","
                   + Integer.toString( populationSize ) );
      out.println( csvField( "repetitions" ) + ","
                   + Integer.toString( repetitions ) );
      out.println( csvField( "goal_fitness" ) + ","
                   + String.format( Locale.ROOT,
                                    "%.17g",
                                    goalFitness ) );
      out.println( csvField( "patience_ms" ) + ","
                   + Long.toString( patienceMillis ) );
      out.println( csvField( "incumbent_update_policy" ) + ","
                   + csvField( incumbentUpdatePolicy.name().toLowerCase(
          Locale.ROOT ) ) );
      out.println( csvField( "patience_reset_policy" ) + ","
                   + csvField( patienceResetPolicy.name().toLowerCase(
          Locale.ROOT ) ) );
      out.println( csvField( "output_prefix" ) + ","
                   + csvField( problemRunName ) );
    } catch( IOException e ) {
      System.out.println( e );
    }
  }

  private static Path metadataFileFor( Path csvFile ) {
    String fileName = csvFile.getFileName().toString();
    int extensionIndex = fileName.lastIndexOf( '.' );
    String stem = extensionIndex >= 0
                  ? fileName.substring( 0,
                                        extensionIndex )
                  : fileName;
    String extension = extensionIndex >= 0
                       ? fileName.substring( extensionIndex )
                       : "";
    return csvFile.resolveSibling( stem + "_meta" + extension );
  }

  /**
   * Best-effort post-processing: generate the ordered figures automatically after the summary CSV is complete.
   *
   * The run itself should not fail just because plotting is unavailable on the local machine.
   *
   * @param summaryFile structured summary produced by this run
   */
  private static void generatePlots( Path summaryFile ) {
    Path scriptPath = Paths.get( "scripts",
                                 "plot_rankga_goal_evaluations.py" );
    Path outputDirectory = Paths.get( "figures" );
    if( !Files.exists( scriptPath ) ) {
      System.out.println( "Plot script not found: " + scriptPath );
      return;
    }

    List<String> command = new ArrayList<>();
    command.add( "python" );
    command.add( scriptPath.toString() );
    command.add( summaryFile.toString() );
    command.add( "--output-dir" );
    command.add( outputDirectory.toString() );

    try {
      Process process = new ProcessBuilder( command )
              .redirectErrorStream( true )
              .start();

      try( BufferedReader reader = new BufferedReader(
                          new InputStreamReader( process.getInputStream() ) ) ) {
        String line;
        while( ( line = reader.readLine() ) != null ) {
          System.out.println( line );
        }
      }

      int exitCode = process.waitFor();
      if( exitCode != 0 ) {
        System.out.println( "Plot generation failed with exit code "
                            + exitCode + " for " + summaryFile );
      }
    } catch( IOException |
             InterruptedException e ) {
      if( e instanceof InterruptedException ) {
        Thread.currentThread().interrupt();
      }
      System.out.println( "Plot generation skipped: " + e.getMessage() );
    }
  }

  // --------------------------------------------------------------------------------------------
  // Improvement detection & snapshot logging
  // --------------------------------------------------------------------------------------------
  /**
   * Evaluate the current best against the incumbent according to the configured policies. Incumbent replacement and patience reset are intentionally decoupled.
   *
   * @param phase          phase tag (“R” after recombination, “M” after mutation, etc.)
   * @param problemRunName log file prefix
   * @param problem        owning problem for deep-copy factory
   */
  private static void checkImprovement( String phase,
                                        String problemRunName,
                                        Problem problem ) {
    Individual candidate = population.getFittest();
    boolean improvedFitness = hasStrictFitnessImprovement( lastBest,
                                                           candidate );
    boolean movedIncumbent = shouldReplaceIncumbent( lastBest,
                                                     candidate,
                                                     incumbentUpdatePolicy );

    if( movedIncumbent ) {
      report( phase,
              problemRunName ); // print and persist a snapshot
      lastBest = problem.getNewIndividual( candidate ); // deep copy new incumbent
      if( shouldResetPatience( improvedFitness,
                               movedIncumbent,
                               patienceResetPolicy ) ) {
        notImproved = new Date();
      }
      lastDisplay = new Date();
    }
    generation++; // increment generation counter after each operator phase
  }

  static boolean hasStrictFitnessImprovement( Individual incumbent,
                                              Individual candidate ) {
    return incumbent.distanceSqTo( candidate ) > 0.0
           && candidate.getFitness() > incumbent.getFitness();
  }

  static boolean shouldReplaceIncumbent(
    Individual incumbent,
    Individual candidate,
    IncumbentUpdatePolicy policy ) {
    if( incumbent.distanceSqTo( candidate ) <= 0.0 ) {
      return false;
    }
    switch( policy ) {
      case STRICT:
        return candidate.getFitness() > incumbent.getFitness();
      case NEUTRAL:
        return candidate.getFitness() >= incumbent.getFitness();
      default:
        throw new IllegalStateException( "Unsupported incumbent policy: "
                                         + policy );
    }
  }

  static boolean shouldResetPatience( boolean improvedFitness,
                                      boolean movedIncumbent,
                                      PatienceResetPolicy policy ) {
    switch( policy ) {
      case FITNESS:
        return improvedFitness;
      case MOVEMENT:
        return movedIncumbent;
      default:
        throw new IllegalStateException( "Unsupported patience policy: "
                                         + policy );
    }
  }

  // --------------------------------------------------------------------------------------------
  // Lightweight progress reporting
  // --------------------------------------------------------------------------------------------
  /**
   * Periodically prints a terse progress line to stdout (no file I/O).
   * <ul>
   * <li>Every patienceMillis/10 milliseconds, shows elapsed, generations, and speed (gen/ms).</li>
   * <li>Useful for monitoring without overwhelming the console.</li>
   * </ul>
   */
  private static void displayProgress() {
    long progressInterval = Math.max( 1L,
                                      patienceMillis / 10L );
    if( ( now.getTime() - lastDisplay.getTime() ) > progressInterval ) {
      lastDisplay = new Date();
      runTime.setTime( now.getTime() - startTime.getTime() ); // total elapsed
      tryTime.setTime( now.getTime() - notImproved.getTime() ); // time since last improvement

      System.out.println(
        "\r" + convertMillisToTimeFormat( runTime.getTime() )
        + " g:" + generation
        + " s:" + ( (float) generation / runTime.getTime() )
        + " ni:" + convertMillisToTimeFormat( tryTime.getTime() ) + " --" );
      System.out.flush();
    }
  }

  // --------------------------------------------------------------------------------------------
  // Snapshot (best-so-far) reporting & file I/O
  // --------------------------------------------------------------------------------------------
  /**
   * Emits a best-so-far snapshot:
   * <ul>
   * <li>Console: a single-line summary with elapsed times, phase tag, diversity (distance), best individual summary, genome string, wall-clock timestamp, and
   * milliseconds since start.</li>
   * <li>File: appends the same line to <code>&lt;problemRunName&gt;.txt</code>.</li>
   * <li>Population dump: writes the full current population to <code>&lt;problemRunName&gt;_&lt;rep&gt;.txt</code> (overwriting the previous dump for this
   * repetition).</li>
   * </ul>
   *
   * @param phase       phase tag: "S" (Start), "R" (after recombination), "M" (after mutation), "L" (Last)
   * @param problemName file prefix (problem name + timestamp)
   */
  static protected void report( String phase,
                                String problemName ) {
    Individual best = population.getFittest();
    double distance = best.distanceSqTo( lastBest ); // diversity relative to previous best copy
    Date now = new Date();

    String reportString = String.format(
           "%s, %s, %d, %d, %9.7f, %s, %.2e, %s, %s, %s, %d\n",
           convertMillisToTimeFormat( now.getTime() - startTime.getTime() ), // t (elapsed since start)
           convertMillisToTimeFormat( now.getTime() - notImproved.getTime() ), // ni (since last improvement)
           repetition, // rep
           generation, // g
           (float) generation / ( now.getTime() - startTime.getTime() ), // s = gen / ms
           phase, // ph
           distance, // d = squared distance
           best, // rank, last mut intensity, best fitness, extra
           best.genomeStr(), // genes (compact)
           now, // DateTime
           ( now.getTime() - startTime.getTime() ) // mil (elapsed ms)
         );

    System.out.print( "\r" + reportString );
    System.out.flush();

    // Persist snapshot and current population state.
    logToFile( problemName,
               SNAPSHOT_HEADER,
               reportString );
    logPopulation( problemName );
  }

  /**
   * Append a line to the run-level log file (<problemRunName>.txt). The file is self-describing because the tab-separated header is written once, before the
   * first snapshot line.
   */
  private static void logToFile( String problemName,
                                 String header,
                                 String content ) {
    Path logFile = Path.of( problemName + ".csv" );
    boolean writeHeader = !Files.exists( logFile );
    try( PrintWriter out = new PrintWriter(
                     new BufferedWriter( new FileWriter( problemName + ".csv",
                                                         true ) ) ) ) {
      if( writeHeader ) {
        out.println( header );
      }
      out.print( content );
    } catch( IOException e ) {
      System.out.println( e );
    }
  }

  /**
   * Write the full current population to a repetition-scoped file (<problemRunName>_<rep>.txt). The file is overwritten on each snapshot so it always contains
   * the latest state plus a header row.
   */
  private static void logPopulation( String problemName ) {
    try( PrintWriter out = new PrintWriter(
                     new BufferedWriter( new FileWriter(
                       problemName + "_" + repetition + ".csv",
                       false ) ) ) ) {
      out.println( POPULATION_HEADER );
      for( int i = 0; i < population.getSize(); i++ ) {
        out.println( population.getIndividual( i ) 
                     + ", "
                     + population.getIndividual( i ).genomeStr() );
      }
    } catch( IOException e ) {
      System.out.println( e );
    }
  }

  /**
   * Print a compact usage summary for the command-line launcher.
   */
  private static void printUsage() {
    System.out.println(
      "Usage: java rankga.RankGA [--problem=name] [--population=N] [--repetitions=R] [--seed=S] [--patience-ms=T] [--incumbent-update=strict|neutral] [--patience-reset=fitness|movement] [--param=value...]" );
    System.out.println( "Problems: " + ProblemFactory.availableProblems() );
    System.out.println( "Examples:" );
    System.out.println( "  java rankga.RankGA" );
    System.out.println( "  java rankga.RankGA --problem=one-max --genome-length=8 --seed=1234" );
    System.out.println( "  java rankga.RankGA --problem=heawood --colors=3 --seed=1234" );
    System.out.println( "  java rankga.RankGA ts-reals --population=30 --repetitions=5" );
  }

  private static String csvField( String value ) {
    return "\"" + value.replace( "\"",
                                 "\"\"" ) + "\"";
  }

  private static String csvSpreadsheetTextField( String value ) {
    // Leading apostrophe keeps long integer-like identifiers as text in
    // spreadsheet tools, avoiding scientific notation in views such as run_id.
    return csvField( "'" + value );
  }

}
