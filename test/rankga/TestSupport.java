package rankga;

import java.util.Random;

/**
 * Small test-only stubs used to exercise the GA core without depending on a
 * concrete problem implementation.
 */
final class TestSupport {

  private TestSupport() {
    // Utility class.
  }

  /**
   * Single-locus discrete gene whose mutation is intentionally a no-op so the
   * tests stay deterministic.
   */
  static final class BinaryGene
    implements Gene {

    private int value;
    private int numValues = 2;

    BinaryGene() {
      this( 0 );
    }

    BinaryGene( int value ) {
      this.value = value;
    }

    BinaryGene( BinaryGene other ) {
      this.value = other.value;
      this.numValues = other.numValues;
    }

    @Override
    public void setIntValue( int value ) {
      if( value < 0 || value >= numValues ) {
        return;
      }
      this.value = value;
    }

    @Override
    public void setDoubleValue( double value ) {
      this.setIntValue( (int) Math.round( value ) );
    }

    @Override
    public double getValue() {
      return this.value;
    }

    @Override
    public void mutate( double intensity ) {
      // No-op: tests use this gene to isolate population-level behavior.
    }

    @Override
    public double distanceTo( Gene other ) {
      return this.value == (int) other.getValue()
             ? 0.0
             : 1.0;
    }

    @Override
    public int getNumValues() {
      return this.numValues;
    }

    @Override
    public void setNumValues( int numValues ) {
      if( numValues < 2 ) {
        throw new IllegalArgumentException(
          "BinaryGene domain size must be at least 2" );
      }
      this.numValues = numValues;
      if( this.value >= this.numValues ) {
        this.value = this.numValues - 1;
      }
    }

  }

  /**
   * Shared base problem for test stubs.
   */
  static abstract class BaseProblem
    implements Problem {

    private final String name;
    private final int genomeLength;

    BaseProblem( String name,
                 int genomeLength ) {
      this.name = name;
      this.genomeLength = genomeLength;
    }

    @Override
    public String getProblemName() {
      return this.name;
    }

    @Override
    public int getGenomeLength() {
      return this.genomeLength;
    }

    @Override
    public int getDisplayModulus() {
      return 1;
    }

    @Override
    public Gene getNewGene( boolean randomize,
                            Random r ) {
      return new BinaryGene( randomize && r.nextBoolean()
                             ? 1
                             : 0 );
    }

    @Override
    public Gene getNewGene( Gene gene ) {
      return new BinaryGene( (BinaryGene) gene );
    }

    @Override
    public Individual getNewIndividual( boolean randomize,
                                        Random r ) {
      return new Individual( this,
                             randomize,
                             r );
    }

    @Override
    public Individual getNewIndividual( Individual individual ) {
      return new Individual( individual );
    }

    @Override
    public double getGlobalSearchIntensity() {
      return 1.0;
    }

    @Override
    public double getLocalSearchIntensity() {
      return 0.5;
    }

  }

  /**
   * Problem whose fitness is the first gene value.
   */
  static final class ValueProblem
    extends BaseProblem {

    ValueProblem( String name ) {
      super( name,
             1 );
    }

    @Override
    public double fitness( Individual individual ) {
      return individual.getGene( 0 ).getValue();
    }

    @Override
    public double getGoalFt() {
      return 1.0;
    }

  }

  /**
   * Problem whose fitness is constant and does not depend on the genome.
   */
  static class ConstantProblem
    extends BaseProblem {

    private final double fitnessValue;
    private final double goalFt;

    ConstantProblem( String name,
                     double fitnessValue,
                     double goalFt ) {
      super( name,
             1 );
      this.fitnessValue = fitnessValue;
      this.goalFt = goalFt;
    }

    @Override
    public double fitness( Individual individual ) {
      return this.fitnessValue;
    }

    @Override
    public double getGoalFt() {
      return this.goalFt;
    }

  }

  /**
   * Constant problem that also counts adaptation calls.
   */
  static final class CountingAdaptiveProblem
    extends ConstantProblem
    implements AdaptiveProblem {

    private int adaptCalls;
    private double lastBestFitness;

    CountingAdaptiveProblem( String name,
                             double fitnessValue,
                             double goalFt ) {
      super( name,
             fitnessValue,
             goalFt );
    }

    @Override
    public void adapt( double bestFitness ) {
      this.adaptCalls++;
      this.lastBestFitness = bestFitness;
    }

    int getAdaptCalls() {
      return this.adaptCalls;
    }

    double getLastBestFitness() {
      return this.lastBestFitness;
    }

  }

}
