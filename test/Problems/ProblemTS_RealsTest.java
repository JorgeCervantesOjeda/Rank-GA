package Problems;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ProblemTS_RealsTest {

  @Test
  public void getNewGeneWithoutRandomizationStartsAtZero() {
    ProblemTS_Reals problem = newQuietProblem();

    assertEquals( 0.0,
                  problem.getNewGene( false,
                                      new Random( 1 ) ).getValue(),
                  0.0 );
  }

  private static ProblemTS_Reals newQuietProblem() {
    PrintStream originalOut = System.out;
    ByteArrayOutputStream sink = new ByteArrayOutputStream();
    try( PrintStream muted = new PrintStream( sink ) ) {
      System.setOut( muted );
      return new ProblemTS_Reals();
    } finally {
      System.setOut( originalOut );
    }
  }

}
