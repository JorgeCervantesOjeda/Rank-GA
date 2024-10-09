/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Problems;

import java.util.Random;
import rankga.*;

/**
 * TaskAssignmentProblem - Implementation for the Task Assignment Problem with
 * Time and Cost constraints.
 *
 * This class provides methods to handle the specific requirements of the task
 * assignment optimization problem, such as calculating fitness, adapting
 * parameters, and creating new individuals and genes.
 *
 * Author: Jorge Cervantes Affiliation: Universidad Autónoma Metropolitana,
 * Mexico City
 */
public class ProblemTaskAssignment
  implements Problem {

  private final double[] taskBenefits;
  private final double[][] taskTimes;
  private final double[] agentHourlyCosts;
  private final double[] costExtraTime;
  private final double maxHours;
  // Define the number of tasks and agents
  private final int numTasks = 100;
  private final int numAgents = 20;

  public ProblemTaskAssignment() {
    Random rand = new Random();

    // Initialize arrays with random values
    this.taskBenefits = new double[ numTasks ];
    this.taskTimes = new double[ numAgents ][ numTasks ];
    this.agentHourlyCosts = new double[ numAgents ];
    this.costExtraTime = new double[ numAgents ];

    // Fill taskBenefits with random values between 1000 and 6000
    System.out.println( "Task Benefits:" );
    for( int i = 0;
         i < numTasks;
         i++ ) {
      this.taskBenefits[ i ] = 1000 + ( 5000 * rand.nextDouble() );
      System.out.printf( "Task %d: %.2f\n",
                         i,
                         this.taskBenefits[ i ] );
    }

    // Fill taskTimes with random values between 1 and 10
    System.out.println( "\nTask Times:" );
    for( int j = 0;
         j < numTasks;
         j++ ) {
      double taskMin = 2 + ( 9 * rand.nextDouble() );
      for( int i = 0;
           i < numAgents;
           i++ ) {
        this.taskTimes[ i ][ j ] = taskMin + ( 10 * rand.nextDouble() );
        System.out.printf( "\t%.2f ",
                           this.taskTimes[ i ][ j ] );
      }
      System.out.println();
    }

    // Fill agentHourlyCosts with random values between 100 and 400
    System.out.println( "\nAgent Hourly Costs:" );
    for( int i = 0;
         i < numAgents;
         i++ ) {
      this.agentHourlyCosts[ i ] = 100 + ( 300 * rand.nextDouble() );
      System.out.printf( "Agent %d: %.2f per hour\n",
                         i,
                         this.agentHourlyCosts[ i ] );
    }

    // Fill costExtraTime with values equal to the hourly costs (for each extra hour worked)
    System.out.println( "\nCost Extra Time:" );
    for( int i = 0;
         i < numAgents;
         i++ ) {
      this.costExtraTime[ i ] = 2 * this.agentHourlyCosts[ i ];
      System.out.printf( "Agent %d: %.2f per extra hour\n",
                         i,
                         this.costExtraTime[ i ] );
    }

    // Set maxHours with a fixed value
    this.maxHours = 40.0;
    System.out.println( "\nMax Hours per Agent: " + this.maxHours );
  }

  @Override
  public void adapt( double bestFitness ) {
    // No adaptation needed as parameters are fixed after initialization.
  }

  @Override
  public double fitness( Individual individual ) {
    double totalBenefit = 0;
    double[] totalCost = new double[ this.numAgents ];
    double[] totalTime = new double[ this.numAgents ];

    for( int task = 0;
         task < this.numTasks;
         task++ ) {
      int agent = individual.getGene( task ).getIntValue();
      if( agent > 0 ) {
        int agentIndex = agent - 1;
        totalBenefit += taskBenefits[ task ];
        totalTime[ agentIndex ] += taskTimes[ agentIndex ][ task ];
        totalCost[ agentIndex ] += taskTimes[ agentIndex ][ task ] * agentHourlyCosts[ agentIndex ];
      }
    }

    double penalty = 0;
    for( int i = 0;
         i < this.numAgents;
         i++ ) {
      if( totalTime[ i ] > maxHours ) {
        double extra = totalTime[ i ] - maxHours;
        penalty += extra * extra * costExtraTime[ i ];
      }
    }

    individual.setExtraString( new StringBuilder() );
    individual.appendExtraString( "p_" + penalty );
    for( int i = 0;
         i < this.numAgents;
         i++ ) {
      double s = Math.floor( totalTime[ i ] * 100 ) / 100.0;
      individual.appendExtraString( "_" + s );
    }

    return totalBenefit - ( penalty + sum( totalCost ) );
  }

  private double sum( double[] array ) {
    double sum = 0;
    for( double v
         : array ) {
      sum += v;
    }
    return sum;
  }

  @Override
  public String getProblemName() {
    return "Task_Assignment";
  }

  @Override
  public int getGenomeLength() {
    return this.numTasks;
  }

  @Override
  public Gene getNewGene( boolean randomize,
                          Random r ) {
    return new GeneInteger( this.numAgents + 1,
                            true,
                            r );
  }

  @Override
  public Gene getNewGene( Gene gene ) {
    return new GeneInteger( ( (GeneInteger) gene ) );
  }

  @Override
  public double getGoalFt() {
    return Double.MAX_VALUE; // As the fitness represents a value to minimize.
  }

  @Override
  public int getDisplayModulus() {
    return 1;
  }

  @Override
  public Individual getNewIndividual( boolean randomize,
                                      Random r ) {
    return new Individual( this,
                           randomize,
                           r );
  }

  @Override
  public Individual getNewIndividual( Individual individual ) {
    return new Individual( individual );
  }

}
