package rankga;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class RankGAAdaptiveTest {

  private static final String PLAIN_PREFIX = "plain_stub_problem";
  private static final String ADAPTIVE_PREFIX = "adaptive_stub_problem";

  @After
  public void cleanGeneratedLogs() throws IOException {
    deleteFilesWithPrefix( PLAIN_PREFIX );
    deleteFilesWithPrefix( ADAPTIVE_PREFIX );
  }

  @Test
  public void plainProblemRunsWithoutTryingToAdapt() throws Exception {
    TestSupport.ConstantProblem problem = new TestSupport.ConstantProblem(
      PLAIN_PREFIX,
      1.0,
      1.0 );

    runQuietly( () -> RankGA.run( problem,
                                  3,
                                  1 ) );
  }

  @Test
  public void adaptiveProblemReceivesExactlyOneAdaptCall() throws Exception {
    TestSupport.CountingAdaptiveProblem problem =
      new TestSupport.CountingAdaptiveProblem( ADAPTIVE_PREFIX,
                                               1.0,
                                               1.0 );

    runQuietly( () -> RankGA.run( problem,
                                  3,
                                  1 ) );

    assertEquals( 1,
                  problem.getAdaptCalls() );
    assertEquals( 1.0,
                  problem.getLastBestFitness(),
                  0.0 );
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

  private static void deleteFilesWithPrefix( String prefix ) throws IOException {
    try( DirectoryStream<Path> stream = Files.newDirectoryStream( Paths.get( "." ),
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

}
