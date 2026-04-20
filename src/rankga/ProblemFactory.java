package rankga;

import Problems.NeedleInHill;
import Problems.ProblemDeceptive;
import Problems.ProblemDistricts;
import Problems.ProblemHeawoodRainbow;
import Problems.ProblemHillSideSearch;
import Problems.ProblemIC;
import Problems.ProblemKnapsack;
import Problems.ProblemLeadingOnes;
import Problems.ProblemNIAH;
import Problems.ProblemNK;
import Problems.ProblemOneMax;
import Problems.ProblemPseudoachromaticIndex;
import Problems.ProblemPseudoachromaticIndexConnex;
import Problems.ProblemRastrigin;
import Problems.ProblemTaskAssignment;
import Problems.ProblemTS;
import Problems.ProblemTS_Jumps;
import Problems.ProblemTS_Reals;
import Problems.ProblemTS_Simple;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;

/**
 * ProblemFactory - explicit problem selection for RankGA.
 *
 * The factory keeps problem construction out of the GA core. It accepts a
 * normalized option map so the launcher can stay simple.
 */
public final class ProblemFactory {

  private ProblemFactory() {
  }

  /**
   * Parse command-line arguments into a normalized option map.
   * <p>
   * Keys are lowercased and stripped of hyphens/underscores. A bare token is
   * treated as the problem id if no problem has been specified yet.</p>
   *
   * @param args command-line arguments
   *
   * @return normalized options
   */
  public static Map<String, String> parseArguments( String[] args ) {
    Map<String, String> options = new LinkedHashMap<>();
    for( String arg : args ) {
      String token = arg.startsWith( "--" )
                     ? arg.substring( 2 )
                     : arg;
      int eq = token.indexOf( '=' );
      if( eq >= 0 ) {
        putOption( options,
                   token.substring( 0,
                                    eq ),
                   token.substring( eq + 1 ) );
      } else if( !options.containsKey( "problem" ) ) {
        putOption( options,
                   "problem",
                   token );
      } else {
        putOption( options,
                   token,
                   "true" );
      }
    }
    return options;
  }

  /**
   * Create a problem instance from the selected id and options.
   *
   * @param problemId problem id, e.g. {@code ts-reals}
   * @param options   normalized options
   *
   * @return a configured problem instance
   */
  public static Problem create( String problemId,
                                Map<String, String> options ) {
    long seed = readLongOption( options,
                                "seed",
                                System.currentTimeMillis() );
    return create( problemId,
                   options,
                   seed );
  }

  /**
   * Create a problem instance from the selected id, options, and explicit seed.
   *
   * @param problemId problem id, e.g. {@code one-max}
   * @param options normalized options
   * @param seed seed used for the problem constructor
   *
   * @return a configured problem instance
   */
  public static Problem create( String problemId,
                                Map<String, String> options,
                                long seed ) {
    String id = normalizeKey( problemId == null
                              ? "one-max"
                              : problemId );
    switch( id ) {
      case "tsreals":
        return new ProblemTS_Reals( new Random( seed ) );
      case "tssimple":
        return new ProblemTS_Simple( new Random( seed ) );
      case "tsjumps":
        return new ProblemTS_Jumps( new Random( seed ) );
      case "ts":
        return new ProblemTS( readIntOption( options,
                                             "n",
                                             20 ),
                              new Random( seed ) );
      case "knapsack":
        return new ProblemKnapsack( new Random( seed ) );
      case "taskassignment":
        return new ProblemTaskAssignment( new Random( seed ) );
      case "districts":
        return new ProblemDistricts();
      case "ic":
        return new ProblemIC();
      case "nk":
        return new ProblemNK( seed );
      case "onemax":
        return new ProblemOneMax( readIntOption( options,
                                                 "genomelength",
                                                 8 ) );
      case "leadingones":
        return new ProblemLeadingOnes( readIntOption( options,
                                                      "genomelength",
                                                      8 ) );
      case "rastrigin":
        return new ProblemRastrigin( readIntOption( options,
                                                     "dimensions",
                                                     10 ) );
      case "needleinhill":
      case "needle":
      case "nih":
        return new NeedleInHill( readIntOption( options,
                                                "genomelength",
                                                64 ),
                                 readIntOption( options,
                                                "plateauwidth",
                                                8 ),
                                 readIntOption( options,
                                                "hillsidewidth",
                                                8 ),
                                 readIntOption( options,
                                                "needledistance",
                                                4 ) );
      case "deceptive":
        return new ProblemDeceptive( readIntOption( options,
                                                    "genomelength",
                                                    100 ),
                                     readIntOption( options,
                                                    "basinwidth",
                                                    10 ) );
      case "hillside":
      case "hillsidesearch":
        return new ProblemHillSideSearch( readIntOption( options,
                                                          "genomesize",
                                                          100 ),
                                          readIntOption( options,
                                                         "basinwidth",
                                                         10 ),
                                          readDoubleOption( options,
                                                            "basinslope",
                                                            0.1 ),
                                          readIntOption( options,
                                                        "optimumdistance",
                                                        20 ) );
      case "niah":
        return new ProblemNIAH( readIntOption( options,
                                               "genomelength",
                                               20 ),
                                readIntOption( options,
                                               "numblocks",
                                               4 ),
                                readIntOption( options,
                                               "needlewidth",
                                               2 ) );
      case "heawood":
        return new ProblemHeawoodRainbow( readIntOption( options,
                                                         "colors",
                                                         3 ) );
      case "pseudo":
      case "pseudoachromaticindex":
        return new ProblemPseudoachromaticIndex( readIntOption( options,
                                                                 "vertices",
                                                                 22 ),
                                                 readIntOption( options,
                                                                "colors",
                                                                3 ),
                                                 (float) readDoubleOption( options,
                                                                           "weight",
                                                                           0.01 ) );
      case "pseudoconnex":
      case "pseudoachromaticconnex":
      case "pseudoachromaticindexconnex":
        return new ProblemPseudoachromaticIndexConnex();
      default:
        throw new IllegalArgumentException(
          "Unknown problem '" + problemId + "'. Available: "
          + availableProblems() );
    }
  }

  /**
   * Describe the effective parameters used to build a problem instance.
   * <p>
   * These values are written to the structured summary so each repetition
   * records the concrete problem configuration alongside the run-level
   * metadata already tracked by RankGA.</p>
   *
   * @param problemId problem id, e.g. {@code ts-reals}
   * @param options normalized options
   * @param problem configured problem instance
   *
   * @return a semicolon-separated list of effective problem parameters
   */
  public static String describeProblemParameters( String problemId,
                                                  Map<String, String> options,
                                                  Problem problem ) {
    String id = normalizeKey( problemId == null
                              ? "one-max"
                              : problemId );
    StringJoiner parameters = new StringJoiner( ";" );
    int genomeLength = problem == null
                       ? -1
                       : problem.getGenomeLength();

    switch( id ) {
      case "ts":
        addParameter( parameters,
                      "n",
                      readIntOption( options,
                                     "n",
                                     20 ) );
        break;
      case "tsreals":
      case "tssimple":
      case "tsjumps":
        if( genomeLength > 0 ) {
          addParameter( parameters,
                        "n",
                        genomeLength );
        }
        break;
      case "knapsack":
        addParameter( parameters,
                      "numItems",
                      genomeLength > 0
                      ? genomeLength
                      : 250 );
        addParameter( parameters,
                      "weightCapacity",
                      6000 );
        addParameter( parameters,
                      "volumeCapacity",
                      5000 );
        break;
      case "taskassignment":
        addParameter( parameters,
                      "numTasks",
                      genomeLength > 0
                      ? genomeLength
                      : 100 );
        addParameter( parameters,
                      "numAgents",
                      20 );
        break;
      case "districts":
        addParameter( parameters,
                      "numSections",
                      genomeLength > 0
                      ? genomeLength
                      : 3135 );
        addParameter( parameters,
                      "numDistricts",
                      19 );
        break;
      case "nk":
        addParameter( parameters,
                      "N",
                      genomeLength > 0
                      ? genomeLength
                      : 100 );
        addParameter( parameters,
                      "K",
                      3 );
        break;
      case "onemax":
      case "leadingones":
        addParameter( parameters,
                      "genomeLength",
                      readIntOption( options,
                                     "genomelength",
                                     8 ) );
        break;
      case "rastrigin":
        addParameter( parameters,
                      "dimensions",
                      readIntOption( options,
                                     "dimensions",
                                     10 ) );
        break;
      case "needleinhill":
      case "needle":
      case "nih":
        addParameter( parameters,
                      "genomeLength",
                      readIntOption( options,
                                     "genomelength",
                                     64 ) );
        addParameter( parameters,
                      "plateauWidth",
                      readIntOption( options,
                                     "plateauwidth",
                                     8 ) );
        addParameter( parameters,
                      "hillsideWidth",
                      readIntOption( options,
                                     "hillsidewidth",
                                     8 ) );
        addParameter( parameters,
                      "needleDistance",
                      readIntOption( options,
                                     "needledistance",
                                     4 ) );
        break;
      case "deceptive":
        addParameter( parameters,
                      "genomeLength",
                      readIntOption( options,
                                     "genomelength",
                                     100 ) );
        addParameter( parameters,
                      "basinWidth",
                      readIntOption( options,
                                     "basinwidth",
                                     10 ) );
        break;
      case "hillside":
      case "hillsidesearch":
        addParameter( parameters,
                      "genomeSize",
                      readIntOption( options,
                                     "genomesize",
                                     100 ) );
        addParameter( parameters,
                      "basinWidth",
                      readIntOption( options,
                                     "basinwidth",
                                     10 ) );
        addParameter( parameters,
                      "basinSlope",
                      readDoubleOption( options,
                                        "basinslope",
                                        0.1 ) );
        addParameter( parameters,
                      "optimumDistance",
                      readIntOption( options,
                                     "optimumdistance",
                                     20 ) );
        break;
      case "niah":
        addParameter( parameters,
                      "genomeLength",
                      readIntOption( options,
                                     "genomelength",
                                     20 ) );
        addParameter( parameters,
                      "numBlocks",
                      readIntOption( options,
                                     "numblocks",
                                     4 ) );
        addParameter( parameters,
                      "needleWidth",
                      readIntOption( options,
                                     "needlewidth",
                                     2 ) );
        break;
      case "heawood":
        addParameter( parameters,
                      "colors",
                      readIntOption( options,
                                     "colors",
                                     3 ) );
        break;
      case "pseudo":
      case "pseudoachromaticindex":
        addParameter( parameters,
                      "vertices",
                      readIntOption( options,
                                     "vertices",
                                     22 ) );
        addParameter( parameters,
                      "colors",
                      readIntOption( options,
                                     "colors",
                                     3 ) );
        addParameter( parameters,
                      "weight",
                      readDoubleOption( options,
                                        "weight",
                                        0.01 ) );
        break;
      case "pseudoconnex":
      case "pseudoachromaticconnex":
      case "pseudoachromaticindexconnex":
        addParameter( parameters,
                      "numVertices",
                      22 );
        addParameter( parameters,
                      "initialColors",
                      2 );
        addParameter( parameters,
                      "weightPairs",
                      0.01 );
        addParameter( parameters,
                      "weightColors",
                      1.0 );
        addParameter( parameters,
                      "weightStd",
                      0.000001 );
        addParameter( parameters,
                      "weightAvg",
                      0.000000001 );
        break;
      default:
        break;
    }

    return parameters.toString();
  }

  /**
   * Read an integer option with a default.
   */
  public static int readIntOption( Map<String, String> options,
                                   String key,
                                   int defaultValue ) {
    String value = options.get( normalizeKey( key ) );
    return value == null
           ? defaultValue
           : Integer.parseInt( value );
  }

  /**
   * Read a double option with a default.
   */
  public static double readDoubleOption( Map<String, String> options,
                                         String key,
                                         double defaultValue ) {
    String value = options.get( normalizeKey( key ) );
    return value == null
           ? defaultValue
           : Double.parseDouble( value );
  }

  /**
   * Read a long option with a default.
   */
  public static long readLongOption( Map<String, String> options,
                                     String key,
                                     long defaultValue ) {
    String value = options.get( normalizeKey( key ) );
    return value == null
           ? defaultValue
           : Long.parseLong( value );
  }

  /**
   * Available problem ids for diagnostics.
   */
  public static String availableProblems() {
    return "ts-reals, ts-simple, ts-jumps, ts, knapsack, task-assignment, "
           + "districts, ic, nk, one-max, leading-ones, rastrigin, needle, deceptive, "
           + "hillside, niah, heawood, pseudo, pseudo-connex";
  }

  private static void putOption( Map<String, String> options,
                                 String key,
                                 String value ) {
    options.put( normalizeKey( key ),
                 value );
  }

  private static void addParameter( StringJoiner parameters,
                                    String key,
                                    Object value ) {
    parameters.add( key + "=" + String.valueOf( value ) );
  }

  private static String normalizeKey( String key ) {
    return key == null
           ? ""
           : key.toLowerCase( Locale.ROOT )
             .replace( "-",
                       "" )
             .replace( "_",
                       "" )
             .trim();
  }

}
