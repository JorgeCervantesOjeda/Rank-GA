package rankga;

import java.util.Random;

/**
 * Individual — Represents a solution candidate in the population for a Genetic
 * Algorithm (GA).
 *
 * <h2>Design summary</h2>
 * <ul>
 * <li>An Individual holds a <b>genome</b> (array of {@link Gene}) that encodes
 * a solution.</li>
 * <li>Its quality is measured by a scalar <b>fitness</b> (higher is better, as
 * used by RankGA).</li>
 * <li>Operators:
 * <ul>
 * <li><b>Mutation</b>: driven by a per-individual <i>mutation intensity</i>
 * (double). This is
 * <b>not a Bernoulli probability per gene</b>. Each {@code Gene} interprets
 * intensity to scale the magnitude of change (e.g., step size, noise stddev,
 * flip strength).</li>
 * <li><b>Recombination</b> (uniform crossover): swap each locus with
 * probability 0.5 with a partner.</li>
 * </ul>
 * </li>
 * <li>Rank-based GA specifics:
 * <ul>
 * <li>Rank is assigned externally by {@code Population} before
 * recombination/mutation.</li>
 * <li>The GA computes an intensity schedule {@code I_i = G * (rank_i)^β} and
 * passes it to {@link #mutate(double)}.</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <h2>Important semantic conventions</h2>
 * <ul>
 * <li><b>Mutation intensity vs probability:</b> The parameter of
 * {@link #mutate(double)} and {@link Gene#mutate(double)} is an
 * <b>intensity</b>. Implementations should not treat it as a naive coin-flip
 * probability per gene unless they explicitly map intensity → probability
 * internally.</li>
 * <li><b>Deep copy:</b> the copy constructor duplicates the genome gene-by-gene
 * using {@code Problem.getNewGene(existingGene)}.</li>
 * <li><b>Logging:</b> {@link #toString()} prints rank, last mutation intensity,
 * fitness and any extra info accumulated.</li>
 * </ul>
 *
 * Author: Jorge Cervantes — Universidad Autónoma Metropolitana, Mexico City
 */
public class Individual {

  /**
   * The ordered list of genes that define this individual's genome (solution
   * encoding).
   */
  protected Gene[] genome;

  /**
   * The problem to which this individual belongs; provides factories and
   * fitness function.
   */
  protected Problem problem;

  /**
   * Additional info field (free-form) that operators or fitness can append to
   * for logging/debug.
   */
  private StringBuilder extraString;

  /**
   * Cached fitness value. It is updated by {@link #updateFitness()} and read by
   * the GA.
   */
  private double fitness;

  /**
   * Rank assigned by the GA (0 = best after sorting, N-1 = worst).
   * <p>
   * Used to produce rank-dependent operator behavior; set via package-private
   * {@link #setRank(int)}.</p>
   */
  private int rank;

  /**
   * The last mutation intensity applied to this individual (for
   * logging/inspection only).
   * <p>
   * <b>Not</b> a per-gene probability. This is the scalar the GA passes to
   * {@link #mutate(double)}.</p>
   */
  protected double mutationIntensity;

  /**
   * Rank of the partner used in the last recombination (for
   * logging/inspection).
   */
  private int mateRank;

  /**
   * RNG shared with the population/problem to ensure reproducibility and seed
   * control.
   */
  protected Random randomizer;

  // --------------------------------------------------------------------------------------------
  // Constructors
  // --------------------------------------------------------------------------------------------
  /**
   * Constructs a new individual, optionally randomizing its genome.
   *
   * @param problem   the owning problem instance; provides factories and
   *                  fitness
   * @param randomize if true, initialize each gene randomly; else use problem
   *                  defaults
   * @param random    the PRNG to use for genome initialization and later
   *                  operators
   */
  public Individual( Problem problem,
                     boolean randomize,
                     Random random ) {
    this.problem = problem;
    this.randomizer = random;
    this.genome = new Gene[ problem.getGenomeLength() ];
    this.extraString = new StringBuilder();
    initializeGenome( randomize );
  }

  /**
   * Deep-copy constructor.
   * <p>
   * Copies the problem reference, PRNG, cached fitness, and <b>deep-copies</b>
   * each gene by calling {@code problem.getNewGene(other.getGene(i))}. Rank and
   * mateRank are reset.</p>
   *
   * @param other source individual
   */
  public Individual( Individual other ) {
    this.problem = other.problem;
    this.genome = new Gene[ problem.getGenomeLength() ];
    this.randomizer = other.randomizer;
    this.extraString = new StringBuilder();
    this.fitness = other.fitness;      // copy cached fitness for convenience; can be recomputed later
    this.rank = -1;                    // rank will be set by Population
    this.mutationIntensity = 0.0;
    this.mateRank = -1;
    copyGenome( other );
  }

  /**
   * Initializes each locus with a fresh Gene from the problem; random or
   * default per flag.
   */
  private void initializeGenome( boolean randomize ) {
    for( int i = 0; i < genome.length; i++ ) {
      genome[ i ] = problem.getNewGene( randomize,
                                        randomizer );
    }
  }

  /**
   * Deep-copies the genome by asking the problem to create a new Gene from an
   * existing one.
   */
  private void copyGenome( Individual other ) {
    for( int i = 0; i < genome.length; i++ ) {
      genome[ i ] = problem.getNewGene( other.getGene( i ) );
    }
  }

  // --------------------------------------------------------------------------------------------
  // Fitness
  // --------------------------------------------------------------------------------------------
  /**
   * Recomputes and caches the fitness using
   * {@link Problem#fitness(Individual)}.
   * <p>
   * Also clears the {@link #extraString} so fresh operator/fitness notes can be
   * appended.</p>
   *
   * @return the updated fitness value
   */
  public double updateFitness() {
    this.setExtraString( new StringBuilder( "" ) );
    this.fitness = problem.fitness( this );
    return this.fitness;
  }

  /**
   * @return the last computed fitness value (call {@link #updateFitness()} to
   *         refresh)
   */
  public double getFitness() {
    return fitness;
  }

  // --------------------------------------------------------------------------------------------
  // Mutation & Recombination
  // --------------------------------------------------------------------------------------------
  /**
   * Mutates this individual by passing a scalar <b>intensity</b> to each gene's
   * {@link Gene#mutate(double)}.
   * <p>
   * <b>Contract:</b> intensity is a unitless scalar whose meaning is defined
   * consistently across gene implementations (e.g., as a step-size, noise
   * variance, or categorical flip strength).</p>
   *
   * <p>
   * <b>Important:</b> This is not a per-gene probability. If a gene
   * implementation wishes to use probabilistic flips, it should map intensity
   * to a probability internally (e.g., via a monotone function) and document
   * the mapping.</p>
   *
   * @param intensity mutation intensity computed by the GA (e.g., rank-based
   *                  schedule)
   */
  public void mutate( double intensity ) {
    this.mutationIntensity = intensity;  // recorded for logging/inspection
    for( Gene gene : genome ) {
      gene.mutate( intensity );
    }
  }

  /**
   * Uniform crossover with another individual.
   * <p>
   * For each locus {@code i}, swap genes with probability 0.5. This preserves
   * marginal distributions, mixes building blocks, and is symmetric w.r.t.
   * parents. The ranks of partners are stored for logging.</p>
   *
   * @param partner the mate with whom to exchange genetic material
   */
  public void recombinate( Individual partner ) {
    this.mateRank = partner.rank;
    partner.mateRank = this.rank;

    for( int i = 0; i < genome.length; i++ ) {
      if( randomizer.nextDouble() < 0.5 ) {
        Gene tempGene = this.getGene( i );
        this.setGene( i,
                      partner.getGene( i ) );
        partner.setGene( i,
                         tempGene );
      }
    }
  }

  // --------------------------------------------------------------------------------------------
  // Genome utilities & diagnostics
  // --------------------------------------------------------------------------------------------
  /**
   * @return a compact human-readable representation of the genome
   * <p>
   * Integers are printed as decimal; non-integers in scientific notation.
   * Groups of genes are separated using the problem-provided display
   * modulus.</p>
   */
  public String genomeStr() {
    StringBuilder genomeString = new StringBuilder();
    int displayModulus = problem.getDisplayModulus();

    for( int i = 0; i < genome.length; i++ ) {
      if( i % displayModulus == 0 && i != 0 ) {
        genomeString.append( " " );
      }
      double value = genome[ i ].getValue();
      String geneStr = ( value == Math.floor( value ) )
                       ? String.format( "%d",
                                        (int) value )     // integer-like
                       : String.format( "%.2e",
                                        value );        // floating-point
      genomeString.append( geneStr );
    }
    return genomeString.toString();
  }

  /**
   * Squared Euclidean distance between this genome and another.
   * <p>
   * Useful to detect actual improvements (non-zero distance) and for
   * diagnostics/diversity metrics.</p>
   */
  public double distanceSqTo( Individual other ) {
    double sumSq = 0;
    for( int i = 0; i < genome.length; i++ ) {
      double d = genome[ i ].distanceTo( other.genome[ i ] );
      sumSq += d * d;
    }
    return sumSq;
  }

  // --------------------------------------------------------------------------------------------
  // Misc accessors
  // --------------------------------------------------------------------------------------------
  /**
   * Append arbitrary text to the extra info buffer (operators/fitness may log
   * here).
   */
  public void appendExtraString( String s ) {
    this.extraString.append( s );
  }

  /**
   * Replace the extra info buffer.
   */
  public void setExtraString( StringBuilder s ) {
    this.extraString = s;
  }

  /**
   * Random-access getter for a specific gene.
   */
  public Gene getGene( int index ) {
    return this.genome[ index ];
  }

  /**
   * Random-access setter for a specific gene (used primarily by crossover).
   */
  public void setGene( int index,
                       Gene gene ) {
    this.genome[ index ] = gene;
  }

  /**
   * Package-level: Population assigns ranks before operators.
   */
  protected void setRank( int rank ) {
    this.rank = rank;
  }

  /**
   * @return arithmetic mean of gene values (quick scalar descriptor for
   *         logs/monitoring)
   */
  public double avg() {
    double sum = 0.0;
    for( Gene gene : genome ) {
      sum += gene.getValue();
    }
    return sum / genome.length;
  }

  /**
   * @return population (biased) std. dev. of gene values (quick dispersion
   *         descriptor)
   */
  public double std() {
    double mean = avg();
    double sumDiffSqr = 0;
    for( Gene gene : genome ) {
      double diff = gene.getValue() - mean;
      sumDiffSqr += diff * diff;
    }
    return Math.sqrt( sumDiffSqr / genome.length );
  }

  /**
   * String summary for logs:
   * <pre>
   * rank \t mutationIntensity \t fitness \t extra
   * </pre> Example:
   * <code>0\t0.12345678901234567  1.23456789012345678e+02  (notes)</code>
   */
  @Override
  public String toString() {
    return String.format( "%d, %18.17f, %18.17e, %s",
                          rank,
                          mutationIntensity,
                          fitness,
                          extraString );
  }

}
