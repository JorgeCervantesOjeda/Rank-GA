// C:/Users/usuario/ownCloud2/RankGA/test/rankga/RankGAStateTest.java
// Verifies that mutable RankGA execution state belongs to driver instances.
package rankga;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.Test;
import static org.junit.Assert.assertFalse;

public class RankGAStateTest {

  private static final String[] INSTANCE_STATE_FIELDS = {
    "population",
    "startTime",
    "runTime",
    "tryTime",
    "notImproved",
    "lastDisplay",
    "lastBest",
    "repetition",
    "generation",
    "currentEvaluationCount",
    "patienceMillis",
    "incumbentUpdatePolicy",
    "patienceResetPolicy"
  };

  @Test
  public void executionStateIsNotStatic() throws Exception {
    for( String fieldName : INSTANCE_STATE_FIELDS ) {
      Field field = RankGA.class.getDeclaredField( fieldName );

      assertFalse( fieldName + " must belong to a RankGA instance",
                   Modifier.isStatic( field.getModifiers() ) );
    }
  }
}
