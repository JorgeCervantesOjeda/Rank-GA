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
public class TrajTriplet {
    private int trajId[];
    
    public TrajTriplet( int t0, int t1, int t2 ){
        trajId = new int[3];
        trajId[0] = t0;
        trajId[1] = t1;
        trajId[2] = t2;
    }
    
    public int getTrajId( int i ){
        return trajId[i];
    }
}
