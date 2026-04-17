package rankga;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class IndividualTest {

  @Test
  public void recombinateSwapsOnlyTheLociWhoseRandomDrawIsBelowHalf() {
    TestSupport.RecordingProblem problem = new TestSupport.RecordingProblem(
      "individual_recombine_test",
      2,
      1.0,
      0.5 );
    TestSupport.ScriptedRandom random = new TestSupport.ScriptedRandom(
      0.0,
      1.0 );

    Individual first = new Individual( problem,
                                       false,
                                       random );
    Individual second = new Individual( problem,
                                        false,
                                        random );

    first.getGene( 0 ).setIntValue( 0 );
    first.getGene( 1 ).setIntValue( 0 );
    second.getGene( 0 ).setIntValue( 1 );
    second.getGene( 1 ).setIntValue( 1 );

    first.recombinate( second );

    assertGenome( first,
                  1,
                  0 );
    assertGenome( second,
                  0,
                  1 );
  }

  private static void assertGenome( Individual individual,
                                    int expected0,
                                    int expected1 ) {
    assertEquals( expected0,
                  individual.getGene( 0 ).getValue(),
                  0.0 );
    assertEquals( expected1,
                  individual.getGene( 1 ).getValue(),
                  0.0 );
  }

}
