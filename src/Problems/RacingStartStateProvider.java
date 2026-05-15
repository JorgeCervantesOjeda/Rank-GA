// C:/Users/usuario/ownCloud2/RankGA/src/Problems/RacingStartStateProvider.java
// Shared provider of comparable start states for each generation.
package Problems;

public interface RacingStartStateProvider {

  void refreshBatch();

  RacingStartState getStartState( int runIndex );

  int countOfStates();
}
