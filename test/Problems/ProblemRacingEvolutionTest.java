// C:/Users/usuario/ownCloud2/RankGA/test/Problems/ProblemRacingEvolutionTest.java
// Integration test for short deterministic evolution on the simple circular racing backend.
package Problems;

import java.util.Random;
import org.junit.Test;
import rankga.Population;
import static org.junit.Assert.assertTrue;

public class ProblemRacingEvolutionTest {

  private static final double MIN_IMPROVEMENT_DELTA = 0.05;

  @Test
  public void fixedOperatorIterationsImproveFitnessOnSimpleCircularBackend() {
    for( long seed = 1L;
         seed <= 5L;
         seed++ ) {
      ProblemRacing problem = buildProblem();
      Population population = new Population( 20,
                                              problem,
                                              true,
                                              new Random( seed ) );
      population.evaluate();
      double initialBestFitness = population.getFittest().getFitness();

      for( int generation = 0;
           generation < 30;
           generation++ ) {
        evolveOneGeneration( population );
      }

      double finalBestFitness = population.getFittest().getFitness();
      assertTrue(
        "Expected short evolution to improve best fitness by more than "
        + MIN_IMPROVEMENT_DELTA
        + " for seed "
        + seed
        + ", but improvement was "
        + ( finalBestFitness - initialBestFitness ),
        finalBestFitness > initialBestFitness + MIN_IMPROVEMENT_DELTA );
    }
  }

  private static ProblemRacing buildProblem() {
    SimpleCircularTrack track = new SimpleCircularTrack( "simple_circle",
                                                         0.0,
                                                         0.0,
                                                         50.0,
                                                         5.0 );
    SimpleCircularRacingBackend backend =
            new SimpleCircularRacingBackend( track,
                                             0.2,
                                             1.0,
                                             12.0,
                                             18.0,
                                             0.05,
                                             40.0 );
    return new ProblemRacing( backend,
                              new SimpleTargetRacingActionAdapter(),
                              new FixedStartStateProvider(),
                              1,
                              1,
                              1.0,
                              2.0,
                              1000 );
  }

  private static void evolveOneGeneration( Population population ) {
    population.select();
    population.recombine();
    population.evaluate();
    population.mutate();
    population.evaluate();
  }

  private static final class FixedStartStateProvider
    implements RacingStartStateProvider {

    private final RacingStartState state = new RacingStartState( 50.0,
                                                                 0.0,
                                                                 0.0,
                                                                 Math.PI / 2.0 );

    @Override
    public void refreshBatch() {
      // Intentionally fixed to keep the experiment deterministic.
    }

    @Override
    public RacingStartState getStartState( int runIndex ) {
      return state;
    }

    @Override
    public int countOfStates() {
      return 1;
    }
  }
}
