# RankGA

RankGA is a rank-based genetic algorithm implemented in Java.
The repository separates the GA core from the problem encodings so that each
problem can be run, tested, and compared independently.

## Repository Layout

- `src/`: GA engine and problem implementations.
- `test/`: JUnit tests for the core contracts.
- `data/`: input files and tabular assets used by some problems.
- `figures/`: generated plots and images kept for reference.
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
ant run -Dapplication.args="--problem=heawood --colors=3"
ant run -Dapplication.args="--problem=ts-reals --population=20 --repetitions=10"
```

If you prefer to run it directly after compilation, use the generated classes
or JAR on the classpath and pass the same arguments.

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

## License

The repository-authored code and text are licensed under the MIT License unless
a file states otherwise.
Third-party PDFs and data files in `references/` and `data/` keep their
original terms or attribution.
