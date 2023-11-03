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
public class ProblemNK
  implements Problem {

  private int N;
  private int K;
  private double values[][];

  public ProblemNK( int n,
                    int k ) {
    N = n;
    K = k;
    values = new double[ N ][];
    Random r = new Random( 9 );
    for( int i = 0;
         i < N;
         i++ ) {
      values[ i ] = new double[ 2 << K ];
      for( int j = 0;
           j < ( 2 << K );
           j++ ) {
        values[ i ][ j ] = r.nextDouble();
        // System.out.println( i + " " + j + " " + values[ i ][ j ] );
      }
    }
  }

  @Override
  public void adapt( double _bestFitness ) {
  }

  public double fitness( Gene[] genome,
                         StringBuilder extraString ) {
    double f = 0;
    for( int n = 0;
         n < N;
         n++ ) {
      f += values[ n ][ intValue( genome,
                                  n ) ];
    }
    extraString.append( "_" );
    return f;
  }

  @Override
  public double fitness( Individual _i ) {
    throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public String getProblemName() {
    return "NK_"
           + this.N + "_"
           + this.K + "_"
           + System.currentTimeMillis();
  }

  @Override
  public int getGenomeLength() {
    return N;
  }

  @Override
  public Gene getNewGene( boolean _randomize_p,
                          Random r ) {
    return new GeneInteger( 2,
                            _randomize_p,
                            r );
  }

  @Override
  public Gene getNewGene( Gene _gene ) {
    return new GeneInteger( (GeneInteger) _gene );
  }

  @Override
  public double getGoalFt() {
    return N;
  }

  @Override
  public int getDisplayModulus() {
    return K;
  }

  private int intValue( Gene[] _genome,
                        int n ) {
    int v = 0;
    for( int i = 0;
         i < K;
         i++ ) {
      v = ( v << 1 ) | _genome[ ( n + i ) % N ].getIntValue();
//      v += _genome[ ( n + i ) % N ].getIntValue() * ( 1 << i );
    }

    return v;
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
