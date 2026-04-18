package rankga;

import Problems.ProblemPseudoachromaticIndexConnex;
import Problems.ProblemRastrigin;
import Problems.ProblemTS;
import Problems.ProblemTS_Reals;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProblemFactoryTest {

  @Test
  public void parseArgumentsNormalizesKeys() {
    Map<String, String> options = ProblemFactory.parseArguments(
      new String[] { "--problem=heawood",
                     "--colors=3",
                     "--population=17",
                     "--seed=1234" } );

    assertEquals( "heawood",
                  options.get( "problem" ) );
    assertEquals( "3",
                  options.get( "colors" ) );
    assertEquals( "17",
                  options.get( "population" ) );
    assertEquals( "1234",
                  options.get( "seed" ) );
  }

  @Test
  public void createBuildsConfiguredRastriginProblem() {
    Map<String, String> options = ProblemFactory.parseArguments(
      new String[] { "--dimensions=4" } );

    Problem problem = ProblemFactory.create( "rastrigin",
                                             options );

    assertTrue( problem instanceof ProblemRastrigin );
    assertEquals( 4,
                  problem.getGenomeLength() );
  }

  @Test
  public void describeProblemParametersIncludesEffectiveValues() {
    Map<String, String> tsOptions = ProblemFactory.parseArguments(
      new String[] { "--n=4" } );
    Problem ts = runQuietly( () -> ProblemFactory.create( "ts",
                                                          tsOptions,
                                                          1234L ) );

    assertEquals( "n=4",
                  ProblemFactory.describeProblemParameters( "ts",
                                                            tsOptions,
                                                            ts ) );

    Map<String, String> emptyOptions = new LinkedHashMap<>();
    Problem taskAssignment = runQuietly(
      () -> ProblemFactory.create( "task-assignment",
                                   emptyOptions,
                                   1234L ) );

    assertEquals( "numTasks=100;numAgents=20",
                  ProblemFactory.describeProblemParameters(
                    "task-assignment",
                    emptyOptions,
                    taskAssignment ) );
  }

  @Test
  public void createUsesSeededTsConstruction() {
    Map<String, String> options = ProblemFactory.parseArguments(
      new String[] { "--n=4", "--seed=1234" } );

    Problem first = runQuietly( () -> ProblemFactory.create( "ts",
                                                             options ) );
    Problem second = runQuietly( () -> ProblemFactory.create( "ts",
                                                              options ) );

    assertTrue( first instanceof ProblemTS );
    assertTrue( second instanceof ProblemTS );
    assertEquals( first.getProblemName(),
                  second.getProblemName() );

    Individual firstIndividual = first.getNewIndividual( true,
                                                         new Random( 7 ) );
    Individual secondIndividual = second.getNewIndividual( true,
                                                           new Random( 7 ) );

    assertEquals( first.fitness( firstIndividual ),
                  second.fitness( secondIndividual ),
                  0.0 );
  }

  @Test
  public void availableProblemsIncludesCoreEntries() {
    String available = ProblemFactory.availableProblems();

    assertTrue( available.contains( "ts-reals" ) );
    assertTrue( available.contains( "heawood" ) );
    assertTrue( available.contains( "pseudo-connex" ) );
  }

  @Test
  public void createDefaultsToTsRealsWhenProblemIdIsNull() {
    Problem problem = runQuietly( () -> ProblemFactory.create( null,
                                                               new LinkedHashMap<>() ) );

    assertTrue( problem instanceof ProblemTS_Reals );
  }

  @Test
  public void createRecognizesHyphenatedAliases() {
    Problem problem = runQuietly( () -> ProblemFactory.create( "pseudo-connex",
                                                               new LinkedHashMap<>() ) );

    assertTrue( problem instanceof ProblemPseudoachromaticIndexConnex );
  }

  @Test
  public void createRejectsUnknownProblemId() {
    try {
      ProblemFactory.create( "not-a-real-problem",
                             new LinkedHashMap<>() );
      fail( "Expected IllegalArgumentException" );
    } catch( IllegalArgumentException expected ) {
      assertTrue( expected.getMessage().contains( "Unknown problem" ) );
      assertTrue( expected.getMessage().contains( "Available" ) );
    }
  }

  private static <T> T runQuietly( ThrowingSupplier<T> action ) {
    PrintStream originalOut = System.out;
    ByteArrayOutputStream sink = new ByteArrayOutputStream();
    try( PrintStream muted = new PrintStream( sink ) ) {
      System.setOut( muted );
      return action.get();
    } finally {
      System.setOut( originalOut );
    }
  }

  private interface ThrowingSupplier<T> {
    T get();
  }
}
