package Problems;

import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class GeneIntegerTest {

  @Test( expected = IllegalArgumentException.class )
  public void constructorRejectsDomainsSmallerThanTwo() {
    new GeneInteger( 1,
                     false,
                     new Random( 1 ) );
  }

  @Test
  public void setNumValuesRejectsDomainsSmallerThanTwo() {
    GeneInteger gene = new GeneInteger( 2,
                                        false,
                                        new Random( 1 ) );

    try {
      gene.setNumValues( 1 );
      fail( "Expected IllegalArgumentException" );
    } catch( IllegalArgumentException expected ) {
      // expected
    }
  }

  @Test
  public void mutateWithProbabilityOneChangesValue() {
    GeneInteger gene = new GeneInteger( 3,
                                        false,
                                        new Random( 1 ) );
    gene.setIntValue( 0 );

    gene.mutate( 1.0 );

    assertTrue( gene.getValue() != 0.0 );
    assertTrue( gene.getValue() >= 0.0 );
    assertTrue( gene.getValue() < 3.0 );
  }

  @Test
  public void setDoubleValueRoundsToNearestInteger() {
    GeneInteger gene = new GeneInteger( 5,
                                        false,
                                        new Random( 1 ) );

    gene.setDoubleValue( 2.6 );

    assertEquals( 3.0,
                  gene.getValue(),
                  0.0 );
  }
}
