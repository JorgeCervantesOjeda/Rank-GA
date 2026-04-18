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

## Build And Test

From the project root:

```bash
ant clean test
```

If you use NetBeans, the standard Clean, Build, and Test actions work with the
included Ant project files.

## Run

The main entry point is `rankga.RankGA`.

Examples:

```bash
ant run -Dapplication.args="--help"
ant run -Dapplication.args="--problem=one-max --genome-length=8 --population=20 --repetitions=100 --seed=1234"
ant run -Dapplication.args="--problem=heawood --colors=3"
ant run -Dapplication.args="--problem=ts-reals --population=20 --repetitions=10"
```

If you prefer to run it directly after compilation, use the generated classes
or JAR on the classpath and pass the same arguments.

Each run writes the legacy `.txt` traces under `runs/<family>/` and also a
structured `*_summary.csv` file with seed, repetition, evaluations, best
fitness, elapsed time, termination reason, and a `problem_parameters` column
with the effective problem-specific settings used in that run.

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
  - `one-max-8_seed1234_evaluations.png`
  - `one-max-8_seed1234_elapsed_ms.png`

Observed result for that run:

- `100` repetitions
- `100` reached the goal fitness
- `0` terminated by patience
- evaluation counts ranged from `20` to `380`

The summary contains one row per repetition with these key fields:

- `seed`
- `repetition`
- `evaluations`
- `best_fitness`
- `goal_fitness`
- `elapsed_ms`
- `termination_reason`
- `problem_parameters`

For the documented OneMax example, `problem_parameters` is
`genomeLength=8`.

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
preserve the `problem_parameters` column from the summary.

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

## Publication Roadmap

The working plan for a serious journal submission is in
[`PLAN_PUBLICACION_REVISTA.md`](PLAN_PUBLICACION_REVISTA.md).

## License

The repository-authored code and text are licensed under the MIT License unless
a file states otherwise.
Third-party PDFs and data files in `references/` and `data/` keep their
original terms or attribution.
