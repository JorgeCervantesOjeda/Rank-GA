/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Problems;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import rankga.Gene;
import rankga.Individual;
import rankga.Problem;

/**
 *
 * @author usuario
 */
public class ProblemTS_Jumps
  implements Problem {

  private int n;
  private double X[], Y[];
  private double M[][] = {
    { 0, 4, 1, 2 },
    { 4, 0, 2, 15 },
    { 1, 2, 0, 4 },
    { 2, 15, 4, 0 }
  };
  private Random r = new Random();
  private double trackFt = 0.0;
  private double covWeight = 0.5;

  private void readDatos() {

    BufferedReader csvReader = null;
    try {
      Path path = FileSystems.getDefault().getPath( "" ).toAbsolutePath();
      csvReader = new BufferedReader( new FileReader(
      path + "\\qatar194.tsp.txt" ) );
      System.out.println( "Leyendo: " + path + "\\qatar194.tsp.txt" );
      String row;

      if( null == ( row = csvReader.readLine() ) ) {
        csvReader.close();
        throw new IOException();
      }

      n = Integer.parseInt( row );
      this.X = new double[ n ];
      this.Y = new double[ n ];

      while( ( row = csvReader.readLine() ) != null ) {
        String data[] = row.split( " " );
        // do something with the data
        int i = Integer.parseInt( data[ 0 ] ) - 1;
        double x = Double.parseDouble( data[ 1 ] );
        double y = Double.parseDouble( data[ 2 ] );
        X[ i ] = x;
        Y[ i ] = y;
      }
      csvReader.close();
    } catch( FileNotFoundException ex ) {
      System.out.println( "------ No existe Datos.csv -------------" );
      System.out.println( ex );
    } catch( IOException ex ) {
      System.out.println( "------ No se pudo leer de Datos.csv -------------" );
      System.out.println( ex );
    } finally {
      try {
        csvReader.close();
      } catch( IOException ex ) {
        Logger.getLogger( ProblemDistricts.class.getName() ).log( Level.SEVERE,
                                                                  null,
                                                                  ex );
      }
    }
  }

  public ProblemTS_Jumps() {
    System.out.println( "Initializing TSP:" );

    this.readDatos();

    double suma = 0.0;

    this.M = new double[ n ][ n ];
    for( int i = 0;
         i < n;
         i++ ) {
      for( int j = i;
           j < n;
           j++ ) {
        M[ i ][ j ] = sqrt( pow( X[ i ] - X[ j ],
                                 2 ) + pow( Y[ i ] - Y[ j ],
                                            2 ) );
        if( i == j || r.nextDouble() < 0 ) {
          M[ i ][ j ] = 1e10;
        } else {
          suma += M[ i ][ j ];
        }
        M[ j ][ i ] = M[ i ][ j ];
        System.out.print( M[ i ][ j ] + " " );
      }
      System.out.println();
    }

    double promedio = 2 * suma / ( n * n - n );
    this.covWeight = promedio * 5;
  }

  @Override
  public void adapt( double _bestFitness ) {
  }

  public double fitness( Gene[] genome,
                         StringBuilder extraString ) {
    return fitnessJumps( genome,
                         extraString );
  }

  @Override
  public double fitness( Individual _i ) {
    throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
  }

  public double fitnessJumps( Gene[] genome,
                              StringBuilder extraString ) {
    long visitas = 0;
    int coverage[] = new int[ n ];
    int pos = 0;
    int orig = (int) genome[ pos ].getValue();
    if( orig >= n ) {
      pos = orig % n;
      orig = (int) genome[ pos ].getValue() % n;
    }
    pos = ( pos + 1 ) % ( 2 * n );

    int dest;
    double cost = 0;

    int next;
    for( int step = 0;
         step < n;
         step++ ) {
      next = (int) genome[ pos ].getValue();
      if( next >= n ) {
        pos = next % n;
        next = (int) genome[ pos ].getValue() % n;
      }
      pos = ( pos + 1 ) % ( 2 * n );

      coverage[ next ]++;
      cost += M[ orig ][ next ];
      orig = next;
    }
    int cov = 0;
    for( int i = 0;
         i < n;
         i++ ) {
      cov += coverage[ i ] > 0
             ? 1
             : 0;
      visitas += coverage[ i ] > 1
                 ? coverage[ i ] - 1
                 : 0;
    }
    extraString.append(
      cov
      + " " + String.format( "%18.17f",
                             cost )
      + " " + visitas );
    return cov * this.covWeight - cost;
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
    return "TSP_" + System.currentTimeMillis();
  }

  @Override
  public int getGenomeLength() {
    return 2 * n;
  }

  @Override
  public Gene getNewGene( boolean _randomize_p,
                          Random r ) {
    return new GeneInteger( 2 * n,
                            _randomize_p,
                            r );
  }

  @Override
  public Gene getNewGene( Gene _gene ) {
    return new GeneInteger( (GeneInteger) _gene );
  }

  @Override
  public double getGoalFt() {
    return 1e10;
  }

  @Override
  public int getDisplayModulus() {
    return 1;
  }

  @Override
  public Individual getNewIndividual( boolean _randomize,
                                      Random _r ) {
    return new Individual( this,
                           _randomize,
                           _r );
  }

  @Override
  public Individual getNewIndividual( Individual individual ) {
    return new Individual( (Individual) individual );
  }

}
