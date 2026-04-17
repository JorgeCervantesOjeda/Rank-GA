package rankga;

import Problems.NeedleInHill;
import Problems.ProblemDeceptive;
import Problems.ProblemDistricts;
import Problems.ProblemHeawoodRainbow;
import Problems.ProblemHillSideSearch;
import Problems.ProblemIC;
import Problems.ProblemKnapsack;
import Problems.ProblemNIAH;
import Problems.ProblemNK;
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
    String id = normalizeKey( problemId == null
                              ? "ts-reals"
                              : problemId );
    switch( id ) {
      case "tsreals":
        return new ProblemTS_Reals();
      case "tssimple":
        return new ProblemTS_Simple();
      case "tsjumps":
        return new ProblemTS_Jumps();
      case "ts":
        return new ProblemTS( readIntOption( options,
                                             "n",
                                             20 ) );
      case "knapsack":
        return new ProblemKnapsack();
      case "taskassignment":
        return new ProblemTaskAssignment();
      case "districts":
        return new ProblemDistricts();
      case "ic":
        return new ProblemIC();
      case "nk":
        return new ProblemNK();
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
   * Available problem ids for diagnostics.
   */
  public static String availableProblems() {
    return "ts-reals, ts-simple, ts-jumps, ts, knapsack, task-assignment, "
           + "districts, ic, nk, rastrigin, needle, deceptive, hillside, "
           + "niah, heawood, pseudo, pseudo-connex";
  }

  private static void putOption( Map<String, String> options,
                                 String key,
                                 String value ) {
    options.put( normalizeKey( key ),
                 value );
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
