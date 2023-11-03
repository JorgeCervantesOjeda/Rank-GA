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
public class ProblemKnapsack
  implements Problem {

  private long CAPACITY;
  private int NUM_ITEMS;
  private int WEIGHT[];
  private int VALUE[];

  public ProblemKnapsack() {
    CAPACITY = 6000;
    System.out.println( "Capacity:" + CAPACITY );
    NUM_ITEMS = 250;
    WEIGHT = new int[ NUM_ITEMS ];
    VALUE = new int[ NUM_ITEMS ];
    for( int i = 0;
         i < NUM_ITEMS;
         i++ ) {
      WEIGHT[ i ] = (int) ( Math.random() * 100 );
      VALUE[ i ] = (int) ( Math.random() * 100 );
      System.out.println( i + "\t" + WEIGHT[ i ] + "\t" + VALUE[ i ] );
    }
  }

  @Override
  public void adapt( double _bestFitness ) {
    throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
  }

  public double fitness( Gene[] genome,
                         StringBuilder extraString ) {
    int weight = 0;
    int value = 0;
    int countOnes = 0;
    for( int i = 0;
         i < NUM_ITEMS;
         i++ ) {
      if( genome[ i ].getIntValue() != 0 ) {
        countOnes++;
        weight += WEIGHT[ i ];
        value += VALUE[ i ];
      }
    }
    if( weight > CAPACITY ) {
      return CAPACITY - weight;
    }
    extraString
      .append( countOnes )
      .append( "\t" )
      .append( weight )
      .append( "\t" )
      .append( value );
    return value;
  }

  @Override
  public double fitness( Individual _i ) {
    throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public String getProblemName() {
    throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public int getGenomeLength() {
    return NUM_ITEMS;
  }

  @Override
  public Gene getNewGene( boolean _randomize_p,
                          Random r ) {
    return new GeneInteger( 2,
                            _randomize_p,
                            r );
  }

  @Override
  public Gene getNewGene( Gene _gene_p ) {
    return new GeneInteger( (GeneInteger) _gene_p );
  }

  @Override
  public double getGoalFt() {
    return 20000;
  }

  @Override
  public int getDisplayModulus() {
    return 5;
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
