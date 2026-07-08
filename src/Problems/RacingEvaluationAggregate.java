// C:/Users/usuario/ownCloud2/RankGA/src/Problems/RacingEvaluationAggregate.java
// Pure aggregate metrics for a batch of deterministic racing evaluations.
package Problems;

final class RacingEvaluationAggregate {

  private final int countOfSafeRuns;
  private final int countOfGoalRuns;
  private final double averageDistanceMeters;
  private final double averageSpeedMetersPerSecond;
  private final double averageGoalTimeSeconds;

  RacingEvaluationAggregate( int countOfSafeRuns,
                             int countOfGoalRuns,
                             double averageDistanceMeters,
                             double averageSpeedMetersPerSecond,
                             double averageGoalTimeSeconds ) {
    this.countOfSafeRuns = countOfSafeRuns;
    this.countOfGoalRuns = countOfGoalRuns;
    this.averageDistanceMeters = averageDistanceMeters;
    this.averageSpeedMetersPerSecond = averageSpeedMetersPerSecond;
    this.averageGoalTimeSeconds = averageGoalTimeSeconds;
  }

  int getCountOfSafeRuns() {
    return countOfSafeRuns;
  }

  int getCountOfGoalRuns() {
    return countOfGoalRuns;
  }

  double getAverageDistanceMeters() {
    return averageDistanceMeters;
  }

  double getAverageSpeedMetersPerSecond() {
    return averageSpeedMetersPerSecond;
  }

  double getAverageGoalTimeSeconds() {
    return averageGoalTimeSeconds;
  }
}
