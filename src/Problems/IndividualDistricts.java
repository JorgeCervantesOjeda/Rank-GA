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
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import rankga.Individual;
import rankga.Problem;

/**
 *
 * @author usuario
 */
public class IndividualDistricts
  extends Individual {

  private final ProblemDistricts pd;
  private boolean[] visitedSection;
  private boolean[] visitedDistrict;
  private boolean[] mutatedSection;

  public IndividualDistricts( Problem _problem,
                              boolean _randomize,
                              Random _r ) {
    super( _problem,
           _randomize,
           _r );
    this.readEjemploCSV();
    this.pd = (ProblemDistricts) problem;
    this.visitedSection = new boolean[ this.pd.NUM_SECTIONS ];
    this.visitedDistrict = new boolean[ this.pd.NUM_DISTRICTS ];
    this.mutatedSection = new boolean[ this.pd.NUM_SECTIONS ];
    this.eliminateExclaves();
  }

  public IndividualDistricts( IndividualDistricts _individual ) {
    super( _individual );
    this.pd = (ProblemDistricts) super.problem;
    this.visitedSection = new boolean[ this.pd.NUM_SECTIONS ];
    this.visitedDistrict = new boolean[ this.pd.NUM_DISTRICTS ];
    this.mutatedSection = new boolean[ this.pd.NUM_SECTIONS ];
  }

  private void readEjemploCSV() {
    BufferedReader csvReader = null;
    try {
      Path path = FileSystems.getDefault().getPath( "" ).toAbsolutePath();
      csvReader = new BufferedReader( new FileReader( path + "\\ejemplo.csv" ) );
      System.out.println( "Leyendo: " + path + "\\ejemplo.csv" );
      String row;
      while( ( row = csvReader.readLine() ) != null ) {
        String[] data = row.split( "," );
        // do something with the data
        this.genome[ Integer.parseInt( data[ 0 ] ) ].setIntValue(
          Integer.parseInt( data[ 1 ] )
        );
      }
      csvReader.close();
    }
    catch( FileNotFoundException ex ) {
      System.out.println( "------ No existe Datos.csv -------------" );
      System.out.println( ex );
    }
    catch( IOException ex ) {
      System.out.println( "------ No se pudo leer de Datos.csv -------------" );
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

  @Override
  public void mutate( double p ) {

    Section[] sections = pd.getSections();

    for( int i = 0;
         i < genome.length;
         i++ ) {
      if( i == 473 ) {
        i = 0 + i;
      }
      GeneDistricts g = ( (GeneDistricts) ( this.genome[ i ] ) );
      g.mutate( p,
                sections[ i ],
                this.genome );
    }
    this.eliminateExclaves();
  }

  @Override
  public void recombinate( Individual that ) {
    super.recombinate( that );
    this.eliminateExclaves();
  }

  private void eliminateExclaves() {
    // System.out.print( "\nIndividual:" + this.p + " ------------------" );
    int count = 0;
    do {
      count++;
    }
    while( this.mutateAllExclaves() );
    // System.out.print( "\nAttempts: " + count );
  }

  private boolean mutateAllExclaves() {

    boolean hadExclaves = false;

    // go through each section
    for( int sectionId = 0;
         sectionId < this.pd.NUM_SECTIONS;
         sectionId++ ) {
      if( !visitedSection[ sectionId ] ) {

        int districtId = (int) this.genome[ sectionId ].getValue();
        int numDistrictSections = this.countSections( districtId );

//        System.out.print(
//          "\nNeighbors of s:" + sectionId + " d:" + districtId + "\t" );
        clearVisitedSections();
        visitedSection[ sectionId ] = true;
        int count = 1 + this.visitNeighbors( sectionId,
                                             districtId );
        if( count < numDistrictSections / 2 ) {
          // exclave
          // System.out.print(
          //   "\nd:" + districtId + " s:" + sectionId
          //   + " " + count + "/" + numDistrictSections );
          hadExclaves = true;
          this.clearMutatedSections();
          this.mutateExclave( sectionId,
                              districtId,
                              0 );
        }
      }
    }
    return hadExclaves;
  }

  private void clearMutatedSections() {
    for( int i = 0;
         i < this.pd.NUM_SECTIONS;
         i++ ) {
      this.mutatedSection[ i ] = false;
    }
  }

  private int countSections( int district ) {
    int count = 0;
    for( int i = 0;
         i < this.pd.NUM_SECTIONS;
         i++ ) {
      if( district == (int) this.genome[ i ].getValue() ) {
        count++;
      }
    }
    return count;
  }

  private void clearVisitedSections() {
    for( int i = 0;
         i < this.pd.NUM_SECTIONS;
         i++ ) {
      this.visitedSection[ i ] = false;
    }
  }

  private int visitNeighbors( int sectionId,
                              int districtId ) {

    int count = 0;

    // System.out.print( sectionId + " " );
    Section[] sections = this.pd.getSections();
    Section s = sections[ sectionId ];
    for( int i = 0;
         i < s.adjoinings.size();
         i++ ) {
      int neighborSectionId = s.adjoinings.get( i ).sectionId;
      if( this.visitedSection[ neighborSectionId ] ) {
        continue;
      }
      if( districtId == (int) this.genome[ neighborSectionId ].getValue() ) {
        // mark
        this.visitedSection[ neighborSectionId ] = true;
        count++;
        count += this.visitNeighbors( neighborSectionId,
                                      districtId );
      }
    }
    return count;
  }

  private int mutateExclave( int sectionId,
                             int districtId,
                             int depth ) {
    int count = 1;
    //System.out.print(
    //  "depth:" + depth + " s:" + sectionId + " d:" + districtId );
    Section[] sections = this.pd.getSections();
    Section s = sections[ sectionId ];
    GeneDistricts g = ( (GeneDistricts) ( genome[ sectionId ] ) );
    g.mutate( 1.0,
              s,
              genome );
    // System.out.println( " to d:" + g.getIntValue() );
    this.mutatedSection[ sectionId ] = true;
    this.visitedSection[ sectionId ] = false;

    // mutate neighboring district sections
    for( int i = 0;
         i < s.adjoinings.size();
         i++ ) {
      int neighborSectionId = s.adjoinings.get( i ).sectionId;
      if( this.mutatedSection[ neighborSectionId ] ) {
        continue;
      }
      if( districtId == (int) this.genome[ neighborSectionId ].getValue() ) {
        count += this.mutateExclave( neighborSectionId,
                                     districtId,
                                     depth + 1 );
      }
    }
    // System.out.println( "mutated:" + count + " depth:" + depth );
    return count;
  }

}
