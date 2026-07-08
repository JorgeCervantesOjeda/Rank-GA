// C:/Users/usuario/ownCloud2/RankGA/test/Problems/ProblemRacingTest.java
// Unit tests for the simulator-neutral racing problem and its adaptive horizon.
package Problems;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import rankga.Individual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProblemRacingTest {

  @Test
  public void adaptMultipliesTimeLimitAfterConfiguredPatience() {
    ScriptedBackend backend = new ScriptedBackend();
    CountingStartStateProvider provider =
            new CountingStartStateProvider(
              new RacingStartState( 1.0,
                                    -1.0,
                                    0.0,
                                    0.0 ) );
    ProblemRacing problem = new ProblemRacing( backend,
                                               new ZeroActionAdapter(),
                                               provider,
                                               1,
                                               1,
                                               2.0,
                                               1.5,
                                               2 );

    assertEquals( 1,
                  provider.getCountOfRefreshCalls() );
    problem.getNewIndividual( false,
                              new Random( 1 ) )
      .updateFitness();

    problem.adapt( 10.0 );
    problem.adapt( 10.0 );
    assertEquals( 2.0,
                  problem.getCurrentTimeLimitSeconds(),
                  0.0 );

    problem.adapt( 10.0 );
    assertEquals( 3.0,
                  problem.getCurrentTimeLimitSeconds(),
                  0.0 );
    assertEquals( 0,
                  problem.getCountOfStagnantGenerations() );
    assertEquals( 4,
                  provider.getCountOfRefreshCalls() );
  }

  @Test
  public void adaptTreatsAnyStrictFitnessIncreaseAsAnImprovement() {
    ScriptedBackend backend = new ScriptedBackend();
    CountingStartStateProvider provider =
            new CountingStartStateProvider(
              new RacingStartState( 1.0,
                                    -1.0,
                                    0.0,
                                    0.0 ) );
    ProblemRacing problem = new ProblemRacing( backend,
                                               new ZeroActionAdapter(),
                                               provider,
                                               1,
                                               1,
                                               2.0,
                                               1.5,
                                               2 );
    problem.getNewIndividual( false,
                              new Random( 1 ) )
      .updateFitness();

    problem.adapt( 10.0 );
    problem.adapt( 10.0 );
    assertEquals( 1,
                  problem.getCountOfStagnantGenerations() );

    problem.adapt( Math.nextUp( 10.0 ) );
    assertEquals( 0,
                  problem.getCountOfStagnantGenerations() );
    assertEquals( 2.0,
                  problem.getCurrentTimeLimitSeconds(),
                  0.0 );
  }

  @Test
  public void adaptDoesNotIncreaseTimeLimitWhenBestRunWentOffTrack() {
    ScriptedBackend backend = new ScriptedBackend();
    CountingStartStateProvider provider =
            new CountingStartStateProvider(
              new RacingStartState( 10.0,
                                    1.0,
                                    0.0,
                                    -1.0 ) );
    ProblemRacing problem = new ProblemRacing( backend,
                                               new ZeroActionAdapter(),
                                               provider,
                                               1,
                                               1,
                                               2.0,
                                               1.5,
                                               1,
                                               1.0,
                                               0.0,
                                               2.0,
                                               0.001,
                                               1.0,
                                               100.0 );
    Individual individual = problem.getNewIndividual( false,
                                                      new Random( 1 ) );

    individual.updateFitness();
    assertTrue( problem.hasBestFitnessSeenGoneOffTrack() );

    problem.adapt( 10.0 );
    problem.adapt( 10.0 );
    assertEquals( computeFiniteGoalFitness( 0,
                                            10.0,
                                            100.0,
                                            0,
                                            2.0 ),
                  individual.getFitness(),
                  1.0e-12 );
    assertEquals( 2.0,
                  problem.getCurrentTimeLimitSeconds(),
                  0.0 );
    assertEquals( 0,
                  problem.getCountOfStagnantGenerations() );
  }

  @Test
  public void fitnessUsesCurrentTimeLimitSeconds() {
    ScriptedBackend backend = new ScriptedBackend();
    CountingStartStateProvider provider =
            new CountingStartStateProvider(
              new RacingStartState( 7.0,
                                    -1.0,
                                    0.0,
                                    0.0 ) );
    ProblemRacing problem = new ProblemRacing( backend,
                                               new ZeroActionAdapter(),
                                               provider,
                                               1,
                                               1,
                                               2.0,
                                               1.5,
                                               2 );
    Individual individual = problem.getNewIndividual( false,
                                                      new Random( 1 ) );

    assertEquals( computeOpenGoalFitness( 1,
                                          14.0 ),
                  problem.fitness( individual ),
                  1.0e-12 );
  }

  @Test
  public void fitnessAggregatesAverageDistanceAcrossConfiguredCountOfRuns() {
    ScriptedBackend backend = new ScriptedBackend();
    CountingStartStateProvider provider =
            new CountingStartStateProvider(
              new RacingStartState( 10.0,
                                    1.0,
                                    0.0,
                                    0.0 ),
              new RacingStartState( 7.0,
                                    -1.0,
                                    0.0,
                                    0.0 ) );
    ProblemRacing problem = new ProblemRacing( backend,
                                               new ZeroActionAdapter(),
                                               provider,
                                               1,
                                               2,
                                               3.0,
                                               1.5,
                                               2 );
    Individual individual = problem.getNewIndividual( false,
                                                      new Random( 1 ) );

    assertEquals( computeOpenGoalFitness( 1,
                                          15.5 ),
                  problem.fitness( individual ),
                  1.0e-12 );
  }

  @Test
  public void aggregateFitnessPrioritizesSafeRunCountBeforeAverageDistance() {
    ProblemRacing saferProblem = buildScriptedProblem(
      2,
      3.0,
      Double.MAX_VALUE,
      new RacingStartState( 1.0,
                            -1.0,
                            0.0,
                            0.0 ),
      new RacingStartState( 1.0,
                            -1.0,
                            0.0,
                            0.0 ) );
    ProblemRacing fartherButUnsafeProblem = buildScriptedProblem(
      2,
      3.0,
      Double.MAX_VALUE,
      new RacingStartState( 100.0,
                            1.0,
                            0.0,
                            -1.0 ),
      new RacingStartState( 100.0,
                            1.0,
                            0.0,
                            -1.0 ) );

    double saferFitness = saferProblem.fitness(
      saferProblem.getNewIndividual( false,
                                     new Random( 1 ) ) );
    double fartherButUnsafeFitness = fartherButUnsafeProblem.fitness(
      fartherButUnsafeProblem.getNewIndividual( false,
                                                new Random( 1 ) ) );

    assertTrue( saferFitness > fartherButUnsafeFitness );
  }

  @Test
  public void distanceOnlyFitnessPrioritizesAverageDistanceOverSafetyCount() {
    ProblemRacing saferProblem = buildScriptedProblem(
      2,
      3.0,
      Double.MAX_VALUE,
      new RacingStartState( 1.0,
                            -1.0,
                            0.0,
                            0.0 ),
      new RacingStartState( 1.0,
                            -1.0,
                            0.0,
                            0.0 ) );
    ProblemRacing fartherButUnsafeProblem = buildScriptedProblem(
      2,
      3.0,
      Double.MAX_VALUE,
      new RacingStartState( 100.0,
                            1.0,
                            0.0,
                            -1.0 ),
      new RacingStartState( 100.0,
                            1.0,
                            0.0,
                            -1.0 ) );
    saferProblem.useDistanceOnlyFitness();
    fartherButUnsafeProblem.useDistanceOnlyFitness();

    double saferFitness = saferProblem.fitness(
      saferProblem.getNewIndividual( false,
                                     new Random( 1 ) ) );
    double fartherButUnsafeFitness = fartherButUnsafeProblem.fitness(
      fartherButUnsafeProblem.getNewIndividual( false,
                                                new Random( 1 ) ) );

    assertTrue( fartherButUnsafeFitness > saferFitness );
  }

  @Test
  public void targetDistanceSpeedFitnessRewardsFasterGoalCompletionAfterTargetDistance() {
    ProblemRacing fasterProblem = buildScriptedProblem(
      1,
      10.0,
      25.0,
      new RacingStartState( 10.0,
                            -1.0,
                            0.0,
                            0.0 ) );
    ProblemRacing slowerProblem = buildScriptedProblem(
      1,
      10.0,
      25.0,
      new RacingStartState( 5.0,
                            -1.0,
                            0.0,
                            0.0 ) );
    fasterProblem.useTargetDistanceSpeedFitness();
    slowerProblem.useTargetDistanceSpeedFitness();

    double fasterFitness = fasterProblem.fitness(
      fasterProblem.getNewIndividual( false,
                                      new Random( 1 ) ) );
    double slowerFitness = slowerProblem.fitness(
      slowerProblem.getNewIndividual( false,
                                      new Random( 1 ) ) );

    assertTrue( fasterFitness > slowerFitness );
  }

  @Test
  public void fitnessTreatsEndingAtZeroSpeedAsOffTrack() {
    ScriptedBackend backend = new ScriptedBackend();
    CountingStartStateProvider provider =
            new CountingStartStateProvider(
              new RacingStartState( 0.0,
                                    -1.0,
                                    0.0,
                                    0.0 ) );
    ProblemRacing problem = new ProblemRacing( backend,
                                               new ZeroActionAdapter(),
                                               provider,
                                               1,
                                               1,
                                               3.0,
                                               1.5,
                                               2 );
    Individual individual = problem.getNewIndividual( false,
                                                      new Random( 1 ) );

    individual.updateFitness();

    assertEquals( computeOpenGoalFitness( 0,
                                          0.0 ),
                  individual.getFitness(),
                  1.0e-12 );
    String individualString = individual.toString();
    assertTrue( individualString.contains( "offTrack=true" ) );
    assertTrue( individualString.contains( "safeRuns=0" ) );
  }

  @Test
  public void randomizedOvalIndividualStartsWithAnchorsOnBothTrackBorders() {
    SimpleOvalTrack track = new SimpleOvalTrack( "simple_oval",
                                                 0.0,
                                                 0.0,
                                                 25.0,
                                                 30.0,
                                                 5.0 );
    SimpleOvalRacingBackend backend = new SimpleOvalRacingBackend( track,
                                                                   0.2,
                                                                   1.0,
                                                                   12.0,
                                                                   18.0,
                                                                   0.05,
                                                                   40.0 );
    ProblemRacing problem = new ProblemRacing(
      backend,
      new ZeroActionAdapter(),
      new CountingStartStateProvider(
        new RacingStartState( 0.0,
                              25.0,
                              0.0,
                              0.0 ) ),
      10,
      1,
      1.0,
      1.5,
      2 );

    Individual individual = problem.getNewIndividual( true,
                                                      new Random( 1234L ) );

    int countOfPositiveBorderAnchors = 0;
    int countOfNegativeBorderAnchors = 0;
    for( int anchorIndex = 0;
         anchorIndex < 10;
         anchorIndex++ ) {
      int offset = anchorIndex * 4;
      double x = backend.getPolicyCenterX()
                 + individual.getGene( offset )
                   .getValue() * backend.getPolicyHalfRangeX();
      double y = backend.getPolicyCenterY()
                 + individual.getGene( offset + 1 )
                   .getValue() * backend.getPolicyHalfRangeY();
      double speedTarget = individual.getGene( offset + 2 )
                           .getValue() * backend.getPolicySpeedScale();
      double directionTarget = wrapToPi( individual.getGene( offset + 3 )
                                         .getValue() * Math.PI );
      SimpleOvalTrack.CenterlinePose pose = track.computeCenterlinePose(
        track.projectProgressMeters( x,
                                     y ) );
      double deltaX = x - pose.getX();
      double deltaY = y - pose.getY();
      double signedLateralDistance = deltaX * pose.getNormalX()
                                     + deltaY * pose.getNormalY();
      double tangentAlignment = Math.cos( directionTarget ) * Math.cos( pose.getHeading() )
                                + Math.sin( directionTarget )
                                  * Math.sin( pose.getHeading() );

      assertTrue( track.isInsideTrack( x,
                                       y ) );
      assertTrue( Math.abs( signedLateralDistance ) > 0.85 * track.getHalfWidth() );
      assertTrue( speedTarget >= 1.0 );
      assertTrue( speedTarget <= 5.0 );
      assertTrue( tangentAlignment > 0.75 );
      if( signedLateralDistance > 0.0 ) {
        countOfPositiveBorderAnchors++;
      } else {
        countOfNegativeBorderAnchors++;
      }
    }

    assertEquals( 5,
                  countOfPositiveBorderAnchors );
    assertEquals( 5,
                  countOfNegativeBorderAnchors );
  }

  @Test
  public void aggregateFitnessPrioritizesAverageDistanceBeforeGoalCount() {
    ProblemRacing fartherProblem = buildScriptedProblem(
      2,
      1.0,
      25.0,
      new RacingStartState( 24.0,
                            -1.0,
                            0.0,
                            0.0 ),
      new RacingStartState( 24.0,
                            -1.0,
                            0.0,
                            0.0 ) );
    ProblemRacing lessFarWithGoalProblem = buildScriptedProblem(
      2,
      1.0,
      25.0,
      new RacingStartState( 25.0,
                            -1.0,
                            0.0,
                            0.0 ),
      new RacingStartState( 1.0,
                            -1.0,
                            0.0,
                            0.0 ) );

    double fartherFitness = fartherProblem.fitness(
      fartherProblem.getNewIndividual( false,
                                       new Random( 1 ) ) );
    double lessFarWithGoalFitness = lessFarWithGoalProblem.fitness(
      lessFarWithGoalProblem.getNewIndividual( false,
                                               new Random( 1 ) ) );

    assertTrue( fartherFitness > lessFarWithGoalFitness );
  }

  @Test
  public void aggregateFitnessUsesGoalCountAfterAverageDistanceTies() {
    ProblemRacing oneGoalProblem = buildScriptedProblem(
      2,
      1.0,
      25.0,
      new RacingStartState( 25.0,
                            -1.0,
                            0.0,
                            0.0 ),
      new RacingStartState( 15.0,
                            -1.0,
                            0.0,
                            0.0 ) );
    ProblemRacing noGoalProblem = buildScriptedProblem(
      2,
      1.0,
      25.0,
      new RacingStartState( 20.0,
                            -1.0,
                            0.0,
                            0.0 ),
      new RacingStartState( 20.0,
                            -1.0,
                            0.0,
                            0.0 ) );

    double oneGoalFitness = oneGoalProblem.fitness(
      oneGoalProblem.getNewIndividual( false,
                                       new Random( 1 ) ) );
    double noGoalFitness = noGoalProblem.fitness(
      noGoalProblem.getNewIndividual( false,
                                      new Random( 1 ) ) );

    assertTrue( oneGoalFitness > noGoalFitness );
  }

  @Test
  public void fitnessExtraStringReportsAggregateDistanceAndOffTrack() {
    ScriptedBackend backend = new ScriptedBackend();
    CountingStartStateProvider provider =
            new CountingStartStateProvider(
              new RacingStartState( 10.0,
                                    1.0,
                                    0.0,
                                    -1.0 ),
              new RacingStartState( 7.0,
                                    -1.0,
                                    0.0,
                                    0.0 ) );
    ProblemRacing problem = new ProblemRacing( backend,
                                               new ZeroActionAdapter(),
                                               provider,
                                               1,
                                               2,
                                               3.0,
                                               1.5,
                                               2 );
    Individual individual = problem.getNewIndividual( false,
                                                      new Random( 1 ) );

    individual.updateFitness();

    String individualString = individual.toString();
    assertTrue( individualString.contains( "distance=15.500000" ) );
    assertTrue( individualString.contains( "offTrack=true" ) );
    assertTrue( individualString.contains( "goalReached=false" ) );
    assertTrue( individualString.contains( "safeRuns=1" ) );
    assertTrue( individualString.contains( "avgDistance=15.500000" ) );
    assertTrue( individualString.contains( "goalRuns=0" ) );
    assertTrue( individualString.contains( "fitness=" ) );
  }

  @Test
  public void finiteGoalFitnessStopsRunAfterGoalDistanceIsReached() {
    ScriptedBackend backend = new ScriptedBackend();
    CountingStartStateProvider provider =
            new CountingStartStateProvider(
              new RacingStartState( 10.0,
                                    -1.0,
                                    0.0,
                                    0.0 ) );
    ProblemRacing problem = new ProblemRacing( backend,
                                               new ZeroActionAdapter(),
                                               provider,
                                               1,
                                               1,
                                               10.0,
                                               1.5,
                                               2,
                                               1.0,
                                               0.0,
                                               2.0,
                                               0.001,
                                               1.0,
                                               25.0 );
    Individual individual = problem.getNewIndividual( false,
                                                      new Random( 1 ) );

    individual.updateFitness();

    assertEquals( 25.0,
                  problem.getGoalFt(),
                  0.0 );
    assertEquals( computeFiniteGoalFitness( 1,
                                            25.0,
                                            25.0,
                                            1,
                                            3.0 ),
                  individual.getFitness(),
                  1.0e-12 );
    assertTrue( individual.toString()
      .contains( "goalReached=true" ) );
    assertTrue( individual.toString()
      .contains( "avgSpeed=8.333333" ) );
  }

  @Test
  public void completionTieBreakDoesNotApplyBeforeGoalDistanceIsReached() {
    ScriptedBackend backend = new ScriptedBackend();
    CountingStartStateProvider provider =
            new CountingStartStateProvider(
              new RacingStartState( 10.0,
                                    -1.0,
                                    0.0,
                                    0.0 ) );
    ProblemRacing problem = new ProblemRacing( backend,
                                               new ZeroActionAdapter(),
                                               provider,
                                               1,
                                               1,
                                               2.0,
                                               1.5,
                                               2,
                                               1.0,
                                               100.0,
                                               2.0,
                                               0.001,
                                               1.0,
                                               25.0 );
    Individual individual = problem.getNewIndividual( false,
                                                      new Random( 1 ) );

    individual.updateFitness();

    assertEquals( computeFiniteGoalFitness( 1,
                                            20.0,
                                            25.0,
                                            0,
                                            2.0 ),
                  individual.getFitness(),
                  1.0e-12 );
    assertTrue( individual.toString()
      .contains( "goalReached=false" ) );
  }

  @Test
  public void completedGoalFitnessRewardsLowerCompletionTime() {
    ProblemRacing fastProblem = buildScriptedGoalSpeedProblem( 10.0 );
    ProblemRacing slowProblem = buildScriptedGoalSpeedProblem( 5.0 );
    Individual fastIndividual = fastProblem.getNewIndividual( false,
                                                              new Random( 1 ) );
    Individual slowIndividual = slowProblem.getNewIndividual( false,
                                                              new Random( 1 ) );

    fastIndividual.updateFitness();
    slowIndividual.updateFitness();

    assertTrue( fastIndividual.getFitness() > slowIndividual.getFitness() );
    assertEquals( computeFiniteGoalFitness( 1,
                                            25.0,
                                            25.0,
                                            1,
                                            3.0 ),
                  fastIndividual.getFitness(),
                  1.0e-12 );
    assertEquals( computeFiniteGoalFitness( 1,
                                            25.0,
                                            25.0,
                                            1,
                                            5.0 ),
                  slowIndividual.getFitness(),
                  1.0e-12 );
  }

  @Test
  public void rankGoalFitnessCanStayOpenAfterLapGoalDistance() {
    ProblemRacing problem = new ProblemRacing(
      new ScriptedBackend(),
      new ZeroActionAdapter(),
      new CountingStartStateProvider(
        new RacingStartState( 10.0,
                              -1.0,
                              0.0,
                              0.0 ) ),
      1,
      1,
      10.0,
      1.5,
      2,
      1.0,
      2.0,
      2.0,
      0.001,
      1.0,
      25.0,
      Double.MAX_VALUE );

    assertEquals( Double.MAX_VALUE,
                  problem.getGoalFt(),
                  0.0 );
  }

  @Test
  public void racingGenomeLogSeparatesEveryGene() {
    ProblemRacing problem = new ProblemRacing(
      new ScriptedBackend(),
      new ZeroActionAdapter(),
      new CountingStartStateProvider(
        new RacingStartState( 10.0,
                              -1.0,
                              0.0,
                              0.0 ) ),
      1,
      1,
      10.0,
      1.5,
      2 );

    assertEquals( 1,
                  problem.getDisplayModulus() );
  }

  @Test
  public void adaptDoesNotIncreaseTimeLimitAfterBestRunReachedGoal() {
    ScriptedBackend backend = new ScriptedBackend();
    CountingStartStateProvider provider =
            new CountingStartStateProvider(
              new RacingStartState( 10.0,
                                    -1.0,
                                    0.0,
                                    0.0 ) );
    ProblemRacing problem = new ProblemRacing( backend,
                                               new ZeroActionAdapter(),
                                               provider,
                                               1,
                                               1,
                                               10.0,
                                               1.5,
                                               1,
                                               1.0,
                                               1.0,
                                               2.0,
                                               0.001,
                                               1.0,
                                               25.0,
                                               Double.MAX_VALUE );
    Individual individual = problem.getNewIndividual( false,
                                                      new Random( 1 ) );

    individual.updateFitness();
    problem.adapt( individual.getFitness() );
    problem.adapt( individual.getFitness() );

    assertTrue( problem.hasBestFitnessSeenReachedGoal() );
    assertTrue( individual.toString()
      .contains( "goalReached=true" ) );
    assertEquals( 10.0,
                  problem.getCurrentTimeLimitSeconds(),
                  0.0 );
    assertEquals( 0,
                  problem.getCountOfStagnantGenerations() );
  }

  @Test
  public void finiteGoalFitnessDoesNotTreatOffTrackGoalDistanceAsValidGoal() {
    ScriptedBackend backend = new ScriptedBackend();
    CountingStartStateProvider provider =
            new CountingStartStateProvider(
              new RacingStartState( 10.0,
                                    3.0,
                                    0.0,
                                    -1.0 ) );
    ProblemRacing problem = new ProblemRacing( backend,
                                               new ZeroActionAdapter(),
                                               provider,
                                               1,
                                               1,
                                               10.0,
                                               1.5,
                                               2,
                                               1.0,
                                               0.0,
                                               2.0,
                                               0.001,
                                               1.0,
                                               25.0 );
    Individual individual = problem.getNewIndividual( false,
                                                      new Random( 1 ) );

    individual.updateFitness();

    assertTrue( individual.getFitness() < problem.getGoalFt() );
    assertTrue( individual.toString()
      .contains( "offTrack=true" ) );
    assertTrue( individual.toString()
      .contains( "goalReached=false" ) );
  }

  @Test
  public void fitnessWithCircularBackendIsFiniteForAHandCraftedAnchor() {
    SimpleCircularTrack track = new SimpleCircularTrack( "simple_circle",
                                                         0.0,
                                                         0.0,
                                                         50.0,
                                                         5.0 );
    SimpleCircularRacingBackend backend =
            new SimpleCircularRacingBackend( track,
                                             0.2,
                                             1.0,
                                             12.0,
                                             18.0,
                                             0.05,
                                             40.0 );
    ProblemRacing problem = new ProblemRacing(
      backend,
      new SimpleTargetRacingActionAdapter(),
      new CountingStartStateProvider(
        new RacingStartState( 50.0,
                              0.0,
                              0.0,
                              Math.PI / 2.0 ) ),
      1,
      1,
      1.0,
      1.5,
      2 );
    Individual individual = problem.getNewIndividual( false,
                                                      new Random( 1 ) );

    individual.getGene( 0 ).setDoubleValue(
      50.0 / backend.getPolicyHalfRangeX() );
    individual.getGene( 1 ).setDoubleValue( 0.0 );
    individual.getGene( 2 ).setDoubleValue(
      10.0 / backend.getPolicySpeedScale() );
    individual.getGene( 3 ).setDoubleValue( 0.5 );

    double fitness = problem.fitness( individual );

    assertTrue( Double.isFinite( fitness ) );
    assertTrue( fitness > 0.0 );
  }

  @Test
  public void policyInterpolatesTargetsFromAnchorsInCartesianStateSpace() {
    ObservationBackend backend = new ObservationBackend();
    CapturingActionAdapter actionAdapter = new CapturingActionAdapter();
    CountingStartStateProvider provider =
            new CountingStartStateProvider(
              new RacingStartState( 5.0,
                                    0.0,
                                    0.0,
                                    0.0 ) );
    ProblemRacing problem = new ProblemRacing( backend,
                                               actionAdapter,
                                               provider,
                                               2,
                                               1,
                                               1.0,
                                               1.5,
                                               2 );
    Individual individual = problem.getNewIndividual( false,
                                                      new Random( 1 ) );

    setAnchor( individual,
               0,
               0.0,
               0.0,
               10.0,
               0.0 );
    setAnchor( individual,
               1,
               10.0,
               0.0,
               20.0,
               Math.PI / 2.0 );

    problem.fitness( individual );

    assertTrue( actionAdapter.getLastPolicyAction() != null );
    assertEquals( 15.0,
                  actionAdapter.getLastPolicyAction().getSpeedTarget(),
                  1.0e-9 );
    assertEquals( Math.PI / 4.0,
                  actionAdapter.getLastPolicyAction().getDirectionTarget(),
                  1.0e-9 );
  }

  @Test
  public void runStartsAtInterpolatedTargetSpeed() {
    ObservationBackend backend = new ObservationBackend();
    CapturingActionAdapter actionAdapter = new CapturingActionAdapter();
    CountingStartStateProvider provider =
            new CountingStartStateProvider(
              new RacingStartState( 5.0,
                                    0.0,
                                    0.0,
                                    0.0 ) );
    ProblemRacing problem = new ProblemRacing( backend,
                                               actionAdapter,
                                               provider,
                                               1,
                                               1,
                                               1.0,
                                               1.5,
                                               2 );
    Individual individual = problem.getNewIndividual( false,
                                                      new Random( 1 ) );

    setAnchor( individual,
               0,
               5.0,
               0.0,
               0.5,
               0.0 );

    problem.fitness( individual );

    assertTrue( actionAdapter.getLastCarState() != null );
    assertEquals( 0.5,
                  actionAdapter.getLastCarState()
                    .getSpeed(),
                  1.0e-12 );
  }

  @Test
  public void policyTargetsDependOnlyOnPositionAndNotOnCurrentSpeedOrHeading() {
    ObservationBackend backend = new ObservationBackend();
    CapturingActionAdapter actionAdapter = new CapturingActionAdapter();
    CountingStartStateProvider provider =
            new CountingStartStateProvider(
              new RacingStartState( 5.0,
                                    0.0,
                                    0.0,
                                    0.0 ),
              new RacingStartState( 5.0,
                                    0.0,
                                    13.0,
                                    -2.4 ) );
    ProblemRacing problem = new ProblemRacing( backend,
                                               actionAdapter,
                                               provider,
                                               2,
                                               2,
                                               1.0,
                                               1.5,
                                               2 );
    Individual individual = problem.getNewIndividual( false,
                                                      new Random( 1 ) );

    setAnchor( individual,
               0,
               0.0,
               0.0,
               10.0,
               0.0 );
    setAnchor( individual,
               1,
               10.0,
               0.0,
               20.0,
               Math.PI / 2.0 );

    problem.fitness( individual );

    assertEquals( 2,
                  actionAdapter.getPolicyActions()
                    .size() );
    RacingPolicyAction firstAction = actionAdapter.getPolicyActions()
      .get( 0 );
    RacingPolicyAction secondAction = actionAdapter.getPolicyActions()
      .get( 1 );
    assertEquals( firstAction.getSpeedTarget(),
                  secondAction.getSpeedTarget(),
                  1.0e-9 );
    assertEquals( firstAction.getDirectionTarget(),
                  secondAction.getDirectionTarget(),
                  1.0e-9 );
  }

  @Test
  public void pureSimpleOvalEvaluatorMatchesMutableBackendReference() {
    String propertyName = "rankga.racing.simpleOvalPure";
    String previousValue = System.getProperty( propertyName );
    try {
      SimpleOvalTrack track = new SimpleOvalTrack( "simple_oval",
                                                   0.0,
                                                   0.0,
                                                   25.0,
                                                   30.0,
                                                   5.0 );
      SimpleTargetRacingActionAdapter actionAdapter =
              new SimpleTargetRacingActionAdapter();
      CountingStartStateProvider provider =
              new CountingStartStateProvider(
                new RacingStartState( -15.0,
                                      25.0,
                                      3.0,
                                      0.0 ),
                new RacingStartState( 0.0,
                                      25.0,
                                      4.0,
                                      0.2 ) );

      System.setProperty( propertyName,
                          "false" );
      ProblemRacing referenceProblem = new ProblemRacing(
        new SimpleOvalRacingBackend( track,
                                     0.2,
                                     1.0,
                                     12.0,
                                     18.0,
                                     0.05,
                                     40.0 ),
        actionAdapter,
        provider,
        3,
        2,
        2.0,
        1.5,
        2,
        1.0,
        0.0,
        2.0,
        0.001,
        1.0,
        100.0 );
      Individual referenceIndividual = referenceProblem.getNewIndividual(
        false,
        new Random( 1 ) );
      setAnchor( referenceIndividual,
                 0,
                 -15.0 / 65.0,
                 25.0 / 30.0,
                 9.0 / 40.0,
                 0.0 );
      setAnchor( referenceIndividual,
                 1,
                 0.0,
                 25.0 / 30.0,
                 10.0 / 40.0,
                 0.0 );
      setAnchor( referenceIndividual,
                 2,
                 15.0 / 65.0,
                 25.0 / 30.0,
                 9.0 / 40.0,
                 0.0 );
      double referenceFitness = referenceProblem.fitness(
        referenceIndividual );

      System.setProperty( propertyName,
                          "true" );
      ProblemRacing pureProblem = new ProblemRacing(
        new SimpleOvalRacingBackend( track,
                                     0.2,
                                     1.0,
                                     12.0,
                                     18.0,
                                     0.05,
                                     40.0 ),
        actionAdapter,
        provider,
        3,
        2,
        2.0,
        1.5,
        2,
        1.0,
        0.0,
        2.0,
        0.001,
        1.0,
        100.0 );
      Individual pureIndividual = pureProblem.getNewIndividual(
        false,
        new Random( 1 ) );
      for( int i = 0; i < referenceProblem.getGenomeLength(); i++ ) {
        pureIndividual.getGene( i )
          .setDoubleValue( referenceIndividual.getGene( i )
            .getValue() );
      }

      assertEquals( referenceFitness,
                    pureProblem.fitness( pureIndividual ),
                    1.0e-12 );
    } finally {
      if( previousValue == null ) {
        System.clearProperty( propertyName );
      } else {
        System.setProperty( propertyName,
                            previousValue );
      }
    }
  }

  private static void setAnchor( Individual individual,
                                 int anchorIndex,
                                 double x,
                                 double y,
                                 double speedTarget,
                                 double directionTarget ) {
    int offset = anchorIndex * 4;
    individual.getGene( offset ).setDoubleValue( x );
    individual.getGene( offset + 1 ).setDoubleValue( y );
    individual.getGene( offset + 2 ).setDoubleValue( speedTarget );
    individual.getGene( offset + 3 ).setDoubleValue(
      wrapToPi( directionTarget ) / Math.PI );
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

  private static double computeOpenGoalFitness( int countOfSafeRuns,
                                                double averageDistanceMeters ) {
    return countOfSafeRuns
           + 0.5 * averageDistanceMeters
             / ( 1.0 + averageDistanceMeters );
  }

  private static double computeFiniteGoalFitness( int countOfSafeRuns,
                                                  double averageDistanceMeters,
                                                  double goalDistanceMeters,
                                                  int countOfGoalRuns,
                                                  double averageGoalTimeSeconds ) {
    double distanceScore = 0.5 * Math.min( 1.0,
                                           averageDistanceMeters / goalDistanceMeters );
    double goalCountScore = 1.0e-6 * countOfGoalRuns;
    double goalTimeScore = countOfGoalRuns > 0
                           ? 1.0e-9 / ( 1.0 + averageGoalTimeSeconds )
                           : 0.0;
    return countOfSafeRuns + distanceScore + goalCountScore + goalTimeScore;
  }

  private static ProblemRacing buildScriptedGoalSpeedProblem(
    double progressMetersPerStep ) {
    return new ProblemRacing(
      new ScriptedBackend(),
      new ZeroActionAdapter(),
      new CountingStartStateProvider(
        new RacingStartState( progressMetersPerStep,
                              -1.0,
                              0.0,
                              0.0 ) ),
      1,
      1,
      10.0,
      1.5,
      2,
      1.0,
      0.0,
      2.0,
      0.001,
      1.0,
      25.0 );
  }

  private static ProblemRacing buildScriptedProblem( int countOfRuns,
                                                     double timeLimitSeconds,
                                                     double goalDistanceMeters,
                                                     RacingStartState... startStates ) {
    return new ProblemRacing(
      new ScriptedBackend(),
      new ZeroActionAdapter(),
      new CountingStartStateProvider( startStates ),
      1,
      countOfRuns,
      timeLimitSeconds,
      1.5,
      2,
      1.0,
      0.0,
      2.0,
      0.001,
      1.0,
      goalDistanceMeters );
  }

  private static final class ZeroActionAdapter
    implements RacingActionAdapter {

    @Override
    public RacingBackendAction toBackendAction( RacingPolicyAction policyAction,
                                                RacingCarState carState ) {
      return new RacingBackendAction( 0.0,
                                      0.0,
                                      0.0 );
    }
  }

  private static final class CapturingActionAdapter
    implements RacingActionAdapter {

    private RacingPolicyAction lastPolicyAction;
    private RacingCarState lastCarState;
    private final List<RacingPolicyAction> policyActions = new ArrayList<RacingPolicyAction>();

    @Override
    public RacingBackendAction toBackendAction( RacingPolicyAction policyAction,
                                                RacingCarState carState ) {
      this.lastPolicyAction = policyAction;
      this.lastCarState = carState;
      policyActions.add( policyAction );
      return new RacingBackendAction( 0.0,
                                      0.0,
                                      0.0 );
    }

    public RacingPolicyAction getLastPolicyAction() {
      return lastPolicyAction;
    }

    public RacingCarState getLastCarState() {
      return lastCarState;
    }

    public List<RacingPolicyAction> getPolicyActions() {
      return policyActions;
    }
  }

  private static final class CountingStartStateProvider
    implements RacingStartStateProvider {

    private final RacingStartState[] startStates;
    private int countOfRefreshCalls;

    private CountingStartStateProvider( RacingStartState... startStates ) {
      this.startStates = startStates;
      this.countOfRefreshCalls = 0;
    }

    @Override
    public void refreshBatch() {
      countOfRefreshCalls++;
    }

    @Override
    public RacingStartState getStartState( int runIndex ) {
      return startStates[ runIndex ];
    }

    @Override
    public int countOfStates() {
      return startStates.length;
    }

    public int getCountOfRefreshCalls() {
      return countOfRefreshCalls;
    }
  }

  private static final class ScriptedBackend
    implements RacingBackend {

    private RacingCarState currentState;
    private double progressMetersPerStep;
    private int crashAfterStep;
    private boolean offTrackOnTermination;

    @Override
    public String getTrackName() {
      return "test_track";
    }

    @Override
    public RacingStartState sampleStartState( Random random ) {
      return new RacingStartState( 1.0,
                                   -1.0,
                                   0.0,
                                   0.0 );
    }

    @Override
    public RacingCarState resetEpisode( RacingStartState startState ) {
      progressMetersPerStep = startState.getX();
      crashAfterStep = (int) Math.round( startState.getY() );
      offTrackOnTermination = startState.getHeading() < 0.0;
      currentState = new RacingCarState( 0.0,
                                         0.0,
                                         startState.getSpeed(),
                                         startState.getHeading(),
                                         0.0 );
      return currentState;
    }

    @Override
    public RacingStepResult step( RacingBackendAction backendAction ) {
      double nextTimeSeconds = currentState.getTimeSeconds() + 1.0;
      double nextProgressMeters = currentState.getX() + progressMetersPerStep;
      currentState = new RacingCarState( nextProgressMeters,
                                         0.0,
                                         progressMetersPerStep,
                                         currentState.getHeading(),
                                         nextTimeSeconds );
      RacingTerminationReason terminationReason;
      if( crashAfterStep > 0 && nextTimeSeconds >= crashAfterStep ) {
        terminationReason = offTrackOnTermination
                            ? RacingTerminationReason.OFF_TRACK
                            : RacingTerminationReason.CRASH;
      } else {
        terminationReason = RacingTerminationReason.NONE;
      }
      return new RacingStepResult( currentState,
                                   nextProgressMeters,
                                   terminationReason );
    }

    @Override
    public double measureProgress( RacingCarState carState ) {
      return carState.getX();
    }

    @Override
    public boolean isInsideTrack( double x,
                                  double y ) {
      return true;
    }

    @Override
    public double getPolicyCenterX() {
      return 0.0;
    }

    @Override
    public double getPolicyCenterY() {
      return 0.0;
    }

    @Override
    public double getPolicyHalfRangeX() {
      return 1.0;
    }

    @Override
    public double getPolicyHalfRangeY() {
      return 1.0;
    }

    @Override
    public double getPolicySpeedScale() {
      return 1.0;
    }
  }

  private static final class ObservationBackend
    implements RacingBackend {

    private RacingCarState currentState;

    @Override
    public String getTrackName() {
      return "observation_track";
    }

    @Override
    public RacingStartState sampleStartState( Random random ) {
      return new RacingStartState( 5.0,
                                   0.0,
                                   0.0,
                                   0.0 );
    }

    @Override
    public RacingCarState resetEpisode( RacingStartState startState ) {
      currentState = new RacingCarState( startState.getX(),
                                         startState.getY(),
                                         startState.getSpeed(),
                                         startState.getHeading(),
                                         0.0 );
      return currentState;
    }

    @Override
    public RacingStepResult step( RacingBackendAction backendAction ) {
      currentState = new RacingCarState( currentState.getX(),
                                         currentState.getY(),
                                         currentState.getSpeed(),
                                         currentState.getHeading(),
                                         1.0 );
      return new RacingStepResult( currentState,
                                   0.0,
                                   RacingTerminationReason.CRASH );
    }

    @Override
    public double measureProgress( RacingCarState carState ) {
      return 0.0;
    }

    @Override
    public boolean isInsideTrack( double x,
                                  double y ) {
      return true;
    }

    @Override
    public double getPolicyCenterX() {
      return 0.0;
    }

    @Override
    public double getPolicyCenterY() {
      return 0.0;
    }

    @Override
    public double getPolicyHalfRangeX() {
      return 1.0;
    }

    @Override
    public double getPolicyHalfRangeY() {
      return 1.0;
    }

    @Override
    public double getPolicySpeedScale() {
      return 1.0;
    }
  }
}
