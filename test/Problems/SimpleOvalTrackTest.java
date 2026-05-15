// C:/Users/usuario/ownCloud2/RankGA/test/Problems/SimpleOvalTrackTest.java
// Geometry tests for the oval centerline projection used to measure track progress.
package Problems;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimpleOvalTrackTest {

  private static final double ASSERT_TOLERANCE = 1.0e-6;

  @Test
  public void projectProgressMatchesCenterlineProgressAcrossRepresentativeSamples() {
    SimpleOvalTrack track = buildTrack();
    double[] samplesOfProgressMeters = new double[] {
      0.0,
      10.0,
      30.0,
      59.9,
      60.0,
      60.1,
      80.0,
      120.0,
      138.5,
      138.6,
      170.0,
      198.5,
      198.6,
      240.0,
      276.9
    };

    for( double progressMeters : samplesOfProgressMeters ) {
      SimpleOvalTrack.CenterlinePose pose = track.computeCenterlinePose(
        progressMeters );
      assertEquals( progressMeters,
                    track.projectProgressMeters( pose.getX(),
                                                 pose.getY() ),
                    ASSERT_TOLERANCE );
    }
  }

  @Test
  public void projectProgressPreservesProgressAcrossStraightTurnTransitionOffsets() {
    SimpleOvalTrack track = buildTrack();
    double[] samplesOfProgressMeters = new double[] {
      58.0,
      59.0,
      59.9,
      60.0,
      60.1,
      61.0,
      62.0,
      65.0
    };
    double[] offsetsMeters = new double[] {
      -4.0,
      -2.0,
      0.0,
      2.0,
      4.0
    };

    for( double progressMeters : samplesOfProgressMeters ) {
      SimpleOvalTrack.CenterlinePose pose = track.computeCenterlinePose(
        progressMeters );
      for( double offsetMeters : offsetsMeters ) {
        double x = pose.getX() + offsetMeters * pose.getNormalX();
        double y = pose.getY() + offsetMeters * pose.getNormalY();
        assertTrue( track.isInsideTrack( x,
                                         y ) );
        assertEquals( progressMeters,
                      track.projectProgressMeters( x,
                                                   y ),
                      ASSERT_TOLERANCE );
      }
    }
  }

  @Test
  public void projectProgressWrapsContinuouslyAcrossLapBoundary() {
    SimpleOvalTrack track = buildTrack();
    double lapLength = track.getLapLength();
    double[] orderedSamplesOfProgressMeters = new double[] {
      lapLength - 3.0,
      lapLength - 1.0,
      lapLength - 0.2,
      lapLength,
      lapLength + 0.2,
      lapLength + 1.0,
      lapLength + 3.0
    };

    double previousProjectedProgressMeters = Double.NaN;
    for( double progressMeters : orderedSamplesOfProgressMeters ) {
      SimpleOvalTrack.CenterlinePose pose = track.computeCenterlinePose(
        progressMeters );
      double projectedProgressMeters = track.projectProgressMeters( pose.getX(),
                                                                    pose.getY() );
      if( !Double.isNaN( previousProjectedProgressMeters ) ) {
        double wrappedDeltaMeters = wrapForwardDelta( projectedProgressMeters
                                                      - previousProjectedProgressMeters,
                                                      lapLength );
        assertTrue( wrappedDeltaMeters > 0.0 );
      }
      previousProjectedProgressMeters = projectedProgressMeters;
    }
  }

  private static double wrapForwardDelta( double deltaMeters,
                                          double lapLength ) {
    if( deltaMeters < 0.0 ) {
      return deltaMeters + lapLength;
    }
    return deltaMeters;
  }

  private static SimpleOvalTrack buildTrack() {
    return new SimpleOvalTrack( "simple_oval",
                                0.0,
                                0.0,
                                25.0,
                                30.0,
                                5.0 );
  }
}
