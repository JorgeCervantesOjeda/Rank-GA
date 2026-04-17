package Problems;

import java.util.Random;
import rankga.Gene;
import rankga.Individual;
import rankga.Problem;

/**
 * ProblemRastrigin - Defines a Rastrigin function optimization problem for use with genetic algorithms. Implements the Problem interface, providing methods for
 * generating individuals, evaluating fitness, and setting up the Rastrigin function environment.
 *
 * Author: Jorge Cervantes Affiliation: Universidad Autónoma Metropolitana, Mexico City
 */
public class ProblemRastrigin
  implements Problem {

  private final int DIMENSIONS; // Number of dimensions for the Rastrigin function
  private final double A = 10; // Rastrigin function constant
  private final double LOWER_BOUND = -10.0; // Lower bound of the search space for each dimension
  private final double UPPER_BOUND = 10.0; // Upper bound of the search space for each dimension
  private final double PENALTY_FACTOR = 1000.0; // Penalty factor for out-of-bound genes

  /**
   * Constructor for the ProblemRastrigin class. Initializes the number of dimensions for the Rastrigin function.
   *
   * @param dimensions The number of dimensions for the Rastrigin function.
   */
  public ProblemRastrigin( int dimensions ) {
    this.DIMENSIONS = dimensions;
  }

  /**
   * Calculate the fitness of a given individual based on the Rastrigin function.
   *
   * @param individual The individual whose fitness is to be calculated.
   *
   * @return The calculated fitness value.
   */
  @Override
  public double fitness( Individual individual ) {
    double fitness = 0.0;
    double penalty = 0.0;

    // Calculate the Rastrigin function value and add penalty for out-of-bound genes
    for( int i = 0;
         i < DIMENSIONS;
         i++ ) {
      double geneValue = individual.getGene( i ).getValue();

      // Add penalty if gene value is out of the feasible interval
      if( geneValue < LOWER_BOUND || geneValue > UPPER_BOUND ) {
        penalty += Math.abs(
        geneValue
        - ( geneValue < LOWER_BOUND
            ? LOWER_BOUND
            : UPPER_BOUND ) ) * PENALTY_FACTOR;
      }

      fitness += geneValue * geneValue - A * Math.cos( 2 * Math.PI * geneValue );
    }

    fitness = A * DIMENSIONS + fitness + penalty; // Complete the Rastrigin function calculation with penalty
    individual.appendExtraString( "" + penalty );

    return -fitness; // Since we're maximizing, negate the value to convert to a minimization problem
  }

  @Override
  public double getGlobalSearchIntensity() {
    return ( this.UPPER_BOUND - this.LOWER_BOUND ) / 1.0;
    // a standard deviation because we use GeneDoublePrecision
  }

  @Override
  public double getLocalSearchIntensity() {
    return ( this.UPPER_BOUND - this.LOWER_BOUND ) / 1000000.0;
    // a standard deviation because we use GeneDoublePrecision
  }

  @Override
  public String getProblemName() {
    return "Rastrigin_" + DIMENSIONS;
  }

  @Override
  public int getGenomeLength() {
    return DIMENSIONS;
  }

  @Override
  public Gene getNewGene( boolean randomize,
                          Random r ) {
    return new GeneDoublePrecision( r,
                                    randomize
                                    ? this.getGlobalSearchIntensity()
                                    : 0.0 );
  }

  @Override
  public Gene getNewGene( Gene gene ) {
    return new GeneDoublePrecision( (GeneDoublePrecision) gene );
  }

  @Override
  public double getGoalFt() {
    return 0.0; // Goal is to find the global minimum of the Rastrigin function, which is 0
  }

  @Override
  public int getDisplayModulus() {
    return 1; // Display every 5 genes for visual clarity
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
