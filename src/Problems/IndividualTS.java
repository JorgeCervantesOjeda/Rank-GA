/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Problems;

import java.util.Random;
import rankga.Individual;
import rankga.Problem;

/**
 *
 * @author usuario
 */
public class IndividualTS
  extends Individual {

  public IndividualTS( Problem _problem,
                       boolean _randomize,
                       Random _r ) {
    super( _problem,
           _randomize,
           _r );
  }

  public IndividualTS( IndividualTS _individual ) {
    super( _individual );
  }

  @Override
  public void mutate( double p ) {

    for( int i = 0;
         i < genome.length;
         i++ ) {
      GeneTS g = ( (GeneTS) ( this.genome[ i ] ) );
      g.mutate( p,
                i,
                this.genome );
    }
  }

}
