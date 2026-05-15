// C:/Users/usuario/ownCloud2/RankGA/src/Problems/RandomRacingStartStateProvider.java
// Pre-samples one comparable batch of start states per generation.
package Problems;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RandomRacingStartStateProvider
  implements RacingStartStateProvider {

  private final RacingBackend backend;
  private final Random random;
  private final int countOfStates;
  private final List<RacingStartState> startStates;

  public RandomRacingStartStateProvider( RacingBackend backend,
                                         Random random,
                                         int countOfStates ) {
    if( backend == null ) {
      throw new IllegalArgumentException( "backend must not be null" );
    }
    if( random == null ) {
      throw new IllegalArgumentException( "random must not be null" );
    }
    if( countOfStates <= 0 ) {
      throw new IllegalArgumentException( "countOfStates must be positive" );
    }
    this.backend = backend;
    this.random = random;
    this.countOfStates = countOfStates;
    this.startStates = new ArrayList<RacingStartState>( countOfStates );
  }

  @Override
  public void refreshBatch() {
    startStates.clear();
    for( int stateIndex = 0;
         stateIndex < countOfStates;
         stateIndex++ ) {
      startStates.add( backend.sampleStartState( random ) );
    }
  }

  @Override
  public RacingStartState getStartState( int runIndex ) {
    if( startStates.size() != countOfStates ) {
      throw new IllegalStateException( "refreshBatch must be called before getStartState" );
    }
    if( runIndex < 0 || runIndex >= countOfStates ) {
      throw new IllegalArgumentException( "runIndex out of range" );
    }
    return startStates.get( runIndex );
  }

  @Override
  public int countOfStates() {
    return countOfStates;
  }
}
