package rankga;

import java.util.Random;

/**
 * Problem — Interface for Optimization Problems.
 *
 * Defines the contract for problems solvable by the RankGA framework. A problem
 * must provide fitness evaluation, optional parameter adaptation, and factories
 * for individuals and genes.
 *
 * Author: Jorge Cervantes — Universidad Autónoma Metropolitana, Mexico City
 */
public interface Problem {

  /**
   * Adapt problem-specific parameters based on the current best fitness.
   *
   * @param bestFitness best fitness value observed so far
   */
  void adapt( double bestFitness );

  /**
   * Compute the fitness of the given individual.
   *
   * @param individual individual to evaluate
   *
   * @return fitness value
   */
  double fitness( Individual individual );

  /**
   * @return the displayable name of the problem
   */
  String getProblemName();

  /**
   * @return the genome length (number of genes) for this problem
   */
  int getGenomeLength();

  /**
   * Create a new gene. If {@code randomize} is true, initialize it randomly.
   *
   * @param randomize whether to randomize the new gene
   * @param r         PRNG to use
   *
   * @return a new gene instance
   */
  Gene getNewGene( boolean randomize,
                   Random r );

  /**
   * Create a new gene as a copy (or projection) of {@code gene}.
   *
   * @param gene source gene
   *
   * @return a new gene derived from {@code gene}
   */
  Gene getNewGene( Gene gene );

  /**
   * @return the target/goal fitness; evolution may stop once this is reached
   */
  double getGoalFt();

  /**
   * @return modulus used for grouping when rendering genomes (logging/prints)
   */
  int getDisplayModulus();

  /**
   * Create a new individual. If {@code randomize} is true, initialize its
   * genome randomly.
   *
   * @param randomize whether to randomize the genome
   * @param r         PRNG to use
   *
   * @return a new individual
   */
  Individual getNewIndividual( boolean randomize,
                               Random r );

  /**
   * Create a new individual as a deep copy of {@code individual}.
   *
   * @param individual source individual
   *
   * @return a new copied individual
   */
  Individual getNewIndividual( Individual individual );

  /**
   * Get the local search mutation intensity.
   * <p>
   * This is the lower bound used in Rank-GA’s rank-based schedule: the mutation
   * exponent is {@code beta = ln(G/L) / ln(N-1)}, and the per-individual
   * intensity is {@code I_i = G * (rank_i)^beta } with {@code rank_i = i/N}.
   * Implementations should interpret “intensity” consistently with their gene
   * operators (e.g., step size, noise scale, categorical flip strength).
   *
   * @return local (minimum) mutation intensity L
   */
  double getLocalSearchIntensity();

  /**
   * Get the global search mutation intensity.
   * <p>
   * This is the upper bound used in Rank-GA’s rank-based schedule (see above).
   * Must satisfy {@code G > L} to ensure {@code beta > 0} so the best-ranked
   * individual receives zero intensity and intensity increases with rank.
   *
   * @return global (maximum) mutation intensity G
   */
  double getGlobalSearchIntensity();

}
