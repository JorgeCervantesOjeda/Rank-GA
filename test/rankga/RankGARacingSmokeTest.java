// C:/Users/usuario/ownCloud2/RankGA/test/rankga/RankGARacingSmokeTest.java
// Smoke test for a real RankGA run on the simple circular racing backend using patience-based stopping.
package rankga;

import Problems.ProblemRacing;
import Problems.RacingStartState;
import Problems.RacingStartStateProvider;
import Problems.SimpleCircularRacingBackend;
import Problems.SimpleCircularTrack;
import Problems.SimpleTargetRacingActionAdapter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class RankGARacingSmokeTest {

  private static final long TEST_SEED = 1234L;
  private static final long TEST_PATIENCE_MILLIS = 200L;
  private static final int COUNT_OF_INDIVIDUALS = 24;
  private static final double MIN_IMPROVEMENT_DELTA = 0.05;
  private static final String TEST_PROBLEM_NAME = "Racing_simple_circle_4";
  private static final String TEST_FIGURE_PREFIX = "racing-simple-circle-4";

  @After
  public void cleanGeneratedArtifacts()
    throws IOException {
    deleteArtifactsWithPrefix( TEST_PROBLEM_NAME + "_seed" + TEST_SEED + "_" );
    deleteFigureArtifactsWithPrefix( TEST_FIGURE_PREFIX + "_seed" + TEST_SEED + "_" );
  }

  @Test
  public void patienceBoundedRunImprovesBestFitnessAcrossPerturbedFixedStarts()
    throws Exception {
    cleanGeneratedArtifacts();
    ProblemRacing initialProblem = buildProblem();
    Population initialPopulation = new Population( COUNT_OF_INDIVIDUALS,
                                                   initialProblem,
                                                   true,
                                                   new Random( TEST_SEED ) );
    initialPopulation.evaluate();
    double initialBestFitness = initialPopulation.getFittest().getFitness();

    ProblemRacing runProblem = buildProblem();
    runQuietly( () -> RankGA.run( runProblem,
                                  COUNT_OF_INDIVIDUALS,
                                  1,
                                  TEST_SEED,
                                  "mode=smoke",
                                  TEST_PATIENCE_MILLIS,
                                  RankGA.IncumbentUpdatePolicy.STRICT,
                                  RankGA.PatienceResetPolicy.FITNESS ) );

    Path summaryFile = findSummaryFile( runProblem.getProblemName(),
                                        TEST_SEED );
    double finalBestFitness = readBestFitness( summaryFile );

    assertTrue(
      "Expected a patience-bounded RankGA run to improve best fitness by more than "
      + MIN_IMPROVEMENT_DELTA
      + " across perturbed starts, but delta was "
      + ( finalBestFitness - initialBestFitness ),
      finalBestFitness > initialBestFitness + MIN_IMPROVEMENT_DELTA );
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
                              new PerturbedFixedStartStateProvider(),
                              4,
                              7,
                              1.0,
                              2.0,
                              1000000 );
  }

  private static void runQuietly( ThrowingRunnable action )
    throws Exception {
    PrintStream originalOut = System.out;
    ByteArrayOutputStream sink = new ByteArrayOutputStream();
    try( PrintStream muted = new PrintStream( sink ) ) {
      System.setOut( muted );
      action.run();
    } finally {
      System.setOut( originalOut );
    }
  }

  private static Path findSummaryFile( String problemName,
                                       long seed )
    throws IOException {
    Path runsDirectory = Paths.get( "runs",
                                    "problem-racing" );
    String prefix = problemName + "_seed" + seed + "_";
    try( DirectoryStream<Path> stream = Files.newDirectoryStream(
                           runsDirectory,
                           prefix + "*_summary.csv" ) ) {
      Path latest = null;
      long latestModifiedMillis = Long.MIN_VALUE;
      for( Path path : stream ) {
        long modifiedMillis = Files.getLastModifiedTime( path )
          .toMillis();
        if( modifiedMillis > latestModifiedMillis ) {
          latest = path;
          latestModifiedMillis = modifiedMillis;
        }
      }
      if( latest == null ) {
        throw new IOException( "Summary file not found for " + prefix );
      }
      return latest;
    }
  }

  private static double readBestFitness( Path summaryFile )
    throws IOException {
    List<String> lines = Files.readAllLines( summaryFile,
                                             StandardCharsets.UTF_8 );
    if( lines.size() < 2 ) {
      throw new IOException( "Summary file is missing data rows: " + summaryFile );
    }
    String[] fields = lines.get( 1 )
      .replace( "\"",
                "" )
      .split( ",",
              -1 );
    if( fields.length < 4 ) {
      throw new IOException( "Summary row has too few fields: " + lines.get( 1 ) );
    }
    return Double.parseDouble( fields[ 3 ] );
  }

  private static void deleteArtifactsWithPrefix( String prefix )
    throws IOException {
    Path runsDirectory = Paths.get( "runs",
                                    "problem-racing" );
    if( !Files.exists( runsDirectory ) ) {
      return;
    }
    try( DirectoryStream<Path> stream = Files.newDirectoryStream( runsDirectory,
                                                                  prefix + "*" ) ) {
      for( Path path : stream ) {
        Files.deleteIfExists( path );
      }
    }
  }

  private static void deleteFigureArtifactsWithPrefix( String prefix )
    throws IOException {
    Path figuresDirectory = Paths.get( "figures" );
    if( !Files.exists( figuresDirectory ) ) {
      return;
    }
    try( DirectoryStream<Path> stream = Files.newDirectoryStream(
                           figuresDirectory,
                           prefix + "*" ) ) {
      for( Path path : stream ) {
        Files.deleteIfExists( path );
      }
    }
  }

  private interface ThrowingRunnable {

    void run()
      throws Exception;
  }

  private static final class PerturbedFixedStartStateProvider
    implements RacingStartStateProvider {

    private final RacingStartState[] states = new RacingStartState[] {
      buildState( 0.0,
                  50.0,
                  tangentHeading( 0.0 ) ),
      buildState( Math.PI / 3.0,
                  52.0,
                  tangentHeading( Math.PI / 3.0 ) + 0.35 ),
      buildState( 2.0 * Math.PI / 3.0,
                  48.0,
                  tangentHeading( 2.0 * Math.PI / 3.0 ) - 0.35 ),
      buildState( Math.PI,
                  51.0,
                  tangentHeading( Math.PI ) + 0.20 ),
      buildState( 4.0 * Math.PI / 3.0,
                  49.0,
                  tangentHeading( 4.0 * Math.PI / 3.0 ) - 0.20 ),
      buildState( 5.0 * Math.PI / 3.0,
                  50.0,
                  tangentHeading( 5.0 * Math.PI / 3.0 ) + 0.10 ),
      buildState( Math.PI / 2.0,
                  50.5,
                  tangentHeading( Math.PI / 2.0 ) - 0.12 )
    };

    @Override
    public void refreshBatch() {
      // Fixed states to keep the smoke test reproducible.
    }

    @Override
    public RacingStartState getStartState( int runIndex ) {
      return states[ runIndex ];
    }

    @Override
    public int countOfStates() {
      return states.length;
    }

    private static double tangentHeading( double angle ) {
      return angle + Math.PI / 2.0;
    }

    private static RacingStartState buildState( double angle,
                                                double radius,
                                                double heading ) {
      return new RacingStartState( radius * Math.cos( angle ),
                                   radius * Math.sin( angle ),
                                   0.0,
                                   heading );
    }
  }
}
