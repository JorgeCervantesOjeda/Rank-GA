package Problems;

import java.util.Random;
import rankga.Gene;
import rankga.Individual;
import rankga.Problem;

/**
 * ProblemNK - Defines a problem using an NK fitness landscape. Implements the
 * Problem interface, providing methods for generating individuals, evaluating
 * fitness, and adapting parameters.
 *
 * Author: Jorge Cervantes Affiliation: Universidad Autónoma Metropolitana,
 * Mexico City
 */
public class ProblemNK
  implements Problem {

  private final int N; // Number of elements in the genome
  private final int K; // Number of neighbors influencing each element
  private final double[][] values; // Precomputed random fitness values for each gene combination

  /**
   * Constructor for the ProblemNK class.
   *
   * @param n The number of genes in the genome.
   * @param k The number of interacting neighbors.
   */
  public ProblemNK() {
    this.N = 100;
    this.K = 3;
    this.values = new double[ N ][];
    Random r = new Random( 9 );

    // Initialize random fitness values for each possible gene combination
    for( int i = 0;
         i < N;
         i++ ) {
      values[ i ] = new double[ 1 << K ]; // Each gene has 2^K possible states
      for( int j = 0;
           j < ( 1 << K );
           j++ ) {
        values[ i ][ j ] = r.nextDouble();
      }
    }
  }

  /**
   * Calculate the fitness of a given individual.
   *
   * @param individual The individual whose fitness is to be calculated.
   *
   * @return The calculated fitness value.
   */
  @Override
  public double fitness( Individual individual ) {
    double totalFitness = 0;

    // Compute the fitness by summing the contribution of each gene and its neighbors
    for( int n = 0;
         n < N;
         n++ ) {
      totalFitness += values[ n ][ calculateInfluenceIndex( individual,
                                                            n ) ];
    }

    // Append extra information (optional)
    individual.appendExtraString( "_fitness: " + totalFitness );
    return totalFitness;
  }

  @Override
  public double getGlobalSearchIntensity() {
    return 0.5;
  }

  @Override
  public double getLocalSearchIntensity() {
    return 1.0 / this.getGenomeLength();
  }

  @Override
  public String getProblemName() {
    return "NK_" + N + "_" + K + "_" + System.currentTimeMillis();
  }

  @Override
  public int getGenomeLength() {
    return N;
  }

  @Override
  public Gene getNewGene( boolean randomize,
                          Random r ) {
    return new GeneInteger( 2,
                            randomize,
                            r ); // A gene can take two values (0 or 1)
  }

  @Override
  public Gene getNewGene( Gene gene ) {
    return new GeneInteger( (GeneInteger) gene );
  }

  @Override
  public double getGoalFt() {
    return N; // Arbitrary goal fitness for demonstration purposes
  }

  @Override
  public int getDisplayModulus() {
    return K; // Modulus used for displaying purposes
  }

  /**
   * Calculate the integer value representing the influence of the current gene
   * and its K neighbors.
   *
   * @param individual The individual whose genome is being analyzed.
   * @param n          The index of the current gene.
   *
   * @return The integer value representing the state of the gene and its
   *         neighbors.
   */
  private int calculateInfluenceIndex( Individual individual,
                                       int n ) {
    int value = 0;
    for( int i = 0;
         i < K;
         i++ ) {
      // Use modulo to wrap around the genome for neighbors
      value = ( value << 1 ) | (int) individual.getGene( ( n + i ) % N )
      .getValue();
    }
    return value;
  }

  @Override
  public Individual getNewIndividual( boolean randomize,
                                      Random random ) {
    return new Individual( this,
                           randomize,
                           random );
  }

  @Override
  public Individual getNewIndividual( Individual another ) {
    return new Individual( another );
  }

}
