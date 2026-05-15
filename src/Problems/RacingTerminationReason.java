// C:/Users/usuario/ownCloud2/RankGA/src/Problems/RacingTerminationReason.java
// Termination reasons for racing simulation episodes.
package Problems;

public enum RacingTerminationReason {
  NONE,
  CRASH,
  OFF_TRACK,
  TIME_LIMIT,
  LAP_LIMIT;

  public boolean isCrashLike() {
    return this == CRASH || this == OFF_TRACK;
  }
}
