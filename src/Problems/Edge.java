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
public class Edge {

  int[] nodo;

  public Edge( int source,
                 int dest ) {
    nodo = new int[ 2 ];
    nodo[ 0 ] = source;
    nodo[ 1 ] = dest;
  }

  public int getSource() {
    return nodo[ 0 ];
  }

  void setSource( int s ) {
    nodo[0] = s;
  }

  public int getDest() {
    return nodo[ 1 ];
  }

  void setDest( int d ) {
    nodo[1] = d;
  }
}
