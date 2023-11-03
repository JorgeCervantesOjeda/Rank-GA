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
class Section {

  double x;
  double y;
  double perimeter;
  double area;
  double population;
  double border;

  ArrayList<Adjoining> adjoinings;

  Section() {
    adjoinings = new ArrayList<>();
  }

}
