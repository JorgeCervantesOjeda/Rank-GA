/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Problems;

/**
 *
 * @author usuario
 */
class City {

  private int index;
  private double x;

  City( int _i,
        double _x ) {
    this.index = _i;
    this.x = _x;
  }

  City() {
    this.index = -1;
    this.x = -1.0;
  }

  public int getIndex() {
    return index;
  }

  void setIndex( int _i ) {
    this.index = _i;
  }

  public double getX() {
    return x;
  }

  void setX( double _x ) {
    this.x = _x;
  }

}
