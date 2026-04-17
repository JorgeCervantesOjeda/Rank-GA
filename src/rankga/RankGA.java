package rankga;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import static rankga.ConvertTime.convertMillisToTimeFormat;

/**
 * RankGA — Genetic Algorithm (GA) driver using rank-based selection, pairwise
 * recombination, and rank-based mutation intensity.
 *
 * <h2>Overview</h2>
 * This class coordinates the full evolutionary loop:
 * <ol>
 * <li><b>Initialization</b>: create a population for a specific
 * {@link Problem}.</li>
 * <li><b>Evaluation</b>: compute fitness and keep population sorted
 * (descending).</li>
 * <li><b>Selection</b>: rank-based cloning with stochastic rounding to keep
 * size exactly N.</li>
 * <li><b>Recombination</b>: pair sorted individuals (0,1), (2,3), … with
 * uniform crossover.</li>
 * <li><b>Mutation</b>: per-individual <em>intensity</em> I<sub>i</sub> = G ·
 * (rank)<sup>β</sup>, β = ln(G/L)/ln(N−1).</li>
 * <li><b>Adaptation</b>: if the problem implements
 * {@link AdaptiveProblem}, allow it to adjust its internal parameters based on
 * the current best fitness.</li>
 * <li><b>Termination</b>: stop when (a) no improvement has occurred for a given
 * patience window, or (b) the problem’s goal fitness is reached.</li>
 * </ol>
 *
 * <h2>Logging</h2>
 * The driver prints compact progress lines to stdout and appends two files per
 * run:
 * <ul>
 * <li><code>&lt;problemRunName&gt;.txt</code>: chronological log of best-so-far
 * snapshots.</li>
 * <li><code>&lt;problemRunName&gt;_&lt;rep&gt;.txt</code>: last full population
 * dump for the repetition.</li>
 * </ul>
 *
 * <h2>Caveats / Assumptions</h2>
 * <ul>
 * <li>Fitness is maximized (higher is better).</li>
 * <li>{@link Population#evaluate()} must be called after operators to maintain
 * sorting.</li>
 * <li>Patience counts wall-clock time without improvement of the <em>best</em>
 * individual (and with a non-zero genotype distance).</li>
 * <li>The default launch problem is selected via {@code --problem}; if absent,
 * {@code ts-reals} is used.</li>
 * </ul>
 *
 * Author: Jorge Cervantes — Universidad Autónoma Metropolitana, Mexico City
 */
public class RankGA {

  /**
   * Max wall-clock time without improvement before stopping (milliseconds).
   */
  private static final long PATIENCE = 1 * 60L * 1000L;

  // --- GA state (static for a simple single-run driver) ---------------------------------------
  /**
   * Current population instance for the active repetition.
   */
  static protected Population population;

  /**
   * Timestamps for run start and periodic progress.
   */
  static protected Date startTime;
  static protected final Date runTime = new Date();  // elapsed since start
  static protected final Date tryTime = new Date();  // elapsed since last improvement
  static protected Date notImproved = new Date();    // last time an improvement was recorded
  static protected Date lastDisplay = new Date();    // last time we printed a progress line
  static protected Date now;                         // current timestamp snapshot

  /**
   * Best-so-far individual (deep copy) used to detect genuine improvements and
   * log snapshots.
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

  // --------------------------------------------------------------------------------------------
  // Entry point
  // --------------------------------------------------------------------------------------------
  public static void main( String[] args ) {
    Map<String, String> options = ProblemFactory.parseArguments( args );
    String problemId = options.getOrDefault( "problem",
                                             "ts-reals" );

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

    Problem problem = ProblemFactory.create( problemId,
                                             options );
    run( problem,
         populationSize,
         repetitions );
  }

  /**
   * Execute a full Rank-GA run for a selected problem.
   *
   * @param problem owning problem
   * @param populationSize population size N
   * @param repetitions number of full runs
   */
  public static void run( Problem problem,
                          int populationSize,
                          int repetitions ) {
    // Unique name for logs (problem name + timestamp).
    String problemRunName = problem.getProblemName() + "_" + System
           .currentTimeMillis();

    System.out.println( "Patience: " + convertMillisToTimeFormat( PATIENCE ) );
    System.out.println( "Problem: " + problemRunName );
    System.out.println( "Population: " + populationSize );
    System.out.println( "Repetitions: " + repetitions );

    // Outer loop: repeat full runs to assess robustness / variance.
    for( repetition = 0; repetition < repetitions; repetition++ ) {

      // --- Reset run-level clocks/state -------------------------------------------------------
      startTime = new Date();
      initializePopulation( problem,
                           populationSize );
      lastBest = problem.getNewIndividual( population.getFittest() ); // deep copy of current best
      generation = 1;
      notImproved = new Date();
      lastDisplay = new Date();
      now = new Date();

      // --- Header for console logging ---------------------------------------------------------
      System.out.println(
        "t\tni\trep\tg\ts\tph\td\trank\tp\tfitness\textra\tgenes\tDateTime\tmil" );

      // Initial snapshot “S” (Start).
      report( "S",
              problemRunName );

      // 3) Evolutionary loop: selection → recombination → mutation (with evaluations in between).
      do {
        evolvePopulation( problemRunName,
                          problem );
        now = new Date();

        // Emit periodic lightweight progress lines (not full snapshots).
        displayProgress();

        // Allow only adaptive problems to adjust internal parameters.
        if( problem instanceof AdaptiveProblem ) {
          ( (AdaptiveProblem) problem ).adapt( lastBest.getFitness() );
        }

        // Loop until: (a) no improvement for PATIENCE window, or (b) goal fitness reached.
      } while( ( now.getTime() - notImproved.getTime() ) < PATIENCE
               && population.getFittest().getFitness() < problem.getGoalFt() );

      // Final snapshot “L” (Last) after termination.
      report( "L",
              problemRunName );
    }
  }

  // --------------------------------------------------------------------------------------------
  // Initialization
  // --------------------------------------------------------------------------------------------
  /**
   * Creates a fresh population and evaluates it (so it starts sorted).
   * <p>
   * Population size is supplied by the launcher.</p>
   */
  private static void initializePopulation( Problem problem,
                                            int populationSize ) {
    population = new Population( populationSize,
                                 problem,
                                 true,        // randomize initial genomes
                                 new Random() // PRNG
    );
    population.evaluate(); // compute fitness and sort descending
  }

  // --------------------------------------------------------------------------------------------
  // Single evolutionary “epoch”: selection → recombination → mutation (+ evaluations)
  // --------------------------------------------------------------------------------------------
  /**
   * Performs one full evolutionary epoch:
   * <ol>
   * <li><b>Selection</b>: clone according to rank-based expected counts.</li>
   * <li><b>Recombination</b>: pair (0,1), (2,3), …, then evaluate and snapshot
   * if improved (“R”).</li>
   * <li><b>Mutation</b>: apply rank-based intensity schedule, then evaluate and
   * snapshot if improved (“M”).</li>
   * </ol>
   *
   * @param problemRunName log file prefix
   * @param problem        owning problem for deep-copy factories
   */
  private static void evolvePopulation( String problemRunName,
                                        Problem problem ) {
    // --- Selection (rank-based cloning) -------------------------------------------------------
    population.select();

    // --- Recombination in adjacent pairs, then evaluate and check improvement ----------------
    population.recombine();
    population.evaluate();
    checkImprovement( "R",
                      problemRunName,
                      problem ); // “R” = post-Recombination snapshot if better

    // --- Mutation with rank-based intensity, then evaluate and check improvement --------------
    population.mutate();
    population.evaluate();
    checkImprovement( "M",
                      problemRunName,
                      problem ); // “M” = post-Mutation snapshot if better
  }

  // --------------------------------------------------------------------------------------------
  // Improvement detection & snapshot logging
  // --------------------------------------------------------------------------------------------
  /**
   * If the current best strictly improves the last-best fitness and is not a
   * duplicate genome (distance > 0), emit a snapshot log and refresh the “last
   * best” copy and timers.
   *
   * @param phase          phase tag (“R” after recombination, “M” after
   *                       mutation, etc.)
   * @param problemRunName log file prefix
   * @param problem        owning problem for deep-copy factory
   */
  private static void checkImprovement( String phase,
                                        String problemRunName,
                                        Problem problem ) {
    if( lastBest.getFitness() < population.getFittest().getFitness()
        && lastBest.distanceSqTo( population.getFittest() ) > 0.0 ) {

      report( phase,
              problemRunName );                            // print and persist a snapshot
      lastBest = problem.getNewIndividual( population.getFittest() ); // deep copy new best
      notImproved = new Date();                                   // reset patience timer
      lastDisplay = new Date();                                   // reset progress display timer
    }
    generation++; // increment generation counter after each operator phase
  }

  // --------------------------------------------------------------------------------------------
  // Lightweight progress reporting
  // --------------------------------------------------------------------------------------------
  /**
   * Periodically prints a terse progress line to stdout (no file I/O).
   * <ul>
   * <li>Every PATIENCE/10 milliseconds, shows elapsed, generations, and speed
   * (gen/ms).</li>
   * <li>Useful for monitoring without overwhelming the console.</li>
   * </ul>
   */
  private static void displayProgress() {
    if( ( now.getTime() - lastDisplay.getTime() ) > PATIENCE / 10 ) {
      lastDisplay = new Date();
      runTime.setTime( now.getTime() - startTime.getTime() );     // total elapsed
      tryTime.setTime( now.getTime() - notImproved.getTime() );   // time since last improvement

      System.out.println(
        "\r" + convertMillisToTimeFormat( runTime.getTime() )
        + " g:" + generation
        + " s:" + ( (float) generation / runTime.getTime() )
        + " ni:" + convertMillisToTimeFormat( tryTime.getTime() ) + " --"
      );
      System.out.flush();
    }
  }

  // --------------------------------------------------------------------------------------------
  // Snapshot (best-so-far) reporting & file I/O
  // --------------------------------------------------------------------------------------------
  /**
   * Emits a best-so-far snapshot:
   * <ul>
   * <li>Console: a single-line summary with elapsed times, phase tag, diversity
   * (distance), best individual summary, genome string, wall-clock timestamp,
   * and milliseconds since start.</li>
   * <li>File: appends the same line to
   * <code>&lt;problemRunName&gt;.txt</code>.</li>
   * <li>Population dump: writes the full current population to
   * <code>&lt;problemRunName&gt;_&lt;rep&gt;.txt</code> (overwriting the
   * previous dump for this repetition).</li>
   * </ul>
   *
   * @param phase       phase tag: "S" (Start), "R" (after recombination), "M"
   *                    (after mutation), "L" (Last)
   * @param problemName file prefix (problem name + timestamp)
   */
  static protected void report( String phase,
                                String problemName ) {
    Individual best = population.getFittest();
    double distance = best.distanceSqTo( lastBest );       // diversity relative to previous best copy
    Date now = new Date();

    // Columns (tab-separated) — matches the header printed in main():
    // t  ni  rep  g  s  ph  d  rank  p  fitness  extra  genes  DateTime  mil
    String reportString = String.format(
           "%s %s %d %d %9.7f %s %.2e\t%s\t%s\t%s %d\n",
           convertMillisToTimeFormat( now.getTime() - startTime.getTime() ), // t  (elapsed since start)
           convertMillisToTimeFormat( now.getTime() - notImproved.getTime() ), // ni (since last improvement)
           repetition,                                                       // rep
           generation,                                                       // g
           (float) generation / ( now.getTime() - startTime.getTime() ),     // s = gen / ms
           phase,                                                            // ph
           distance,                                                         // d = squared distance
           best,                                                             // rank, last mut intensity, best fitness, extra
           best.genomeStr(),                                                 // genes (compact)
           now,                                                              // DateTime
           ( now.getTime() - startTime.getTime() ) // mil (elapsed ms)
         );

    System.out.print( "\r" + reportString );
    System.out.flush();

    // Persist snapshot and current population state.
    logToFile( problemName,
               reportString );
    logPopulation( problemName );
  }

  /**
   * Append a line to the run-level log file (<problemRunName>.txt).
   */
  private static void logToFile( String problemName,
                                 String content ) {
    try( PrintWriter out = new PrintWriter(
                     new BufferedWriter( new FileWriter( problemName + ".txt",
                                                         true ) ) ) ) {
      out.print( content );
    } catch( IOException e ) {
      System.out.println( e );
    }
  }

  /**
   * Write the full current population to a repetition-scoped file
   * (<problemRunName>_<rep>.txt). The file is overwritten on each snapshot so
   * it always contains the latest state.
   */
  private static void logPopulation( String problemName ) {
    try( PrintWriter out = new PrintWriter(
                     new BufferedWriter( new FileWriter(
                       problemName + "_" + repetition + ".txt",
                       false ) ) ) ) {
      for( int i = 0; i < population.getSize(); i++ ) {
        out.println( population.getIndividual( i ) + "\t" + population
          .getIndividual( i ).genomeStr() );
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
      "Usage: java rankga.RankGA [--problem=name] [--population=N] [--repetitions=R] [--param=value...]" );
    System.out.println( "Problems: " + ProblemFactory.availableProblems() );
    System.out.println( "Examples:" );
    System.out.println( "  java rankga.RankGA" );
    System.out.println( "  java rankga.RankGA --problem=heawood --colors=3" );
    System.out.println( "  java rankga.RankGA ts-reals --population=30 --repetitions=5" );
  }

}
