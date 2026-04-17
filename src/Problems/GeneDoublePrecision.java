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
public final class GeneDoublePrecision
  implements Gene {

  private double value;
  private final Random r;

  public GeneDoublePrecision( Random r,
                              double std ) {
    this.value = 0;
    this.r = r;
    this.mutate( std );
  }

  public GeneDoublePrecision( GeneDoublePrecision other ) {
    this.value = other.value;
    this.r = other.r;
  }

  @Override
  public double distanceTo( Gene other ) {
    return this.getValue() - other.getValue();
  }

  @Override
  public int getNumValues() {
    throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void setNumValues( int _numValues ) {
    throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void mutate( double std ) {
    //value = r.nextDouble();
    value += std * r.nextGaussian();
  }

  @Override
  public void setIntValue( int newValue ) {
    this.value = newValue;
  }

  @Override
  public void setDoubleValue( double newValue ) {
    this.value = newValue;
  }

  @Override
  public double getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.format( "%18.17f",
                          value );
  }

}
