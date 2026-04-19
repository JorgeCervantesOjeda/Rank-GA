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


def metadata_path_for(csv_path: Path) -> Path:
    return csv_path.with_name(f"{csv_path.stem}_meta{csv_path.suffix}")


def load_metadata(csv_path: Path) -> dict[str, str]:
    metadata_path = metadata_path_for(csv_path)
    if not metadata_path.exists():
        return {}
    with metadata_path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        result: dict[str, str] = {}
        for row in reader:
            key = row.get("key", "").strip()
            if key:
                result[key] = row.get("value", "")
        return result


def strip_spreadsheet_text_prefix(value: str) -> str:
    if value.startswith("'"):
        return value[1:]
    return value

def parse_repetition(row: dict[str, str]) -> int:
    return int(row.get("repetition", "0"))


def parse_evaluations(row: dict[str, str]) -> int:
    return int(row.get("evaluations", "0"))


def parse_elapsed_ms(row: dict[str, str]) -> int:
    return int(row.get("elapsed_ms", "0"))


def parse_repetition_seed(row: dict[str, str]) -> str:
    return strip_spreadsheet_text_prefix(
        row.get("repetition_seed", row.get("seed", "unknown"))
    )


def parse_base_seed(metadata: dict[str, str], rows: list[dict[str, str]]) -> str:
    if metadata.get("base_seed"):
        return strip_spreadsheet_text_prefix(metadata["base_seed"])
    if rows:
        return strip_spreadsheet_text_prefix(rows[0].get("seed", "unknown"))
    return "unknown"


def parse_problem_name(metadata: dict[str, str],
                       row: dict[str, str],
                       fallback: str) -> str:
    value = metadata.get("problem_name", "").strip()
    if value:
        return value
    value = row.get("problem_name", "").strip()
    return value or fallback


def slugify(raw: str) -> str:
    slug = re.sub(r"([a-z0-9])([A-Z])", r"\1-\2", raw.strip())
    slug = re.sub(r"[^A-Za-z0-9]+", "-", slug)
    slug = re.sub(r"-+", "-", slug)
    slug = slug.strip("-").lower()
    return slug or "runs"


def output_stem(problem_name: str,
                base_seed: str,
                fallback: str,
                metadata: dict[str, str]) -> str:
    base = slugify(problem_name or fallback)
    run_id = strip_spreadsheet_text_prefix(metadata.get("run_id", "").strip())
    run_match = re.search(r"_seed[^_]+_(\d+)(?:_summary)?$", fallback)
    if base_seed and base_seed != "unknown":
        if run_id:
            return f"{base}_seed{base_seed}_{run_id}"
        if run_match:
            return f"{base}_seed{base_seed}_{run_match.group(1)}"
        return f"{base}_seed{base_seed}"
    return base


def ordered_rows(rows: list[dict[str, str]], metric_key: str) -> list[dict[str, str]]:
    metric_parser = parse_evaluations if metric_key == "evaluations" else parse_elapsed_ms
    return sorted(rows, key=lambda row: (metric_parser(row), parse_repetition(row)))


def write_ordered_csv(output_path: Path,
                      ordered_rows: list[dict[str, str]],
                      metric_key: str) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow([
            "order",
            "repetition",
            "repetition_seed",
            "evaluations",
            "elapsed_ms",
            "termination_reason",
            "best_fitness",
        ])
        for order, row in enumerate(ordered_rows, start=1):
            writer.writerow([
                order,
                parse_repetition(row),
                parse_repetition_seed(row),
                parse_evaluations(row),
                parse_elapsed_ms(row),
                row.get("termination_reason", ""),
                row.get("best_fitness", ""),
            ])


def plot_metric(summary_path: Path,
                output_dir: Path,
                rows: list[dict[str, str]],
                metadata: dict[str, str],
                problem_name: str,
                base_seed: str,
                metric_key: str,
                metric_label: str,
                log_scale: bool) -> Path:
    ordered = ordered_rows(rows, metric_key)
    output_dir.mkdir(parents=True, exist_ok=True)
    stem = output_stem(problem_name, base_seed, summary_path.stem, metadata)
    suffix = metric_key

    ordered_csv = output_dir / f"{stem}_{suffix}_sorted.csv"
    write_ordered_csv(ordered_csv, ordered, metric_key)

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
    ax.set_title(f"{problem_name} | seed {base_seed} | ordered by {metric_key}")
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

    metadata = load_metadata(summary_path)
    problem_name = parse_problem_name(metadata, rows[0], summary_path.stem)
    base_seed = parse_base_seed(metadata, rows)
    return [
        plot_metric(summary_path,
                    output_dir,
                    rows,
                    metadata,
                    problem_name,
                    base_seed,
                    "evaluations",
                    "Fitness evaluations until termination (log scale)",
                    True),
        plot_metric(summary_path,
                    output_dir,
                    rows,
                    metadata,
                    problem_name,
                    base_seed,
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
