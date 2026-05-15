// C:/Users/usuario/ownCloud2/RankGA/src/Problems/RacingPolicyAction.java
// High-level action produced by the evolved racing policy.
package Problems;

public final class RacingPolicyAction {

  private final double speedTarget;
  private final double directionTarget;

  public RacingPolicyAction( double speedTarget,
                             double directionTarget ) {
    validateFinite( "speedTarget",
                    speedTarget );
    validateFinite( "directionTarget",
                    directionTarget );
    this.speedTarget = speedTarget;
    this.directionTarget = directionTarget;
  }

  public double getSpeedTarget() {
    return speedTarget;
  }

  public double getDirectionTarget() {
    return directionTarget;
  }

  private static void validateFinite( String label,
                                      double value ) {
    if( Double.isNaN( value ) || Double.isInfinite( value ) ) {
      throw new IllegalArgumentException( label + " must be finite" );
    }
  }
}
