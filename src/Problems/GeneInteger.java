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
public class GeneInteger
  implements Gene {

  private int NUM_VALUES;
  protected int value;
  protected Random r;

  public GeneInteger( int numValues,
                      boolean randomize,
                      Random r ) {
    this.NUM_VALUES = numValues;
    this.r = r;
    this.value = (int) Math.floor( randomize
                                   ? r.nextDouble() * NUM_VALUES
                                   : 0 );
  }

  GeneInteger( GeneInteger other ) {
    this.value = other.value;
    this.NUM_VALUES = other.NUM_VALUES;
    this.r = other.r;
  }

  @Override
  public double distanceTo( Gene other ) {
    return ( this.value - other.getIntValue() != 0 )
           ? 1
           : 0;
  }

  @Override
  public void setIntValue( int newValue ) {
    if( newValue >= this.NUM_VALUES ) {
      return;
    }
    this.value = newValue;
  }

  @Override
  public void setNumValues( int _numValues ) {
    this.NUM_VALUES = _numValues;
  }

  @Override
  public void mutate( double p ) {
    if( r.nextDouble() >= p ) {
      return;
    }
    this.value = (int) Math.floor( r.nextDouble() * NUM_VALUES );
  }

  @Override
  public int getIntValue() {
    return value;
  }

  @Override
  public double getDoubleValue() {
    return value;
  }

  @Override
  public String toString() {
    Math.log10( this.NUM_VALUES );
    return "" + value;
  }

  @Override
  public void multiplyDoubleValue( double _d ) {
    throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
  }

}
