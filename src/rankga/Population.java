package rankga;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
  private double masApto;
  private int numGen;
  private final double avgFtRand;
  private double exponent;
  private double maxMut;
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
    numGen = 0;
    maxMut = 2.0 / problem.getGenomeLength();
    exponent = Math.log( problem.getGenomeLength() * maxMut ) / Math.log(
    numIndividuos - 1 );
    avgFtRand = 0;
    System.out.println( "NumIndividuos = " + numIndividuos );
    System.out.println( "K = " + K );
    System.out.println( "maxMut = " + maxMut );
    System.out.println( "exponent = " + exponent );

  }

  /**
   * Evaluate the fitness of individuals in the population in parallel using
   * multi-threading.
   */
  void evaluateParallel() {
    final int nThreads = theIndividuals.size();
    final ExecutorService executor = Executors.newFixedThreadPool( 2 );
    final List<Future<Double>> futures = new ArrayList<>();

    for( Individual individual
         : theIndividuals ) {
      futures.add( executor.submit( ()
        -> individual.updateFitness() ) );
    }

    futures.forEach( ( Future<Double> f )
      -> {
        try {
          Double aResult = f.get();
        } catch( InterruptedException |
                 ExecutionException ex ) {
          System.out.println( "-----<<< " + ex + ">>>-----" );
        }
      } );

    executor.shutdown();
  }

  /**
   * Evaluate the fitness of individuals in the population sequentially.
   */
  public void evaluate() {
    for( int i = 0;
         i < theIndividuals.size();
         i++ ) {
      theIndividuals.get( i ).updateFitness();
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
    if( a.getFitness() > b.getFitness() ) {
      return -1;
    }
    if( a.getFitness() < b.getFitness() ) {
      return 1;
    }
    return 0;
  }

  /**
   * Implement the selection phase of the genetic algorithm to create clones of
   * individuals based on their fitness and selective pressure.
   */
  public void select() {
    double numClones;
    double r;
    ArrayList<Individual> clones = new ArrayList<>();

    for( int i = 0;
         i < theIndividuals.size();
         i++ ) {
      r = i / (double) theIndividuals.size();
      numClones = K * Math.pow( 1 - r,
                                K - 1 );

      for( int j = 0;
           j < Math.floor( numClones );
           j++ ) {
        clones.add( problem.getNewIndividual( theIndividuals.get( i ) ) );
      }
    }

    double extraProbability;
    do {
      for( int i = 0;
           i < theIndividuals.size() && clones.size() < theIndividuals.size();
           i++ ) {
        r = i / (double) theIndividuals.size();
        numClones = K * Math.pow( 1 - r,
                                  K - 1 );
        extraProbability = numClones - Math.floor( numClones );
        if( this.randomizer.nextDouble() < extraProbability ) {
          clones.add( problem.getNewIndividual( theIndividuals.get( i ) ) );
        } else if( extraProbability > 0
                   && i == 0 ) {
          //System.out.println( "No hubo clon extra del mejor." );
        }
      }
    } while( clones.size() < theIndividuals.size() );

    theIndividuals = clones;
    this.sort();
  }

  /**
   * Implement the recombination phase of the genetic algorithm. Individuals are
   * recombined to create new individuals.
   */
  public void recombinate() {
    for( int i = 0;
         i < theIndividuals.size();
         i++ ) {
      theIndividuals.get( i ).setRank( i );
    }

    for( int i = 0;
         i < theIndividuals.size() - 1;
         i = i + 2 ) {
      theIndividuals.get( i ).recombinate( theIndividuals.get( i + 1 ) );
    }
  }

  /**
   * Update mutation parameters based on various strategies.
   *
   * @param a The strategy index: -1 for random search, 0 for Rank GA, 1 for HS
   *          Rank GA by exponent, 2 for HS Rank GA by maxMut.
   */
  public void updateMutationParameters( int a ) {
    numGen++;

    switch( a ) {
      case -1: // Random search
        maxMut = theIndividuals.size();
        break;
      case 0: // Rank GA
        break;
      case 1: // HS Rank GA by exponent
        updateMutExponent();
        break;
      case 2: // HS Rank GA by maxMut
        updateMaxMut();
        break;
    }
  }

  /**
   * Update the maximum mutation parameter based on population fitness.
   */
  private void updateMaxMut() {
    double popFraction = 0.7;
    double baseFt = theIndividuals.get(
           (int) ( theIndividuals.size() * popFraction ) - 1 ).getFitness();

    if( baseFt <= avgFtRand ) {
      if( maxMut > 0.1 * 0.98 ) {
        maxMut *= 0.98;
      }
    } else if( maxMut < 100 / 1.01 ) {
      maxMut *= 1.01;
    }
  }

  /**
   * Update the mutation exponent parameter based on population fitness.
   */
  private void updateMutExponent() {
    double popFraction = 0.7;
    double baseFt = theIndividuals.get(
           (int) ( theIndividuals.size() * popFraction ) - 1 ).getFitness();

    if( baseFt <= avgFtRand ) {
      if( exponent < 5 / 1.1 ) {
        exponent *= 1.01;
      }
    } else if( exponent > 0.1 * 0.9 ) {
      exponent *= 0.99;
    }
  }

// (Other methods are explained in the code with their corresponding comments)
  /**
   * Mutate the individuals in the population based on their rank and mutation
   * parameters.
   */
  public void mutate() {
    for( int i = 0;
         i < theIndividuals.size();
         i++ ) {
      theIndividuals.get( i ).setRank( i );
    }
    double r;
    int indexRand = theIndividuals.size();

    for( int i = 0;
         i < theIndividuals.size();
         i++ ) {
      if( i > 0 && i < indexRand && theIndividuals.get( i ).getFitness() < avgFtRand ) {
        indexRand = i;
      }

      if( true || i < indexRand ) {
        r = i / (double) ( theIndividuals.size() - 1 );
      } else {
        r = indexRand / (double) theIndividuals.size();
      }

      theIndividuals.get( i ).mutate( 0.0 + maxMut * Math.pow( r,
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
    String s = "e:" + this.exponent + "\tm:" + this.maxMut + "\tafr:" + this.avgFtRand + "\n";

    for( int i = theIndividuals.size() - 1;
         i >= 0;
         i-- ) {
      s = s + i + "\t" + theIndividuals.get( i ) + "\n";
    }
    return s;
  }

  /**
   * Generate a random individual and calculate its fitness for parameter
   * updates.
   *
   * @return The fitness of a random individual.
   */
  private double ftRand() {
    Individual ind = problem.getNewIndividual( true,
                                               randomizer );
    return ind.updateFitness();
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
   * Get the average random fitness value.
   *
   * @return The average random fitness value.
   */
  public double getAvgFtRand() {
    return this.avgFtRand;
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
