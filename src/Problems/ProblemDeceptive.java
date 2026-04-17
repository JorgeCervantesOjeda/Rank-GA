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
    throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
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
    throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public double getGlobalSearchIntensity() {
    throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public double getLocalSearchIntensity() {
    throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public String getProblemName() {
    throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
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
    throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Individual getNewIndividual( Individual _get ) {
    throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
  }

}
