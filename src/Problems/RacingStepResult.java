// C:/Users/usuario/ownCloud2/RankGA/src/Problems/RacingStepResult.java
// One simulation step result returned by a racing backend.
package Problems;

public final class RacingStepResult {

  private final RacingCarState carState;
  private final double progressMeters;
  private final RacingTerminationReason terminationReason;

  public RacingStepResult( RacingCarState carState,
                           double progressMeters,
                           RacingTerminationReason terminationReason ) {
    if( carState == null ) {
      throw new IllegalArgumentException( "carState must not be null" );
    }
    if( Double.isNaN( progressMeters ) || Double.isInfinite( progressMeters ) ) {
      throw new IllegalArgumentException( "progressMeters must be finite" );
    }
    if( terminationReason == null ) {
      throw new IllegalArgumentException( "terminationReason must not be null" );
    }
    this.carState = carState;
    this.progressMeters = progressMeters;
    this.terminationReason = terminationReason;
  }

  public RacingCarState getCarState() {
    return carState;
  }

  public double getProgressMeters() {
    return progressMeters;
  }

  public RacingTerminationReason getTerminationReason() {
    return terminationReason;
  }

  public boolean isTerminal() {
    return terminationReason != RacingTerminationReason.NONE;
  }
}
