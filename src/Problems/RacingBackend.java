// C:/Users/usuario/ownCloud2/RankGA/src/Problems/RacingBackend.java
// Neutral interface that a concrete racing simulator backend must implement.
package Problems;

import java.util.Random;

public interface RacingBackend {

  String getTrackName();

  RacingStartState sampleStartState( Random random );

  RacingCarState resetEpisode( RacingStartState startState );

  RacingStepResult step( RacingBackendAction backendAction );

  double measureProgress( RacingCarState carState );

  boolean isInsideTrack( double x,
                         double y );

  double getPolicyCenterX();

  double getPolicyCenterY();

  double getPolicyHalfRangeX();

  double getPolicyHalfRangeY();

  double getPolicySpeedScale();
}
