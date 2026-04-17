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

  @Test
  public void mutateAppliesTheRankScheduleToEachIndividual() {
    TestSupport.RecordingProblem problem = new TestSupport.RecordingProblem(
      "population_mutate_test",
      1,
      1.0,
      0.5 );
    Population population = newQuietPopulation( problem,
                                                3,
                                                false,
                                                new Random( 1 ) );

    population.mutate();

    assertEquals( 0.0,
                  recordingGene( population,
                                 0 ).getLastIntensity(),
                  1e-12 );
    assertEquals( 0.5,
                  recordingGene( population,
                                 1 ).getLastIntensity(),
                  1e-12 );
    assertEquals( 1.0,
                  recordingGene( population,
                                 2 ).getLastIntensity(),
                  1e-12 );
  }

  @Test
  public void recombinePairsAdjacentIndividualsWithFixedRandomness() {
    TestSupport.RecordingProblem problem = new TestSupport.RecordingProblem(
      "population_recombine_test",
      2,
      1.0,
      0.5 );
    Population population = newQuietPopulation( problem,
                                                4,
                                                false,
                                                new TestSupport.ScriptedRandom(
                                                  0.0,
                                                  1.0,
                                                  0.0,
                                                  1.0 ) );

    setGenome( population.getIndividual( 0 ),
               0,
               0 );
    setGenome( population.getIndividual( 1 ),
               1,
               1 );
    setGenome( population.getIndividual( 2 ),
               2,
               2 );
    setGenome( population.getIndividual( 3 ),
               3,
               3 );

    population.recombine();

    assertGenome( population.getIndividual( 0 ),
                  1,
                  0 );
    assertGenome( population.getIndividual( 1 ),
                  0,
                  1 );
    assertGenome( population.getIndividual( 2 ),
                  3,
                  2 );
    assertGenome( population.getIndividual( 3 ),
                  2,
                  3 );
  }

  private static Population newQuietPopulation( Problem problem ) {
    return newQuietPopulation( problem,
                              3,
                              false,
                              new Random( 1 ) );
  }

  private static Population newQuietPopulation( Problem problem,
                                                int size,
                                                boolean randomize,
                                                Random randomizer ) {
    PrintStream originalOut = System.out;
    ByteArrayOutputStream sink = new ByteArrayOutputStream();
    try( PrintStream muted = new PrintStream( sink ) ) {
      System.setOut( muted );
      return new Population( size,
                             problem,
                             randomize,
                             randomizer );
    } finally {
      System.setOut( originalOut );
    }
  }

  private static void assertGenome( rankga.Individual individual,
                                    double expected0,
                                    double expected1 ) {
    assertEquals( expected0,
                  individual.getGene( 0 ).getValue(),
                  0.0 );
    assertEquals( expected1,
                  individual.getGene( 1 ).getValue(),
                  0.0 );
  }

  private static void setGenome( rankga.Individual individual,
                                 int value0,
                                 int value1 ) {
    TestSupport.RecordingGene gene0 = (TestSupport.RecordingGene) individual
      .getGene( 0 );
    TestSupport.RecordingGene gene1 = (TestSupport.RecordingGene) individual
      .getGene( 1 );
    gene0.setNumValues( 4 );
    gene1.setNumValues( 4 );
    gene0.setIntValue( value0 );
    gene1.setIntValue( value1 );
  }

  private static TestSupport.RecordingGene recordingGene( Population population,
                                                          int index ) {
    return (TestSupport.RecordingGene) population.getIndividual( index )
      .getGene( 0 );
  }

}
