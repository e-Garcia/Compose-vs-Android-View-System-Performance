# Repository Guidelines

## Project Structure & Module Organization
- `app-compose/` hosts the Jetpack Compose side, and `app-view/` mirrors it with XML + RecyclerView for apples-to-apples benchmarks.
- `shared/` holds reusable models, fake repositories, and assets consumed by both apps; `benchmark/` houses the Macrobenchmark suites and produces reports under `benchmark/build/`.
- Root Gradle scripts (`build.gradle.kts`, `settings.gradle.kts`, `gradlew*`, etc.) live beside generated artifacts in `build/`.

## Build, Test, and Development Commands
- Build release/benchmark APKs: `./gradlew :app-compose:assembleRelease :app-view:assembleRelease :benchmark:assembleBenchmark`.
- Run Macrobenchmarks (pick the target class and package to measure). Example class-targeted commands:
  - Compose cold-start (single test):
    `./gradlew :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=dev.egarcia.andperf.benchmark.ComposeBenchmarks#coldStartup_compose -Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=dev.egarcia.andperf.compose --info --stacktrace`
  - View cold-start (single test):
    `./gradlew :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=dev.egarcia.andperf.benchmark.ViewBenchmarks#coldStartup_view -Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=dev.egarcia.andperf.view --info --stacktrace`

- New convenience tasks (recommended) — these assemble/install the target app and set the instrumentation arguments for you:
  - `./gradlew runBenchmarkComposeClass`  # assembles & installs :app-compose and runs the Compose benchmark class
  - `./gradlew runBenchmarkViewClass`     # assembles & installs :app-view and runs the View benchmark class

- Install the benchmarking trio before recording results: `./gradlew :app-compose:installBenchmark :app-view:installBenchmark :benchmark:installBenchmark`.
- Pre-PR checks: `./gradlew lint` and `./gradlew test` (for Android Lint/JVM test feedback); run `./gradlew clean` when cached artifacts cause nondeterministic results.

## Coding Style & Naming Conventions
- Kotlin and Kotlin DSL dominate—use 4-space indentation, descriptive `val`/`fun`, `CamelCase` for classes, and `camelCase` for members.
- Mirror package hierarchies with directory structure (e.g., `dev.egarcia.andperf.compose` vs. `.view` under `src/main/java` or `kotlin`), keeping resources per module in their `res/` tree.
- Benchmark classes end with `Benchmarks`, and `@Test` methods use `metric_app` suffixes (e.g., `coldStartup_compose`) so the app under test is obvious.
- Keep Compose entry points in `app-compose/src/main/java/...` and XML layouts in `app-view/src/main/res/layout/`; match function names to screen concepts such as `ComposeListScreen`.

## Testing Guidelines
- Macrobenchmark instrumentation (AndroidX Macrobenchmark) must target a single connected device. Prefer class-based targeting (see examples above) and pass `serial` when multiple endpoints exist.
- Tests now skip (JUnit Assume) when the expected target package is not installed; this avoids hard failures when running the wrong class against a device that doesn't have the corresponding app installed.
- Use the HTML report in `benchmark/build/reports/androidTests/connected/benchmark/index.html` or the per-device pages to verify metrics after each run.
- Collect JSON/CSV outputs from `benchmark/build/outputs/connected_android_test_additional_output/benchmark/connected/<DEVICE_LABEL>/` and mention any attached data files in the PR narrative.

## Commit & Pull Request Guidelines
- Commit messages follow `type(scope): description` with ticket references like `ANDPERF-4` (see history such as `feat(ANDPERF-4): add Compose and View cold-start benchmark tests`).
- PR descriptions should summarize the change, link the relevant ticket, describe benchmark impacts (results, thresholds, device serials), and note any configuration tweaks (e.g., `gradle.properties` serial overrides).
- When your change touches benchmarking or instrumentation, rerun the relevant Macrobenchmark command and state the exact flags/serials used so reviewers can reproduce.
