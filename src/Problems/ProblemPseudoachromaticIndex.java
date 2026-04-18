/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
 * Connected pseudoachromatic-index benchmark.
 * See `references/edge_colorings_complete_graphs_connected_classes.pdf`.
 *
 * @author usuario
 */
public class ProblemPseudoachromaticIndex
  implements Problem,
             AdaptiveProblem {

  private int numVertices;
  private int numEdges;
  private int numColors;
  int[][] connectedVertices;
  float weight;

  public ProblemPseudoachromaticIndex( int _numVertices,
                                       int _numColors,
                                       float _weight ) {
    if( _numColors < 2 ) {
      throw new IllegalArgumentException(
        "Pseudoachromatic index requires at least 2 colors" );
    }
    numVertices = _numVertices;
    numEdges = numVertices * ( numVertices - 1 ) / 2;
    numColors = _numColors;
    weight = _weight;
  }

  @Override
  public void adapt( double _bestFitness ) {
    if( _bestFitness >= numColors ) {
      numColors++;
    }
  }

  @Override
  public double fitness( Individual individual ) {

    int usedColors[] = new int[ numColors ];
    int colorCount = 0;
    connectedVertices = new int[ numColors ][ numVertices ];
    individual.setExtraString( new StringBuilder() );

    // calculate used colors
    for( int edge = 0;
         edge < numEdges;
         edge++ ) {
      if( usedColors[ (int) individual.getGene( edge ).getValue() ] == 0 ) {
        colorCount++;
      }
      usedColors[ (int) individual.getGene( edge ).getValue() ] = 1;
    }
    // set connected vertices
    int edge = 0;
    for( int origin = 0;
         origin < numVertices - 1;
         origin++ ) {
      for( int end = origin + 1;
           end < numVertices;
           end++ ) {
        if( usedColors[ (int) individual.getGene( edge ).getValue() ] > 0 ) {
          connectedVertices[ (int) individual.getGene( edge ).getValue() ][ origin ] = 1;
          connectedVertices[ (int) individual.getGene( edge ).getValue() ][ end ] = 1;
        }
        edge++;
      }
    }

    // check each color pair for common vertices
    int penalty = 0;
    for( int colorA = 0;
         colorA < numColors - 1;
         colorA++ ) {
      if( usedColors[ colorA ] > 0 ) {
        for( int colorB = colorA + 1;
             colorB < numColors;
             colorB++ ) {
          if( usedColors[ colorB ] > 0 ) {
            boolean connected = false;
            for( int vertex = 0;
                 vertex < numVertices && !connected;
                 vertex++ ) {
              if( connectedVertices[ colorA ][ vertex ] > 0 && connectedVertices[ colorB ][ vertex ] > 0 ) {
                connected = true;
              }
            }
            if( !connected ) {
              penalty++;
              if( penalty < -1 ) {
                individual.appendExtraString( "+" + colorA + "_" + colorB );
              }
            }
          }
        }
      }
    }

    individual.appendExtraString(
      " " + colorCount + "_" + penalty + "_" + individual.avg() );
    return colorCount - penalty * weight;
  }

  @Override
  public double getGlobalSearchIntensity() {
    // For categorical genes, a mutation should be able to jump to any other
    // color with equal probability once it happens.
    return 1.0 - 1.0 / this.numColors;
  }

  @Override
  public double getLocalSearchIntensity() {
    return 1.0 / this.getGenomeLength();
  }

  @Override
  public String getProblemName() {
    return "PseudoacromaticIndex_"
           + this.numVertices + "_"
           + this.numColors + "_"
           + this.weight;
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
    // open file
    try( BufferedReader br = new BufferedReader(
      new FileReader( initIndividualFile() ) ) ) {
      String line;
      int i = 0;

      // read Individual
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

  /**
   * Build the path to the initialization file for this instance.
   */
  private File initIndividualFile() {
    return RunOutputPaths.ensureFamilyDirectory(
      ProblemPseudoachromaticIndex.class
    ).resolve( "init_individual_"
               + this.numVertices + "_"
               + this.numColors
               + ".txt" ).toFile();
  }

  @Override
  public Individual getNewIndividual( Individual another ) {
    return new Individual( another );
  }

}
