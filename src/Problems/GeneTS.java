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
public class GeneTS
  extends GeneInteger {

  public GeneTS( int _numValues,
                 boolean _randomize,
                 Random _r ) {
    super( _numValues,
           _randomize,
           _r );
  }

  GeneTS( GeneTS _gene ) {
    super( _gene );
  }

  public void mutate( double p,
                      int forbidden,
                      Gene[] genome ) {

    if( r.nextDouble() >= p ) {
      return;
    }
    do {
      super.mutate( p );
    } while( this.value == forbidden );
  }

}
