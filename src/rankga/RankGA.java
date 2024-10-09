package rankga;

import Problems.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Random;
import static rankga.ConvertTime.convertMillisToTimeFormat;

/**
 * RankGA - Genetic Algorithm for Optimization
 *
 * This Java program implements a Genetic Algorithm (GA) framework for solving
 * optimization problems. Genetic Algorithms are a class of search heuristics
 * inspired by the process of natural selection. They evolve a population of
 * individuals over generations to find solutions that optimize a given fitness
 * function.
 *
 * Key Features: - Patience for terminating the algorithm after a specified
 * time. - Logging and reporting of progress, fitness values, and solution data.
 *
 * Usage: To use this program, you need to define a specific problem by creating
 * an instance of a class that implements the Problem interface. Then, customize
 * the parameters and run the algorithm.
 *
 * Author: Jorge Cervantes Affiliation: Universidad Autónoma Metropolitana,
 * Mexico City
 *
 * @see Problem
 * @see Population
 */
public class RankGA {

  // Maximum time without improvement before the algorithm stops (in milliseconds).
  private static final long PATIENCE = 60L * 1000L;

  static protected Population population;
  static protected Date startTime;
  static protected final Date runTime = new Date();
  static protected final Date tryTime = new Date();
  static protected Date notImproved = new Date();
  static protected Date lastDisplay = new Date();
  static protected Date now;
  static protected Individual lastBest;
  static protected int repetition;
  static protected long generation;

  public static void main( String[] args ) {
    // Define the problem to be optimized
    Problem problem = new ProblemNK();
    String problemRunName = problem.getProblemName() + "_" + System.currentTimeMillis();

    System.out.println( "Patience: " + convertMillisToTimeFormat( PATIENCE ) );
    System.out.println( "Problem: " + problemRunName );

    // Loop for multiple repetitions to validate robustness
    for( repetition = 0;
         repetition < 10;
         repetition++ ) {
      startTime = new Date();
      initializePopulation( problem );
      lastBest = problem.getNewIndividual( population.getFittest() );

      generation = 1;
      notImproved = new Date();
      lastDisplay = new Date();
      now = new Date();

      // Log headers
      System.out.println(
        "t\tni\trep\tg\ts\tph\td\trank\tp\tfitness\textra\tgenes\tDateTime\tmil" );

      report( "S",
              problemRunName );

      // Main genetic algorithm loop
      do {
        evolvePopulation( problemRunName,
                          problem );
        now = new Date();

        // Display periodic progress
        displayProgress();

        // Adapt problem parameters based on the best solution
        problem.adapt( lastBest.getFitness() );

      }
      while( ( now.getTime() - notImproved.getTime() ) < PATIENCE
             && population.getFittest().getFitness() < problem.getGoalFt() );

      // Final reporting after completion of evolution
      report( "L",
              problemRunName );
    }
  }

  // Initializes the population for the problem
  private static void initializePopulation( Problem problem ) {
    population = new Population( 20,
                                 problem,
                                 true,
                                 new Random() );
    population.evaluate();
  }

  // Handles the process of evolving the population through selection, recombination, and mutation
  private static void evolvePopulation( String problemRunName,
                                        Problem problem ) {
    // Selection phase
    population.select();

    // Recombination phase
    population.recombinate();
    population.evaluate();
    checkImprovement( "R",
                      problemRunName,
                      problem );

    // Mutation phase
    population.mutate();
    population.evaluate();
    checkImprovement( "M",
                      problemRunName,
                      problem );
  }

  // Checks if there has been an improvement in the population
  private static void checkImprovement( String phase,
                                        String problemRunName,
                                        Problem problem ) {
    if( lastBest.getFitness() <= population.getFittest().getFitness()
        && lastBest.distanceSqTo( population.getFittest() ) > 0.0 ) {
      report( phase,
              problemRunName );
      lastBest = problem.getNewIndividual( population.getFittest() );
      notImproved = new Date();
      lastDisplay = new Date();
    }
    generation++;
  }

  // Displays progress periodically to the console
  private static void displayProgress() {
    if( ( now.getTime() - lastDisplay.getTime() ) > PATIENCE / 10 ) {
      lastDisplay = new Date();
      runTime.setTime( now.getTime() - startTime.getTime() );
      tryTime.setTime( now.getTime() - notImproved.getTime() );
      System.out.println( "\r" + convertMillisToTimeFormat( runTime.getTime() )
                          + " g:" + generation + " s:" + ( (float) generation / runTime.getTime() )
                          + " ni:" + convertMillisToTimeFormat(
          tryTime.getTime() ) + " --" );
      System.out.flush();
    }
  }

  // Logs the current state of the genetic algorithm
  static protected void report( String phase,
                                String problemName ) {
    Individual best = population.getFittest();
    double distance = best.distanceSqTo( lastBest );
    Date now = new Date();
    String reportString = String.format(
           "%s %s %d %d %9.7f %s %f\t%s\t%s\t%s %d\n",
           convertMillisToTimeFormat( now.getTime() - startTime.getTime() ),
           convertMillisToTimeFormat( now.getTime() - notImproved.getTime() ),
           repetition,
           generation,
           (float) generation / ( now.getTime() - startTime.getTime() ),
           phase,
           distance,
           best,
           best.genomeStr(),
           now,
           ( now.getTime() - startTime.getTime() ) );

    System.out.print( "\r" + reportString );
    System.out.flush();

    logToFile( problemName,
               reportString );
    logPopulation( problemName );
  }

  // Logs general report details to a file
  private static void logToFile( String problemName,
                                 String content ) {
    try( PrintWriter out = new PrintWriter( new BufferedWriter( new FileWriter(
                     problemName + ".txt",
                     true ) ) ) ) {
      out.print( content );
    }
    catch( IOException e ) {
      System.out.println( e );
    }
  }

  // Logs the population details to a file
  private static void logPopulation( String problemName ) {
    try( PrintWriter out = new PrintWriter( new BufferedWriter( new FileWriter(
                     problemName + "_" + repetition + ".txt",
                     false ) ) ) ) {
      for( int i = 0;
           i < population.getSize();
           i++ ) {
        out.println(
          population.getIndividual( i ) + "\t" + population.getIndividual( i ).genomeStr() );
      }
    }
    catch( IOException e ) {
      System.out.println( e );
    }
  }

}
