# RankGA

RankGA is a rank-based genetic algorithm implemented in Java.
The repository separates the GA core from the problem encodings so that each
problem can be run, tested, and compared independently.

## Repository Layout

- `src/`: GA engine and problem implementations.
- `test/`: JUnit tests for the core contracts.
- `data/`: input files and tabular assets used by some problems.
- `figures/`: generated plots and images kept for reference.
- `runs/`: archived text outputs from algorithm runs, grouped by problem family,
  plus structured CSV summaries for each run.
- `references/`: papers and other reference PDFs.
- `scripts/`: helper scripts used for experiments and plotting.
- `edge_colorings/`: additional edge-coloring tooling.

## Requirements

- JDK 8 or newer.
- Ant or NetBeans.

Ant is useful for the standard project lifecycle, but it is not required to run
the algorithm if you use the provided launch scripts.

## Quick Start

### Windows

Run the test suite:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-tests.ps1
```

Run the default problem (`one-max`, 8 bits):

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-rankga.ps1
```

Run a custom experiment:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-rankga.ps1 --problem=one-max --genome-length=100 --population=20 --repetitions=100 --seed=1234
```

You can also use the cmd wrapper:

```cmd
scripts\run-rankga.cmd --problem=heawood --colors=3
```

### Linux / macOS

If Ant is available:

```bash
ant clean test
ant run -Dapplication.args="--problem=one-max --genome-length=8 --population=20 --repetitions=100 --seed=1234"
```

## Build And Test

From the project root:

```bash
ant clean test
```

If you use NetBeans, the standard Clean, Build, and Test actions work with the
included Ant project files.

The repository also includes a self-contained PowerShell runner that uses the
bundled JUnit jar and is useful as a Windows fallback:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-tests.ps1
```

That script compiles `src/` and `test/` and runs the full JUnit suite using
the bundled `lib/junit-4.7.jar`.

## Run

The main entry point is `rankga.RankGA`.

### Recommended Windows Launcher

Use:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-rankga.ps1 --problem=one-max --genome-length=8 --population=20 --repetitions=100 --seed=1234
```

This launcher compiles `src/` and then runs `rankga.RankGA` directly with
`java -cp build\classes`.

Do not rely on `ant run -Dapplication.args=...` from PowerShell in this
environment. PowerShell may pass those arguments incorrectly to `ant.bat`,
causing RankGA to launch the default problem instead of the requested one.

Examples:

```bash
ant run -Dapplication.args="--help"
ant run -Dapplication.args=""
ant run -Dapplication.args="--problem=one-max --genome-length=8 --population=20 --repetitions=100 --seed=1234"
ant run -Dapplication.args="--problem=heawood --colors=3"
ant run -Dapplication.args="--problem=ts-reals --population=20 --repetitions=10"
ant run -Dapplication.args="--problem=one-max --genome-length=8 --population=20 --repetitions=100 --seed=1234 --patience-ms=60000 --incumbent-update=neutral --patience-reset=movement"
```

If you prefer to run it directly after compilation, use the generated classes
or JAR on the classpath and pass the same arguments.

If no `--problem` is provided, the launcher defaults to `one-max` with
`genome-length=8`.

Each run writes the legacy `.txt` traces under `runs/<family>/`, a structured
`*_summary.csv` with one row per repetition, and a companion
`*_summary_meta.csv` with the run-level metadata shared by all repetitions.

The launcher also accepts:

- `--patience-ms=<milliseconds>`
- `--incumbent-update=strict|neutral`
- `--patience-reset=fitness|movement`

`strict` keeps the incumbent fixed unless fitness improves strictly. `neutral`
allows the incumbent to move to a different genotype with the same fitness.
`fitness` resets patience only on strict fitness improvement. `movement`
resets patience whenever the incumbent changes according to the selected
incumbent policy.

### Default Parameters

#### Global launcher defaults

| Parameter | Default |
| --- | --- |
| `--problem` | `one-max` |
| `--seed` | current system time |
| `--population` | `20` |
| `--repetitions` | `10` |
| `--patience-ms` | `60000` |
| `--incumbent-update` | `strict` |
| `--patience-reset` | `fitness` |

#### Problem-specific defaults

| Problem | Defaults |
| --- | --- |
| `one-max` | `genome-length=8` |
| `ts` | `n=20` |
| `ts-reals` | no CLI size parameter exposed; instance size comes from the problem data |
| `ts-simple` | no CLI parameter exposed |
| `ts-jumps` | no CLI parameter exposed |
| `knapsack` | `numItems=250`, `weightCapacity=6000`, `volumeCapacity=5000` |
| `task-assignment` | `numTasks=100`, `numAgents=20` |
| `districts` | `numSections=3135`, `numDistricts=19` |
| `nk` | `N=100`, `K=3` |
| `rastrigin` | `dimensions=10` |
| `needle` | `genome-length=64`, `plateau-width=8`, `hillside-width=8`, `needle-distance=4` |
| `deceptive` | `genome-length=100`, `basin-width=10` |
| `hillside` | `genome-size=100`, `basin-width=10`, `basin-slope=0.1`, `optimum-distance=20` |
| `niah` | `genome-length=20`, `num-blocks=4`, `needle-width=2` |
| `heawood` | `colors=3` |
| `pseudo` | `vertices=22`, `colors=3`, `weight=0.01` |
| `pseudo-connex` | `numVertices=22`, `initialColors=2`, `weightPairs=0.01`, `weightColors=1.0`, `weightStd=0.000001`, `weightAvg=0.000000001` |

### Reproducible Example: OneMax (8 bits)

This is the smallest complete example currently documented in the repository:

```bash
ant run -Dapplication.args="--problem=one-max --genome-length=8 --population=20 --repetitions=100 --seed=1234"
```

Observed outputs from the documented run:

- legacy traces under `runs/one-max/`
- structured summary:
  `runs/one-max/one_max_8_seed1234_1776480547372_summary.csv`
- two figures generated automatically in `figures/`:
  - `one-max-8_seed1234_1776480547372_evaluations.png`
  - `one-max-8_seed1234_1776480547372_elapsed_ms.png`

Observed result for that run:

- `100` repetitions
- `100` reached the goal fitness
- `0` terminated by patience
- evaluation counts ranged from `20` to `380`

The summary contains one row per repetition with these key fields:

- `repetition`
- `repetition_seed`
- `evaluations`
- `best_fitness`
- `elapsed_ms`
- `termination_reason`

The companion `*_summary_meta.csv` contains the repeated run metadata:

- `problem_name`
- `problem_parameters`
- `base_seed`
- `run_id`
- `population_size`
- `repetitions`
- `goal_fitness`
- `patience_ms`
- `incumbent_update_policy`
- `patience_reset_policy`

## Plot Ordered Repetitions

To generate the ordered plots for all repetitions, run:

```bash
python scripts/plot_rankga_goal_evaluations.py runs/tsp/reals
```

The script writes two figures and two sorted CSV files to `figures/`, naming
them after the problem and seed when available:

- one ordered by `evaluations` with a logarithmic y-axis
- one ordered by `elapsed_ms` with a logarithmic y-axis

All repetitions are included in a single ordering; the script no longer
separates runs that reached the goal from those that did not. The sorted CSVs
keep only repetition-varying data; repeated metadata stays in the
`*_summary_meta.csv` file produced by the run.

## Data Files

Some problems expect files under `data/`:

- `data/qatar194.tsp.txt`
- `data/Datos.csv`
- `data/Colindancias.csv`
- `data/ejemplo.csv`

These paths are resolved relative to the repository root.

## Tests

The current test set covers:

- `ProblemFactory` parsing and problem selection.
- `GeneInteger` domain and mutation behavior.
- `Population` selection behavior.
- `RankGA` dispatch of `adapt()` for adaptive problems.

Primary test command:

```bash
ant test
```

Windows fallback:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-tests.ps1
```

## Citation And Archival

The repository includes both `CITATION.cff` and `.zenodo.json` metadata files.
GitHub uses `CITATION.cff` for citation guidance, while Zenodo uses
`.zenodo.json` when archiving a GitHub release.

Until a Zenodo DOI is minted for a specific release, cite the corresponding
GitHub release URL. Once Zenodo archival is enabled for the repository, each
new GitHub release can be archived and cited via its Zenodo DOI.

The release archive intended for citation excludes supplementary materials that
are not part of the minimal software package, such as `references/`,
`runs/`, generated `figures/`, local NetBeans private files, and selected data
analysis spreadsheets. The working repository may still keep those materials
for local research use.

## License

The repository-authored code and text are licensed under the MIT License unless
a file states otherwise.
Third-party PDFs and data files in `references/` and `data/` keep their
original terms or attribution.
