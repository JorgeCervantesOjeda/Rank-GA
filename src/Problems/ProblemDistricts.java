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
import static java.lang.Math.abs;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
public class ProblemDistricts
  implements Problem {

  public final int NUM_SECTIONS;
  public final int NUM_DISTRICTS;

  private Section[] sections;

  @Override
  public void adapt( double _bestFitness ) {
    // Fixed input data; no adaptive parameters.
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
    return 1.0 / this.NUM_DISTRICTS;
  }

  @Override
  public double getLocalSearchIntensity() {
    return 1.0 / this.NUM_SECTIONS;
  }

  @Override
  public String getProblemName() {
    return "Districts_" + this.NUM_SECTIONS + "_" + this.NUM_DISTRICTS;
  }

  public Section[] getSections() {
    return sections;
  }

  public ProblemDistricts() {
    NUM_SECTIONS = 3135;
    NUM_DISTRICTS = 19;

    sections = new Section[ NUM_SECTIONS ];

    readDatos();
    readColindancias();
  }

  private Double polygonArea( ArrayList<Point> hull ) {
    // Initialize area
    double area = 0.0;
    int n = hull.size();

    // Calculate value of shoelace formula
    Point a;
    Point b;

    int j = n - 1;
    for( int i = 0;
         i < n;
         i++ ) {
      a = hull.get( i );
      b = hull.get( j );
      area += ( b.x + a.x ) * ( b.y - a.y );
      j = i;  // j is previous vertex to i
    }

    // Return absolute value
    return abs( area / 2.0 );
  }

  private double polygonPerimeter( ArrayList<Point> _hull ) {
    double perimeter = 0;
    Point last = _hull.get( 0 );
    for( Point p
         : _hull ) {
      if( p != last ) {
        perimeter += Math.sqrt( Math.pow( p.x - last.x,
                                          2 )
                                + Math.pow( p.y - last.y,
                                            2 ) );
      }
      last = p;
    }
    Point p = _hull.get( 0 );
    perimeter += Math.sqrt( Math.pow( p.x - last.x,
                                      2 )
                            + Math.pow( p.y - last.y,
                                        2 ) );

    return perimeter;
  }

  private void readDatos() {
    BufferedReader csvReader = null;
    try {
      Path path = FileSystems.getDefault().getPath( "" ).toAbsolutePath();
      csvReader = new BufferedReader( new FileReader( path + "\\datos.csv" ) );
      System.out.println( "Leyendo: " + path + "\\datos.csv" );
      String row;
      while( ( row = csvReader.readLine() ) != null ) {
        String[] data = row.split( "," );
        // do something with the data
        int sectionId = Integer.parseInt( data[ 0 ] ) - 1;
        double perimeter = Double.parseDouble( data[ 1 ] );
        double x = Double.parseDouble( data[ 2 ] );
        double y = Double.parseDouble( data[ 3 ] );
        double area = Double.parseDouble( data[ 4 ] );
        double population = Double.parseDouble( data[ 5 ] );
        double border = Double.parseDouble( data[ 6 ] );

        sections[ sectionId ] = new Section();
        sections[ sectionId ].perimeter = perimeter;
        sections[ sectionId ].x = x;
        sections[ sectionId ].y = y;
        sections[ sectionId ].area = area;
        sections[ sectionId ].population = population;
        sections[ sectionId ].border = border;
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

  private void readColindancias() {

    BufferedReader csvReader = null;
    try {
      Path path = FileSystems.getDefault().getPath( "" ).toAbsolutePath();
      csvReader = new BufferedReader( new FileReader(
      path + "\\colindancias.csv" ) );
      System.out.println( "Leyendo: " + path + "\\colindancias.csv" );
      String row;
      while( ( row = csvReader.readLine() ) != null ) {
        String[] data = row.split( "," );
        // do something with the data
        int rowId = Integer.parseInt( data[ 0 ] ) - 1;
        int a = Integer.parseInt( data[ 1 ] ) - 1;
        int b = Integer.parseInt( data[ 2 ] ) - 1;
        double longitud = Double.parseDouble( data[ 3 ] );

        sections[ a ].adjoinings.add( new Adjoining( b,
                                                     longitud ) );
      }
      csvReader.close();
    } catch( FileNotFoundException ex ) {
      System.out.println( "------ No existe Colindancias.csv -------------" );
      System.out.println( ex );
    } catch( IOException ex ) {
      System.out.println(
        "------ No se pudo leer de Colindancias.csv -------------" );
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

  // To find orientation of ordered triplet (p, q, r).
  // The function returns following values
  // 0 --> p, q and r are colinear
  // 1 --> Clockwise
  // 2 --> Counterclockwise
  public static int orientation( Point p,
                                 Point q,
                                 Point r ) {
    double val = ( q.y - p.y ) * ( r.x - q.x )
                 - ( q.x - p.x ) * ( r.y - q.y );

    if( val == 0 ) {
      return 0;  // collinear
    }
    return ( val > 0 )
           ? 1
           : -1; // clock or counterclock wise
  }

  // Prints convex hull of a set of n points.
  public static ArrayList<Point> convexHull( List<Point> points ) {
    // There must be at least 3 points
    if( points.size() < 3 ) {
      return null;
    }

    // Initialize Result
    ArrayList<Point> hull = new ArrayList<>();

    // Find the leftmost point
    int left = 0;
    for( int i = 1;
         i < points.size();
         i++ ) {
      if( points.get( i ).x < points.get( left ).x ) {
        left = i;
      }
    }

    // Start from leftmost point, keep moving
    // counterclockwise until reach the start point
    // again. This loop runs O(h) times where h is
    // number of points in result or output.
    int p = left, q;
    do {
      // Add current point to result
      hull.add( points.get( p ) );

      // Search for a point 'q' such that
      // orientation(p, q, x) is counterclockwise
      // for all points 'x'. The idea is to keep
      // track of last visited most counterclock-
      // wise point in q. If any point 'i' is more
      // counterclock-wise than q, then update q.
      q = ( p + 1 ) % points.size();

      for( int i = 0;
           i < points.size();
           i++ ) {
        // If i is more counterclockwise than
        // current q, then update q
        if( orientation( points.get( p ),
                         points.get( i ),
                         points.get( q ) )
            == -1 ) {
          q = i;
        }
      }

      // Now q is the most counterclockwise with
      // respect to p. Set p as q for next iteration,
      // so that q is added to result 'hull'
      p = q;

    } while( p != left );  // While we don't come to first point

    return hull;
  }

  private double sectionBordersExcluding( Section s,
                                          int clusterId, // exclude this
                                          Gene[] genome ) {
    double sum = s.border;
    for( Adjoining a
         : s.adjoinings ) {
      int otherClusterId = (int) genome[ a.sectionId ].getValue();
      if( clusterId != otherClusterId ) {
        sum += a.borderLength;
      }
    }
    return sum;
  }

  private Double area( ArrayList<Point> hull ) {
    // Initialize area
    double area = 0.0;
    int n = hull.size();

    // Calculate value of shoelace formula
    Point a;
    Point b;

    int j = n - 1;
    for( int i = 0;
         i < n;
         i++ ) {
      a = hull.get( i );
      b = hull.get( j );
      area += ( b.x + a.x ) * ( b.y - a.y );
      j = i;  // j is previous vertex to i
    }

    // Return absolute value
    return abs( area / 2.0 );
  }

  private double perimeter( ArrayList<Point> _hull ) {
    double perimeter = 0;
    Point last = _hull.get( 0 );
    for( Point p
         : _hull ) {
      if( p != last ) {
        perimeter += Math.sqrt( Math.pow( p.x - last.x,
                                          2 )
                                + Math.pow( p.y - last.y,
                                            2 ) );
      }
      last = p;
    }
    Point p = _hull.get( 0 );
    perimeter += Math.sqrt( Math.pow( p.x - last.x,
                                      2 )
                            + Math.pow( p.y - last.y,
                                        2 ) );

    return perimeter;
  }

  private double fitnessA( Gene[] genome,
                           StringBuilder extraString ) {

    // Hashing of sections by cluster id
    // Create the array of clusters
    ArrayList<ArrayList<Point>> clusters = new ArrayList<>();
    ArrayList<Double> populations = new ArrayList<>();
    for( int i = 0;
         i < this.NUM_DISTRICTS;
         i++ ) {
      clusters.add( new ArrayList<>() );
      populations.add( 0.0 );
    }
    // add points to clusters
    for( int i = 0;
         i < this.NUM_SECTIONS;
         i++ ) {
      Point point = new Point( sections[ i ].x,
                               sections[ i ].y );
      int clusterId = (int) genome[ i ].getValue();

      clusters.get( clusterId ).add( point );
      populations.set( clusterId,
                       populations.get( clusterId )
                       + sections[ i ].population );
    }

    // calculate compactness
    double compactness = 0;
    for( int i = 0;
         i < clusters.size();
         i++ ) {
      ArrayList<Point> hull = convexHull( clusters.get( i ) );
      compactness +=
      Math.pow( perimeter( hull ),
                2 )
      / area( hull );
    }
    compactness /= this.NUM_DISTRICTS;

    // calculate equality
    double populationSum = 0;
    for( Double population
         : populations ) {
      populationSum += population;
    }
    double populationAvg = populationSum / populations.size();
    double equality = 0;
    for( Double population
         : populations ) {
      equality += Math.abs( population - populationAvg );
    }
    equality /= this.NUM_DISTRICTS;

    // calculate fitness
    double fitness = compactness + equality;

    extraString.append( "e:" + equality + "\tc:" + compactness );
    return -fitness;
  }

  public double fitnessB( Gene[] genome,
                          StringBuilder extraString ) {

    double areas[];
    double perimeters[];
    double populations[];

    areas = new double[ this.NUM_DISTRICTS ];
    perimeters = new double[ this.NUM_DISTRICTS ];
    populations = new double[ this.NUM_DISTRICTS ];

    for( int sectionId = 0;
         sectionId < this.NUM_SECTIONS;
         sectionId++ ) {
      int clusterId = (int) genome[ sectionId ].getValue();
      areas[ clusterId ] += sections[ sectionId ].area;
      perimeters[ clusterId ] += sectionBordersExcluding( sections[ sectionId ],
                                                          clusterId,
                                                          genome );
      populations[ clusterId ] += sections[ sectionId ].population;
    }

    // calculate compactness
    double compactness = 0;
    for( int districtId = 0;
         districtId < this.NUM_DISTRICTS;
         districtId++ ) {
      compactness += 1
                     * Math.pow( perimeters[ districtId ],
                                 2 )
                     / areas[ districtId ];
    }
    compactness /= this.NUM_DISTRICTS;

    // calculate equality
    double populationSum = 0;
    for( int districtId = 0;
         districtId < this.NUM_DISTRICTS;
         districtId++ ) {
      populationSum += populations[ districtId ];
    }
    double populationAvg = populationSum / this.NUM_DISTRICTS;
    double equality = 0;
    for( Double population
         : populations ) {
      equality += Math.pow( population - populationAvg,
                            2 );
    }
    equality /= this.NUM_DISTRICTS;
    // root mean square deviation
    equality = Math.sqrt( equality ) * 1e-5;

    // calculate fitness
    double fitness = compactness + equality;

    extraString.append( "e:" + equality + "\tc:" + compactness );
    return -fitness;
  }

  public double fitness( Gene[] genome,
                         StringBuilder extraString ) {
    double ftB = fitnessB( genome,
                           extraString );
    extraString.append( "\t" );
    double ftA = fitnessA( genome,
                           extraString );
    extraString.append( "\t" + ftA + "\t" );
    return ftB;
  }

  @Override
  public int getDisplayModulus() {
    return 1;
  }

  @Override
  public int getGenomeLength() {
    return NUM_SECTIONS;
  }

  @Override
  public double getGoalFt() {
    return 0;
  }

  @Override
  public Gene getNewGene( boolean _randomize_p,
                          Random _r ) {
    return new GeneDistricts( NUM_DISTRICTS,
                              _randomize_p,
                              _r );
  }

  @Override
  public Gene getNewGene( Gene _gene_p ) {
    return new GeneDistricts( (GeneDistricts) _gene_p );
  }

  @Override
  public Individual getNewIndividual( boolean _randomize,
                                      Random _r ) {
    return new IndividualDistricts( this,
                                    _randomize,
                                    _r );
  }

  @Override
  public Individual getNewIndividual( Individual individual ) {
    return new IndividualDistricts( (IndividualDistricts) individual );
  }

}
