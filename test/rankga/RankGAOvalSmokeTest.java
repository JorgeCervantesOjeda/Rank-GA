// C:/Users/usuario/ownCloud2/RankGA/test/rankga/RankGAOvalSmokeTest.java
// Smoke test for a real RankGA run on the simple oval racing backend using patience-based stopping.
package rankga;

import Problems.ProblemRacing;
import Problems.RacingStartState;
import Problems.RacingStartStateProvider;
import Problems.SimpleOvalRacingBackend;
import Problems.SimpleOvalTrack;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RankGAOvalSmokeTest {

  private static final long TEST_SEED = 1234L;
  private static final long TEST_PATIENCE_MILLIS = 100L;
  private static final int COUNT_OF_INDIVIDUALS = 24;
  private static final double MIN_IMPROVEMENT_DELTA = 0.05;
  private static final String TEST_PROBLEM_NAME = "Racing_simple_oval_6";
  private static final String TEST_FIGURE_PREFIX = "racing-simple-oval-6";

  @After
  public void cleanGeneratedArtifacts()
    throws IOException {
    deleteArtifactsWithPrefix( TEST_PROBLEM_NAME + "_seed" + TEST_SEED + "_" );
    deleteFigureArtifactsWithPrefix( TEST_FIGURE_PREFIX + "_seed" + TEST_SEED + "_" );
  }

  @Test
  public void patienceBoundedRunImprovesBestFitnessOnSimpleOvalTrack()
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
                                  "mode=oval-smoke",
                                  TEST_PATIENCE_MILLIS,
                                  RankGA.IncumbentUpdatePolicy.STRICT,
                                  RankGA.PatienceResetPolicy.FITNESS ) );

    Path summaryFile = findSummaryFile( runProblem.getProblemName(),
                                        TEST_SEED );
    double finalBestFitness = readBestFitness( summaryFile );

    assertTrue(
      "Expected a patience-bounded RankGA run to improve best fitness by more than "
      + MIN_IMPROVEMENT_DELTA
      + " on the oval track, but delta was "
      + ( finalBestFitness - initialBestFitness ),
      finalBestFitness > initialBestFitness + MIN_IMPROVEMENT_DELTA );
  }

  @Test
  public void ovalRunnerScalesGlobalSearchIntensityFromTrackFootprint() {
    SimpleOvalTrack track = buildTrack();
    double widthMeters = 2.0 * ( 30.0 + 25.0 + 5.0 );
    double heightMeters = 2.0 * ( 25.0 + 5.0 );
    double expectedIntensity = Math.hypot( widthMeters,
                                           heightMeters ) / 10.0;

    assertEquals( expectedIntensity,
                  RunRacingOvalExperiment.computeTrackScaledGlobalSearchIntensity( track ),
                  1.0e-9 );
  }

  @Test
  public void aggregateGoalFitnessThresholdRepresentsAllRunsCompleted() {
    assertEquals( 7.500001,
                  ProblemRacing.computeAllRunsGoalFitnessThreshold( 7 ),
                  1.0e-12 );
  }

  private static ProblemRacing buildProblem() {
    SimpleOvalTrack track = buildTrack();
    SimpleOvalRacingBackend backend = buildBackend( track );
    return new ProblemRacing( backend,
                              new Problems.SimpleTargetRacingActionAdapter(),
                              new OvalFixedStartStateProvider( track ),
                              6,
                              7,
                              1.0,
                              2.0,
                              1000000 );
  }

  private static SimpleOvalTrack buildTrack() {
    return new SimpleOvalTrack( "simple_oval",
                                0.0,
                                0.0,
                                25.0,
                                30.0,
                                5.0 );
  }

  private static SimpleOvalRacingBackend buildBackend( SimpleOvalTrack track ) {
    return new SimpleOvalRacingBackend( track,
                                        0.2,
                                        1.0,
                                        12.0,
                                        18.0,
                                        0.05,
                                        40.0 );
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

  private static final class OvalFixedStartStateProvider
    implements RacingStartStateProvider {

    private final RacingStartState[] states;

    private OvalFixedStartStateProvider( SimpleOvalTrack track ) {
      this.states = new RacingStartState[] {
        buildState( track,
                    10.0,
                    0.0,
                    0.0 ),
        buildState( track,
                    50.0,
                    1.0,
                    0.12 ),
        buildState( track,
                    80.0,
                    -0.8,
                    -0.18 ),
        buildState( track,
                    140.0,
                    0.7,
                    0.10 ),
        buildState( track,
                    190.0,
                    -0.6,
                    -0.10 ),
        buildState( track,
                    225.0,
                    0.5,
                    0.16 ),
        buildState( track,
                    260.0,
                    -0.4,
                    -0.08 )
      };
    }

    @Override
    public void refreshBatch() {
      // Fixed states keep the smoke test reproducible across generations.
    }

    @Override
    public RacingStartState getStartState( int runIndex ) {
      return states[ runIndex ];
    }

    @Override
    public int countOfStates() {
      return states.length;
    }

    private static RacingStartState buildState( SimpleOvalTrack track,
                                                double progressMeters,
                                                double lateralOffsetMeters,
                                                double headingOffsetRadians ) {
      SimpleOvalTrack.CenterlinePose pose = track.computeCenterlinePose(
        progressMeters );
      return new RacingStartState( pose.getX()
                                   + lateralOffsetMeters * pose.getNormalX(),
                                   pose.getY()
                                   + lateralOffsetMeters * pose.getNormalY(),
                                   0.0,
                                   pose.getHeading() + headingOffsetRadians );
    }
  }
}
