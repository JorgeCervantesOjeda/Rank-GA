package rankga;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Utility helpers for organizing run outputs under {@code runs/}.
 */
public final class RunOutputPaths {

  private static final String RUNS_ROOT = "runs";

  private RunOutputPaths() {
  }

  /**
   * Create or reuse the family directory for a problem instance.
   *
   * @param problem the active problem
   *
   * @return the directory that should hold its outputs
   */
  public static Path ensureFamilyDirectory( Problem problem ) {
    return ensureFamilyDirectory( problem.getClass() );
  }

  /**
   * Create or reuse the family directory for a problem class.
   *
   * @param problemClass problem implementation class
   *
   * @return the directory that should hold its outputs
   */
  public static Path ensureFamilyDirectory( Class<?> problemClass ) {
    Path directory = familyDirectory( problemClass );
    try {
      Files.createDirectories( directory );
    } catch( IOException e ) {
      throw new IllegalStateException( "Unable to create run output directory: "
                                       + directory,
                                       e );
    }
    return directory;
  }

  /**
   * Build a stable output directory for a problem family.
   *
   * The directory name is derived from the class name and normalized to a
   * filesystem-friendly slug.
   *
   * @param problemClass problem implementation class
   *
   * @return the corresponding path under {@code runs/}
   */
  public static Path familyDirectory( Class<?> problemClass ) {
    String simpleName = problemClass == null
                        ? ""
                        : problemClass.getSimpleName();

    switch( simpleName ) {
      case "ProblemTS":
        return Paths.get( RUNS_ROOT,
                          "tsp",
                          "classic" );
      case "ProblemTS_Reals":
        return Paths.get( RUNS_ROOT,
                          "tsp",
                          "reals" );
      case "ProblemTS_Simple":
        return Paths.get( RUNS_ROOT,
                          "tsp",
                          "simple" );
      case "ProblemTS_Jumps":
        return Paths.get( RUNS_ROOT,
                          "tsp",
                          "jumps" );
      case "ProblemIC":
        return Paths.get( RUNS_ROOT,
                          "graph-identifying-code" );
      case "ProblemHeawoodRainbow":
        return Paths.get( RUNS_ROOT,
                          "heawood-rainbow" );
      case "ProblemKnapsack":
        return Paths.get( RUNS_ROOT,
                          "knapsack" );
      case "ProblemNIAH":
        return Paths.get( RUNS_ROOT,
                          "niah" );
      case "ProblemNK":
        return Paths.get( RUNS_ROOT,
                          "nk" );
      case "ProblemOneMax":
        return Paths.get( RUNS_ROOT,
                          "one-max" );
      case "ProblemDeceptive":
        return Paths.get( RUNS_ROOT,
                          "deceptive" );
      case "ProblemHillSideSearch":
        return Paths.get( RUNS_ROOT,
                          "hillside-search" );
      case "NeedleInHill":
        return Paths.get( RUNS_ROOT,
                          "needle-in-hill" );
      case "ProblemPseudoachromaticIndex":
        return Paths.get( RUNS_ROOT,
                          "pseudoacromatic-index" );
      case "ProblemPseudoachromaticIndexConnex":
        return Paths.get( RUNS_ROOT,
                          "pseudoacromatic-index-connex" );
      case "ProblemRastrigin":
        return Paths.get( RUNS_ROOT,
                          "rastrigin" );
      case "ProblemTaskAssignment":
        return Paths.get( RUNS_ROOT,
                          "task-assignment" );
      case "ProblemDistricts":
        return Paths.get( RUNS_ROOT,
                          "districts" );
      default:
        break;
    }

    return Paths.get( RUNS_ROOT,
                      normalizeSlug( simpleName ) );
  }

  /**
   * Normalize a string for safe directory names.
   *
   * @param raw raw name
   *
   * @return a compact slug with only letters, digits, dots, underscores and
   *         hyphens
   */
  public static String normalizeSlug( String raw ) {
    if( raw == null ) {
      return "runs";
    }

    String slug = raw.trim()
      .replaceAll( "([a-z0-9])([A-Z])",
                   "$1-$2" )
      .replaceAll( "[^A-Za-z0-9]+",
                   "-" )
      .replaceAll( "-+",
                   "-" )
      .replaceAll( "^-|-$",
                   "" )
      .toLowerCase( Locale.ROOT );

    return slug.isEmpty()
           ? "runs"
           : slug;
  }

}
