# Next Steps

Status as of 2026-07-24. See `CLAUDE.md`'s "Known Issues / Audit Findings" section for the
full history of what's already been fixed and why.

## Current state

- PR [#26](https://github.com/e-Garcia/Compose-vs-Android-View-System-Performance/pull/26)
  is open against `main` with a maintenance-audit pass: fixed Gradle task graph, wired the
  MetricCapability architecture into cold-start benchmarks, corrected a mislabeled run date
  and a stale maintenance-status header, fixed a `MainActivity` base-class parity gap
  (`AppCompatActivity` → `ComponentActivity`), deduped fast-scroll gesture code, and addressed
  Copilot's review feedback on the PR itself.
- Everything in that PR was verified with `./gradlew build` (all modules/variants/tests) and
  targeted `--dry-run`/`assemble` checks. **No device or emulator was available in this
  environment**, so nothing benchmark-numeric was actually re-run.

## P0 — blocks trusting any published number

1. **Re-run the full benchmark suite on a real device (or at minimum a fresh emulator).**
   The `ComponentActivity` switch in PR #26 changes what `app-view` actually does at
   `onCreate()` — the cold-start numbers in `benchmark-results.md` and `README.md` were
   measured against the old `AppCompatActivity` version and may no longer be accurate.
   This single run would also refresh the "still emulator-only" concern for the numbers
   that already exist.
2. **Get a physical-device run.** Every published number to date (including anything from
   PR #26's era) is from an Android 16 SwiftShader (software-rendered) emulator. CLAUDE.md's
   own device requirements say emulators aren't recommended for this kind of measurement —
   the project can't make its core "View vs Compose" claim credible without at least one
   physical-device data point.

## P1 — safe, well-scoped code fixes (no device needed to implement, but verify behavior after)

3. **Tighten `catch (t: Throwable) { Assume.assumeTrue(...) }`** in
   `ComposeBenchmarks`/`ViewBenchmarks`. Right now it converts *any* exception — including
   real crashes or assertion failures — into a silent "skipped" test result. Narrow it to
   the specific metric-unavailability exceptions `measureStartupWithCapabilityReport`/
   `measureRepeated` can actually throw, or check first whether the underlying capability
   report already covers this (in which case the outer catch may be mostly dead weight now).
4. **Replace the nested-Gradle `Exec` invocation** in `runBenchmarkComposeClass`/
   `runBenchmarkViewClass` (`build.gradle.kts`) — they shell out to `bash ./gradlew ...`,
   which is exactly the pattern the comment on `assembleInstallCompose` says to avoid
   (nested Gradle daemons, fragile under CI). Replace with real task dependencies on the
   Macrobenchmark plugin's actual `connectedBenchmarkAndroidTest`-family tasks in the same
   Gradle invocation, the way `assembleInstallCompose`/`assembleInstallView` already do.

## P2 — research-quality / architecture polish

5. **Extend capability-aware metric selection to `fastScroll_*` and `SmokeBenchmark.kt`.**
   Only `coldStartup_compose()`/`coldStartup_view()` use
   `measureStartupWithCapabilityReport()` today; the fast-scroll tests still hardcode
   `listOf(FrameTimingMetric())` and `SmokeBenchmark` hardcodes its own list. Doable, but
   `measureStartupWithCapabilityReport()` is shaped around `startActivityAndWait()` only —
   it'll need generalizing to accept a custom `measureBlock` and `startupMode` first.
6. **Re-investigate the run-manifest "Operator: Hermes Agent (cron executor)" attribution.**
   PR #26 fixed the *date* on this run (see CLAUDE.md), but the automated-operator claim
   itself was never independently verified — worth understanding before citing this
   project's results anywhere external.
7. **Pin `textColor`/font family in the row-parity checklist** (`README.md`). CLAUDE.md's
   "Important Constraints" already flags that Compose's Material3 `Text` (inherits
   `onSurface` theming) and the View's plain `TextView` (no explicit `textAppearance`)
   aren't guaranteed to render identically — the checklist only covers size/height/padding.

## P3 — minor / low-impact

8. **Revisit the `Thread.sleep(150)` fixed-duration pacing** in
   `BenchmarkUtils.performFastScrollGestures()`. Simple and reproducible, but adds ~1.2s of
   dead time per fast-scroll iteration and isn't tied to actual gesture completion.
9. **Decide what to do with the untracked `REVIEW.md`** sitting in this checkout. It has some
   accurate observations but at least one confirmed-wrong recommendation (DiffUtil on a
   static, never-updated `ItemAdapter` — see CLAUDE.md's note) and an unrelated
   Copilot-reviewer HOWTO tacked onto the end. Either fold the verified parts into `CLAUDE.md`
   and delete it, or leave it alone — but don't act on its recommendations without
   independently re-checking them first, same as any other undated/unattributed doc.

## Not on this list

Nothing here proposes new benchmark *scenarios* (memory, power, thermal, network) — the
`MetricType` enum already has room for them (`POWER`, `MEMORY`, `THERMAL`, `NETWORK`), but
none of the underlying Macrobenchmark metric classes or device probes exist yet. That's a
bigger, separate feature, not a next step off the current audit.
