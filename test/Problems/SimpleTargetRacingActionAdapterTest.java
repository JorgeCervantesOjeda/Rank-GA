// C:/Users/usuario/ownCloud2/RankGA/test/Problems/SimpleTargetRacingActionAdapterTest.java
// Tests for the proportional adapter that turns policy targets into backend actuator commands.
package Problems;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimpleTargetRacingActionAdapterTest {

  @Test
  public void toBackendActionAcceleratesWhenSpeedTargetIsHigher() {
    SimpleTargetRacingActionAdapter adapter =
            new SimpleTargetRacingActionAdapter();
    RacingBackendAction backendAction =
            adapter.toBackendAction(
              new RacingPolicyAction( 10.0,
                                      Math.PI / 2.0 ),
              new RacingCarState( 0.0,
                                  0.0,
                                  0.0,
                                  0.0,
                                  0.0 ) );

    assertTrue( backendAction.getThrottleCommand() > 0.0 );
    assertEquals( 0.0,
                  backendAction.getBrakeCommand(),
                  0.0 );
    assertTrue( backendAction.getSteeringCommand() > 0.0 );
  }

  @Test
  public void toBackendActionBrakesWhenSpeedTargetIsLower() {
    SimpleTargetRacingActionAdapter adapter =
            new SimpleTargetRacingActionAdapter();
    RacingBackendAction backendAction =
            adapter.toBackendAction(
              new RacingPolicyAction( 2.0,
                                      0.0 ),
              new RacingCarState( 0.0,
                                  0.0,
                                  10.0,
                                  0.0,
                                  0.0 ) );

    assertEquals( 0.0,
                  backendAction.getThrottleCommand(),
                  0.0 );
    assertTrue( backendAction.getBrakeCommand() > 0.0 );
  }

  @Test
  public void toBackendActionUsesCurrentSpeedToTrackTheSamePolicyTarget() {
    SimpleTargetRacingActionAdapter adapter =
            new SimpleTargetRacingActionAdapter();
    RacingPolicyAction policyAction = new RacingPolicyAction( 10.0,
                                                              0.0 );
    RacingBackendAction slowerCarAction =
            adapter.toBackendAction(
              policyAction,
              new RacingCarState( 5.0,
                                  3.0,
                                  4.0,
                                  0.0,
                                  0.0 ) );
    RacingBackendAction fasterCarAction =
            adapter.toBackendAction(
              policyAction,
              new RacingCarState( 5.0,
                                  3.0,
                                  14.0,
                                  0.0,
                                  0.0 ) );

    assertTrue( slowerCarAction.getThrottleCommand() > 0.0 );
    assertEquals( 0.0,
                  slowerCarAction.getBrakeCommand(),
                  0.0 );
    assertEquals( 0.0,
                  fasterCarAction.getThrottleCommand(),
                  0.0 );
    assertTrue( fasterCarAction.getBrakeCommand() > 0.0 );
  }

  @Test
  public void toBackendActionUsesCurrentHeadingToTrackTheSamePolicyTarget() {
    SimpleTargetRacingActionAdapter adapter =
            new SimpleTargetRacingActionAdapter();
    RacingPolicyAction policyAction = new RacingPolicyAction( 8.0,
                                                              Math.PI / 2.0 );
    RacingBackendAction rightOfTargetHeadingAction =
            adapter.toBackendAction(
              policyAction,
              new RacingCarState( -2.0,
                                  1.0,
                                  8.0,
                                  0.0,
                                  0.0 ) );
    RacingBackendAction leftOfTargetHeadingAction =
            adapter.toBackendAction(
              policyAction,
              new RacingCarState( -2.0,
                                  1.0,
                                  8.0,
                                  Math.PI,
                                  0.0 ) );

    assertTrue( rightOfTargetHeadingAction.getSteeringCommand() > 0.0 );
    assertTrue( leftOfTargetHeadingAction.getSteeringCommand() < 0.0 );
    assertEquals( 0.0,
                  rightOfTargetHeadingAction.getThrottleCommand(),
                  0.0 );
    assertEquals( 0.0,
                  leftOfTargetHeadingAction.getThrottleCommand(),
                  0.0 );
    assertEquals( 0.0,
                  rightOfTargetHeadingAction.getBrakeCommand(),
                  0.0 );
    assertEquals( 0.0,
                  leftOfTargetHeadingAction.getBrakeCommand(),
                  0.0 );
  }
}
