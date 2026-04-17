/*
 * Package: Problems
 *
 * GeneInteger — discrete gene with domain {0, 1, ..., NUM_VALUES-1}.
 *
 * Key semantics:
 * - The gene stores an integer value in [0, NUM_VALUES-1].
 * - Mutation uses a parameter p interpreted as a MUTATION PROBABILITY:
 *     with probability p, the current value is replaced by a different value
 *     chosen uniformly at random from the domain.
 *   (This is NOT a continuous “intensity”. If your GA computes an intensity per individual,
 *    convert intensity → probability before calling mutate(p), or use an adapter elsewhere.)
 * - Distance to another gene is Hamming distance on a single locus:
 *     0 if equal, 1 if different.
 *
 * Notes:
 * - setDoubleValue(...) throws UnsupportedOperationException because this gene is strictly integer.
 * - setIntValue(...) ignores out-of-range inputs (>= NUM_VALUES) instead of throwing.
 * - The RNG is injected and shared by reference so reproducibility is controlled at a higher level.
 */
package Problems;

import java.util.Random;
import rankga.Gene;

/**
 * Implementation of {@link Gene} for a discrete integer domain {0, ...,
 * NUM_VALUES-1}.
 * <p>
 * Appropriate for categorical/integer encodings. Mutation is "random
 * replacement": with probability p, the value is changed to a uniformly random
 * different value in the domain; with probability (1 - p), the value remains
 * unchanged.
 */
public class GeneInteger
  implements Gene {

  /**
   * Domain cardinality (number of admissible values). Must be >= 1.
   */
  private int NUM_VALUES;

  /**
   * Current integer value in [0, NUM_VALUES-1].
   */
  protected int value;

  /**
   * Random source used for initialization and mutation.
   */
  protected final Random r;

  /**
   * Main constructor.
   *
   * @param numValues domain cardinality (>= 1)
   * @param randomize if true, initialize uniformly in [0, numValues-1]; if
   *                  false, initialize to 0
   * @param r         PRNG for reproducibility
   */
  public GeneInteger( int numValues,
                      boolean randomize,
                      Random r ) {
    this.NUM_VALUES = numValues;
    this.r = r;
    // Initialization: if randomize is true, sample uniformly; otherwise start at 0.
    this.value = (int) Math.floor( randomize
                                   ? r.nextDouble() * NUM_VALUES
                                   : 0 );
  }

  /**
   * Copy constructor (deep-copy state, share RNG).
   *
   * @param other source instance
   */
  GeneInteger( GeneInteger other ) {
    this.value = other.value;
    this.NUM_VALUES = other.NUM_VALUES;
    this.r = other.r;
  }

  // --------------------------------------------------------------------------------------------
  // Distance / metric
  // --------------------------------------------------------------------------------------------
  /**
   * Hamming distance on a single locus: 0 if equal, 1 if different.
   *
   * @param other another gene to compare
   *
   * @return 0 if values are equal, 1 otherwise
   */
  @Override
  public double distanceTo( Gene other ) {
    return ( this.value - (int) other.getValue() != 0 )
           ? 1
           : 0;
  }

  // --------------------------------------------------------------------------------------------
  // Value and cardinality setters/getters
  // --------------------------------------------------------------------------------------------
  /**
   * Setting a double value is not supported for a strictly integer gene.
   *
   * @param _parseInt ignored
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public void setDoubleValue( double _parseInt ) {
    throw new UnsupportedOperationException( "Not supported yet." );
  }

  /**
   * Set the integer value if within range; otherwise ignore silently.
   *
   * @param newValue proposed integer value
   */
  @Override
  public void setIntValue( int newValue ) {
    if( newValue >= this.NUM_VALUES ) {
      return; // out of range: ignore
    }
    this.value = newValue;
  }

  /**
   * @return domain cardinality (number of admissible values)
   */
  @Override
  public int getNumValues() {
    return this.NUM_VALUES;
  }

  /**
   * Change the domain cardinality.
   * <p>
   * WARNING: This method does not revalidate or adjust the current value. If
   * you shrink the domain and the current value becomes out-of-range,
   * consistency must be handled externally.
   *
   * @param _numValues new domain cardinality
   */
  @Override
  public void setNumValues( int _numValues ) {
    this.NUM_VALUES = _numValues;
  }

  // --------------------------------------------------------------------------------------------
  // Mutation (by replacement probability)
  // --------------------------------------------------------------------------------------------
  /**
   * Random-replacement mutation with probability p.
   * <p>
   * With probability p, sample a new integer uniformly from {0, ...,
   * NUM_VALUES-1} that is different from the current value; with probability (1
   * - p), do nothing.
   * <p>
   * If your GA uses “intensity” rather than probability, define a mapping
   * externally (e.g., p = min(1, k * intensity) or p = 1 - exp(-k * intensity))
   * and pass p here.
   *
   * @param p mutation probability in [0, 1]
   */
  @Override
  public void mutate( double p ) {
    // Bernoulli(p): bail out if the trial fails.
    if( r.nextDouble() >= p ) {
      return;
    }
    // Sample a new value uniformly until it differs from the current one.
    int mutation;
    do {
      mutation = (int) Math.floor( r.nextDouble() * NUM_VALUES );
    } while( mutation == value );
    value = mutation;
  }

  // --------------------------------------------------------------------------------------------
  // Value access and textual representation
  // --------------------------------------------------------------------------------------------
  /**
   * @return current value as a double (for interface compatibility)
   */
  @Override
  public double getValue() {
    return value;
  }

  /**
   * Simple textual representation of the integer value. The
   * Math.log10(NUM_VALUES) call from the original code is preserved (no-op).
   */
  @Override
  public String toString() {
    return "" + value;
  }

}
