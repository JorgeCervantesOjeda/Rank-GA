package Problems;

import java.util.Random;
import org.junit.Test;
import rankga.Individual;
import static org.junit.Assert.assertEquals;

public class ProblemLeadingOnesTest {

  @Test
  public void fitnessCountsEntirePrefixWhenAllGenesAreOne() {
    ProblemLeadingOnes problem = new ProblemLeadingOnes( 5 );
    Individual individual = problem.getNewIndividual( false,
                                                      new Random( 1 ) );

    for( int i = 0; i < problem.getGenomeLength(); i++ ) {
      individual.getGene( i ).setIntValue( 1 );
    }

    assertEquals( 5.0,
                  problem.fitness( individual ),
                  0.0 );
  }

  @Test
  public void fitnessStopsAtFirstZero() {
    ProblemLeadingOnes problem = new ProblemLeadingOnes( 5 );
    Individual individual = problem.getNewIndividual( false,
                                                      new Random( 1 ) );

    individual.getGene( 0 ).setIntValue( 1 );
    individual.getGene( 1 ).setIntValue( 1 );
    individual.getGene( 2 ).setIntValue( 0 );
    individual.getGene( 3 ).setIntValue( 1 );
    individual.getGene( 4 ).setIntValue( 1 );

    assertEquals( 2.0,
                  problem.fitness( individual ),
                  0.0 );
  }

  @Test
  public void fitnessIsZeroWhenFirstGeneIsZero() {
    ProblemLeadingOnes problem = new ProblemLeadingOnes( 5 );
    Individual individual = problem.getNewIndividual( false,
                                                      new Random( 1 ) );

    individual.getGene( 0 ).setIntValue( 0 );
    individual.getGene( 1 ).setIntValue( 1 );
    individual.getGene( 2 ).setIntValue( 1 );
    individual.getGene( 3 ).setIntValue( 1 );
    individual.getGene( 4 ).setIntValue( 1 );

    assertEquals( 0.0,
                  problem.fitness( individual ),
                  0.0 );
  }
}
