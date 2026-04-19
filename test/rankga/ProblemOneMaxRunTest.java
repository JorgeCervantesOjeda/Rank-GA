package rankga;

import Problems.ProblemOneMax;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Regression test for the documented OneMax example.
 *
 * This protects against reintroducing the old copy-RNG bug that caused a
 * simple 8-bit OneMax instance to stall and terminate by patience.
 */
public class ProblemOneMaxRunTest {

  private static final long SEED = 24680L;
  private static final String SUMMARY_PREFIX = "one_max_8_seed" + SEED + "_";
  private static final String FIGURE_PREFIX = "one-max-8_seed" + SEED + "_";

  @After
  public void cleanGeneratedArtifacts() throws IOException {
    deleteSummaryFiles();
    deleteFigureFiles();
  }

  @Test
  public void documentedOneMaxExampleReachesGoalInAllRepetitions()
    throws Exception {
    runQuietly( () -> RankGA.run( new ProblemOneMax( 8 ),
                                  20,
                                  100,
                                  SEED,
                                  "genomeLength=8" ) );

    Path summaryFile = findSummaryFile();
    Path metadataFile = metadataFileFor( summaryFile );
    List<String> lines = Files.readAllLines( summaryFile,
                                             StandardCharsets.UTF_8 );
    List<String> metadataLines = Files.readAllLines( metadataFile,
                                                     StandardCharsets.UTF_8 );

    assertEquals( 101,
                  lines.size() );

    int terminationIndex = columnIndex( lines.get( 0 ),
                                        "termination_reason" );

    int goal = 0;
    int patience = 0;
    for( int i = 1; i < lines.size(); i++ ) {
      List<String> fields = splitCsvLine( lines.get( i ) );
      if( "goal".equals( fields.get( terminationIndex ) ) ) {
        goal++;
      } else if( "patience".equals( fields.get( terminationIndex ) ) ) {
        patience++;
      }
    }

    assertEquals( 100,
                  goal );
    assertEquals( 0,
                  patience );
    assertTrue( metadataLines.contains(
      "\"problem_parameters\",\"genomeLength=8\"" ) );
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

  private static Path findSummaryFile() throws IOException {
    Path runsRoot = Paths.get( "runs",
                               "one-max" );
    try( Stream<Path> paths = Files.walk( runsRoot ) ) {
      return paths.filter( Files::isRegularFile )
        .filter( path -> path.getFileName().toString().startsWith(
          SUMMARY_PREFIX ) )
        .filter( path -> path.getFileName().toString().endsWith(
          "_summary.csv" ) )
        .findFirst()
        .orElseThrow( () -> new IOException(
          "Summary file not found for OneMax seed " + SEED ) );
    }
  }

  private static void deleteSummaryFiles() throws IOException {
    Path runsRoot = Paths.get( "runs",
                               "one-max" );
    if( !Files.exists( runsRoot ) ) {
      return;
    }
    try( Stream<Path> paths = Files.walk( runsRoot ) ) {
      paths.filter( Files::isRegularFile )
        .filter( path -> path.getFileName().toString().startsWith(
          SUMMARY_PREFIX ) )
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

  private static Path metadataFileFor( Path summaryFile ) {
    String fileName = summaryFile.getFileName().toString();
    int extensionIndex = fileName.lastIndexOf( '.' );
    String stem = extensionIndex >= 0
                  ? fileName.substring( 0,
                                        extensionIndex )
                  : fileName;
    String extension = extensionIndex >= 0
                       ? fileName.substring( extensionIndex )
                       : "";
    return summaryFile.resolveSibling( stem + "_meta" + extension );
  }

  private static void deleteFigureFiles() throws IOException {
    Path figuresRoot = Paths.get( "figures" );
    if( !Files.exists( figuresRoot ) ) {
      return;
    }
    try( DirectoryStream<Path> stream = Files.newDirectoryStream( figuresRoot,
                                                                  FIGURE_PREFIX
                                                                  + "*" ) ) {
      for( Path path : stream ) {
        Files.deleteIfExists( path );
      }
    }
  }

  private static int columnIndex( String headerLine,
                                  String columnName ) {
    List<String> columns = splitCsvLine( headerLine );
    int index = columns.indexOf( columnName );
    if( index < 0 ) {
      throw new IllegalArgumentException( "Missing CSV column " + columnName );
    }
    return index;
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
