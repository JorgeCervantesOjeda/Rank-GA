// C:/Users/usuario/ownCloud2/RankGA/src/Problems/SimpleTargetRacingActionAdapter.java
// Simple proportional adapter from policy targets to backend actuator commands.
package Problems;

public final class SimpleTargetRacingActionAdapter
  implements RacingActionAdapter {

  private final double directionGain;
  private final double throttleGain;
  private final double brakeGain;

  public SimpleTargetRacingActionAdapter() {
    this( 1.0,
          0.2,
          0.2 );
  }

  public SimpleTargetRacingActionAdapter( double directionGain,
                                          double throttleGain,
                                          double brakeGain ) {
    validatePositive( "directionGain",
                      directionGain );
    validatePositive( "throttleGain",
                      throttleGain );
    validatePositive( "brakeGain",
                      brakeGain );
    this.directionGain = directionGain;
    this.throttleGain = throttleGain;
    this.brakeGain = brakeGain;
  }

  @Override
  public RacingBackendAction toBackendAction( RacingPolicyAction policyAction,
                                              RacingCarState carState ) {
    if( policyAction == null ) {
      throw new IllegalArgumentException( "policyAction must not be null" );
    }
    if( carState == null ) {
      throw new IllegalArgumentException( "carState must not be null" );
    }
    double steeringCommand = clamp( directionGain
                                    * wrapToPi( policyAction.getDirectionTarget()
                                                - carState.getHeading() ),
                                    -1.0,
                                    1.0 );
    double speedError = policyAction.getSpeedTarget() - carState.getSpeed();
    double throttleCommand = speedError > 0.0
                             ? clamp( throttleGain * speedError,
                                      0.0,
                                      1.0 )
                             : 0.0;
    double brakeCommand = speedError < 0.0
                          ? clamp( brakeGain * -speedError,
                                   0.0,
                                   1.0 )
                          : 0.0;
    return new RacingBackendAction( steeringCommand,
                                    throttleCommand,
                                    brakeCommand );
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
    if( Double.isNaN( value ) || Double.isInfinite( value ) ) {
      throw new IllegalArgumentException( label + " must be finite" );
    }
    if( value <= 0.0 ) {
      throw new IllegalArgumentException( label + " must be positive" );
    }
  }
}
