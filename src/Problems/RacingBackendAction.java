// C:/Users/usuario/ownCloud2/RankGA/src/Problems/RacingBackendAction.java
// Low-level action consumed by a concrete racing simulator backend.
package Problems;

public final class RacingBackendAction {

  private final double steeringCommand;
  private final double throttleCommand;
  private final double brakeCommand;
  private final boolean hasHeadingTarget;
  private final double headingTarget;
  private final boolean hasSpeedTarget;
  private final double speedTarget;

  public RacingBackendAction( double steeringCommand,
                              double throttleCommand,
                              double brakeCommand ) {
    this( steeringCommand,
          throttleCommand,
          brakeCommand,
          false,
          0.0,
          false,
          0.0 );
  }

  public RacingBackendAction( double steeringCommand,
                              double throttleCommand,
                              double brakeCommand,
                              double headingTarget ) {
    this( steeringCommand,
          throttleCommand,
          brakeCommand,
          true,
          headingTarget,
          false,
          0.0 );
  }

  public RacingBackendAction( double steeringCommand,
                              double throttleCommand,
                              double brakeCommand,
                              double headingTarget,
                              double speedTarget ) {
    this( steeringCommand,
          throttleCommand,
          brakeCommand,
          true,
          headingTarget,
          true,
          speedTarget );
  }

  private RacingBackendAction( double steeringCommand,
                               double throttleCommand,
                               double brakeCommand,
                               boolean hasHeadingTarget,
                               double headingTarget,
                               boolean hasSpeedTarget,
                               double speedTarget ) {
    validateFinite( "steeringCommand",
                    steeringCommand );
    validateFinite( "throttleCommand",
                    throttleCommand );
    validateFinite( "brakeCommand",
                    brakeCommand );
    if( hasHeadingTarget ) {
      validateFinite( "headingTarget",
                      headingTarget );
    }
    if( hasSpeedTarget ) {
      validateFinite( "speedTarget",
                      speedTarget );
    }
    if( throttleCommand < 0.0 ) {
      throw new IllegalArgumentException( "throttleCommand must be non-negative" );
    }
    if( brakeCommand < 0.0 ) {
      throw new IllegalArgumentException( "brakeCommand must be non-negative" );
    }
    this.steeringCommand = steeringCommand;
    this.throttleCommand = throttleCommand;
    this.brakeCommand = brakeCommand;
    this.hasHeadingTarget = hasHeadingTarget;
    this.headingTarget = headingTarget;
    this.hasSpeedTarget = hasSpeedTarget;
    this.speedTarget = speedTarget;
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

  public boolean hasHeadingTarget() {
    return hasHeadingTarget;
  }

  public double getHeadingTarget() {
    if( !hasHeadingTarget ) {
      throw new IllegalStateException( "headingTarget is not available" );
    }
    return headingTarget;
  }

  public boolean hasSpeedTarget() {
    return hasSpeedTarget;
  }

  public double getSpeedTarget() {
    if( !hasSpeedTarget ) {
      throw new IllegalStateException( "speedTarget is not available" );
    }
    return speedTarget;
  }

  private static void validateFinite( String label,
                                      double value ) {
    if( Double.isNaN( value ) || Double.isInfinite( value ) ) {
      throw new IllegalArgumentException( label + " must be finite" );
    }
  }
}
