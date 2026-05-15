package Problems;

import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimpleCircularRacingBackendTest {

  @Test
  public void sampleStartStateStaysInsideTrackWithZeroSpeed() {
    SimpleCircularTrack track = buildTrack();
    SimpleCircularRacingBackend backend = buildBackend( track );
    Random random = new Random( 1234 );

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
  public void stepAdvancesProgressAlongTrackTangent() {
    SimpleCircularTrack track = buildTrack();
    SimpleCircularRacingBackend backend = buildBackend( track );
    RacingStartState startState = new RacingStartState( 50.0,
                                                        0.0,
                                                        0.0,
                                                        Math.PI / 2.0 );
    backend.resetEpisode( startState );

    RacingStepResult lastStepResult = null;
    for( int stepIndex = 0;
         stepIndex < 6;
         stepIndex++ ) {
      lastStepResult = backend.step( new RacingBackendAction( 0.2,
                                                              1.0,
                                                              0.0 ) );
    }

    assertTrue( lastStepResult != null );
    assertEquals( RacingTerminationReason.NONE,
                  lastStepResult.getTerminationReason() );
    assertTrue( lastStepResult.getProgressMeters() > 0.0 );
    assertTrue( backend.isInsideTrack( lastStepResult.getCarState().getX(),
                                       lastStepResult.getCarState().getY() ) );
  }

  @Test
  public void stepReportsOffTrackWhenCarExitsTheAnnulus() {
    SimpleCircularTrack track = buildTrack();
    SimpleCircularRacingBackend backend = buildBackend( track );
    RacingStartState startState = new RacingStartState( 54.9,
                                                        0.0,
                                                        10.0,
                                                        0.0 );
    backend.resetEpisode( startState );

    RacingStepResult stepResult = backend.step( new RacingBackendAction( 0.0,
                                                                         0.0,
                                                                         0.0 ) );

    assertEquals( RacingTerminationReason.OFF_TRACK,
                  stepResult.getTerminationReason() );
  }

  private static SimpleCircularTrack buildTrack() {
    return new SimpleCircularTrack( "simple_circle",
                                    0.0,
                                    0.0,
                                    50.0,
                                    5.0 );
  }

  private static SimpleCircularRacingBackend buildBackend(
    SimpleCircularTrack track ) {
    return new SimpleCircularRacingBackend( track,
                                            0.2,
                                            1.0,
                                            12.0,
                                            18.0,
                                            0.05,
                                            40.0 );
  }
}
