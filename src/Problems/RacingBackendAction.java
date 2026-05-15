// C:/Users/usuario/ownCloud2/RankGA/src/Problems/RacingBackendAction.java
// Low-level action consumed by a concrete racing simulator backend.
package Problems;

public final class RacingBackendAction {

  private final double steeringCommand;
  private final double throttleCommand;
  private final double brakeCommand;

  public RacingBackendAction( double steeringCommand,
                              double throttleCommand,
                              double brakeCommand ) {
    validateFinite( "steeringCommand",
                    steeringCommand );
    validateFinite( "throttleCommand",
                    throttleCommand );
    validateFinite( "brakeCommand",
                    brakeCommand );
    if( throttleCommand < 0.0 ) {
      throw new IllegalArgumentException( "throttleCommand must be non-negative" );
    }
    if( brakeCommand < 0.0 ) {
      throw new IllegalArgumentException( "brakeCommand must be non-negative" );
    }
    this.steeringCommand = steeringCommand;
    this.throttleCommand = throttleCommand;
    this.brakeCommand = brakeCommand;
  }

  public double getSteeringCommand() {
    return steeringCommand;
  }

  public double getThrottleCommand() {
    return throttleCommand;
  }

  public double getBrakeCommand() {
    return brakeCommand;
  }

  private static void validateFinite( String label,
                                      double value ) {
    if( Double.isNaN( value ) || Double.isInfinite( value ) ) {
      throw new IllegalArgumentException( label + " must be finite" );
    }
  }
}
