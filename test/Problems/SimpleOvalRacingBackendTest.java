// C:/Users/usuario/ownCloud2/RankGA/test/Problems/SimpleOvalRacingBackendTest.java
// Backend tests for the deterministic oval racing geometry with two straights and two turns.
package Problems;

import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimpleOvalRacingBackendTest {

  @Test
  public void sampleStartStateStaysInsideTrackWithZeroSpeed() {
    SimpleOvalTrack track = buildTrack();
    SimpleOvalRacingBackend backend = buildBackend( track );
    Random random = new Random( 1234L );

    for( int sampleIndex = 0;
         sampleIndex < 20;
         sampleIndex++ ) {
      RacingStartState startState = backend.sampleStartState( random );
      assertTrue( track.isInsideTrack( startState.getX(),
                                       startState.getY() ) );
      assertEquals( 0.0,
                    startState.getSpeed(),
                    0.0 );
    }
  }

  @Test
  public void stepAdvancesProgressAlongTopStraight() {
    SimpleOvalTrack track = buildTrack();
    SimpleOvalRacingBackend backend = buildBackend( track );
    SimpleOvalTrack.CenterlinePose pose = track.computeCenterlinePose( 15.0 );
    RacingStartState startState = new RacingStartState( pose.getX(),
                                                        pose.getY(),
                                                        0.0,
                                                        pose.getHeading() );
    backend.resetEpisode( startState );

    RacingStepResult lastStepResult = null;
    for( int stepIndex = 0;
         stepIndex < 6;
         stepIndex++ ) {
      lastStepResult = backend.step( new RacingBackendAction( 0.0,
                                                              1.0,
                                                              0.0 ) );
    }

    assertTrue( lastStepResult != null );
    assertEquals( RacingTerminationReason.NONE,
                  lastStepResult.getTerminationReason() );
    assertTrue( lastStepResult.getProgressMeters() > 0.0 );
    assertTrue( track.isInsideTrack( lastStepResult.getCarState().getX(),
                                     lastStepResult.getCarState().getY() ) );
  }

  @Test
  public void stepUsesHeadingTargetImmediatelyWhenProvided() {
    SimpleOvalTrack track = buildTrack();
    SimpleOvalRacingBackend backend = buildBackend( track );
    SimpleOvalTrack.CenterlinePose pose = track.computeCenterlinePose( 15.0 );
    backend.resetEpisode( new RacingStartState( pose.getX(),
                                                pose.getY(),
                                                0.0,
                                                0.0 ) );

    RacingStepResult stepResult = backend.step( new RacingBackendAction(
      0.0,
      1.0,
      0.0,
      Math.PI / 2.0 ) );

    assertEquals( Math.PI / 2.0,
                  stepResult.getCarState().getHeading(),
                  1.0e-12 );
  }

  @Test
  public void stepAdvancesProgressThroughRightTurn() {
    SimpleOvalTrack track = buildTrack();
    SimpleOvalRacingBackend backend = buildBackend( track );
    double progressMetersBeforeRightTurn = 65.0;
    SimpleOvalTrack.CenterlinePose pose = track.computeCenterlinePose(
      progressMetersBeforeRightTurn );
    RacingStartState startState = new RacingStartState( pose.getX(),
                                                        pose.getY(),
                                                        5.0,
                                                        pose.getHeading() );
    backend.resetEpisode( startState );

    RacingStepResult lastStepResult = null;
    for( int stepIndex = 0;
         stepIndex < 5;
         stepIndex++ ) {
      lastStepResult = backend.step( new RacingBackendAction( -0.2,
                                                              0.2,
                                                              0.0 ) );
    }

    assertTrue( lastStepResult != null );
    assertEquals( RacingTerminationReason.NONE,
                  lastStepResult.getTerminationReason() );
    assertTrue( lastStepResult.getProgressMeters() > 0.0 );
  }

  @Test
  public void stepReportsOffTrackWhenCarExitsTheTrackBand() {
    SimpleOvalTrack track = buildTrack();
    SimpleOvalRacingBackend backend = buildBackend( track );
    SimpleOvalTrack.CenterlinePose pose = track.computeCenterlinePose( 10.0 );
    double outwardX = pose.getX() + 0.95 * track.getHalfWidth()
                      * pose.getNormalX();
    double outwardY = pose.getY() + 0.95 * track.getHalfWidth()
                      * pose.getNormalY();
    RacingStartState startState = new RacingStartState( outwardX,
                                                        outwardY,
                                                        10.0,
                                                        pose.getHeading()
                                                        + Math.PI / 2.0 );
    backend.resetEpisode( startState );

    RacingStepResult stepResult = backend.step( new RacingBackendAction( 0.0,
                                                                         0.0,
                                                                         0.0 ) );

    assertEquals( RacingTerminationReason.OFF_TRACK,
                  stepResult.getTerminationReason() );
  }

  @Test
  public void auxiliaryCenterlineLookaheadKeepsBackendProgressMonotonicAcrossMultipleSegments() {
    SimpleOvalTrack track = buildTrack();
    SimpleOvalRacingBackend backend = buildBackend( track );
    SimpleTargetRacingActionAdapter actionAdapter =
            new SimpleTargetRacingActionAdapter( 1.2,
                                                 0.2,
                                                 0.2 );
    double startProgressMeters = 45.0;
    SimpleOvalTrack.CenterlinePose startPose = track.computeCenterlinePose(
      startProgressMeters );
    RacingCarState currentState = backend.resetEpisode(
      new RacingStartState( startPose.getX(),
                            startPose.getY(),
                            6.0,
                            startPose.getHeading() ) );

    double previousProgressMeters = 0.0;
    for( int stepIndex = 0;
         stepIndex < 80;
         stepIndex++ ) {
      RacingBackendAction backendAction = buildAuxiliaryLookaheadAction(
        track,
        actionAdapter,
        currentState );
      RacingStepResult stepResult = backend.step( backendAction );
      assertEquals( RacingTerminationReason.NONE,
                    stepResult.getTerminationReason() );
      assertTrue( stepResult.getProgressMeters() >= previousProgressMeters );
      assertTrue( track.isInsideTrack( stepResult.getCarState().getX(),
                                       stepResult.getCarState().getY() ) );
      previousProgressMeters = stepResult.getProgressMeters();
      currentState = stepResult.getCarState();
    }

    assertTrue( previousProgressMeters > 120.0 );
  }

  @Test
  public void sameAuxiliarySpatialFieldCanProducePositiveEarlyProgressFromSharedPositionUnderModerateHeadingErrors() {
    SimpleOvalTrack track = buildTrack();
    SimpleTargetRacingActionAdapter actionAdapter =
            new SimpleTargetRacingActionAdapter( 1.2,
                                                 0.2,
                                                 0.2 );
    double startProgressMeters = 45.0;
    SimpleOvalTrack.CenterlinePose startPose = track.computeCenterlinePose(
      startProgressMeters );

    double finalProgressWithPositiveHeadingError =
            runAuxiliarySpatialFieldEpisode( track,
                                             buildBackend( track ),
                                             actionAdapter,
                                             new RacingStartState(
                                               startPose.getX(),
                                               startPose.getY(),
                                               6.0,
                                               startPose.getHeading() + 0.35 ),
                                             20 );
    double finalProgressWithNegativeHeadingError =
            runAuxiliarySpatialFieldEpisode( track,
                                             buildBackend( track ),
                                             actionAdapter,
                                             new RacingStartState(
                                               startPose.getX(),
                                               startPose.getY(),
                                               6.0,
                                               startPose.getHeading() - 0.35 ),
                                             20 );

    assertTrue( Double.isFinite( finalProgressWithPositiveHeadingError ) );
    assertTrue( Double.isFinite( finalProgressWithNegativeHeadingError ) );
    assertTrue( finalProgressWithPositiveHeadingError > 0.0 );
    assertTrue( finalProgressWithNegativeHeadingError > 0.0 );
  }

  private static SimpleOvalTrack buildTrack() {
    return new SimpleOvalTrack( "simple_oval",
                                0.0,
                                0.0,
                                25.0,
                                30.0,
                                5.0 );
  }

  private static SimpleOvalRacingBackend buildBackend( SimpleOvalTrack track ) {
    return new SimpleOvalRacingBackend( track,
                                        0.2,
                                        1.0,
                                        12.0,
                                        18.0,
                                        0.05,
                                        40.0 );
  }

  private static double runAuxiliarySpatialFieldEpisode(
    SimpleOvalTrack track,
    SimpleOvalRacingBackend backend,
    SimpleTargetRacingActionAdapter actionAdapter,
    RacingStartState startState,
    int countOfSteps ) {
    RacingCarState currentState = backend.resetEpisode( startState );
    double previousProgressMeters = 0.0;
    double maxProgressMeters = 0.0;

    for( int stepIndex = 0;
         stepIndex < countOfSteps;
         stepIndex++ ) {
      RacingBackendAction backendAction = buildAuxiliaryLookaheadAction(
        track,
        actionAdapter,
        currentState );
      RacingStepResult stepResult = backend.step( backendAction );
      assertTrue( Double.isFinite( stepResult.getProgressMeters() ) );
      assertTrue( stepResult.getProgressMeters() >= previousProgressMeters );
      previousProgressMeters = stepResult.getProgressMeters();
      maxProgressMeters = Math.max( maxProgressMeters,
                                    previousProgressMeters );
      currentState = stepResult.getCarState();
      if( stepResult.isTerminal() ) {
        break;
      }
    }

    return maxProgressMeters;
  }

  private static RacingBackendAction buildAuxiliaryLookaheadAction(
    SimpleOvalTrack track,
    SimpleTargetRacingActionAdapter actionAdapter,
    RacingCarState currentState ) {
    double lookaheadProgressMeters = track.projectProgressMeters(
      currentState.getX(),
      currentState.getY() )
                                     + 8.0;
    SimpleOvalTrack.CenterlinePose lookaheadPose = track.computeCenterlinePose(
      lookaheadProgressMeters );
    double directionTarget = Math.atan2( lookaheadPose.getY()
                                         - currentState.getY(),
                                         lookaheadPose.getX()
                                         - currentState.getX() );
    return actionAdapter.toBackendAction(
      new RacingPolicyAction( 10.0,
                              directionTarget ),
      currentState );
  }
}
