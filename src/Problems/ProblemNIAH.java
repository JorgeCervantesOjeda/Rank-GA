/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Problems;

import static java.lang.Math.pow;
import java.util.Random;
import rankga.Gene;
import rankga.Individual;
import rankga.Problem;

/**
 *
 * @author usuario
 */
public class ProblemNIAH
  implements Problem {

  int GENOME_LENGTH;
  int NUM_BLOCKS;
  int NEEDLE_WIDTH;
  long stats[];
  long countEvals;

  public ProblemNIAH( int genomeLength,
                      int numBlocks,
                      int needleWidth ) {
    GENOME_LENGTH = genomeLength;
    NUM_BLOCKS = numBlocks;
    NEEDLE_WIDTH = needleWidth;
    stats = new long[ (int) pow( 2,
                                 GENOME_LENGTH ) ];
    countEvals = 0;
  }

  @Override
  public double fitness( Individual individual ) {
    double sum = 0;
    int countOnes = 0;
    individual.setExtraString( new StringBuilder() );
    int index = 0;

    for( int block = 0;
         block < NUM_BLOCKS;
         block++ ) {
      int count = 0;
      for( int i = 0;
           i < GENOME_LENGTH / NUM_BLOCKS;
           i++ ) {
        index = ( index << 1 )
                | (int) individual
          .getGene( block * GENOME_LENGTH / NUM_BLOCKS + i )
          .getValue();
        if( individual.getGene( block * GENOME_LENGTH / NUM_BLOCKS + i )
          .getValue() > 0 ) {
          count++;
        }
      }
      if( count > GENOME_LENGTH / NUM_BLOCKS - NEEDLE_WIDTH ) {
        sum += 2;
      } else {
        sum += 1 + 0.000 * count;
      }
      countOnes += count;
    }
    stats[ index ]++;
    countEvals++;
    individual.appendExtraString(
      "" + countOnes + "_" + stats[ index ] / (double) countEvals );
    return sum;
  }

  @Override
  public double getGlobalSearchIntensity() {
    return 0.5;
  }

  @Override
  public double getLocalSearchIntensity() {
    return 1.0 / this.getGenomeLength();
  }

  @Override
  public String getProblemName() {
    return "NIAH_"
           + this.GENOME_LENGTH + "_"
           + this.NUM_BLOCKS + "_"
           + this.NEEDLE_WIDTH;
  }

  @Override
  public int getGenomeLength() {
    return GENOME_LENGTH;
  }

  @Override
  public Gene getNewGene( boolean randomize,
                          Random r ) {
    return new GeneInteger( 2,
                            randomize,
                            r );
  }

  @Override
  public Gene getNewGene( Gene other ) {
    return new GeneInteger( (GeneInteger) other );
  }

  @Override
  public double getGoalFt() {
    return 3 * NUM_BLOCKS;
  }

  @Override
  public int getDisplayModulus() {
    return 4;
  }

  @Override
  public Individual getNewIndividual( boolean _randomize,
                                      Random _r ) {
    return new Individual( this,
                           _randomize,
                           _r );
  }

  @Override
  public Individual getNewIndividual( Individual another ) {
    return new Individual( another );
  }

}
