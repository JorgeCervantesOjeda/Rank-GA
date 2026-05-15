// C:/Users/usuario/ownCloud2/RankGA/src/rankga/AdaptiveProblem.java
// Optional problem extension for adaptive parameters during a RankGA run.
package rankga;

/**
 * AdaptiveProblem - Optional extension for problems that adjust parameters
 * during the evolutionary run.
 *
 * Implement this interface only when the problem has state that should change
 * across generations.
 */
public interface AdaptiveProblem {

  /**
   * Adapt problem-specific parameters based on the current best fitness.
   *
   * @param bestFitness best fitness value observed so far
   */
  void adapt( double bestFitness );

  /**
   * Adapt problem-specific parameters using the current best individual.
   *
   * @param bestIndividual best individual in the current population
   */
  default void adapt( Individual bestIndividual ) {
    if( bestIndividual == null ) {
      throw new IllegalArgumentException( "bestIndividual must not be null" );
    }
    adapt( bestIndividual.getFitness() );
  }

}
