package rankga;

import java.util.ArrayList;
import java.util.Random;

/**
 * Population - Class Representing a Population of Individuals
 *
 * This class represents a population of individuals for use in genetic
 * algorithm-based optimization. It contains methods for population
 * initialization, evaluation, selection, recombination, mutation, and parameter
 * updates.
 *
 * Author: Jorge Cervantes Affiliation: Universidad Autónoma Metropolitana,
 * Mexico City
 */
public class Population {

  private ArrayList<Individual> theIndividuals;
  private final double K; // Selective pressure
  private final double exponent;
  private final double maxMut;
  private final Problem problem;
  private final Random randomizer;

  /**
   * Constructor for the Population class.
   *
   * @param numIndividuos The number of individuals in the population.
   * @param problem       The problem being optimized.
   * @param randomize     Whether to randomize the initial population.
   * @param r             The random number generator.
   */
  public Population( int numIndividuos,
                     Problem problem,
                     boolean randomize,
                     Random r ) {
    this.problem = problem;
    this.randomizer = r;
    theIndividuals = new ArrayList<>();
    for( int i = 0;
         i < numIndividuos;
         i++ ) {
      theIndividuals.add( problem.getNewIndividual( randomize,
                                                    r ) );
    }
    K = 3.0;
    maxMut = 0.1;
    exponent = Math.log( problem.getGenomeLength() * maxMut ) / Math.log(
    numIndividuos - 1 );
    System.out.println( "NumIndividuos = " + numIndividuos );
    System.out.println( "K = " + K );
    System.out.println( "maxMut = " + maxMut );
    System.out.println( "exponent = " + exponent );
  }

  /**
   * Evaluate the fitness of individuals in the population sequentially.
   */
  public void evaluate() {
    for( Individual individual
         : theIndividuals ) {
      individual.updateFitness();
    }
    this.sort();
  }

  /**
   * Sort the individuals in the population based on their fitness values in
   * descending order.
   */
  private void sort() {
    theIndividuals.sort( ( a, b )
      -> compareIndividuals( a,
                             b ) );
  }

  /**
   * Compare two individuals based on their fitness values.
   *
   * @param a The first individual.
   * @param b The second individual.
   *
   * @return -1 if a is fitter, 1 if b is fitter, 0 if they have equal fitness.
   */
  private int compareIndividuals( Individual a,
                                  Individual b ) {
    return Double.compare( b.getFitness(),
                           a.getFitness() );
  }

  /**
   * Implement the selection phase of the genetic algorithm to create clones of
   * individuals based on their fitness and selective pressure.
   */
  public void select() {
    double numClones;
    ArrayList<Individual> clones = new ArrayList<>();
    int[] totalClones = new int[ theIndividuals.size() ];
    int totalCloneCount = 0;

    // First phase: Calculate the base number of clones for each individual
    for( int i = 0;
         i < theIndividuals.size();
         i++ ) {
      double r = i / (double) theIndividuals.size();
      numClones = K * Math.pow( 1 - r,
                                K - 1 );
      totalClones[ i ] = (int) Math.floor( numClones );
      totalCloneCount += totalClones[ i ];
    }

    // Second phase: Calculate the extra clones based on the fractional part
    while( totalCloneCount < theIndividuals.size() ) {
      for( int i = 0;
           i < theIndividuals.size() && totalCloneCount < theIndividuals.size();
           i++ ) {
        double r = i / (double) theIndividuals.size();
        numClones = K * Math.pow( 1 - r,
                                  K - 1 );
        double extraProbability = numClones - Math.floor( numClones );

        if( this.randomizer.nextDouble() < extraProbability ) {
          totalClones[ i ]++;
          totalCloneCount++;
        }
      }
    }

    // Generate all the clones based on the calculated totalClones array
    for( int i = 0;
         i < theIndividuals.size();
         i++ ) {
      for( int j = 0;
           j < totalClones[ i ];
           j++ ) {
        clones.add( problem.getNewIndividual( theIndividuals.get( i ) ) );
      }
    }

    theIndividuals = clones;
  }

  /**
   * Implement the recombination phase of the genetic algorithm. Individuals are
   * recombined to create new individuals.
   */
  public void recombinate() {
    // Set the rank for each individual (used for reporting purposes)
    for( int i = 0;
         i < theIndividuals.size();
         i++ ) {
      theIndividuals.get( i ).setRank( i );
    }

    // Recombination is done in pairs
    for( int i = 0;
         i < theIndividuals.size() - 1;
         i += 2 ) {
      theIndividuals.get( i ).recombinate( theIndividuals.get( i + 1 ) );
    }
  }

  /**
   * Mutate the individuals in the population based on their rank and mutation
   * parameters.
   */
  public void mutate() {
    // Set the rank for each individual (used for reporting purposes)
    for( int i = 0;
         i < theIndividuals.size();
         i++ ) {
      theIndividuals.get( i ).setRank( i );
    }

    // Apply mutation based on the individual's rank
    for( int i = 0;
         i < theIndividuals.size();
         i++ ) {
      double r = i / (double) theIndividuals.size();
      theIndividuals.get( i ).mutate( maxMut * Math.pow( r,
                                                         exponent ) );
    }
  }

  /**
   * Get the fittest individual in the population.
   *
   * @return The fittest individual.
   */
  public Individual getFittest() {
    return theIndividuals.get( 0 );
  }

  /**
   * Get a string representation of the population, including its mutation
   * parameters.
   *
   * @return A string representing the population.
   */
  @Override
  public String toString() {
    StringBuilder s = new StringBuilder(
                  "e:" + this.exponent + "\tm:" + this.maxMut + "\n" );

    for( int i = theIndividuals.size() - 1;
         i >= 0;
         i-- ) {
      s.append( i ).append( "\t" ).append( theIndividuals.get( i ) ).append(
        "\n" );
    }
    return s.toString();
  }

  /**
   * Get the mutation exponent parameter.
   *
   * @return The mutation exponent.
   */
  public double getExponent() {
    return exponent;
  }

  /**
   * Get the maximum mutation parameter.
   *
   * @return The maximum mutation parameter.
   */
  public double getMaxMutation() {
    return maxMut;
  }

  /**
   * Get the number of individuals in the population.
   *
   * @return The size of the population.
   */
  public int getSize() {
    return this.theIndividuals.size();
  }

  /**
   * Get an individual from the population by index.
   *
   * @param _i The index of the individual to retrieve.
   *
   * @return The individual at the specified index.
   */
  public Individual getIndividual( int _i ) {
    return this.theIndividuals.get( _i );
  }

}
