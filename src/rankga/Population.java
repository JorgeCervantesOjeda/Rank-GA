package rankga;

import java.util.ArrayList;
import java.util.Random;

/**
 * Population — Cohort of Individuals managed by Rank-GA.
 *
 * <h2>Purpose</h2>
 * Encapsulates the core GA loop components that act at the population level:
 * <ul>
 * <li><b>Initialization</b>: create N individuals via the {@link Problem}
 * factory.</li>
 * <li><b>Evaluation</b>: compute fitness for all individuals and keep the
 * population sorted in <b>descending</b> order (index 0 = best).</li>
 * <li><b>Selection</b>: rank-based deterministic + stochastic rounding of clone
 * counts using a selection pressure S, producing exactly N clones.</li>
 * <li><b>Recombination</b>: uniform crossover in ordered adjacent pairs (0,1),
 * (2,3), …</li>
 * <li><b>Mutation</b>: rank-based per-individual <i>intensity</i> schedule
 * {@code I_i = G * (rank_i)^beta}, where {@code beta = ln(G/L)/ln(N-1)} and
 * {@code rank_i = i/N}.</li>
 * </ul>
 *
 * <h2>Key conventions</h2>
 * <ul>
 * <li><b>Fitness orientation</b>: higher is better. Sorting is descending.</li>
 * <li><b>Rank definition</b>: after sorting, index i∈[0..N-1] induces rank r_i
 * = i/N. Thus r_0 = 0 (best), r_{N-1} ≈ 1 (worst, strictly (N-1)/N).</li>
 * <li><b>Mutation intensity</b>: the scalar passed to
 * {@code Individual.mutate(double)} is an <b>intensity</b>, not a per-gene
 * probability. Each Gene interprets intensity consistently (e.g., step size,
 * noise scale).</li>
 * <li><b>Preconditions</b>: it is assumed that {@code G > L}, so
 * {@code beta > 0}. This preserves elites (best individual receives intensity
 * 0).</li>
 * </ul>
 *
 * <h2>Complexity</h2>
 * <ul>
 * <li>Evaluation: O(N * cost(fitness)).</li>
 * <li>Sorting: O(N log N).</li>
 * <li>Selection (cloning + rounding): O(N).</li>
 * <li>Recombination: O(N * genomeLength).</li>
 * <li>Mutation: O(N * genomeLength).</li>
 * </ul>
 *
 * Author: Jorge Cervantes — Universidad Autónoma Metropolitana, Mexico City
 */
public class Population {

  /**
   * Current population contents; maintained in descending fitness order after
   * evaluate().
   */
  private ArrayList<Individual> individuals;

  /**
   * Intensities supplied by the {@link Problem}:
   * <ul>
   * <li>{@code globalSearchIntensity} (G): upper bound (max) used at worst
   * ranks.</li>
   * <li>{@code localSearchIntensity} (L): lower bound (min) used to define the
   * exponent.</li>
   * </ul>
   * Both are treated as <b>intensities</b> (unitless), not probabilities.
   */
  private final double localSearchIntensity;
  private final double globalSearchIntensity;

  /**
   * Selection pressure S fixed by algorithm design.
   * <p>
   * Clone expectation per rank uses: c_i = S * (1 - r_i)^{S - 1} with r_i =
   * i/N. Here S is intentionally fixed at 3.0, not exposed as a tunable
   * hyperparameter.</p>
   */
  private static final double SELECTION_PRESSURE = 3.0;

  /**
   * Mutation exponent β = ln(G/L) / ln(N-1).
   * <p>
   * Combines with rank r_i = i/N to produce per-individual intensity I_i = G *
   * (r_i)^β. With G > L ⇒ β > 0 ⇒ I_0 = 0 (elite preservation) and intensity
   * increases with rank.</p>
   */
  private final double mutationExponent;

  /**
   * Problem factory & PRNG for constructing/copying individuals and for
   * stochastic steps.
   */
  private final Problem problem;
  private final Random randomizer;

  // --------------------------------------------------------------------------------------------
  // Construction & initialization
  // --------------------------------------------------------------------------------------------
  /**
   * Constructs a population of size {@code numIndividuals}. Initializes each
   * individual using the problem factory (random or default per flag), and
   * precomputes the mutation exponent β based on {@code G} and {@code L}
   * retrieved from the problem.
   *
   * @param numIndividuals Number of individuals (N). Must be ≥ 3 to keep the
   *                       rank-based mutation schedule well-defined.
   *                       ln(N-1).
   * @param problem        Problem that supplies factories, intensities, and
   *                       fitness.
   * @param randomize      Whether to randomize initial genomes.
   * @param randomizer     PRNG for initialization and subsequent stochastic
   *                       steps.
   */
  public Population( int numIndividuals,
                     Problem problem,
                     boolean randomize,
                     Random randomizer ) {
    if( numIndividuals < 3 ) {
      throw new IllegalArgumentException(
        "Population size must be at least 3" );
    }

    this.problem = problem;
    this.randomizer = randomizer;

    // Create initial cohort.
    individuals = new ArrayList<>();
    for( int i = 0; i < numIndividuals; i++ ) {
      individuals.add( problem.getNewIndividual( randomize,
                                                 randomizer ) );
    }

    // Intensities from the Problem (see Problem.getGlobalSearchIntensity / getLocalSearchIntensity).
    this.globalSearchIntensity = problem.getGlobalSearchIntensity(); // G
    this.localSearchIntensity = problem.getLocalSearchIntensity();  // L

    // β = ln(G/L) / ln(N-1). Assumes G > L and N > 1. Matches the Java used in mutate().
    this.mutationExponent =
    Math.log( globalSearchIntensity / localSearchIntensity )
    / Math.log( numIndividuals - 1 );

    // Optional: log hyperparameters for traceability.
    System.out.println( "NumIndividuals = " + numIndividuals );
    System.out.println( "Selective Pressure = " + SELECTION_PRESSURE + " (fixed)" );
    System.out.println( "Max Mutation Rate (G) = " + globalSearchIntensity );
    System.out.println( "Mutation Exponent (beta) = " + mutationExponent );
  }

  // --------------------------------------------------------------------------------------------
  // Evaluation & ordering
  // --------------------------------------------------------------------------------------------
  /**
   * Evaluate fitness for every individual, then keep the array ordered in
   * descending fitness.
   * <p>
   * Sorting is central because rank-based selection/recombination/mutation
   * depend on index order.</p>
   */
  public void evaluate() {
    for( Individual individual : individuals ) {
      individual.updateFitness();
    }
    sortIndividualsByFitness();
  }

  /**
   * Internal ordering by descending fitness: index 0 is the best individual.
   */
  private void sortIndividualsByFitness() {
    individuals.sort( ( a, b )
      -> Double.compare( b.getFitness(),
                         a.getFitness() ) );
  }

  // --------------------------------------------------------------------------------------------
  // Selection: rank-based cloning with stochastic rounding
  // --------------------------------------------------------------------------------------------
  /**
   * Rank-based selection via expected clone counts:
   * <pre>
   *   r_i = i / (N - 1)
   *   E[clones_i] = c_i = S * (1 - r_i)^{S - 1}
   * </pre>
   *
   * Procedure:
   * <ol>
   * <li>Allocate floor(c_i) clones for each i.</li>
   * <li>If total < N, loop cyclically over i and, with probability equal to the
   * fractional part f_i = c_i - floor(c_i), add one extra clone until the
   * population reaches N.</li> <li>Replace the current population by the list
   * of clones (deep copies)
   * .</li>
   * </ol>
   *
   * Properties:
   * <ul>
   * <li>Top ranks (small r_i) rece ive mor e e x pec ted clones when S >
   * 1.</li>
   * <li>Total size is exactly N; this avoids drift in population size.</li>
   * <li>Uses problem factory {@code getNewIndividual(individual)} to clone
   * individuals.</li>
   * </ul>
   */
  public void select() {
    int numIndividuals = individuals.size();
    ArrayList<Individual> clones = new ArrayList<>();
    int[] cloneCounts = new int[ numIndividuals ];
    int totalClones = 0;

    // 1) Deterministic allocation: floor(c_i).
    for( int i = 0; i < numIndividuals; i++ ) {
      double r_i = i / (double) ( numIndividuals - 1 ); // rank in [0, 1)
      double c_i = SELECTION_PRESSURE * Math.pow( 1 - r_i,
                                                  SELECTION_PRESSURE - 1 );
      cloneCounts[ i ] = (int) Math.floor( c_i );
      totalClones += cloneCounts[ i ];
    }

    // 2) Stochastic rounding of the fractional parts to reach exactly N clones.
    while( totalClones < numIndividuals ) {
      for( int i = 0; i < numIndividuals && totalClones < numIndividuals; i++ ) {
        // Use the same rank convention as the deterministic pass.
        double r_i = i / (double) ( numIndividuals - 1 );
        double c_i = SELECTION_PRESSURE * Math.pow( 1 - r_i,
                                                    SELECTION_PRESSURE - 1 );
        double fractional = c_i - Math.floor( c_i );
        if( randomizer.nextDouble() < fractional ) {
          cloneCounts[ i ]++;
          totalClones++;
        }
      }
    }

    // 3) Materialize clones as deep copies using the Problem factory.
    for( int i = 0; i < numIndividuals; i++ ) {
      for( int j = 0; j < cloneCounts[ i ]; j++ ) {
        clones.add( problem.getNewIndividual( individuals.get( i ) ) );
      }
    }

    // Replace the old population with the cloned one.
    individuals = clones;
  }

  /**
   * Assigns ranks (index order) to individuals. Must be called after sorting
   * and before rank-dependent operators (recombination/mutation).
   */
  private void setRanks() {
    for( int i = 0; i < individuals.size(); i++ ) {
      individuals.get( i ).setRank( i ); // package-private setter in Individual
    }
  }

  // --------------------------------------------------------------------------------------------
  // Recombination: pair adjacent by rank
  // --------------------------------------------------------------------------------------------
  /**
   * Rank-based pairing and uniform crossover:
   * <ul>
   * <li>Re-sort (via prior evaluate()) and set ranks.</li>
   * <li>Pair (0,1), (2,3), …, calling {@code recombinate} for each pair.</li>
   * </ul>
   * This scheme is simple and stable; alternative pairing strategies (e.g.,
   * random pairing) are possible but this version matches the reference
   * implementation.
   */
  public void recombine() {
    this.setRanks();
    for( int i = 0; i < individuals.size() - 1; i += 2 ) {
      individuals.get( i ).recombinate( individuals.get( i + 1 ) );
    }
  }

  // --------------------------------------------------------------------------------------------
  // Mutation: rank-based intensity schedule
  // --------------------------------------------------------------------------------------------
  /**
   * Applies per-individual mutation intensity based on rank:
   * <pre>
   *   r_i   = i / N
   *   beta  = ln(G / L) / ln(N - 1)
   *   I_i   = G * (r_i)^{beta}
   * </pre> Assumptions:
   * <ul>
   * <li>G = {@code globalSearchIntensity} > L = {@code localSearchIntensity},
   * thus beta > 0.</li>
   * <li>The best individual (i=0) receives I_0 = 0 (elite preservation).</li>
   * <li>Each {@link Gene} interprets intensity consistently (e.g., step size /
   * noise scale).</li>
   * </ul>
   */
  public void mutate() {
    this.setRanks();
    int N = individuals.size();
    for( int i = 0; i < N; i++ ) {
      double r_i = i / (double) ( N - 1 ); // in [0, 1)
      double intensity = globalSearchIntensity * Math.pow( r_i,
                                                           mutationExponent );
      individuals.get( i ).mutate( intensity );
    }
  }

  // --------------------------------------------------------------------------------------------
  // Accessors & diagnostics
  // --------------------------------------------------------------------------------------------
  /**
   * @return the fittest (index 0) individual; requires the population to be
   *         sorted descending.
   */
  public Individual getFittest() {
    return individuals.get( 0 );
  }

  /**
   * Textual dump of mutation parameters followed by individuals from worst to
   * best.
   * <p>
   * Useful for quick external inspection of the current state.</p>
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append( "Mutation Exponent (beta): " ).append( this.mutationExponent )
      .append( "\tMax Mutation Intensity (G): " ).append(
      this.globalSearchIntensity )
      .append( "\n" );

    // From worst (last) to best (0), mirroring the original implementation.
    for( int i = individuals.size() - 1; i >= 0; i-- ) {
      sb.append( i ).append( "\t" ).append( individuals.get( i ) )
        .append( "\n" );
    }
    return sb.toString();
  }

  /**
   * @return current population size (N).
   */
  public int getSize() {
    return this.individuals.size();
  }

  /**
   * Random-access getter for an individual by index.
   * <p>
   * Note: the logical “rank” equals the index only immediately after sorting &
   * rank assignment.</p>
   */
  public Individual getIndividual( int index ) {
    return this.individuals.get( index );
  }

}
