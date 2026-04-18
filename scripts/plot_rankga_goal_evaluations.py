#!/usr/bin/env python3
"""Plot RankGA repetitions ordered by evaluations and elapsed time."""

from __future__ import annotations

import argparse
import csv
import glob
import re
from pathlib import Path
from typing import Iterable

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt


def repo_root() -> Path:
    return Path(__file__).resolve().parents[1]


def default_output_dir() -> Path:
    return repo_root() / "figures"


def load_summary_rows(summary_path: Path) -> list[dict[str, str]]:
    with summary_path.open("r", encoding="utf-8", newline="") as handle:
      reader = csv.DictReader(handle)
      return list(reader)

def parse_repetition(row: dict[str, str]) -> int:
    return int(row.get("repetition", "0"))


def parse_evaluations(row: dict[str, str]) -> int:
    return int(row.get("evaluations", "0"))


def parse_elapsed_ms(row: dict[str, str]) -> int:
    return int(row.get("elapsed_ms", "0"))


def parse_seed(row: dict[str, str]) -> str:
    return row.get("seed", "unknown")


def parse_problem_name(row: dict[str, str], fallback: str) -> str:
    value = row.get("problem_name", "").strip()
    return value or fallback


def slugify(raw: str) -> str:
    slug = re.sub(r"([a-z0-9])([A-Z])", r"\1-\2", raw.strip())
    slug = re.sub(r"[^A-Za-z0-9]+", "-", slug)
    slug = re.sub(r"-+", "-", slug)
    slug = slug.strip("-").lower()
    return slug or "runs"


def output_stem(problem_name: str,
                seed: str,
                fallback: str) -> str:
    base = slugify(problem_name or fallback)
    run_match = re.search(r"_seed[^_]+_(\d+)(?:_summary)?$", fallback)
    if seed and seed != "unknown":
        if run_match:
            return f"{base}_seed{seed}_{run_match.group(1)}"
        return f"{base}_seed{seed}"
    return base


def ordered_rows(rows: list[dict[str, str]], metric_key: str) -> list[dict[str, str]]:
    metric_parser = parse_evaluations if metric_key == "evaluations" else parse_elapsed_ms
    return sorted(rows, key=lambda row: (metric_parser(row), parse_repetition(row)))


def write_ordered_csv(output_path: Path,
                      ordered_rows: list[dict[str, str]],
                      source_name: str,
                      metric_key: str) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow([
            "order",
            "repetition",
            metric_key,
            "evaluations",
            "elapsed_ms",
            "seed",
            "problem_name",
            "problem_parameters",
            "termination_reason",
            "best_fitness",
            "goal_fitness",
            "source_summary",
        ])
        for order, row in enumerate(ordered_rows, start=1):
            writer.writerow([
                order,
                parse_repetition(row),
                row.get(metric_key, ""),
                parse_evaluations(row),
                parse_elapsed_ms(row),
                parse_seed(row),
                parse_problem_name(row, source_name),
                row.get("problem_parameters", ""),
                row.get("termination_reason", ""),
                row.get("best_fitness", ""),
                row.get("goal_fitness", ""),
                source_name,
            ])


def plot_metric(summary_path: Path,
                output_dir: Path,
                rows: list[dict[str, str]],
                problem_name: str,
                seed: str,
                metric_key: str,
                metric_label: str,
                log_scale: bool) -> Path:
    ordered = ordered_rows(rows, metric_key)
    output_dir.mkdir(parents=True, exist_ok=True)
    stem = output_stem(problem_name, seed, summary_path.stem)
    suffix = metric_key

    ordered_csv = output_dir / f"{stem}_{suffix}_sorted.csv"
    write_ordered_csv(ordered_csv, ordered, summary_path.name, metric_key)

    fig, ax = plt.subplots(figsize=(9, 5.5))
    x_values = list(range(1, len(ordered) + 1))
    metric_parser = parse_evaluations if metric_key == "evaluations" else parse_elapsed_ms
    y_values = [metric_parser(row) for row in ordered]

    ax.plot(x_values,
            y_values,
            marker="o",
            linewidth=1.8,
            color="#1f77b4")

    if log_scale:
        ax.set_yscale("log")
    ax.set_xlabel("Repetition order (smallest to largest)")
    ax.set_ylabel(metric_label)
    ax.set_title(f"{problem_name} | seed {seed} | ordered by {metric_key}")
    ax.grid(True, which="both", linestyle=":", linewidth=0.7, alpha=0.8)

    if len(ordered) <= 24:
        for x_pos, row in enumerate(ordered, start=1):
            ax.annotate(f"rep {parse_repetition(row)}",
                        (x_pos, metric_parser(row)),
                        textcoords="offset points",
                        xytext=(0, 6),
                        ha="center",
                        fontsize=8)

    fig.tight_layout()

    png_path = output_dir / f"{stem}_{suffix}.png"
    pdf_path = output_dir / f"{stem}_{suffix}.pdf"
    fig.savefig(png_path, dpi=200)
    fig.savefig(pdf_path)
    plt.close(fig)

    print(f"[ok] {png_path}")
    print(f"[ok] {pdf_path}")
    print(f"[ok] {ordered_csv}")
    return png_path


def plot_summary(summary_path: Path, output_dir: Path) -> list[Path]:
    rows = load_summary_rows(summary_path)
    if not rows:
        raise ValueError(f"{summary_path} has no rows")

    problem_name = parse_problem_name(rows[0], summary_path.stem)
    seed = parse_seed(rows[0])
    return [
        plot_metric(summary_path,
                    output_dir,
                    rows,
                    problem_name,
                    seed,
                    "evaluations",
                    "Fitness evaluations until termination (log scale)",
                    True),
        plot_metric(summary_path,
                    output_dir,
                    rows,
                    problem_name,
                    seed,
                    "elapsed_ms",
                    "Elapsed time until termination (ms, log scale)",
                    True),
    ]


def iter_summary_files(inputs: Iterable[str]) -> list[Path]:
    result: list[Path] = []
    for raw in inputs:
        path = Path(raw)
        if path.is_dir():
            result.extend(sorted(path.rglob("*_summary.csv")))
        elif path.is_file():
            result.append(path)
        else:
            matches = sorted(glob.glob(raw))
            result.extend([Path(match) for match in matches if Path(match).is_file()])
    deduped: list[Path] = []
    seen = set()
    for path in result:
        resolved = path.resolve()
        if resolved in seen:
            continue
        seen.add(resolved)
        deduped.append(path)
    return deduped


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Plot RankGA repetitions ordered by evaluations and elapsed time."
    )
    parser.add_argument(
        "inputs",
        nargs="+",
        help="Summary CSV files, directories, or glob patterns."
    )
    parser.add_argument(
        "--output-dir",
        default=str(default_output_dir()),
        help="Directory where the figure and sorted CSV will be written."
    )
    args = parser.parse_args()

    summary_files = iter_summary_files(args.inputs)
    if not summary_files:
        raise SystemExit("No summary CSV files found.")

    output_dir = Path(args.output_dir)
    plotted = 0
    for summary_path in summary_files:
        plotted += len(plot_summary(summary_path, output_dir))

    if plotted == 0:
        raise SystemExit("No summary CSV rows were plotted.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
