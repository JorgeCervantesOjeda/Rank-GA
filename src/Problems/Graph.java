package Problems;

import java.util.ArrayList;
import java.util.List;

public class Graph {

  private final List<List<Integer>> adjacencyList;

  public Graph() {
    adjacencyList = new ArrayList<>();
  }

  public void addVertex() {
    adjacencyList.add( new ArrayList<>() );
  }

  public void addEdge( int v1,
                       int v2 ) {
    ensureVertexExists( v1 );
    ensureVertexExists( v2 );
    adjacencyList.get( v1 ).add( v2 );
    adjacencyList.get( v2 ).add( v1 ); // assuming an undirected graph
  }

  private void ensureVertexExists( int v ) {
    while( v >= adjacencyList.size() ) {
      addVertex();
    }
  }

  public List<Integer> getNeighbors( int v ) {
    return adjacencyList.get( v );
  }

  public int getVerticesCount() {
    return adjacencyList.size();
  }

}
