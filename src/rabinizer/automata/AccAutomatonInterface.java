/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

/**
 *
 * @author jkretinsky
 */
public interface AccAutomatonInterface {

    public String toHOA();

    public String toDotty();

    public String acc();

    public int size();
    
    public int pairNumber();

}
