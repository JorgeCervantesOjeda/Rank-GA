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
  private GeneDoublePrecision g;

  City( int _i,
        GeneDoublePrecision _g ) {
    this.index = _i;
    this.g = _g;
  }

  public int getIndex() {
    return index;
  }

  void setIndex( int _i ) {
    this.index = _i;
  }

  public GeneDoublePrecision getGene() {
    return g;
  }

  public void setGeneValue( double x ) {
    g.setDoubleValue( x );
  }

}
