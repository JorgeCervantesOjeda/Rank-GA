// C:/Users/usuario/ownCloud2/RankGA/src/Problems/RacingCarState.java
// Immutable observable state of the racing car.
package Problems;

public final class RacingCarState {

  private final double x;
  private final double y;
  private final double speed;
  private final double heading;
  private final double timeSeconds;

  public RacingCarState( double x,
                         double y,
                         double speed,
                         double heading,
                         double timeSeconds ) {
    validateFinite( "x",
                    x );
    validateFinite( "y",
                    y );
    validateFinite( "speed",
                    speed );
    validateFinite( "heading",
                    heading );
    validateFinite( "timeSeconds",
                    timeSeconds );
    this.x = x;
    this.y = y;
    this.speed = speed;
    this.heading = heading;
    this.timeSeconds = timeSeconds;
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

  public double getTimeSeconds() {
    return timeSeconds;
  }

  private static void validateFinite( String label,
                                      double value ) {
    if( Double.isNaN( value ) || Double.isInfinite( value ) ) {
      throw new IllegalArgumentException( label + " must be finite" );
    }
  }
}
