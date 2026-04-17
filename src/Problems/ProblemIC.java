package Problems;

import java.util.Random;
import rankga.*;

/**
 * Identifying-code benchmark on grid graphs.
 * See `references/identifying_code_population_based_local_search.pdf` and
 * `references/minimum_identifying_code_enhanced_genetic_algorithms.md`.
 */
public class ProblemIC
  implements Problem {

  private Graph graph;

  public ProblemIC() {
    Graph theGraph = new Graph();

    int gridSize = 15;
    for( int row = 0;
         row < gridSize;
         row++ ) {
      for( int col = 0;
           col < gridSize;
           col++ ) {
        int vertex = row * gridSize + col;
        if( col < gridSize - 1 ) {
          // Add edge to the right neighbor
          theGraph.addEdge( vertex,
                            vertex + 1 );
        }
        if( row < gridSize - 1 ) {
          // Add edge to the bottom neighbor
          theGraph.addEdge( vertex,
                            vertex + gridSize );
        }
      }
    }

    this.graph = theGraph;
  }

  @Override
  public double fitness( Individual individual ) {
    int penalty = calculatePenalty( individual );
    int size = 0;
    for( int i = 0;
         i < graph.getVerticesCount();
         i++ ) {
      size += individual.getGene( i ).getValue();
    }
    return -size - penalty * 2; // Apply a penalty factor
  }

  public int calculatePenalty( Individual individual ) {
    int penalty = 0;
    int length = graph.getVerticesCount();
    boolean[] bits = new boolean[ length ];
    for( int i = 0;
         i < length;
         i++ ) {
      bits[ i ] = ( individual.getGene( i ).getValue() != 0 );
    }

    // Penalty for non-dominating set
    for( int i = 0;
         i < length;
         i++ ) {
      final int index = i;
      if( !bits[ i ] && graph.getNeighbors( index ).stream().noneMatch( neighbor
        -> bits[ neighbor ] ) ) {
        penalty++;
      }
    }

    // Penalty for non-separating set
    for( int i = 0;
         i < length;
         i++ ) {
      for( int j = i + 1;
           j < length;
           j++ ) {
        final int indexI = i;
        final int indexJ = j;
        if( !bits[ indexI ] && !bits[ indexJ ] ) {
          boolean iHasNeighborInSet = graph.getNeighbors( indexI ).stream()
                  .anyMatch(
                    neighbor
                    -> bits[ neighbor ] );
          boolean jHasNeighborInSet = graph.getNeighbors( indexJ ).stream()
                  .anyMatch(
                    neighbor
                    -> bits[ neighbor ] );

          if( iHasNeighborInSet && jHasNeighborInSet ) {
            boolean distinctIntersections =
                    graph.getNeighbors( indexI ).stream()
                      .anyMatch(
                        neighbor
                        -> bits[ neighbor ]
                           && !graph.getNeighbors( indexJ ).contains(
                          neighbor ) )
                    || graph.getNeighbors( indexJ ).stream()
                      .anyMatch(
                        neighbor
                        -> bits[ neighbor ]
                           && !graph.getNeighbors( indexI ).contains(
                          neighbor ) );
            if( !distinctIntersections ) {
              penalty++;
            }
          }
        }
      }
    }

    StringBuilder s = new StringBuilder( "" );
    s.append( penalty );
    individual.setExtraString( s );

    return penalty;
  }

  @Override
  public double getGlobalSearchIntensity() {
    return 0.5;
  }

  @Override
  public double getLocalSearchIntensity() {
    return 1.0 / this.getGenomeLength();
  }

  @Override
  public String getProblemName() {
    return "Graph Minimum Identifier Code Problem";
  }

  @Override
  public int getGenomeLength() {
    return graph.getVerticesCount();
  }

  @Override
  public Gene getNewGene( boolean randomize,
                          Random r ) {
    return new GeneInteger( 2,
                            randomize,
                            r );
  }

  @Override
  public Gene getNewGene( Gene gene ) {
    return new GeneInteger( (GeneInteger) gene );
  }

  @Override
  public double getGoalFt() {
    return -1; // Set a specific goal fitness if applicable
  }

  @Override
  public int getDisplayModulus() {
    return 5;
  }

  @Override
  public Individual getNewIndividual( boolean randomize,
                                      Random r ) {
    return new Individual( this,
                           randomize,
                           r );
  }

  @Override
  public Individual getNewIndividual( Individual individual ) {
    return new Individual( individual );
  }

}
