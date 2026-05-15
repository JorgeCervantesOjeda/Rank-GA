// C:/Users/usuario/ownCloud2/RankGA/src/Problems/RacingActionAdapter.java
// Adapter from high-level racing policy outputs to backend actions.
package Problems;

public interface RacingActionAdapter {

  RacingBackendAction toBackendAction( RacingPolicyAction policyAction,
                                       RacingCarState carState );
}
