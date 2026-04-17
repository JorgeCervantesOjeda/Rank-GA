/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Problems;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import java.util.Random;
import rankga.Gene;
import rankga.Individual;
import rankga.Problem;

/**
 *
 * @author usuario
 */
public class ProblemTS
  implements Problem {

  private int n;
  private double M[][] = {
    { 0, 4, 1, 2 },
    { 4, 0, 2, 15 },
    { 1, 2, 0, 4 },
    { 2, 15, 4, 0 }
  };
  private Random r = new Random();

  public ProblemTS( int _n ) {
    System.out.println( "Initializing TSP:" );
    this.n = _n;

    this.M = new double[ n ][ n ];
    for( int i = 0;
         i < n;
         i++ ) {
      for( int j = 0;
           j < n;
           j++ ) {
        M[ i ][ j ] = r.nextDouble();
        System.out.print( M[ i ][ j ] + " " );
      }
      System.out.println();
    }
  }

  @Override
  public void adapt( double _bestFitness ) {
  }

  public double fitness( Gene[] genome,
                         StringBuilder extraString ) {
    double cost = 0.0;
    Long start[] = new Long[ n ];
    Long result[] = new Long[ n ];
    for( int i = 0;
         i < n;
         i++ ) {
      start[ i ] = 0L;
      result[ i ] = 0L;
    }
    int initial = 0;//r.nextInt( n );
    start[ initial ] = 1L;
    //result[ initial ] = 1;
    Long visitas = 0L;
    int coverage[] = new int[ n ];

    for( int step = 0;
         step < n;
         step++ ) {
      for( int i = 0;
           i < n;
           i++ ) {
        for( int j = 0;
             j < n;
             j++ ) {
          if( i == j ) {
            genome[ j * n + i ].setIntValue( 0 );
          }
          result[ i ] += start[ j ]
                         * (long) genome[ j * n + i ].getValue();
          cost += start[ j ]
                  * genome[ j * n + i ].getValue()
                  * M[ j ][ i ];
        }
      }
      for( int i = 0;
           i < n;
           i++ ) {
        start[ i ] = result[ i ];
        if( result[ i ] > 0 ) {
          visitas = min( visitas + result[ i ],
                         2000000000L );
          coverage[ i ] = 1;
        }
        result[ i ] = 0L;
      }
    }
    int cov = 0;
    long rowOnes[] = new long[ n ];
    long colOnes[] = new long[ n ];
    for( int i = 0;
         i < n;
         i++ ) {
      cov += coverage[ i ];
      for( int j = 0;
           j < n;
           j++ ) {
        rowOnes[ i ] += genome[ i * n + j ].getValue();
        colOnes[ j ] += genome[ i * n + j ].getValue();
      }
    }
    long extraOnes = 0;
    for( int i = 0;
         i < n;
         i++ ) {
      if( rowOnes[ i ] > 1 ) {
        extraOnes += rowOnes[ i ] - 1;
      }
      if( colOnes[ i ] > 1 ) {
        extraOnes += colOnes[ i ] - 1;
      }
    }
    extraString.append(
      cov + " " + cost + " " + visitas + " " + extraOnes + " " + initial );

    return cov * 1e-0
           - extraOnes * 1e-2
           - cost * 1e-6
           - abs( visitas - n ) * 1e-4;
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
    return "TSP_Matrix_" + this.n;
  }

  @Override
  public int getGenomeLength() {
    return n * n;
  }

  @Override
  public Gene getNewGene( boolean _randomize_p,
                          Random r ) {
    return new GeneTS( 2,
                       _randomize_p,
                       r );
  }

  @Override
  public Gene getNewGene( Gene _gene ) {
    return new GeneTS( (GeneTS) _gene );
  }

  @Override
  public double getGoalFt() {
    return n;
  }

  @Override
  public int getDisplayModulus() {
    return n;
  }

  @Override
  public Individual getNewIndividual( boolean _randomize,
                                      Random _r ) {
    return new IndividualTS( this,
                             _randomize,
                             _r );
  }

  @Override
  public Individual getNewIndividual( Individual individual ) {
    return new IndividualTS( (IndividualTS) individual );
  }

}
