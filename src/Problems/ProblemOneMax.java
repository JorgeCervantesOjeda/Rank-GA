package Problems;

import java.util.Random;
import rankga.Gene;
import rankga.Individual;
import rankga.Problem;

/**
 * Canonical OneMax benchmark over a binary string.
 *
 * The objective is to maximize the number of ones. This class exists mainly as
 * a minimal, reproducible example problem for the repository and the launcher.
 */
public class ProblemOneMax implements Problem {

  private final int genomeLength;
  private final Random copyRandom;

  public ProblemOneMax() {
    this( 8,
          987654321L );
  }

  public ProblemOneMax( int genomeLength ) {
    this( genomeLength,
          987654321L );
  }

  public ProblemOneMax( int genomeLength,
                        long copySeed ) {
    if( genomeLength < 1 ) {
      throw new IllegalArgumentException(
        "OneMax genome length must be at least 1." );
    }
    this.genomeLength = genomeLength;
    // Gene copies must not re-seed the RNG identically per locus, or mutation
    // becomes synchronized across clones and the benchmark stops being
    // representative of the GA behavior.
    this.copyRandom = new Random( copySeed );
  }

  @Override
  public double fitness( Individual individual ) {
    double sum = 0.0;
    for( int i = 0; i < genomeLength; i++ ) {
      sum += individual.getGene( i ).getValue();
    }
    return sum;
  }

  @Override
  public String getProblemName() {
    return "one_max_" + genomeLength;
  }

  @Override
  public int getGenomeLength() {
    return genomeLength;
  }

  @Override
  public Gene getNewGene( boolean randomize,
                          Random r ) {
    return new GeneInteger( 2,
                            randomize,
                            r );
  }

  @Override
  public Gene getNewGene( Gene gene ) {
    GeneInteger copy = new GeneInteger( 2,
                                        false,
                                        copyRandom );
    copy.setIntValue( (int) gene.getValue() );
    return copy;
  }

  @Override
  public double getGoalFt() {
    return genomeLength;
  }

  @Override
  public int getDisplayModulus() {
    return 4;
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
  public double getLocalSearchIntensity() {
    return 1.0 / genomeLength;
  }

  @Override
  public double getGlobalSearchIntensity() {
    return 0.5;
  }
}
