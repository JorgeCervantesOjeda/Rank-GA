package rankga;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RankGAPolicyTest {

  @Test
  public void strictPolicyRejectsNeutralReplacement() {
    TestSupport.ConstantProblem problem = new TestSupport.ConstantProblem(
      "neutral_policy_problem",
      1.0,
      2.0 );
    Individual incumbent = problem.getNewIndividual( false,
                                                     null );
    incumbent.getGene( 0 ).setIntValue( 0 );

    Individual candidate = problem.getNewIndividual( false,
                                                     null );
    candidate.getGene( 0 ).setIntValue( 1 );

    assertFalse( RankGA.shouldReplaceIncumbent(
      incumbent,
      candidate,
      RankGA.IncumbentUpdatePolicy.STRICT ) );
  }

  @Test
  public void neutralPolicyAcceptsNeutralReplacement() {
    TestSupport.ConstantProblem problem = new TestSupport.ConstantProblem(
      "neutral_policy_problem",
      1.0,
      2.0 );
    Individual incumbent = problem.getNewIndividual( false,
                                                     null );
    incumbent.getGene( 0 ).setIntValue( 0 );

    Individual candidate = problem.getNewIndividual( false,
                                                     null );
    candidate.getGene( 0 ).setIntValue( 1 );

    assertTrue( RankGA.shouldReplaceIncumbent(
      incumbent,
      candidate,
      RankGA.IncumbentUpdatePolicy.NEUTRAL ) );
  }

  @Test
  public void patienceResetDependsOnSelectedPolicy() {
    assertFalse( RankGA.shouldResetPatience(
      false,
      true,
      RankGA.PatienceResetPolicy.FITNESS ) );
    assertTrue( RankGA.shouldResetPatience(
      false,
      true,
      RankGA.PatienceResetPolicy.MOVEMENT ) );
    assertTrue( RankGA.shouldResetPatience(
      true,
      true,
      RankGA.PatienceResetPolicy.FITNESS ) );
  }
}
