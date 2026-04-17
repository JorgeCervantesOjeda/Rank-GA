package rankga;

/**
 * Gene — Atomic unit of an individual's genome.
 *
 * <h2>Purpose</h2>
 * A {@code Gene} encapsulates the representation and local operators at a
 * single locus:
 * <ul>
 * <li><b>Value</b>: read/write accessors for numeric state (int/double);
 * categorical genes may encode categories as integers.</li>
 * <li><b>Mutation</b>: applies a change according to a given <b>intensity</b>
 * (not probability).</li>
 * <li><b>Distance</b>: returns a scalar distance to another gene at the same
 * locus (used for diagnostics/diversity/termination checks).</li>
 * </ul>
 *
 * <h2>Mutation intensity contract</h2>
 * The {@link #mutate(double)} parameter is a unitless scalar whose meaning
 * should be
 * <b>consistent across a problem's gene types</b>. Common interpretations
 * include:
 * <ul>
 * <li><i>Reals</i>: step size or noise standard deviation (e.g., Gaussian with
 * σ = intensity).</li>
 * <li><i>Integers/categorical</i>: neighborhood radius, number of perturbation
 * attempts, or a monotone mapping to a flip probability (document the mapping
 * if used).</li>
 * </ul>
 * Implementations should be deterministic given the PRNG used at construction
 * time by the owning {@code Problem}.
 */
public interface Gene {

  // --------------------------------------------------------------------------------------------
  // Value API
  // --------------------------------------------------------------------------------------------
  /**
   * Sets the integer value for this gene.
   * <p>
   * Typical for categorical/ordinal encodings. Implementations should validate
   * bounds if applicable.</p>
   *
   * @param value integer value to set
   */
  void setIntValue( int value );

  /**
   * Sets the double value for this gene.
   * <p>
   * Typical for real-parameter optimization. Implementations may clamp to valid
   * ranges.</p>
   *
   * @param value double value to set
   */
  void setDoubleValue( double value );

  /**
   * @return the current (double) value of this gene
   * <p>
   * Categorical/integer genes may expose their internal integer as a double for
   * uniform logging/metrics.</p>
   */
  double getValue();

  // --------------------------------------------------------------------------------------------
  // Operators
  // --------------------------------------------------------------------------------------------
  /**
   * Applies mutation to this gene using the provided <b>intensity</b>.
   * <p>
   * <b>Not</b> a per-gene Bernoulli probability. If an implementation requires
   * probabilistic flips, it should internally map intensity → probability via a
   * documented monotone function.</p>
   *
   * @param intensity mutation intensity (unitless scalar)
   */
  void mutate( double intensity );

  /**
   * Distance to another gene located at the <b>same locus</b>.
   * <p>
   * Used for diversity diagnostics and to verify non-trivial improvements:
   * RankGA checks that the best individual is not identical using distance &gt;
   * 0.</p>
   *
   * @param other another gene instance to compare (must be comparable at this
   *              locus)
   *
   * @return non-negative distance (0 if identical)
   */
  double distanceTo( Gene other );

  // --------------------------------------------------------------------------------------------
  // Metadata (optional, implementation-defined)
  // --------------------------------------------------------------------------------------------
  /**
   * @return number of admissible values (for categorical/discrete genes); for
   *         continuous genes, may return 0 or -1
   */
  int getNumValues();

  /**
   * Sets the number of admissible values (categorical/discrete encodings).
   * <p>
   * Implementations may ignore this for continuous genes.</p>
   *
   * @param numValues number of possible values
   */
  void setNumValues( int numValues );

}
