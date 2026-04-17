package Problems;

import java.util.Random;
import rankga.Gene;
import rankga.Individual;
import rankga.Problem;

/**
 * ProblemKnapsack - Defines a knapsack problem for use with genetic algorithms. Implements the Problem interface, providing methods for generating individuals,
 * evaluating fitness, and setting up the knapsack environment.
 *
 * Author: Jorge Cervantes Affiliation: Universidad Autónoma Metropolitana, Mexico City
 */
public class ProblemKnapsack
  implements Problem {

  private final long WEIGHT_CAPACITY; // Weight capacity of the knapsack
  private final long VOLUME_CAPACITY; // Volume capacity of the knapsack
  private final int NUM_ITEMS; // Number of items available to put in the knapsack
  private final int[] WEIGHT; // Array of item weights
  private final int[] VOLUME; // Array of item volumes
  private final int[] VALUE; // Array of item values
  private final Random random;

  /**
   * Constructor for the ProblemKnapsack class. Initializes the items with random weights, volumes, and values.
   */
  public ProblemKnapsack() {
    WEIGHT_CAPACITY = 6000;
    VOLUME_CAPACITY = 5000;
    NUM_ITEMS = 250;
    WEIGHT = new int[ NUM_ITEMS ];
    VOLUME = new int[ NUM_ITEMS ];
    VALUE = new int[ NUM_ITEMS ];
    this.random = new Random();

    // Initialize weights, volumes, and values for each item
    System.out.println( "Weight Capacity: " + WEIGHT_CAPACITY );
    System.out.println( "Volume Capacity: " + VOLUME_CAPACITY );
    for( int i = 0;
         i < NUM_ITEMS;
         i++ ) {
      WEIGHT[ i ] = random.nextInt( 100 ) + 1; // Assign random weights between 1 and 100
      VOLUME[ i ] = random.nextInt( 50 ) + 1; // Assign random volumes between 1 and 50
      VALUE[ i ] = random.nextInt( 100 ) + 1; // Assign random values between 1 and 100
      System.out.println(
        i + "\t" + WEIGHT[ i ] + "\t" + VOLUME[ i ] + "\t" + VALUE[ i ] );
    }
  }

  /**
   * Calculate the fitness of a given individual.
   *
   * @param individual The individual whose fitness is to be calculated.
   *
   * @return The calculated fitness value.
   */
  @Override
  public double fitness( Individual individual ) {
    int totalWeight = 0;
    int totalVolume = 0;
    int totalValue = 0;
    int countOnes = 0;

    // Calculate the total weight, volume, and value of items included in the knapsack
    for( int i = 0;
         i < NUM_ITEMS;
         i++ ) {
      if( 0 != (int) individual.getGene( i ).getValue() ) { // If item is included
        countOnes++;
        totalWeight += WEIGHT[ i ];
        totalVolume += VOLUME[ i ];
        totalValue += VALUE[ i ];
      }
    }

    // If the total weight or volume exceeds the respective capacities, penalize the fitness
    double penalty = 0;
    if( totalWeight > WEIGHT_CAPACITY ) {
      penalty += ( totalWeight - WEIGHT_CAPACITY ) * 0.1; // Penalize proportional to excess weight
    }
    if( totalVolume > VOLUME_CAPACITY ) {
      penalty += ( totalVolume - VOLUME_CAPACITY ) * 0.1; // Penalize proportional to excess volume
    }

    double finalFitness = totalValue - penalty;

    // Append extra information (optional)
    individual.appendExtraString(
      countOnes + "\t" + totalWeight + "\t" + totalVolume + "\t" + totalValue + "\tPenalty: " + penalty );
    return finalFitness; // Return the total value adjusted by penalties as the fitness score
  }

  @Override
  public double getGlobalSearchIntensity() {
    // mutation probability for global search
    return 1 - 1 / this.getNewGene( false,
                                    this.random ).getNumValues();
  }

  @Override
  public double getLocalSearchIntensity() {
    // mutation probability for local search
    return 1 / this.getGenomeLength();
  }

  @Override
  public String getProblemName() {
    return "Knapsack_" + NUM_ITEMS + "_WeightCap" + WEIGHT_CAPACITY + "_VolumeCap" + VOLUME_CAPACITY;
  }

  @Override
  public int getGenomeLength() {
    return NUM_ITEMS;
  }

  @Override
  public Gene getNewGene( boolean randomize,
                          Random r ) {
    return new GeneInteger( 2,
                            randomize,
                            r ); // A gene can take two values: 0 (not included) or 1 (included)
  }

  @Override
  public Gene getNewGene( Gene gene ) {
    return new GeneInteger( (GeneInteger) gene );
  }

  @Override
  public double getGoalFt() {
    return 20000; // Arbitrary goal fitness for demonstration purposes
  }

  @Override
  public int getDisplayModulus() {
    return 5; // Display every 5 items for visual clarity
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
