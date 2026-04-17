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

}
