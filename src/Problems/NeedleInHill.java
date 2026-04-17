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

public class NeedleInHill
  implements Problem {

  int PLATEAU_WIDTH;
  int HILLSIDE_WIDTH;
  int HAMM_DIST_OF_NEEDLE;
  int GENOME_LENGTH;
  private int countOnes;

  public NeedleInHill( int genomeLength,
                       int plateauWidth,
                       int hillsideWidth,
                       int hammDistOfNeedle ) {
    GENOME_LENGTH = genomeLength;
    PLATEAU_WIDTH = plateauWidth;
    HILLSIDE_WIDTH = hillsideWidth;
    HAMM_DIST_OF_NEEDLE = hammDistOfNeedle;
  }

  @Override
  public void adapt( double _bestFitness ) {
    // No adaptive parameters in this landscape.
  }

  public double fitness( Gene[] genome,
                         StringBuilder extraString ) {
    int i;
    countOnes = 0;
    for( i = 0;
         i < GENOME_LENGTH && (int) genome[ i ].getValue() == 1;
         i++ ) {
      countOnes++;
    }
    extraString.append( countOnes );

    if( countOnes == GENOME_LENGTH - HAMM_DIST_OF_NEEDLE ) {
      // cadena candidata a ser la aguja
      // falta ver si tiene puros ceros en el resto de la cadena
      for( ;
        i < GENOME_LENGTH && (int) genome[ i ].getValue() == 0;
        i++ )
                ;
      if( i == GENOME_LENGTH ) // es la aguja (óptimo global)
      {
        return 3;
      }
    }

    countOnes = 0;
    for( i = 0;
         i < GENOME_LENGTH;
         i++ ) {
      if( 0 < (int) genome[ i ].getValue() ) {
        countOnes++;
      }
    }
    int hammingDist = GENOME_LENGTH - countOnes;

    if( hammingDist < PLATEAU_WIDTH ) // en la meseta
    {
      return 2 - ( 0.1 / PLATEAU_WIDTH ) * hammingDist;
    }

    if( hammingDist < PLATEAU_WIDTH + HILLSIDE_WIDTH ) // en la ladera
    {
      return 1.9 - ( 0.9 / HILLSIDE_WIDTH ) * ( hammingDist - PLATEAU_WIDTH );
    }

    // fuera de la cuenca de atracción
    return 1;

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
    // Binary loci: allow broad exploration in the worst ranks.
    return 0.5;
  }

  @Override
  public double getLocalSearchIntensity() {
    // Near-elites should mutate sparsely.
    return 1.0 / this.getGenomeLength();
  }

  @Override
  public String getProblemName() {
    return "NeedleInHill_"
           + this.GENOME_LENGTH + "_"
           + this.PLATEAU_WIDTH + "_"
           + this.HILLSIDE_WIDTH + "_"
           + this.HAMM_DIST_OF_NEEDLE;
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
    return 3;
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
