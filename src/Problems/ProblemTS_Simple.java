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
 * TSP variant that follows the same representation lineage as
 * `references/A_1006529012972.pdf`.
 *
 * @author usuario
 */
public class ProblemTS_Simple
  implements Problem {

  private double[] X;
  private double[] Y;
  private double covWeight;

  private int n;
  private double M[][] = {
    { 0, 4, 1, 2 },
    { 4, 0, 2, 15 },
    { 1, 2, 0, 4 },
    { 2, 15, 4, 0 }
  };
  private final Random r;

  private void readDatos() {

    BufferedReader csvReader = null;
    try {
      Path path = FileSystems.getDefault().getPath( "" ).toAbsolutePath();
      Path dataFile = path.resolve( "data" )
        .resolve( "qatar194.tsp.txt" );
      csvReader = new BufferedReader( new FileReader( dataFile.toFile() ) );
      System.out.println( "Leyendo: " + dataFile );
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
      System.out.println( "------ No existe data/qatar194.tsp.txt -------------" );
      System.out.println( ex );
    } catch( IOException ex ) {
      System.out.println( "------ No se pudo leer de data/qatar194.tsp.txt -------------" );
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

  public ProblemTS_Simple() {
    this( new Random() );
  }

  public ProblemTS_Simple( Random random ) {
    System.out.println( "Initializing TSP:" );
    this.r = random;

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
      this.covWeight = 2 * suma / ( n * n - n );
    }
  }

  public double fitness( Gene[] genome,
                         StringBuilder extraString ) {
    double cost = 0.0;
    int coverage[] = new int[ n ];
    int orig = (int) genome[ n - 1 ].getValue();

    for( int step = 0;
         step < n;
         step++ ) {
      int dest = (int) genome[ step ].getValue();
      coverage[ dest ]++;
      cost += M[ orig ][ dest ];
      orig = dest;
    }
    int cov = 0;
    for( int i = 0;
         i < n;
         i++ ) {
      cov += coverage[ i ] > 0
             ? 1
             : 0;
    }
    extraString.append(
      cov + " " + cost + " " );

    return cov * this.covWeight - cost;
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
    // Global exploration: a mutated locus is effectively free to jump anywhere.
    double genomeLength = this.getGenomeLength();
    return 1.0 - 1.0 / genomeLength;
  }

  @Override
  public double getLocalSearchIntensity() {
    // Minimal relevant mutation: about one locus changed per genome on average.
    double genomeLength = this.getGenomeLength();
    return 1.0 / genomeLength;
  }

  @Override
  public String getProblemName() {
    return "TSP_Simple_" + this.n;
  }

  @Override
  public int getGenomeLength() {
    return n;
  }

  @Override
  public Gene getNewGene( boolean _randomize_p,
                          Random r ) {
    return new GeneTS( n,
                       _randomize_p,
                       r );
  }

  @Override
  public Gene getNewGene( Gene _gene ) {
    return new GeneTS( (GeneTS) _gene );
  }

  @Override
  public double getGoalFt() {
    return n * this.covWeight;
  }

  @Override
  public int getDisplayModulus() {
    return 1;
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
