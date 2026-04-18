package rankga;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RankGAAdaptiveTest {

  private static final String PLAIN_PREFIX = "plain_stub_problem";
  private static final String ADAPTIVE_PREFIX = "adaptive_stub_problem";

  @After
  public void cleanGeneratedLogs() throws IOException {
    deleteRunArtifactsWithPrefix( PLAIN_PREFIX );
    deleteRunArtifactsWithPrefix( ADAPTIVE_PREFIX );
  }

  @Test
  public void plainProblemRunsWithoutTryingToAdapt() throws Exception {
    TestSupport.ConstantProblem problem = new TestSupport.ConstantProblem(
      PLAIN_PREFIX,
      1.0,
      1.0 );

    runQuietly( () -> RankGA.run( problem,
                                  3,
                                  1,
                                  1234L ) );
  }

  @Test
  public void adaptiveProblemDoesNotAdaptWhenInitialPopulationAlreadyMeetsGoal()
    throws Exception {
    TestSupport.CountingAdaptiveProblem problem =
      new TestSupport.CountingAdaptiveProblem( ADAPTIVE_PREFIX,
                                               1.0,
                                               1.0 );

    runQuietly( () -> RankGA.run( problem,
                                  3,
                                  1,
                                  1234L ) );

    assertEquals( 0,
                  problem.getAdaptCalls() );
  }

  @Test
  public void adaptiveProblemWritesStructuredSummary() throws Exception {
    TestSupport.CountingAdaptiveProblem problem =
      new TestSupport.CountingAdaptiveProblem( ADAPTIVE_PREFIX,
                                               1.0,
                                               1.0 );

    runQuietly( () -> RankGA.run( problem,
                                  3,
                                  1,
                                  1234L,
                                  "mode=adaptive;goal=1.0" ) );

    Path summaryFile = findSummaryFile( ADAPTIVE_PREFIX,
                                        1234L );
    List<String> lines = Files.readAllLines( summaryFile,
                                             StandardCharsets.UTF_8 );

    assertEquals( 2,
                  lines.size() );
    assertTrue( lines.get( 0 ).contains(
      "algorithm,problem_class,problem_name,problem_parameters,seed,run_id,repetition,population_size,repetitions,evaluations,best_fitness,elapsed_ms,termination_reason,goal_fitness,output_prefix" ) );
    assertTrue( lines.get( 1 ).contains( "\"RankGA\"" ) );
    assertTrue( lines.get( 1 ).contains( "\"adaptive_stub_problem\"" ) );
    assertTrue( lines.get( 1 ).contains( "\"mode=adaptive;goal=1.0\"" ) );
    assertTrue( lines.get( 1 ).contains( ",1234," ) );
    assertTrue( lines.get( 1 ).contains( "\"goal\"" ) );
    assertEquals( "3",
                  splitCsvLine( lines.get( 1 ) ).get( 9 ) );
  }

  @Test
  public void runAndPopulationLogsIncludeHeaders() throws Exception {
    TestSupport.CountingAdaptiveProblem problem =
      new TestSupport.CountingAdaptiveProblem( ADAPTIVE_PREFIX,
                                               1.0,
                                               1.0 );

    runQuietly( () -> RankGA.run( problem,
                                  3,
                                  1,
                                  1234L,
                                  "mode=adaptive;goal=1.0" ) );

    Path summaryFile = findSummaryFile( ADAPTIVE_PREFIX,
                                        1234L );
    List<String> summaryLines = Files.readAllLines( summaryFile,
                                                    StandardCharsets.UTF_8 );
    Path runPrefix = Paths.get( readOutputPrefix( summaryLines.get( 1 ) ) );

    List<String> runLines = Files.readAllLines( Paths.get( runPrefix.toString()
                                                           + ".txt" ),
                                                StandardCharsets.UTF_8 );
    assertEquals( "t\tni\trep\tg\ts\tph\td\trank\tp\tfitness\textra\tgenes\tDateTime\tmil",
                  runLines.get( 0 ) );

    List<String> populationLines = Files.readAllLines(
      Paths.get( runPrefix.toString() + "_0.txt" ),
      StandardCharsets.UTF_8 );
    assertEquals( "rank\tmutationIntensity\tfitness\textra\tgenes",
                  populationLines.get( 0 ) );
  }

  private static void runQuietly( ThrowingRunnable action ) throws Exception {
    PrintStream originalOut = System.out;
    ByteArrayOutputStream sink = new ByteArrayOutputStream();
    try( PrintStream muted = new PrintStream( sink ) ) {
      System.setOut( muted );
      action.run();
    } finally {
      System.setOut( originalOut );
    }
  }

  private static void deleteRunArtifactsWithPrefix( String prefix )
    throws IOException {
    deleteFilesWithPrefix( Paths.get( "." ),
                           prefix );
    Path runsRoot = Paths.get( "runs" );
    if( !Files.exists( runsRoot ) ) {
      return;
    }
    try( Stream<Path> paths = Files.walk( runsRoot ) ) {
      paths.filter( Files::isRegularFile )
        .filter( path -> path.getFileName().toString().startsWith( prefix ) )
        .forEach( path -> {
          try {
            Files.deleteIfExists( path );
          } catch( IOException e ) {
            throw new RuntimeException( e );
          }
        } );
    } catch( RuntimeException e ) {
      if( e.getCause() instanceof IOException ) {
        throw( IOException ) e.getCause();
      }
      throw e;
    }
  }

  private static void deleteFilesWithPrefix( Path directory,
                                             String prefix )
    throws IOException {
    if( !Files.exists( directory ) ) {
      return;
    }
    try( DirectoryStream<Path> stream = Files.newDirectoryStream( directory,
                                                                  prefix + "*" ) ) {
      for( Path path : stream ) {
        Files.deleteIfExists( path );
      }
    }
  }

  private static Path findSummaryFile( String prefix,
                                        long seed ) throws IOException {
    Path runsRoot = Paths.get( "runs" );
    String expectedPrefix = prefix + "_seed" + seed + "_";
    try( Stream<Path> paths = Files.walk( runsRoot ) ) {
      return paths.filter( Files::isRegularFile )
        .filter( path -> path.getFileName().toString().startsWith( expectedPrefix ) )
        .filter( path -> path.getFileName().toString().endsWith( "_summary.csv" ) )
        .findFirst()
        .orElseThrow( () -> new IOException( "Summary file not found for "
                                              + prefix ) );
    }
  }

  private static String readOutputPrefix( String summaryLine ) {
    int lastComma = summaryLine.lastIndexOf( ',' );
    String outputPrefix = summaryLine.substring( lastComma + 1 )
      .trim();
    if( outputPrefix.startsWith( "\"" )
        && outputPrefix.endsWith( "\"" )
        && outputPrefix.length() >= 2 ) {
      return outputPrefix.substring( 1,
                                     outputPrefix.length() - 1 );
    }
    return outputPrefix;
  }

  private static List<String> splitCsvLine( String line ) {
    return Arrays.asList( line.replace( "\"",
                                        "" ).split( ",",
                                                     -1 ) );
  }

  private interface ThrowingRunnable {
    void run()
      throws Exception;
  }

}
