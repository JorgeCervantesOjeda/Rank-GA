package Problems;

import java.util.Random;
import rankga.Gene;
import rankga.Individual;
import rankga.Problem;

/**
 * Canonical LeadingOnes benchmark over a binary string.
 *
 * The objective is to maximize the number of consecutive leading ones from the
 * first locus onward. Once the first zero appears, remaining loci do not
 * contribute to fitness.
 */
public class ProblemLeadingOnes implements Problem {

  private final int genomeLength;
  private final Random copyRandom;

  public ProblemLeadingOnes() {
    this( 8,
          246813579L );
  }

  public ProblemLeadingOnes( int genomeLength ) {
    this( genomeLength,
          246813579L );
  }

  public ProblemLeadingOnes( int genomeLength,
                             long copySeed ) {
    if( genomeLength < 1 ) {
      throw new IllegalArgumentException(
        "LeadingOnes genome length must be at least 1." );
    }
    this.genomeLength = genomeLength;
    // Keep gene copies independent across loci and clones.
    this.copyRandom = new Random( copySeed );
  }

  @Override
  public double fitness( Individual individual ) {
    int prefixLength = 0;
    while( prefixLength < genomeLength
           && individual.getGene( prefixLength ).getValue() > 0.0 ) {
      prefixLength++;
    }
    return prefixLength;
  }

  @Override
  public String getProblemName() {
    return "leading_ones_" + genomeLength;
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
