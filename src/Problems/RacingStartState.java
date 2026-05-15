// C:/Users/usuario/ownCloud2/RankGA/src/Problems/RacingStartState.java
// Immutable initial state for one racing episode.
package Problems;

public final class RacingStartState {

  private final double x;
  private final double y;
  private final double speed;
  private final double heading;

  public RacingStartState( double x,
                           double y,
                           double speed,
                           double heading ) {
    validateFinite( "x",
                    x );
    validateFinite( "y",
                    y );
    validateFinite( "speed",
                    speed );
    validateFinite( "heading",
                    heading );
    this.x = x;
    this.y = y;
    this.speed = speed;
    this.heading = heading;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public double getSpeed() {
    return speed;
  }

  public double getHeading() {
    return heading;
  }

  private static void validateFinite( String label,
                                      double value ) {
    if( Double.isNaN( value ) || Double.isInfinite( value ) ) {
      throw new IllegalArgumentException( label + " must be finite" );
    }
  }
}
