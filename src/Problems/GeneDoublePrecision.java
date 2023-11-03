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
public class GeneDoublePrecision
  implements Gene {

  double value;
  double std;
  Random r;

  public GeneDoublePrecision( double std,
                              Random r ) {
    this.value = 0;
    this.std = std;
    this.r = r;
    mutate( std );
  }

  public GeneDoublePrecision( GeneDoublePrecision other ) {
    this.value = other.value;
    this.std = other.std;
    this.r = other.r;
  }

  @Override
  public double distanceTo( Gene other ) {
    return this.getDoubleValue() - other.getDoubleValue();
  }

  @Override
  public void setNumValues( int _numValues ) {
    throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void mutate( double p ) {
    //value = r.nextDouble();
    value = ( 1 - p ) * value + p * std * r.nextGaussian();
  }

  @Override
  public int getIntValue() {
    return (int) value;
  }

  @Override
  public void setIntValue( int newValue ) {
    this.value = newValue;
  }

  @Override
  public double getDoubleValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.format( "%18.17f",
                          value );
  }

  @Override
  public void multiplyDoubleValue( double _d ) {
    this.value *= _d;
  }

}
