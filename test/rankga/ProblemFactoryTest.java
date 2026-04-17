package rankga;

import Problems.ProblemPseudoachromaticIndexConnex;
import Problems.ProblemRastrigin;
import Problems.ProblemTS_Reals;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProblemFactoryTest {

  @Test
  public void parseArgumentsNormalizesKeys() {
    Map<String, String> options = ProblemFactory.parseArguments(
      new String[] { "--problem=heawood", "--colors=3", "--population=17" } );

    assertEquals( "heawood",
                  options.get( "problem" ) );
    assertEquals( "3",
                  options.get( "colors" ) );
    assertEquals( "17",
                  options.get( "population" ) );
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
