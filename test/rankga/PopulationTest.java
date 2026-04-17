package rankga;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class PopulationTest {

  @Test
  public void selectKeepsExactlyThreeEliteClonesWhenRanksUseNMinusOne() {
    TestSupport.ValueProblem problem = new TestSupport.ValueProblem(
      "population_select_test" );
    Population population = newQuietPopulation( problem );

    // Put the best value in the middle so evaluate() must sort before selection.
    population.getIndividual( 0 ).getGene( 0 ).setIntValue( 0 );
    population.getIndividual( 1 ).getGene( 0 ).setIntValue( 1 );
    population.getIndividual( 2 ).getGene( 0 ).setIntValue( 0 );

    population.evaluate();
    assertEquals( 1.0,
                  population.getFittest().getGene( 0 ).getValue(),
                  0.0 );

    population.select();

    assertEquals( 3,
                  population.getSize() );
    for( int i = 0; i < population.getSize(); i++ ) {
      assertEquals( 1.0,
                    population.getIndividual( i ).getGene( 0 ).getValue(),
                    0.0 );
    }

    assertNotSame( population.getIndividual( 0 ),
                   population.getIndividual( 1 ) );
    assertNotSame( population.getIndividual( 0 ),
                   population.getIndividual( 2 ) );
    assertNotSame( population.getIndividual( 1 ),
                   population.getIndividual( 2 ) );
  }

  private static Population newQuietPopulation( Problem problem ) {
    PrintStream originalOut = System.out;
    ByteArrayOutputStream sink = new ByteArrayOutputStream();
    try( PrintStream muted = new PrintStream( sink ) ) {
      System.setOut( muted );
      return new Population( 3,
                             problem,
                             false,
                             new Random( 1 ) );
    } finally {
      System.setOut( originalOut );
    }
  }

}
