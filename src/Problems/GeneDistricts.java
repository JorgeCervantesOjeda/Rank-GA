/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Problems;

import java.util.Random;
import rankga.Gene;

/**
 *
 * @author usuario
 */
public class GeneDistricts
  extends GeneInteger {

  public GeneDistricts( int _numValues,
                        boolean _randomize,
                        Random _r ) {
    super( _numValues,
           _randomize,
           _r );
  }

  GeneDistricts( GeneDistricts _gene ) {
    super( _gene );
  }

  public void mutate( double p,
                      Section s,
                      Gene[] genome ) {

    if( r.nextDouble() >= p ) {
      return;
    }
    // pick a neighbor
    int index = (int) ( r.nextDouble() * s.adjoinings.size() );
    int sectionId = s.adjoinings.get( index ).sectionId;

    // mutate to neighbor's district
    this.value = (int) genome[ sectionId ].getValue();
  }

}
