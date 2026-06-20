# Benchmark Results Artifact Policy

This directory is the tracked index for benchmark evidence. It is not a dump
folder for every generated Macrobenchmark artifact.

## What is tracked here

- Curated result summaries, such as small JSON, CSV, Markdown, or manifest files
  that are intentionally reviewed and suitable for version control.
- A per-run manifest that records the source location for raw artifacts when the
  raw files are too large or too transient to commit.
- Methodology notes needed to connect README result values to a reproducible run.

## What stays out of Git

- Generated Android build outputs under
  `benchmark/build/outputs/connected_android_test_additional_output/`.
- HTML reports under `benchmark/build/reports/androidTests/connected/`.
- Large perfetto traces (`*.perfetto-trace`) unless a future task explicitly
  approves committing a small, curated sample.
- Device-local files that still need to be pulled from
  `/sdcard/Android/media/dev.egarcia.andperf.benchmark/additional_test_output`.

## Required manifest fields for a verified run

Before publishing README numbers, add a manifest or summary in this directory
with at least:

- Run date and operator.
- Device model, Android/API version, and device serial handling policy.
- Exact Gradle command(s), benchmark class/method filters, and target package.
- Thermal, animation, network, and battery assumptions.
- Raw artifact retention location for benchmark JSON, HTML reports, and perfetto
  traces.
- Curated Compose and View values used in documentation, with units and sample
  counts.

README result values should cite one of these tracked summaries/manifests so
future reviewers can trace every published number back to retained evidence.