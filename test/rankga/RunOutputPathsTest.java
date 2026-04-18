package rankga;

import Problems.ProblemHeawoodRainbow;
import Problems.ProblemIC;
import Problems.ProblemKnapsack;
import Problems.ProblemNIAH;
import Problems.ProblemNK;
import Problems.ProblemPseudoachromaticIndex;
import Problems.ProblemPseudoachromaticIndexConnex;
import Problems.ProblemTS;
import Problems.ProblemTS_Jumps;
import Problems.ProblemTS_Reals;
import Problems.ProblemTS_Simple;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class RunOutputPathsTest {

  @Test
  public void familyDirectoryKeepsTheTspVariantsTogether() {
    assertEquals( Paths.get( "runs",
                             "tsp",
                             "classic" ),
                  RunOutputPaths.familyDirectory( ProblemTS.class ) );
    assertEquals( Paths.get( "runs",
                             "tsp",
                             "reals" ),
                  RunOutputPaths.familyDirectory( ProblemTS_Reals.class ) );
    assertEquals( Paths.get( "runs",
                             "tsp",
                             "simple" ),
                  RunOutputPaths.familyDirectory( ProblemTS_Simple.class ) );
    assertEquals( Paths.get( "runs",
                             "tsp",
                             "jumps" ),
                  RunOutputPaths.familyDirectory( ProblemTS_Jumps.class ) );
  }

  @Test
  public void familyDirectoryUsesStableProblemSlugs() {
    assertEquals( Paths.get( "runs",
                             "heawood-rainbow" ),
                  RunOutputPaths.familyDirectory( ProblemHeawoodRainbow.class ) );
    assertEquals( Paths.get( "runs",
                             "graph-identifying-code" ),
                  RunOutputPaths.familyDirectory( ProblemIC.class ) );
    assertEquals( Paths.get( "runs",
                             "knapsack" ),
                  RunOutputPaths.familyDirectory( ProblemKnapsack.class ) );
    assertEquals( Paths.get( "runs",
                             "pseudoacromatic-index" ),
                  RunOutputPaths.familyDirectory(
                    ProblemPseudoachromaticIndex.class ) );
    assertEquals( Paths.get( "runs",
                             "pseudoacromatic-index-connex" ),
                  RunOutputPaths.familyDirectory(
                    ProblemPseudoachromaticIndexConnex.class ) );
    assertEquals( Paths.get( "runs",
                             "niah" ),
                  RunOutputPaths.familyDirectory( ProblemNIAH.class ) );
    assertEquals( Paths.get( "runs",
                             "nk" ),
                  RunOutputPaths.familyDirectory( ProblemNK.class ) );
  }

  @Test
  public void normalizeSlugProducesLowerCaseHyphenatedNames() {
    assertEquals( "graph-minimum-identifier-code-problem",
                  RunOutputPaths.normalizeSlug(
                    "Graph Minimum Identifier Code Problem" ) );
    assertEquals( "heawood-rainbow",
                  RunOutputPaths.normalizeSlug( "HeawoodRainbow" ) );
  }

}
