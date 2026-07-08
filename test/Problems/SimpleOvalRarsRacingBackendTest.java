// C:/Users/usuario/ownCloud2/RankGA/test/Problems/SimpleOvalRarsRacingBackendTest.java
// Tests for the RARS-inspired point-mass oval backend.
package Problems;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimpleOvalRarsRacingBackendTest {

  @Test
  public void stepUsesCommandedDirectionAsWheelDirectionNotInstantHeading() {
    SimpleOvalTrack track = buildTrack();
    SimpleOvalRarsRacingBackend backend = buildBackend( track );
    SimpleOvalTrack.CenterlinePose pose = track.computeCenterlinePose( 15.0 );
    backend.resetEpisode( new RacingStartState( pose.getX(),
                                                pose.getY(),
                                                8.0,
                                                0.0 ) );

    RacingStepResult stepResult = backend.step( new RacingBackendAction(
      0.0,
      0.0,
      0.0,
      Math.PI / 2.0,
      8.0 ) );

    assertTrue( Math.abs( stepResult.getCarState()
      .getHeading()
                          - Math.PI / 2.0 ) > 0.1 );
    assertTrue( stepResult.getCarState()
      .getY() > pose.getY() );
  }

  @Test
  public void oppositeCommandedWheelDirectionsProduceDifferentTrajectories() {
    SimpleOvalTrack track = buildTrack();
    SimpleOvalTrack.CenterlinePose pose = track.computeCenterlinePose( 15.0 );
    RacingStartState startState = new RacingStartState( pose.getX(),
                                                        pose.getY(),
                                                        8.0,
                                                        0.0 );

    RacingCarState leftState = runFixedDirection( track,
                                                  startState,
                                                  Math.PI / 3.0 );
    RacingCarState rightState = runFixedDirection( track,
                                                   startState,
                                                   -Math.PI / 3.0 );

    assertTrue( leftState.getY() - rightState.getY() > 1.0 );
    assertTrue( Math.abs( wrapToPi( leftState.getHeading()
                                    - rightState.getHeading() ) ) > 0.1 );
  }

  @Test( expected = IllegalArgumentException.class )
  public void stepRequiresExplicitSpeedTarget() {
    SimpleOvalTrack track = buildTrack();
    SimpleOvalRarsRacingBackend backend = buildBackend( track );
    SimpleOvalTrack.CenterlinePose pose = track.computeCenterlinePose( 15.0 );
    backend.resetEpisode( new RacingStartState( pose.getX(),
                                                pose.getY(),
                                                8.0,
                                                0.0 ) );

    backend.step( new RacingBackendAction( 0.0,
                                           0.0,
                                           0.0,
                                           0.0 ) );
  }

  @Test
  public void adapterPropagatesPolicySpeedTargetForRarsBackend() {
    SimpleTargetRacingActionAdapter adapter = new SimpleTargetRacingActionAdapter();

    RacingBackendAction backendAction = adapter.toBackendAction(
      new RacingPolicyAction( 7.5,
                              0.25 ),
      new RacingCarState( 0.0,
                          0.0,
                          1.0,
                          0.0,
                          0.0 ) );

    assertTrue( backendAction.hasSpeedTarget() );
    assertEquals( 7.5,
                  backendAction.getSpeedTarget(),
                  0.0 );
  }

  private static RacingCarState runFixedDirection( SimpleOvalTrack track,
                                                   RacingStartState startState,
                                                   double wheelDirection ) {
    SimpleOvalRarsRacingBackend backend = buildBackend( track );
    RacingCarState currentState = backend.resetEpisode( startState );
    for( int stepIndex = 0;
         stepIndex < 20;
         stepIndex++ ) {
      RacingStepResult stepResult = backend.step( new RacingBackendAction(
        0.0,
        0.0,
        0.0,
        wheelDirection,
        8.0 ) );
      currentState = stepResult.getCarState();
      if( stepResult.isTerminal() ) {
        break;
      }
    }
    return currentState;
  }

  private static SimpleOvalTrack buildTrack() {
    return new SimpleOvalTrack( "simple_oval",
                                0.0,
                                0.0,
                                25.0,
                                30.0,
                                5.0 );
  }

  private static SimpleOvalRarsRacingBackend buildBackend( SimpleOvalTrack track ) {
    return new SimpleOvalRarsRacingBackend( track,
                                            0.05,
                                            1100.0,
                                            135000.0,
                                            1.4,
                                            2.0,
                                            0.45,
                                            40.0 );
  }

  private static double wrapToPi( double angle ) {
    double wrappedAngle = angle;
    while( wrappedAngle <= -Math.PI ) {
      wrappedAngle += 2.0 * Math.PI;
    }
    while( wrappedAngle > Math.PI ) {
      wrappedAngle -= 2.0 * Math.PI;
    }
    return wrappedAngle;
  }
}
