package rankga;

import Problems.ProblemPseudoachromaticIndexConnex;
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
 * The key components of this program include: - Population: Representing a
 * population of individuals. - Problem: Describing the optimization problem to
 * be solved. - Selection, Recombination, and Mutation: Operators for evolving
 * the population.
 *
 * The program follows a generational model where each generation of individuals
 * is evolved using these operators.
 *
 * Key Features: - Patience for terminating the algorithm after a specified
 * time. - Logging and reporting of progress, fitness values, and solution data.
 *
 * Usage: To use this program, you need to define a specific problem by creating
 * an instance of a class that implements the Problem interface. Then, you can
 * customize the parameters and run the algorithm.
 *
 * Author: Jorge Cervantes Affiliation: Universidad Autónoma Metropolitana,
 * Mexico City
 *
 * Note: The code can be further improved and modularized for different problem
 * domains.
 *
 * @see Problem
 * @see Population
 */
public class RankGA {

  private static final long PATIENCE = 5L * 24L * 60L * 60L * 1000L; // 5 days

  private static Population population;
  private static Date startTime;
  private static final Date runTime = new Date();
  private static final Date tryTime = new Date();
  private static Date notImproved = new Date();
  private static Date lastDisplay = new Date();
  private static Date now;
  private static Individual lastBest;
  private static int repetition;
  private static long generation;

  public static void main( String[] args ) {

    System.out.println( "Patience: " + convertMillisToTimeFormat( PATIENCE ) );

    // Define the optimization problem
    Problem problem = new ProblemPseudoachromaticIndexConnex( 25,
                                                              1,
                                                              0.001,
                                                              1,
                                                              0.00000001,
                                                              0.00000000 );
    String problemRunName = problem.getProblemName() + "_" + System
           .currentTimeMillis();
    System.out.println( "Problem: " + problemRunName );

    for( repetition = 0;
         repetition < 100;
         repetition++ ) {
      startTime = new Date();

      // Initialize the population
      population = new Population( problem.getGenomeLength(),
                                   problem,
                                   true,
                                   new Random() );
      population.evaluate();
      lastBest = problem.getNewIndividual( population.getFittest() );

      generation = 1;
      notImproved = new Date();
      lastDisplay = new Date();
      now = new Date();

      System.out.println(
        "t\tni\trep\tg\ts\tph\td\trank\tp\tfitness\textra\tgenes\tDateTime\tmil" );

      // Report the initial state
      report( "S",
              problemRunName );

      do {
        // Selection phase
        population.select();
        // Recombination phase
        population.recombinate();
        population.evaluate();

        // Check if there's an improvement in fitness
        if( lastBest.getFitness() <= population.getFittest().getFitness()
            && lastBest.distanceSqTo( population.getFittest() ) > 0.0 ) {
          report( "R",
                  problemRunName );
          lastBest = problem.getNewIndividual( population.getFittest() );
          notImproved = new Date();
          lastDisplay = new Date();
        }
        generation++;

        // Mutation phase
        population.mutate();
        population.evaluate();
        population.updateMutationParameters( 0 ); // Hill Side Addition

        // Check if there's an improvement in fitness
        if( lastBest.getFitness() <= population.getFittest().getFitness()
            && lastBest.distanceSqTo( population.getFittest() ) > 0.0 ) {
          report( "M",
                  problemRunName );
          lastBest = problem.getNewIndividual( population.getFittest() );
          notImproved = new Date();
          lastDisplay = new Date();
        }
        generation++;
        now = new Date();

        // Display progress
        if( ( now.getTime() - lastDisplay.getTime() ) > PATIENCE / 10 ) {
          lastDisplay = new Date();
          runTime.setTime( now.getTime() - startTime.getTime() );
          tryTime.setTime( now.getTime() - notImproved.getTime() );
          System.out.println( "\r" + convertMillisToTimeFormat(
            runTime.getTime() )
                              + " g:" + generation + " s:" + ( (float) generation / runTime
                                                                .getTime() )
                              + " ni:" + convertMillisToTimeFormat(
              tryTime.getTime() ) + " --" );
          System.out.flush();
        }

        // Adaptation of parameters
        problem.adapt( lastBest.getFitness() );
      } while( ( now.getTime() - notImproved.getTime() ) < PATIENCE
               && population.getFittest().getFitness() < problem.getGoalFt() );
      // Report the final state
      report( "L",
              problemRunName );
    }
  }

  private static void report( String phase,
                              String problemName ) {
    Individual best = population.getFittest();
    double distance = best.distanceSqTo( lastBest );
    Date now = new Date();
    String s = "" + convertMillisToTimeFormat(
           now.getTime() - startTime.getTime() )
               + " " + convertMillisToTimeFormat(
             now.getTime() - notImproved.getTime() )
               + " " + repetition
               + " " + generation
               + " " + String.format( "%9.7f",
                                      (float) ( generation ) / ( now.getTime() - startTime
                                                                .getTime() ) )
               + " " + phase
               + " " + distance
               //+ " " + deltaFt
               + "\t" + best
               + "\t" + best.genomeStr()
               + "\t" + now
               + " " + ( now.getTime() - startTime.getTime() ) + "\n";
    System.out.print( "\r" + s );
    System.out.flush();

    try( PrintWriter out = new PrintWriter(
                     new BufferedWriter(
                       new FileWriter(
                         problemName + ".txt",
                         true ) ) ) ) {
      out.print( s );

    } catch( IOException e ) {
      System.out.println( e );
    }

    try( PrintWriter out = new PrintWriter(
                     new BufferedWriter(
                       new FileWriter( problemName + "_" + repetition + ".txt",
                                       false ) ) ) ) {
      for( int i = 0;
           i < population.getSize();
           i++ ) {
        out.println(
          population.getIndividual( i ) + "\t" + population.getIndividual(
          i ).genomeStr() );
      }

    } catch( IOException e ) {
      System.out.println( e );
    }
  }

}
