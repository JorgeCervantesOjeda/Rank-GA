package Problems;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Random;
import rankga.AdaptiveProblem;
import rankga.Gene;
import rankga.Individual;
import rankga.Problem;
import rankga.RunOutputPaths;

/**
 * ProblemPseudoachromaticIndexConnex - Defines a specific problem related to
 * the Pseudoachromatic Index Connex. Implements the Problem interface and
 * provides methods for evaluating fitness, generating individuals, and adapting
 * parameters for the genetic algorithm.
 *
 * See `references/edge_colorings_complete_graphs_connected_classes.pdf`.
 *
 * Author: Jorge Cervantes Affiliation: Universidad Autónoma Metropolitana,
 * Mexico City
 */
public class ProblemPseudoachromaticIndexConnex
  implements Problem,
             AdaptiveProblem {

  // Problem-specific parameters
  private final int numVertices;
  private final int numEdges;
  private int numColors;
  private boolean[][] isColorInVertex;
  private final double weightPairs;
  private final double weightColors;
  private final double weightStd;
  private final double weightAvg;

  /**
   * Constructor for ProblemPseudoachromaticIndexConnex. Initializes the problem
   * parameters.
   */
  public ProblemPseudoachromaticIndexConnex() {
    numVertices = 22;
    numEdges = numVertices * ( numVertices - 1 ) / 2;
    numColors = 2;
    weightPairs = 0.01;
    weightColors = 1;
    weightStd = 0.000001;
    weightAvg = 0.000000001;

    // Print the problem parameters for debugging or analysis
    System.out.println( "ProblemPseudoachromaticIndexConnex parameters:" );
    System.out.println( "numVertices: " + numVertices );
    System.out.println( "numEdges: " + numEdges );
    System.out.println( "numColors: " + numColors );
    System.out.println( "weightPairs: " + weightPairs );
    System.out.println( "weightColors: " + weightColors );
    System.out.println( "weightStd: " + weightStd );
    System.out.println( "weightAvg: " + weightAvg );
  }

  @Override
  public void adapt( double bestFitness ) {
    // Increment the number of colors if the best fitness meets the threshold
    if( bestFitness >= numColors ) {
      numColors++;
    }
  }

  @Override
  public double fitness( Individual individual ) {
    boolean[] isUsedColor = new boolean[ numColors ];
    int colorCount = 0;
    isColorInVertex = new boolean[ numColors ][ numVertices ];
    individual.setExtraString( new StringBuilder() );

    // Set connected vertices
    int edge = 0;
    for( int vertexA = 0;
         vertexA < numVertices - 1;
         vertexA++ ) {
      for( int vertexB = vertexA + 1;
           vertexB < numVertices;
           vertexB++ ) {
        int edgeColor = (int) individual.getGene( edge ).getValue();
        if( !isUsedColor[ edgeColor ] ) {
          colorCount++;
        }
        isUsedColor[ edgeColor ] = true;
        isColorInVertex[ edgeColor ][ vertexA ] = true;
        isColorInVertex[ edgeColor ][ vertexB ] = true;
        edge++;
      }
    }

    // Check each color pair and count pair connections
    int countNotConnectedPairs = 0;
    int sumNumPairConnections = 0;
    int numPairs = 0;
    for( int colorA = 0;
         colorA < numColors - 1;
         colorA++ ) {
      if( isUsedColor[ colorA ] ) {
        for( int colorB = colorA + 1;
             colorB < numColors;
             colorB++ ) {
          if( isUsedColor[ colorB ] ) {
            numPairs++;
            int numPairConnections = 0;
            for( int vertex = 0;
                 vertex < numVertices;
                 vertex++ ) {
              if( isColorInVertex[ colorA ][ vertex ] && isColorInVertex[ colorB ][ vertex ] ) {
                numPairConnections++;
              }
            }
            sumNumPairConnections += numPairConnections;
            if( numPairConnections == 0 ) {
              countNotConnectedPairs++;
            }
          }
        }
      }
    }
    double avgNumPairConnections = sumNumPairConnections / (double) ( numPairs == 0
                                                                      ? 1
                                                                      : numPairs );

    // Count the number of disjoint class components
    int countNotConnectedColor = 0;
    for( int color = 0;
         color < numColors;
         color++ ) {
      if( isUsedColor[ color ] ) {
        if( !isConnected( color,
                          individual ) ) {
          countNotConnectedColor++;
        }
      }
    }

    // Calculate the color histogram
    int[] colorHistogram = new int[ numColors ];
    for( int i = 0;
         i < this.getGenomeLength();
         i++ ) {
      colorHistogram[ (int) individual.getGene( i ).getValue() ]++;
    }
    double std = calculateStandardDeviation( colorHistogram );
    double avg = calculateAverage( individual );

    // Append extra information to the individual's extra string
    individual.appendExtraString(
      " " + colorCount
      + "_" + String.format( "%02d",
                             countNotConnectedPairs )
      + "_" + String.format( "%02d",
                             countNotConnectedColor )
      + "_" + std
      + "_" + avg
      + "_" + avgNumPairConnections
    );
    individual.appendExtraString( "~" );
    for( int i = 0;
         i < numColors;
         i++ ) {
      individual.appendExtraString( "|" + colorHistogram[ i ] );
    }

    // Calculate fitness
    double fitness = colorCount
                     - countNotConnectedPairs * weightPairs
                     - countNotConnectedColor * weightColors
                     - std * weightStd
                     - avg * weightAvg;

    if( colorCount == numColors && countNotConnectedPairs == 0 && countNotConnectedColor == 0 ) {
      return colorCount;
    }
    return fitness;
  }

  @Override
  public double getGlobalSearchIntensity() {
    // For categorical genes, a mutation should be able to jump to any other
    // color with equal probability once it happens.
    return 1.0 - 1.0 / this.numColors;
  }

  @Override
  public double getLocalSearchIntensity() {
    return 1 / (double) getGenomeLength();
  }

  @Override
  public String getProblemName() {
    return "PseudoachromaticIndexConnex_"
           + this.numVertices + "_"
           + this.numColors + "_"
           + this.weightPairs + "_"
           + this.weightColors + "_"
           + this.weightStd;
  }

  @Override
  public int getGenomeLength() {
    return numEdges;
  }

  @Override
  public Gene getNewGene( boolean randomize,
                          Random r ) {
    return new GeneInteger( numColors,
                            randomize,
                            r );
  }

  @Override
  public Gene getNewGene( Gene gene ) {
    GeneInteger newGene = new GeneInteger( (GeneInteger) gene );
    newGene.setNumValues( numColors );
    return newGene;
  }

  @Override
  public double getGoalFt() {
    return numColors;
  }

  @Override
  public int getDisplayModulus() {
    return 1;
  }

  @Override
  public Individual getNewIndividual( boolean randomize,
                                      Random random ) {
    Individual individual = new Individual( this,
                                            randomize,
                                            random );
    if( randomize ) {
      return individual;
    }

    // Initialize individual from a file
    try( BufferedReader br = new BufferedReader(
      new FileReader( initIndividualFile() ) ) ) {
      String line;
      int i = 0;

      // Read individual from file
      while( ( line = br.readLine() ) != null && i < this.getGenomeLength() ) {
        String[] parts = line.trim().split( "\\s+" );
        for( String part
             : parts ) {
          if( i >= this.getGenomeLength() ) {
            break;
          }
          GeneInteger gene = new GeneInteger( this.numColors,
                                              false,
                                              random );
          gene.setIntValue( Integer.parseInt( part ) );
          individual.setGene( i,
                              gene );
          i++;
        }
      }

      if( i < this.getGenomeLength() ) {
        throw new IllegalArgumentException( "Not enough integers in the file." );
      }
    } catch( Exception ex ) {
      System.out.println( ex );
    }

    return individual;
  }

  /**
   * Build the path to the initialization file for this instance.
   */
  private File initIndividualFile() {
    return RunOutputPaths.ensureFamilyDirectory(
      ProblemPseudoachromaticIndexConnex.class
    ).resolve( "init_individual_"
               + this.numVertices + "_"
               + this.numColors
               + ".txt" ).toFile();
  }

  @Override
  public Individual getNewIndividual( Individual another ) {
    return new Individual( another );
  }

  /**
   * Checks if all vertices of a given color are connected.
   *
   * @param color      The color to check for.
   * @param individual The individual containing the genes.
   *
   * @return True if all vertices of the color are connected, false otherwise.
   */
  private boolean isConnected( int color,
                               Individual individual ) {
    boolean[] visited = new boolean[ numVertices ];
    int vertexA = 0;

    // Find the first vertex with the specified color
    while( !isColorInVertex[ color ][ vertexA ] && vertexA < numVertices ) {
      vertexA++;
    }

    // Count vertices with the color
    int countColor = 0;
    for( int i = 0;
         i < numVertices;
         i++ ) {
      if( isColorInVertex[ color ][ i ] ) {
        countColor++;
      }
    }

    int count = countConnectedVertices( vertexA,
                                        color,
                                        individual,
                                        visited );
    return count == countColor;
  }

  /**
   * Count the number of connected vertices with the same color.
   *
   * @param vertexA    The starting vertex.
   * @param color      The color to check for.
   * @param individual The individual containing the genes.
   * @param visited    Array to keep track of visited vertices.
   *
   * @return The count of connected vertices with the same color.
   */
  private int countConnectedVertices( int vertexA,
                                      int color,
                                      Individual individual,
                                      boolean[] visited ) {
    if( visited[ vertexA ] ) {
      return 0;
    }

    int count = 1;
    visited[ vertexA ] = true;
    for( int neighbor = 0;
         neighbor < numVertices;
         neighbor++ ) {
      if( vertexA != neighbor ) {
        if( getColor( vertexA,
                      neighbor,
                      individual ) == color ) {
          count += countConnectedVertices( neighbor,
                                           color,
                                           individual,
                                           visited );
        }
      }
    }
    return count;
  }

  /**
   * Get the color of an edge between two vertices.
   *
   * @param i          The first vertex index.
   * @param j          The second vertex index.
   * @param individual The individual containing the genes.
   *
   * @return The color of the edge between the vertices.
   */
  private int getColor( int i,
                        int j,
                        Individual individual ) {
    if( i > j ) {
      int aux = i;
      i = j;
      j = aux;
    }
    return (int) individual.getGene(
      i * ( numVertices - 1 ) + j - ( i + 1 ) * ( i + 2 ) / 2 + i
    ).getValue();
  }

  /**
   * Calculate the standard deviation of an array of integers.
   *
   * @param arr The array of integers.
   *
   * @return The standard deviation of the array.
   */
  private double calculateStandardDeviation( int[] arr ) {
    double sum = 0;
    for( int value
         : arr ) {
      sum += value;
    }
    double avg = sum / arr.length;
    double sumDiffSqr = 0;
    for( int value
         : arr ) {
      double diff = value - avg;
      sumDiffSqr += diff * diff;
    }
    return Math.sqrt( sumDiffSqr / arr.length );
  }

  /**
   * Calculate the average value of the genes in an individual's genome.
   *
   * @param individual The individual whose genes are being averaged.
   *
   * @return The average value of the genes.
   */
  private double calculateAverage( Individual individual ) {
    double sum = 0;
    for( int i = 0;
         i < this.getGenomeLength();
         i++ ) {
      sum += individual.getGene( i ).getValue();
    }
    return sum / this.getGenomeLength();
  }

}
