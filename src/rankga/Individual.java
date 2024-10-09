package rankga;

import java.util.Random;

/**
 * Individual - Represents an individual in the population used for genetic
 * algorithms.
 *
 * This class provides methods for creating individuals, manipulating genomes,
 * and evaluating fitness. Each individual is composed of an array of genes,
 * which can mutate and recombine.
 *
 * Author: Jorge Cervantes Affiliation: Universidad Autónoma Metropolitana,
 * Mexico City
 */
public class Individual {

  protected Gene[] genome; // The genes that make up the genome of this individual.
  protected Problem problem; // The problem associated with this individual.
  private StringBuilder extraString; // Extra information string for this individual.
  private double fitness; // The fitness value of this individual.
  private int rank; // The rank of this individual in the population.
  protected double mutationProbability; // Mutation probability for this individual.
  private int mateRank; // Mate (partner) rank for recombination.
  protected Random randomizer; // Random number generator for randomization.

  /**
   * Create a new individual with a random or specified genome.
   *
   * @param problem   The problem associated with this individual.
   * @param randomize If true, create a random genome. Otherwise, use the
   *                  default values.
   * @param random    The random number generator.
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
   * Create a copy of another individual.
   *
   * @param other The individual to copy.
   */
  public Individual( Individual other ) {
    this.problem = other.problem;
    this.genome = new Gene[ problem.getGenomeLength() ];
    this.randomizer = other.randomizer;
    this.extraString = new StringBuilder();
    this.fitness = other.fitness;
    this.rank = -1;
    this.mutationProbability = 0.0;
    this.mateRank = -1;
    copyGenome( other );
  }

  /**
   * Initialize the genome with either random values or default values.
   *
   * @param randomize Whether to randomize the genome.
   */
  private void initializeGenome( boolean randomize ) {
    for( int i = 0;
         i < genome.length;
         i++ ) {
      genome[ i ] = problem.getNewGene( randomize,
                                        randomizer );
    }
  }

  /**
   * Copy the genome from another individual.
   *
   * @param other The individual to copy the genome from.
   */
  private void copyGenome( Individual other ) {
    for( int i = 0;
         i < genome.length;
         i++ ) {
      genome[ i ] = problem.getNewGene( other.getGene( i ) );
    }
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
   * @param index The index.
   * @param gene  The gene to set.
   */
  public void setGene( int index,
                       Gene gene ) {
    this.genome[ index ] = gene;
  }

  /**
   * Update the fitness of this individual by evaluating the problem's fitness
   * function.
   *
   * @return The updated fitness value.
   */
  public double updateFitness() {
    this.setExtraString( new StringBuilder( "" ) );
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
    this.mutationProbability = probability;
    for( Gene gene
         : genome ) {
      gene.mutate( probability );
    }
  }

  /**
   * Recombine genes with another individual. Genes are swapped randomly based
   * on a 50% chance.
   *
   * @param partner The other individual to recombine with.
   */
  public void recombinate( Individual partner ) {
    this.mateRank = partner.rank;
    partner.mateRank = this.rank;

    for( int i = 0;
         i < genome.length;
         i++ ) {
      if( randomizer.nextDouble() < 0.5 ) {
        Gene tempGene = this.getGene( i );
        this.setGene( i,
                      partner.getGene( i ) );
        partner.setGene( i,
                         tempGene );
      }
    }
  }

  /**
   * Get a string representation of the genome.
   *
   * @return The genome as a formatted string.
   */
  public String genomeStr() {
    StringBuilder genomeString = new StringBuilder();
    for( int i = 0;
         i < genome.length;
         i++ ) {
      String geneStr = String.format( "%01d",
                                      genome[ i ].getIntValue() );
      genomeString.append( ( i % problem.getDisplayModulus() == 0
                             ? " "
                             : "" ) + geneStr );
    }
    return genomeString.toString();
  }

  /**
   * Get a string representation of this individual, including rank, mutation
   * probability, fitness, and extra information.
   *
   * @return The individual as a formatted string.
   */
  @Override
  public String toString() {
    return String.format( "%d\t%18.17f %18.17e %s",
                          rank,
                          mutationProbability,
                          fitness,
                          extraString );
  }

  /**
   * Get a gene at a specific index in the genome.
   *
   * @param index The index of the gene to retrieve.
   *
   * @return The gene at the specified index.
   */
  public Gene getGene( int index ) {
    return this.genome[ index ];
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
    for( int i = 0;
         i < genome.length;
         i++ ) {
      double distance = genome[ i ].distanceTo( other.genome[ i ] );
      sumSq += distance * distance;
    }
    return sumSq;
  }

  /**
   * Set the rank of this individual.
   *
   * @param rank The rank to set.
   */
  protected void setRank( int rank ) {
    this.rank = rank;
  }

  /**
   * Calculate the average value of the genes in this individual's genome.
   *
   * @return The average value of the genes.
   */
  public double avg() {
    double sum = 0.0;
    for( Gene gene
         : genome ) {
      sum += gene.getDoubleValue();
    }
    return sum / genome.length;
  }

  /**
   * Calculate the standard deviation of gene values for this individual.
   *
   * @return The standard deviation of gene values.
   */
  public double std() {
    double avg = avg();
    double sumDiffSqr = 0;
    for( Gene gene
         : genome ) {
      double diff = gene.getDoubleValue() - avg;
      sumDiffSqr += diff * diff;
    }
    return Math.sqrt( sumDiffSqr / genome.length );
  }

}
