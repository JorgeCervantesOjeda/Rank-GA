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
import java.util.ArrayList;
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
public class ProblemTS_Reals
  implements Problem {

  private double[] X;
  private double[] Y;

  private int n;
  private double M[][] = {
    { 0, 4, 1, 2 },
    { 4, 0, 2, 15 },
    { 1, 2, 0, 4 },
    { 2, 15, 4, 0 }
  };
  private Random r = new Random();

  private ArrayList<City> cities = new ArrayList<>();

  private int compareCities( City _a,
                             City _b ) {
    return (int) Math.signum(
      Math.IEEEremainder( _a.getGene().getValue(),
                          1.0 )
      - Math.IEEEremainder( _b.getGene().getValue(),
                            1.0 ) );
  }

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
    }
    catch( FileNotFoundException ex ) {
      System.out.println( "------ No existe el archivo -------------" );
      System.out.println( ex );
    }
    catch( IOException ex ) {
      System.out.println( "------ No se pudo leer del archivo -------------" );
      System.out.println( ex );
    }
    finally {
      try {
        csvReader.close();
      }
      catch( IOException ex ) {
        Logger.getLogger( ProblemDistricts.class.getName() ).log( Level.SEVERE,
                                                                  null,
                                                                  ex );
      }
    }
  }

  public ProblemTS_Reals() {
    System.out.println( "Initializing TSP:" );

    this.readDatos();

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
        }
        M[ j ][ i ] = M[ i ][ j ];
        System.out.print( M[ i ][ j ] + " " );
      }
      System.out.println();
    }
  }

  @Override
  public double fitness( Individual _i ) {
    double sum = 0.0;
    double sumSqr = 0.0;
    cities.clear();
    for( int i = 0;
         i < n;
         i++ ) {
      cities.add(
        new City(
          i,
          (GeneDoublePrecision) _i.getGene( i )
        )
      );
    }
    cities.sort( ( a, b )
      -> compareCities( a,
                        b ) );

    int orig = cities.get( n - 1 ).getIndex();

    double cost = 0.0;
    for( int step = 0;
         step < n;
         step++ ) {
      int dest = cities.get( step ).getIndex();
      cost += M[ orig ][ dest ];
      orig = dest;
    }

    for( int i = 0;
         i < n;
         i++ ) {
      cities.get( i ).setGeneValue( 1.0 / n * i );
    }

    return -cost;
  }

  @Override
  public double getGlobalSearchIntensity() {
    return 1;
  }

  @Override
  public double getLocalSearchIntensity() {
    return 1.0 / n * 0.01;
  }

  @Override
  public String getProblemName() {
    return "TSP_Reals";
  }

  @Override
  public int getGenomeLength() {
    return n;
  }

  @Override
  public Gene getNewGene( boolean _randomize_p,
                          Random r ) {
    // Random-key encoding: randomize=false starts the locus deterministically at 0.0.
    return new GeneDoublePrecision( r,
                                    _randomize_p
                                    ? this.getGlobalSearchIntensity()
                                    : 0.0 );
  }

  @Override
  public Gene getNewGene( Gene _gene ) {
    return new GeneDoublePrecision( (GeneDoublePrecision) _gene );
  }

  @Override
  public double getGoalFt() {
    return 0;
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
    return new Individual( individual );
  }

}
