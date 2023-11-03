package rankga;

import java.util.Random;

/**
 * Problem - Interface for Optimization Problems
 *
 * This interface defines the contract for optimization problems that can be
 * solved using the RankGA genetic algorithm framework. Implementations of this
 * interface should provide methods for evaluating the fitness of individuals,
 * adapting parameters, and creating new individuals and genes.
 *
 * Author: Jorge Cervantes Affiliation: Universidad Autónoma Metropolitana,
 * Mexico City
 */
public interface Problem {

  /**
   * Adapt problem-specific parameters based on the best fitness value.
   *
   * @param bestFitness The best fitness value achieved.
   */
  public void adapt( double bestFitness );

  /**
   * Calculate the fitness of an individual for this problem.
   *
   * @param individual The individual to evaluate.
   *
   * @return The fitness value of the individual.
   */
  public double fitness( Individual individual );

  /**
   * Get the name of the optimization problem.
   *
   * @return The name of the problem.
   */
  public String getProblemName();

  /**
   * Get the length of the genome for individuals in this problem.
   *
   * @return The length of the genome.
   */
  public int getGenomeLength();

  /**
   * Create a new gene for this problem. Optionally randomize its value.
   *
   * @param randomize Whether to randomize the gene's value.
   * @param r         The random number generator to use.
   *
   * @return A new gene.
   */
  public Gene getNewGene( boolean randomize,
                          Random r );

  /**
   * Create a new gene based on an existing gene.
   *
   * @param gene The source gene.
   *
   * @return A new gene based on the source gene.
   */
  public Gene getNewGene( Gene gene );

  /**
   * Get the target fitness value to achieve in this problem.
   *
   * @return The target fitness value.
   */
  public double getGoalFt();

  /**
   * Get the display modulus for reporting progress.
   *
   * @return The display modulus.
   */
  public int getDisplayModulus();

  /**
   * Create a new individual for this problem. Optionally randomize its genome.
   *
   * @param randomize Whether to randomize the individual's genome.
   * @param r         The random number generator to use.
   *
   * @return A new individual.
   */
  public Individual getNewIndividual( boolean randomize,
                                      Random r );

  /**
   * Create a new individual based on an existing individual.
   *
   * @param individual The source individual.
   *
   * @return A new individual based on the source individual.
   */
  public Individual getNewIndividual( Individual individual );

}
