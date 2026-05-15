// C:/Users/usuario/ownCloud2/RankGA/src/Problems/SimpleCircularTrack.java
// Immutable circular track geometry for deterministic racing backend tests.
package Problems;

public final class SimpleCircularTrack {

  private final String trackName;
  private final double centerX;
  private final double centerY;
  private final double radiusOfCenterline;
  private final double halfWidth;

  public SimpleCircularTrack( String trackName,
                              double centerX,
                              double centerY,
                              double radiusOfCenterline,
                              double halfWidth ) {
    if( trackName == null || trackName.trim().isEmpty() ) {
      throw new IllegalArgumentException( "trackName must not be blank" );
    }
    validateFinite( "centerX",
                    centerX );
    validateFinite( "centerY",
                    centerY );
    validatePositive( "radiusOfCenterline",
                      radiusOfCenterline );
    validatePositive( "halfWidth",
                      halfWidth );
    if( radiusOfCenterline <= halfWidth ) {
      throw new IllegalArgumentException(
        "radiusOfCenterline must be greater than halfWidth" );
    }
    this.trackName = trackName;
    this.centerX = centerX;
    this.centerY = centerY;
    this.radiusOfCenterline = radiusOfCenterline;
    this.halfWidth = halfWidth;
  }

  public String getTrackName() {
    return trackName;
  }

  public double getCenterX() {
    return centerX;
  }

  public double getCenterY() {
    return centerY;
  }

  public double getRadiusOfCenterline() {
    return radiusOfCenterline;
  }

  public double getHalfWidth() {
    return halfWidth;
  }

  public double getInnerRadius() {
    return radiusOfCenterline - halfWidth;
  }

  public double getOuterRadius() {
    return radiusOfCenterline + halfWidth;
  }

  public double computeAngle( double x,
                              double y ) {
    return Math.atan2( y - centerY,
                       x - centerX );
  }

  public double computeRadius( double x,
                               double y ) {
    double deltaX = x - centerX;
    double deltaY = y - centerY;
    return Math.sqrt( deltaX * deltaX + deltaY * deltaY );
  }

  public boolean isInsideTrack( double x,
                                double y ) {
    double radius = computeRadius( x,
                                   y );
    return radius >= getInnerRadius() && radius <= getOuterRadius();
  }

  private static void validatePositive( String label,
                                        double value ) {
    validateFinite( label,
                    value );
    if( value <= 0.0 ) {
      throw new IllegalArgumentException( label + " must be positive" );
    }
  }

  private static void validateFinite( String label,
                                      double value ) {
    if( Double.isNaN( value ) || Double.isInfinite( value ) ) {
      throw new IllegalArgumentException( label + " must be finite" );
    }
  }
}
