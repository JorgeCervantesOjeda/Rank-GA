package rankga;

import Problems.ProblemRastrigin;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

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
}
