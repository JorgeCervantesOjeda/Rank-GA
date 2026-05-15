// C:/Users/usuario/ownCloud2/RankGA/src/Problems/SimpleOvalTrack.java
// Immutable oval track geometry with two straights and two semicircular turns.
package Problems;

public final class SimpleOvalTrack {

  private static final double HALF_PI = Math.PI / 2.0;

  private final String trackName;
  private final double centerX;
  private final double centerY;
  private final double radiusOfTurns;
  private final double halfLengthOfStraight;
  private final double halfWidth;
  private final double lengthOfStraightSection;
  private final double lengthOfTurnSection;
  private final double lapLength;

  public SimpleOvalTrack( String trackName,
                          double centerX,
                          double centerY,
                          double radiusOfTurns,
                          double halfLengthOfStraight,
                          double halfWidth ) {
    if( trackName == null || trackName.trim().isEmpty() ) {
      throw new IllegalArgumentException( "trackName must not be blank" );
    }
    validateFinite( "centerX",
                    centerX );
    validateFinite( "centerY",
                    centerY );
    validatePositive( "radiusOfTurns",
                      radiusOfTurns );
    validatePositive( "halfLengthOfStraight",
                      halfLengthOfStraight );
    validatePositive( "halfWidth",
                      halfWidth );
    if( radiusOfTurns <= halfWidth ) {
      throw new IllegalArgumentException(
        "radiusOfTurns must be greater than halfWidth" );
    }
    this.trackName = trackName;
    this.centerX = centerX;
    this.centerY = centerY;
    this.radiusOfTurns = radiusOfTurns;
    this.halfLengthOfStraight = halfLengthOfStraight;
    this.halfWidth = halfWidth;
    this.lengthOfStraightSection = 2.0 * halfLengthOfStraight;
    this.lengthOfTurnSection = Math.PI * radiusOfTurns;
    this.lapLength = 2.0 * lengthOfStraightSection + 2.0 * lengthOfTurnSection;
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

  public double getRadiusOfTurns() {
    return radiusOfTurns;
  }

  public double getHalfLengthOfStraight() {
    return halfLengthOfStraight;
  }

  public double getHalfWidth() {
    return halfWidth;
  }

  public double getLapLength() {
    return lapLength;
  }

  public boolean isInsideTrack( double x,
                                double y ) {
    return computeProjection( x,
                              y ).getDistanceMeters() <= halfWidth;
  }

  public double projectProgressMeters( double x,
                                       double y ) {
    return computeProjection( x,
                              y ).getProgressMeters();
  }

  public CenterlinePose computeCenterlinePose( double progressMeters ) {
    double normalizedProgressMeters = normalizeProgress( progressMeters );

    if( normalizedProgressMeters < lengthOfStraightSection ) {
      double localX = -halfLengthOfStraight + normalizedProgressMeters;
      return buildPose( localX,
                        radiusOfTurns,
                        0.0,
                        normalizedProgressMeters );
    }

    double progressAfterTopStraight = normalizedProgressMeters
                                      - lengthOfStraightSection;
    if( progressAfterTopStraight < lengthOfTurnSection ) {
      double angle = HALF_PI - progressAfterTopStraight / radiusOfTurns;
      double localX = halfLengthOfStraight
                      + radiusOfTurns * Math.cos( angle );
      double localY = radiusOfTurns * Math.sin( angle );
      double heading = Math.atan2( -Math.cos( angle ),
                                   Math.sin( angle ) );
      return buildPose( localX,
                        localY,
                        heading,
                        normalizedProgressMeters );
    }

    double progressAfterRightTurn = progressAfterTopStraight
                                    - lengthOfTurnSection;
    if( progressAfterRightTurn < lengthOfStraightSection ) {
      double localX = halfLengthOfStraight - progressAfterRightTurn;
      return buildPose( localX,
                        -radiusOfTurns,
                        Math.PI,
                        normalizedProgressMeters );
    }

    double progressAfterBottomStraight = progressAfterRightTurn
                                         - lengthOfStraightSection;
    double angle = -HALF_PI + progressAfterBottomStraight / radiusOfTurns;
    double localX = -halfLengthOfStraight
                    - radiusOfTurns * Math.cos( angle );
    double localY = radiusOfTurns * Math.sin( angle );
    double heading = Math.atan2( Math.cos( angle ),
                                 Math.sin( angle ) );
    return buildPose( localX,
                      localY,
                      heading,
                      normalizedProgressMeters );
  }

  private CenterlinePose buildPose( double localX,
                                    double localY,
                                    double heading,
                                    double progressMeters ) {
    double normalizedHeading = wrapToPi( heading );
    double normalX = -Math.sin( normalizedHeading );
    double normalY = Math.cos( normalizedHeading );
    return new CenterlinePose( centerX + localX,
                               centerY + localY,
                               normalizedHeading,
                               normalX,
                               normalY,
                               progressMeters );
  }

  private Projection computeProjection( double x,
                                        double y ) {
    double localX = x - centerX;
    double localY = y - centerY;

    Projection bestProjection = projectionOnTopStraight( localX,
                                                         localY );
    bestProjection = pickClosest( bestProjection,
                                  projectionOnRightTurn( localX,
                                                         localY ) );
    bestProjection = pickClosest( bestProjection,
                                  projectionOnBottomStraight( localX,
                                                              localY ) );
    return pickClosest( bestProjection,
                        projectionOnLeftTurn( localX,
                                              localY ) );
  }

  private Projection projectionOnTopStraight( double localX,
                                              double localY ) {
    double projectedX = clamp( localX,
                               -halfLengthOfStraight,
                               halfLengthOfStraight );
    double projectedY = radiusOfTurns;
    double progressMeters = projectedX + halfLengthOfStraight;
    return new Projection( progressMeters,
                           distanceMeters( localX,
                                           localY,
                                           projectedX,
                                           projectedY ) );
  }

  private Projection projectionOnRightTurn( double localX,
                                            double localY ) {
    double angle = clamp( Math.atan2( localY,
                                      localX - halfLengthOfStraight ),
                          -HALF_PI,
                          HALF_PI );
    double projectedX = halfLengthOfStraight
                        + radiusOfTurns * Math.cos( angle );
    double projectedY = radiusOfTurns * Math.sin( angle );
    double progressMeters = lengthOfStraightSection
                            + ( HALF_PI - angle ) * radiusOfTurns;
    return new Projection( progressMeters,
                           distanceMeters( localX,
                                           localY,
                                           projectedX,
                                           projectedY ) );
  }

  private Projection projectionOnBottomStraight( double localX,
                                                 double localY ) {
    double projectedX = clamp( localX,
                               -halfLengthOfStraight,
                               halfLengthOfStraight );
    double projectedY = -radiusOfTurns;
    double progressMeters = lengthOfStraightSection
                            + lengthOfTurnSection
                            + ( halfLengthOfStraight - projectedX );
    return new Projection( progressMeters,
                           distanceMeters( localX,
                                           localY,
                                           projectedX,
                                           projectedY ) );
  }

  private Projection projectionOnLeftTurn( double localX,
                                           double localY ) {
    double angle = clamp( Math.atan2( localY,
                                      -( localX + halfLengthOfStraight ) ),
                          -HALF_PI,
                          HALF_PI );
    double projectedX = -halfLengthOfStraight
                        - radiusOfTurns * Math.cos( angle );
    double projectedY = radiusOfTurns * Math.sin( angle );
    double progressMeters = lengthOfStraightSection
                            + lengthOfTurnSection
                            + lengthOfStraightSection
                            + ( angle + HALF_PI ) * radiusOfTurns;
    return new Projection( progressMeters,
                           distanceMeters( localX,
                                           localY,
                                           projectedX,
                                           projectedY ) );
  }

  private static Projection pickClosest( Projection currentBest,
                                         Projection candidate ) {
    return candidate.getDistanceMeters() < currentBest.getDistanceMeters()
           ? candidate
           : currentBest;
  }

  private double normalizeProgress( double progressMeters ) {
    double normalizedProgressMeters = progressMeters % lapLength;
    if( normalizedProgressMeters < 0.0 ) {
      normalizedProgressMeters += lapLength;
    }
    return normalizedProgressMeters;
  }

  private static double distanceMeters( double x,
                                        double y,
                                        double projectedX,
                                        double projectedY ) {
    double deltaX = x - projectedX;
    double deltaY = y - projectedY;
    return Math.sqrt( deltaX * deltaX + deltaY * deltaY );
  }

  private static double clamp( double value,
                               double minValue,
                               double maxValue ) {
    if( value < minValue ) {
      return minValue;
    }
    if( value > maxValue ) {
      return maxValue;
    }
    return value;
  }

  private static double wrapToPi( double angle ) {
    double wrappedAngle = angle;
    while( wrappedAngle <= -Math.PI ) {
      wrappedAngle += 2.0 * Math.PI;
    }
    while( wrappedAngle > Math.PI ) {
      wrappedAngle -= 2.0 * Math.PI;
    }
    return wrappedAngle;
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

  public static final class CenterlinePose {

    private final double x;
    private final double y;
    private final double heading;
    private final double normalX;
    private final double normalY;
    private final double progressMeters;

    private CenterlinePose( double x,
                            double y,
                            double heading,
                            double normalX,
                            double normalY,
                            double progressMeters ) {
      this.x = x;
      this.y = y;
      this.heading = heading;
      this.normalX = normalX;
      this.normalY = normalY;
      this.progressMeters = progressMeters;
    }

    public double getX() {
      return x;
    }

    public double getY() {
      return y;
    }

    public double getHeading() {
      return heading;
    }

    public double getNormalX() {
      return normalX;
    }

    public double getNormalY() {
      return normalY;
    }

    public double getProgressMeters() {
      return progressMeters;
    }
  }

  private static final class Projection {

    private final double progressMeters;
    private final double distanceMeters;

    private Projection( double progressMeters,
                        double distanceMeters ) {
      this.progressMeters = progressMeters;
      this.distanceMeters = distanceMeters;
    }

    public double getProgressMeters() {
      return progressMeters;
    }

    public double getDistanceMeters() {
      return distanceMeters;
    }
  }
}
