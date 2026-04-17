/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Problems;

import java.util.Random;
import rankga.Gene;
import rankga.Individual;
import rankga.Problem;

/**
 *
 * @author usuario
 */
public class ProblemHillSideSearch
  implements Problem {

  private final int basinWidth;
  private final double basinSlope;
  private final int optimumDistance;
  //private int countOnes;
  private final int genomeSize;

  public ProblemHillSideSearch( int genomeSize,
                                int basinWidth,
                                double basinSlope,
                                int optimumDistance ) {
    this.genomeSize = genomeSize;
    this.basinWidth = basinWidth;
    this.basinSlope = basinSlope;
    this.optimumDistance = optimumDistance;
    System.out.println(
      "gs:" + genomeSize + "\tbw:" + basinWidth + "\tbs:" + basinSlope + "\tod:" + optimumDistance + "\n" );
  }

  private boolean isOptimum( Gene[] genome ) {
    int i = 0;
    while( i < genome.length && genome[ i ].getValue() == 0 ) {
      i++;
    }
    if( i != optimumDistance ) {
      return false;
    }

    while( i < genome.length && genome[ i ].getValue() == 1 ) {
      i++;
    }
    return i == genome.length;
  }

  @Override
  public void adapt( double _bestFitness ) {
    // Static fitness landscape; nothing to adapt.
  }

  public double fitness( Gene[] genome,
                         StringBuilder extraString ) {
    int count = 0;
    for( int i = 0;
         i < genome.length;
         i++ ) {
      if( genome[ i ].getValue() > 0 ) {
        count++;
      }
    }
    extraString.append( count );

    if( isOptimum( genome ) ) {
      return 3;
    }

    return count
           >= genome.length - basinWidth + 1
           ? ( 2 - basinSlope * ( genome.length - count ) )
           : ( 1 - basinSlope * ( genome.length - count ) );
  }

  @Override
  public double fitness( Individual _i ) {
    Gene[] genome = new Gene[ this.getGenomeLength() ];
    for( int i = 0; i < genome.length; i++ ) {
      genome[ i ] = _i.getGene( i );
    }
    StringBuilder extraString = new StringBuilder();
    double ft = this.fitness( genome,
                              extraString );
    _i.setExtraString( extraString );
    return ft;
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
    return "HillSideSearch_"
           + this.genomeSize + "_"
           + this.basinWidth + "_"
           + this.basinSlope + "_"
           + this.optimumDistance;
  }

  @Override
  public int getGenomeLength() {
    return this.genomeSize;
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
    return 3;
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
  public Individual getNewIndividual( Individual _get ) {
    return new Individual( _get );
  }

}
