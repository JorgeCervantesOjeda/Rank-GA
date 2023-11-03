/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Problems;

import java.util.ArrayList;

/**
 *
 * @author usuario
 */
public class NodePair {
    private int origen;
    private int destino;
    private int [][] trayectorias;
    private ArrayList<TrajTriplet> ternas;
    private int ternaIndex;
    private int trayectoriaIndex;
    private int nodoIndex;
    
    public NodePair( int origen, int destino, int [][] trayectorias, ArrayList<TrajTriplet> ternas ){
        this.origen         = origen;
        this.destino        = destino;
        this.trayectorias   = trayectorias;
        this.ternas         = ternas;
        reset();
    }
    
    public void reset(){
        ternaIndex          = 0;
        trayectoriaIndex    = 0;
        nodoIndex           = 0;
    }
    
    private int nextTernaIndex(){
        ternaIndex = ( ternaIndex + 1 ) % ternas.size();
        return ternaIndex;
    }
    
    private int nextTrayectoriaIndex(){
        trayectoriaIndex = ( trayectoriaIndex + 1 ) % 3;
        return trayectoriaIndex;
    }
    
    private int nextNodoIndex( ){
        nodoIndex = ( nodoIndex + 1 ) % trayectorias[ trayectoriaIndex ].length;
        return nodoIndex;
    }
    
    public int nextNodo(){
        if( nextNodoIndex()==0 )
            if( nextTrayectoriaIndex()==0 )
                if( nextTernaIndex()==0 )
                    return -1;
        return trayectorias[ trayectoriaIndex ][ nodoIndex ];
    }
    
    public int getDestino(){
        return destino;
    }

    public int getNumTernas() {
        return ternas.size();
    }

    public TrajTriplet getTerna(int ternaId) {
        return ternas.get(ternaId);
    }

    int[][] getTrayectorias() {
        return trayectorias;
    }
}
