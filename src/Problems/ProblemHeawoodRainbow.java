/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Problems;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.nio.file.Path;
import rankga.Gene;
import rankga.Individual;
import rankga.Problem;
import rankga.RunOutputPaths;

/**
 * Rainbow-coloring benchmark in the Moore-cage / Heawood line of work.
 * See `references/rainbow_connectivity_rank_ga_moore_cages_girth_six.pdf`.
 *
 * @author usuario
 */
public class ProblemHeawoodRainbow
    implements Problem {

  public static int NUM_COLORS;
  public static Node[] nodes;
  public static int[][] index;
  // public static int[][] trajsAdj;
  public static int[][][][] trajectories;
  public static List<List<ArrayList<TrajTriplet>>> trajTriplets;
  public static Edge[] edges;
  public static ArrayList<NodePair> nodePairs;
  private Gene[] genome;
  private int countNodePairsOK;
  // private int[] parejasOK;

  public ProblemHeawoodRainbow( int num_colors ) {
    if( num_colors < 2 ) {
      throw new IllegalArgumentException(
        "Heawood rainbow requires at least 2 colors" );
    }

    this.NUM_COLORS = num_colors;

    int source, dest;

    // inicializa matriz de adyacencia de rankga
    initNodes();
    initEdges();

    //**************************************************
    // find trajectories between source/dest node pairs
    //**************************************************
    trajectories = new int[ 14 ][ 14 ][][];
    for( source = 0;
         source < 13;
         source++ ) {
      for( dest = source + 1;
           dest < 14;
           dest++ ) {
        if( source != dest ) {
          trajectories[ source ][ dest ] = findTrajectories( source,
                                                             dest,
                                                             NUM_COLORS );
        }
      }
    }

    writeTrajs();

    //**************************************************
    // Encuentra las trajectories disjuntas
    //**************************************************
    trajTriplets = new ArrayList<>();
    for( int i = 0;
         i < 13;
         i++ ) { // i: origen
      List<ArrayList<TrajTriplet>> row = new ArrayList<>();
      for( int j = 0;
           j < 14;
           j++ ) {
        row.add( new ArrayList<>() );
      }
      trajTriplets.add( row );
    }
    for( int i = 0;
         i < 13;
         i++ ) {
      for( int j = i + 1;
           j < 14;
           j++ ) { // j: destino
        trajTriplets.get( i ).set( j,
                                   findDisjointTrajTriplets( trajectories[ i ][ j ] ) );
      }
    }
    writeTrajsDisj();

    // Crea nodePairs de puntos con sus ternas de trajectories disjuntas
    nodePairs = new ArrayList<>();
    int k = 0;
    for( int i = 0;
         i < 13;
         i++ ) {
      for( int j = i + 1;
           j < 14;
           j++ ) {
        nodePairs
          .add( new NodePair( i,
                              j,
                              trajectories[ i ][ j ],
                              trajTriplets.get( i ).get( j ) ) );
        System.out.println( "p" + k + " " + i + " " + j );
        k++;
      }
    }

  }

  // *******************************************************
  // Methods to obtain trajectories
  // *******************************************************
  private static void writeTrajsDisj() {
    int source, dest;
    File f;
    f = outputFile( "trayDisjuntas.txt" );
    try {
      FileWriter w = new FileWriter(f);
      BufferedWriter bw = new BufferedWriter(w);
      PrintWriter wr = new PrintWriter(bw);

      for (int i = 0; i < 13; i++) // i: origen
      {
        for (int j = i + 1; j < 14; j++) {// j: destino
          wr.append("Ternas de trayectorias disjuntas entre " + i + " y "
              + j + ": ");
          bw.newLine();
          bw.newLine();
          ArrayList<TrajTriplet> ternas = trajTriplets.get( i ).get( j );
          for (int m = 0; m < ternas.size(); m++) {
            wr.append("terna " + m + ":");
            bw.newLine();
            for (int n = 0; n < 3; n++) {
              TrajTriplet terna = ternas.get( m );
              int trajId = terna.getTrajId( n );
              wr.append(n + "\t");
              for (int k = 0; k < trajectories[i][j][ trajId ].length; k++) {
                wr.append( trajectories[i][j][ trajId ][k]
                           + "\t"); // escribimos en el archivo
              }
              bw.newLine();
            }
          }
          bw.newLine();
        }
      }
      // ahora cerramos los flujos de canales de datos,
      // al cerrarlos el archivo quedará guardado con información escrita
      // de no hacerlo no se escribirá nada en el archivo
      wr.close();
      bw.close();

    } catch (IOException e) {
    }
    ;
  }

  private static void writeTrajs() {
    int source, dest;
    File f;
    f = outputFile( "salida.txt" );
    try {
      FileWriter w = new FileWriter(f);
      BufferedWriter bw = new BufferedWriter(w);
      PrintWriter wr = new PrintWriter(bw);

      for (source = 0; source < 13; source++) {
        for (dest = source + 1; dest < 14; dest++) {
          wr
              .append(
                  "Trayectorias entre " + source + " y " + dest + ": " + trajectories[source][dest].length);
          bw.newLine();
          bw.newLine();
          int k = 0;
          for (int[] item : trajectories[source][dest]) {
            wr.append(k++ + "\t");
            for (int n = 0; n < item.length; n++) {
              wr.append(" " + item[n]); // escribimos en el archivo
              wr.append(",\t"); // concatenamos en el archivo sin borrar lo existente
            }
            bw.newLine();
          }
          bw.newLine();
        }
      }
      // ahora cerramos los flujos de canales de datos,
      // al cerrarlos el archivo quedará guardado con información escrita
      // de no hacerlo no se escribirá nada en el archivo
      wr.close();
      bw.close();

    } catch (IOException e) {
    }
    ;
  }

  private static File outputFile( String fileName ) {
    Path directory = RunOutputPaths.ensureFamilyDirectory(
      ProblemHeawoodRainbow.class );
    return directory.resolve( fileName )
      .toFile();
  }

  // Matriz de Heawood
  public static void initNodes() {
    nodes = new Node[14];
    for (int i = 0; i < 14; i++) {
      nodes[i] = new Node();
    }

    for (int i = 0; i < 14; i++) {
      // +1
      nodes[i].neighbors[0] = (14 + i + 1) % 14;
      // -1
      nodes[i].neighbors[1] = (14 + i - 1) % 14;
      // +5 -5
      nodes[i].neighbors[2] = (14 + i - ((i % 2) * 2 - 1) * 5) % 14;
    }
  }

  public static void initEdges() {
    edges = new Edge[21];
    int temp;
    for (int i = 0, k = 0; i <= 18; i = i + 3, k = k + 2) {
      edges[i] = new Edge(k,
          nodes[k].neighbors[0]);
      edges[i + 1] = new Edge(k,
          nodes[k].neighbors[1]);
      edges[i + 2] = new Edge(k,
          nodes[k].neighbors[2]);

      for (int m = 0; m < 3; m++) {
        if (edges[i + m].getSource() > edges[i + m].getDest()) {
          temp = edges[i + m].getSource();
          edges[i + m].setSource(edges[i + m].getDest());
          edges[i + m].setDest(temp);
        }
      }
    }

    for (int i = 0; i < 21; i++) {
      System.out.println(edges[i].getSource() + ", " + edges[i]
          .getDest());
    }

    index = new int[14][14];
    for (int a = 0; a < 21; a++) {
      index[edges[a].getSource()][edges[a].getDest()] = a;
    }

  }

  public static int[][] findTrajectories(int source,
      int dest,
      int length) {
    int result[][];
    int subresult[][][];

    if (nodes[source].visited) {
      return null;
    }

    if (Objects.equals(source,
        dest)) {
      // base case
      result = new int[1][];
      result[0] = new int[1];
      result[0][0] = dest;
      return result;
    }

    if (length <= 0) {
      return null;
    }

    subresult = new int[3][][];
    nodes[source].visited = true;
    int n = 0;
    for (int i = 0; i < 3; i++) {
      // recursive call
      subresult[i] = findTrajectories(nodes[source].neighbors[i],
          dest,
          length - 1);
      n = n + (subresult[i] == null
          ? 0
          : subresult[i].length);
    }
    nodes[source].visited = false;

    result = new int[n][];
    int k = 0;
    int j;
    int i;
    int m;
    for (i = 0; i < 3; i++) {
      for (m = 0; m < (subresult[i] == null
          ? 0
          : subresult[i].length); m++) {
        result[k] = new int[subresult[i][m].length + 1];
        result[k][0] = source;
        for (j = 0; j < subresult[i][m].length; j++) {
          result[k][j + 1] = subresult[i][m][j];
        }
        k++;
      }
    }
    return result;

  }

  // Algoritmo para encontrar las ternas de trajectories disjuntas
  public static ArrayList<TrajTriplet> findDisjointTrajTriplets(
      int[][] trayectorias) {

    ArrayList<TrajTriplet> result = new ArrayList<TrajTriplet>();
    for (int i = 0; i < trayectorias.length - 2; i++) {
      for (int j = i + 1; j < trayectorias.length - 1; j++) {
        if (disjunta(i,
            j,
            trayectorias)) {
          for (int k = j + 1; k < trayectorias.length; k++) {
            if (disjunta(i,
                k,
                trayectorias)
                && disjunta(j,
                    k,
                    trayectorias)) {
              result.add(new TrajTriplet(i,
                  j,
                  k));// guardarTrio( i,j,k );
            }
          }
        }
      }
    }
    return result;
  }

  public static boolean disjunta(int i,
      int j,
      int[][] trayectorias) {
    boolean[] nodoUsado = new boolean[14];
    for (int m = 0; m < 14; m++) {
      nodoUsado[m] = false;
    }
    for (int k = 1; k < trayectorias[i].length - 1; k++) {
      nodoUsado[trayectorias[i][k]] = true;
    }
    for (int k = 1; k < trayectorias[j].length - 1; k++) {
      if (nodoUsado[trayectorias[j][k]]) {
        return false;
      }
    }
    return true;

  }

  public double fRainbow(StringBuilder extraString) {
    double result = 1;
    countNodePairsOK = 0;
    double r;

    int[] parejasOK = new int[91];

    for (int parejaId = 0; parejaId < 91; parejaId++) {
      parejasOK[parejaId] = 0;
      r = fRainbowPareja(nodePairs.get(parejaId),
          genome);
      if (r == 1.0) {
        countNodePairsOK++;
        parejasOK[parejaId] = 1;
      }
      result = result * r;
    }
    extraString.append(getExtraString(parejasOK));

    return Math.pow(result,
        1 / 91.0);
  }

  private double fRainbowPareja(NodePair pareja,
      Gene[] genome) {
    double result = 0, r;
    for (int ternaId = 0; ternaId < pareja.getNumTernas(); ternaId++) {
      r = fRainbowTerna(pareja.getTerna(ternaId),
          pareja
              .getTrayectorias(),
          genome);
      if (r > result) {
        result = r;
      }
    }
    return result;
  }

  private double fRainbowTerna(TrajTriplet terna,
      int[][] trayectorias,
      Gene[] genome) {
    double result = 1, r;
    for (int trayectIndx = 0; trayectIndx < 3; trayectIndx++) {
      r = fRainbowTrayectoria(trayectorias[terna
          .getTrajId(trayectIndx)],
          genome);
      result = result * Math.pow(r,
          1 / 3.0);
    }

    return result;
  }

  private double fRainbowTrayectoria(int[] trayectoria,
      Gene[] genome) {
    return numColoresDist(trayectoria,
        genome) / (double) (trayectoria.length - 1);
  }

  private double numColoresDist(int[] trayectoria,
      Gene[] genome) {

    boolean[] coloresUsados = new boolean[NUM_COLORS];
    for (int i = 0; i < NUM_COLORS; i++) { // ningun color se ha usado
      coloresUsados[i] = false;
    }

    int color;
    for (int nodo = 1; nodo < trayectoria.length; nodo++) {
      color = (int) genome[getIndex(trayectoria[nodo - 1],
          trayectoria[nodo])].getValue();
      coloresUsados[color] = true; // ya se usó este color
    }

    int numColores = 0;
    for (int i = 0; i < NUM_COLORS; i++) {
      if (coloresUsados[i]) {
        numColores++;
      }
    }

    return numColores;
  }

  private int getIndex(int source,
      int dest) {
    if (source > dest) {
      return index[dest][source];
    }
    return index[source][dest];
  }

  public double fitness(Gene[] genome,
      StringBuilder extraString) {
    this.genome = genome;
    double f = this.fRainbow(extraString);
    return f;
  }

  @Override
  public double fitness(Individual _i) {
    Gene[] genome = new Gene[ this.getGenomeLength() ];
    for( int i = 0; i < genome.length; i++ ) {
      genome[ i ] = _i.getGene( i );
    }
    StringBuilder extraString = new StringBuilder();
    double ft = this.fitness( genome,
                              extraString );
    _i.setExtraString( extraString );
    return ft;
  }

  public String getExtraString(int[] parejasOK) {
    String s = " ";
    int n = 12;
    for (int i = 0, k = n; i < 91; i++, k--) {
      s = s + parejasOK[i];
      if (k == 0) {
        s = s + " ";
        k = n--;
      }
    }
    return countNodePairsOK + s;
  }

  @Override
  public double getGlobalSearchIntensity() {
    // For categorical genes, this makes a mutated locus uniformly distributed
    // over the full color set: P(stay) = 1 / NUM_COLORS.
    return 1.0 - 1.0 / this.NUM_COLORS;
  }

  @Override
  public double getLocalSearchIntensity() {
    // Minimal relevant mutation: roughly one changed locus per genome.
    return 1.0 / this.getGenomeLength();
  }

  @Override
  public String getProblemName() {
    return "Heawood_" + this.getGenomeLength() + "_" + this.NUM_COLORS;
  }

  @Override
  public int getGenomeLength() {
    return edges.length;
  }

  @Override
  public Gene getNewGene(boolean randomize,
      Random r) {
    return new GeneInteger(NUM_COLORS,
        randomize,
        r);
  }

  @Override
  public Gene getNewGene(Gene gene_p) {
    return new GeneInteger((GeneInteger) gene_p);
  }

  @Override
  public double getGoalFt() {
    return 1.0;
  }

  @Override
  public int getDisplayModulus() {
    return 3;
  }

  @Override
  public Individual getNewIndividual(boolean _randomize,
      Random _r) {
    return new Individual(this,
        _randomize,
        _r);
  }

  @Override
  public Individual getNewIndividual(Individual another) {
    return new Individual(another);
  }

}
