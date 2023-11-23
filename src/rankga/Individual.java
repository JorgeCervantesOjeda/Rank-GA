package rankga;

import java.util.Random;

/**
 * Represents an individual in the population.
 */
public class Individual {

  protected Gene[] genome; // The genes that make up the genome of this individual.
  protected Problem problem; // The problem associated with this individual.
  private StringBuilder extraString; // Extra information string for this individual.
  private double fitness; // The fitness value of this individual.
  private int rank; // The rank of this individual in the population.
  protected double p; // Mutation probability for this individual.
  private int mate; // Mate (partner) rank for recombination.
  protected Random randomizer; // Random number generator for randomization.
  protected double sum; // Sum of gene values (used in some strategies).

  /**
   * Create a new individual with a random or specified genome.
   *
   * @param problem   The problem associated with this individual.
   * @param randomize If true, create a random genome. Otherwise, use the
   *                  default values.
   * @param r         The random number generator.
   */
  public Individual( Problem problem,
                     boolean randomize,
                     Random r ) {
    this.problem = problem;
    genome = new Gene[ problem.getGenomeLength() ];

    for( int i = 0;
         i < genome.length;
         i++ ) {
      genome[ i ] = problem.getNewGene( randomize,
                                        r );
    }
    this.randomizer = r;
  }

  /**
   * Create a copy of another individual.
   *
   * @param other The individual to copy.
   */
  public Individual( Individual other ) {
    this.problem = other.problem;
    genome = new Gene[ problem.getGenomeLength() ];

    for( int i = 0;
         i < genome.length;
         i++ ) {
      genome[ i ] = problem.getNewGene( other.getGene( i ) );
    }

    this.extraString = new StringBuilder();
    this.fitness = other.fitness;
    this.rank = -1;
    this.p = 0.0;
    this.mate = -1;
    this.randomizer = other.randomizer;
  }

  /**
   * Append extra information to the extra string.
   *
   * @param s The string to append.
   */
  public void appendExtraString( String s ) {
    this.extraString.append( s );
  }

  /**
   * Set the extra string for this individual.
   *
   * @param s The extra information string.
   */
  public void setExtraString( StringBuilder s ) {
    this.extraString = s;
  }

  /**
   * Set a gene at a specific index in the genome.
   *
   * @param i The index.
   * @param g The gene to set.
   */
  public void setGene( int i,
                       Gene g ) {
    this.genome[ i ] = g;
  }

  /**
   * Update the fitness of this individual by evaluating the problem's fitness
   * function.
   *
   * @return The updated fitness value.
   */
  public double updateFitness() {
    this.fitness = problem.fitness( this );
    return this.fitness;
  }

  /**
   * Get the fitness value of this individual.
   *
   * @return The fitness value.
   */
  public double getFitness() {
    return fitness;
  }

  /**
   * Mutate the genes of this individual with a given mutation probability.
   *
   * @param probability The mutation probability.
   */
  public void mutate( double probability ) {
    this.p = probability;
    for( int i = 0;
         i < genome.length;
         i++ ) {
      genome[ i ].mutate( probability );
    }
  }

  /**
   * Recombine genes with another individual. Genes are swapped randomly based
   * on a 50% chance.
   *
   * @param that The other individual to recombine with.
   */
  public void recombinate( Individual that ) {
    Gene geneA, geneB;
    this.mate = that.rank;
    that.mate = this.rank;
    for( int i = 0;
         i < genome.length;
         i++ ) {
      if( randomizer.nextDouble() < 0.5 ) {
        geneA = this.getGene( i );
        geneB = that.getGene( i );
        this.setGene( i,
                      geneB );
        that.setGene( i,
                      geneA );
      }
    }
  }

  /**
   * Get a string representation of the genome.
   *
   * @return The genome as a formatted string.
   */
  public String genomeStr() {
    String s = "";
    String g;
    for( int i = 0;
         i < genome.length;
         i++ ) {
      g = String.format( "%02d",
                         genome[ i ].getIntValue() );
      s = s + ( i % problem.getDisplayModulus() == 0
                ? " "
                : "" ) + g;
    }
    return s;
  }

  /**
   * Get a string representation of this individual, including rank, mutation
   * probability, fitness, and extra information.
   *
   * @return The individual as a formatted string.
   */
  @Override
  public String toString() {
    String s = rank
               + "\t" + String.format( "%18.17f",
                                       p )
               + " " + String.format( "%18.17e",
                                      fitness )
               + " " + extraString;
    return s;
  }

  /**
   * Get a gene at a specific index in the genome.
   *
   * @param i The index of the gene to retrieve.
   *
   * @return The gene at the specified index.
   */
  public Gene getGene( int i ) {
    return this.genome[ i ];
  }

  /**
   * Calculate the squared Euclidean distance between this individual and
   * another individual.
   *
   * @param other The other individual to compare.
   *
   * @return The squared Euclidean distance.
   */
  public double distanceSqTo( Individual other ) {
    double sumSq = 0;
    double d;
    for( int i = 0;
         i < genome.length;
         i++ ) {
      d = genome[ i ].distanceTo( other.genome[ i ] );
      sumSq += d * d;
    }
    return sumSq;
  }

  /**
   * Set the rank of this individual.
   *
   * @param i The rank to set.
   */
  protected void setRank( int i ) {
    this.rank = i;
  }

  /**
   * Calculate the sum of gene values for this individual.
   *
   * @return The sum of gene values.
   */
  public double avg() {
    sum = 0.0;
    for( int i = 0;
         i < genome.length;
         i++ ) {
      sum += genome[ i ].getDoubleValue();
    }
    return sum / genome.length;
  }

  public double std() {
    double avg = avg();
    double sumDiffSqr = 0;
    double diff;
    for( int i = 0;
         i < genome.length;
         i++ ) {
      diff = genome[ i ].getDoubleValue() - avg;
      sumDiffSqr += diff * diff;
    }
    return Math.sqrt( sumDiffSqr / genome.length );
  }

}
