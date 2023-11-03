package Problems;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Random;
import rankga.Gene;
import rankga.Individual;
import rankga.Problem;

/**
 * This class defines a specific problem related to Pseudoachromatic Index
 * Connex. It implements the Problem interface.
 */
public class ProblemPseudoachromaticIndexConnex
  implements Problem {

  private final int numVertices;
  private final int numEdges;
  private int numColors;
  private boolean[][] isColorInVertex;
  private final double weightPairs;
  private final double weightColors;
  private final double weightSum;

  /**
   * Constructor for ProblemPseudoachromaticIndexConnex.
   *
   * @param _numVertices  Number of vertices in the problem.
   * @param _numColors    Number of colors to be used.
   * @param _weightPairs  Weight factor for considering pairs of colors.
   * @param _weightColors Weight factor for considering colors.
   * @param _weightSum    Weight factor for considering the sum of genes.
   */
  public ProblemPseudoachromaticIndexConnex( int _numVertices,
                                             int _numColors,
                                             double _weightPairs,
                                             double _weightColors,
                                             double _weightSum ) {
    numVertices = _numVertices;
    numEdges = numVertices * ( numVertices - 1 ) / 2;
    numColors = _numColors;
    weightPairs = _weightPairs;
    weightColors = _weightColors;
    weightSum = _weightSum;
  }

  @Override
  public void adapt( double _bestFitness ) {
    // Adjust the number of colors if the best fitness is greater or equal to the current number of colors.
    if( _bestFitness >= numColors ) {
      numColors++;
    }
  }

  @Override
  public double fitness( Individual individual ) {
    boolean isUsedColor[] = new boolean[ numColors ];
    int colorCount = 0;
    isColorInVertex = new boolean[ numColors ][ numVertices ];
    individual.setExtraString( new StringBuilder() );

    // Set connected vertices
    int edge = 0;
    for( int vertex_A = 0;
         vertex_A < numVertices - 1;
         vertex_A++ ) {
      for( int vertex_B = vertex_A + 1;
           vertex_B < numVertices;
           vertex_B++ ) {
        int edgeColor = individual.getGene( edge ).getIntValue();
        if( !isUsedColor[ edgeColor ] ) {
          colorCount++;
        }
        isUsedColor[ edgeColor ] = true;
        isColorInVertex[ edgeColor ][ vertex_A ] = true;
        isColorInVertex[ edgeColor ][ vertex_B ] = true;
        edge++;
      }
    }

    // Check each color pair for common vertices
    int countNotConnectedPairs = 0;
    for( int color_A = 0;
         color_A < numColors - 1;
         color_A++ ) {
      if( isUsedColor[ color_A ] ) {
        for( int color_B = color_A + 1;
             color_B < numColors;
             color_B++ ) {
          if( isUsedColor[ color_B ] ) {
            boolean pairConnected = false;
            for( int vertex = 0;
                 vertex < numVertices && !pairConnected;
                 vertex++ ) {
              if( isColorInVertex[ color_A ][ vertex ] && isColorInVertex[ color_B ][ vertex ] ) {
                pairConnected = true;
              }
            }
            if( !pairConnected ) {
              countNotConnectedPairs++;
            }
          }
        }
      }
    }

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

    individual.appendExtraString(
      " " + colorCount
      + "_" + countNotConnectedPairs
      + "_" + countNotConnectedColor
      + "_" + individual.sum()
    );

    int colorHistogram[] = new int[ numColors ];
    for( int i = 0;
         i < this.getGenomeLength();
         i++ ) {
      colorHistogram[ individual.getGene( i ).getIntValue() ]++;
    }
    individual.appendExtraString( "~" );
    for( int i = 0;
         i < numColors;
         i++ ) {
      individual.appendExtraString( "|" + colorHistogram[ i ] );
    }

    return colorCount - countNotConnectedPairs * weightPairs - countNotConnectedColor * weightColors - individual
      .sum() * weightSum;
  }

  @Override
  public String getProblemName() {
    return "PseudoacromaticIndexConnex_"
           + this.numVertices + "_"
           + this.numColors + "_"
           + this.weightPairs + "_"
           + this.weightColors + "_"
           + this.weightSum;
  }

  @Override
  public int getGenomeLength() {
    return numEdges;
  }

  @Override
  public Gene getNewGene( boolean _randomize_p,
                          Random r ) {
    return new GeneInteger( numColors,
                            _randomize_p,
                            r );
  }

  @Override
  public Gene getNewGene( Gene _gene ) {
    GeneInteger g = new GeneInteger( (GeneInteger) _gene );
    g.setNumValues( numColors );
    return g;
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
  public Individual getNewIndividual( boolean _randomize,
                                      Random _r ) {
    Individual ind = new Individual( this,
                                     _randomize,
                                     _r );
    if( _randomize ) {
      return ind;
    }

    // Open file for initializing the individual
    try( BufferedReader br = new BufferedReader( new FileReader(
                        "init_individual_" + this.numVertices + "_" + this.numColors + ".txt" ) ) ) {
      String line;
      int i = 0;

      // Read Individual
      while( ( line = br.readLine() ) != null && i < this.getGenomeLength() ) {
        String[] parts = line.trim().split( "\\s+" );
        for( String part
             : parts ) {
          if( i >= this.getGenomeLength() ) {
            break;
          }
          GeneInteger g = new GeneInteger( this.numColors,
                                           false,
                                           _r );
          g.setIntValue( Integer.parseInt( part ) );
          ind.setGene( i,
                       g );
          i++;
        }
      }

      if( i < this.getGenomeLength() ) {
        throw new IllegalArgumentException( "Not enough integers in the file." );
      }
    } catch( Exception ex ) {
      System.out.println( ex );
    }

    return ind;
  }

  @Override
  public Individual getNewIndividual( Individual another ) {
    return new Individual( (Individual) another );
  }

  /**
   * Checks if two vertices are connected with the same color.
   *
   * @param color      The color to check for.
   * @param individual The individual containing the genes.
   *
   * @return True if the vertices are connected with the same color, false
   *         otherwise.
   */
  private boolean isConnected( int color,
                               Individual individual ) {
    boolean[] visited = new boolean[ numVertices ];
    int vertex_A = 0;
    // Find the first vertex with the color
    while( !isColorInVertex[ color ][ vertex_A ] && vertex_A < numVertices ) {
      vertex_A++;
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

    int count = countConnectedVertices( vertex_A,
                                        color,
                                        individual,
                                        visited );

    return count == countColor;
  }

  /**
   * Count the number of connected vertices with the same color.
   *
   * @param vertex_A   The starting vertex.
   * @param color      The color to check for.
   * @param individual The individual containing the genes.
   * @param visited    Array to keep track of visited vertices.
   *
   * @return The count of connected vertices with the same color.
   */
  private int countConnectedVertices( int vertex_A,
                                      int color,
                                      Individual individual,
                                      boolean[] visited ) {
    if( visited[ vertex_A ] ) {
      return 0;
    }

    int count = 1;
    visited[ vertex_A ] = true;
    for( int neighbor = 0;
         neighbor < numVertices;
         neighbor++ ) {
      if( vertex_A != neighbor ) {
        if( color( vertex_A,
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
  private int color( int i,
                     int j,
                     Individual individual ) {
    if( i > j ) {
      int aux = i;
      i = j;
      j = aux;
    }
    return individual.getGene(
      i * ( numVertices - 1 ) + j - ( i + 1 ) * ( i + 2 ) / 2 + i )
      .getIntValue();
  }

}
