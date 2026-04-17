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
public class ProblemDeceptive
  implements Problem {

  private int GENOME_LENGTH;
  private int BASIN_WIDTH;

  public ProblemDeceptive( int genomeLength,
                           int basinWidth ) {
    GENOME_LENGTH = genomeLength;
    BASIN_WIDTH = basinWidth;
  }

  @Override
  public void adapt( double _bestFitness ) {
    // Static landscape; nothing to adapt.
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

    return count < BASIN_WIDTH
           ? GENOME_LENGTH - count
           : ( count - BASIN_WIDTH );
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
    return "Deceptive_" + this.GENOME_LENGTH + "_" + this.BASIN_WIDTH;
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
    return GENOME_LENGTH + 1;
  }

  @Override
  public int getDisplayModulus() {
    return 10;
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
