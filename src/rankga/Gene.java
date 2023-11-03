package rankga;

/**
 * An interface representing a gene, the basic unit of an individual's genome.
 */
public interface Gene {

  /**
   * Set the integer value of this gene.
   *
   * @param _parseInt The integer value to set.
   */
  public void setIntValue( int _parseInt );

  /**
   * Mutate this gene with a given mutation probability.
   *
   * @param p The mutation probability.
   */
  public void mutate( double p );

  /**
   * Get the integer value of this gene.
   *
   * @return The integer value of the gene.
   */
  public int getIntValue();

  /**
   * Get the double value of this gene.
   *
   * @return The double value of the gene.
   */
  public double getDoubleValue();

  /**
   * Multiply the double value of this gene by a given factor.
   *
   * @param _d The factor to multiply by.
   */
  public void multiplyDoubleValue( double _d );

  /**
   * Calculate the distance between this gene and another gene.
   *
   * @param other The other gene to calculate distance to.
   *
   * @return The distance between this gene and the other gene.
   */
  public double distanceTo( Gene other );

  /**
   * Set the number of possible values for this gene (e.g., for categorical
   * genes).
   *
   * @param _numValues The number of possible values.
   */
  public void setNumValues( int _numValues );

}
