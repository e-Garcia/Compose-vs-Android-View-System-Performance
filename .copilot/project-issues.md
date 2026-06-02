awesome—here’s a clean set of bite-size GitHub issues you can drop straight into your repo. They’re ordered for execution, include dependencies, and each one has crisp acceptance criteria.

---

# 1) Architecture: Define Metric Capability Model & Reasons

**Description**
Introduce a canonical model for metric types and availability reasons (e.g., OS_TOO_OLD, HW_UNSUPPORTED, NO_DATA_COLLECTED, RUNTIME_ERROR).

**Tasks**

* Add `MetricType` enum (Startup, FrameTiming, Power, Memory, …).
* Add `AvailabilityReason` enum + human-readable messages.
* Define `MetricCapability` data class (requested, supported, reason?, details?).
* Define `DeviceInfo` data class (model, manufacturer, sdkInt, abi, build).

**Acceptance Criteria**

* Model compiles and is unit-tested.
* Reasons map to friendly messages.
* JSON serialization supported (Kotlinx or Jackson).

**Labels**: tech-debt, arch
**Depends on**: —

---

# 2) Capability Detection: OS & Device Probes

**Description**
Implement pre-run probes that determine if each `MetricType` is supported on the current device/OS *before* running benchmarks.

**Tasks**

* Implement OS version checks (e.g., FrameTiming ≥ API 23).
* Add device feature checks (power stats availability, perfetto/atrace presence if relevant).
* Add configurable “known issue” heuristics (e.g., View + frame timing during cold start).
* Return `MetricCapability` results per metric with reason on failure.

**Acceptance Criteria**

* Running the probe returns a capability map for all defined metrics.
* Unit tests cover each check and a few fake device profiles.

**Labels**: enhancement
**Depends on**: #1

---

# 3) Capability Detection: Lightweight Runtime Probe (Optional Step)

**Description**
Add a fast “dry-run” measurement attempt (where safe) to detect `NO_DATA_COLLECTED` and classify failures more accurately.

**Tasks**

* Implement a minimal no-op activity launch to attempt metric collection (guarded by timeouts).
* Map exceptions/timeouts to `AvailabilityReason`.
* Allow opt-out via build flag to avoid overhead.

**Acceptance Criteria**

* Dry-run can classify `NO_DATA_COLLECTED` vs other reasons.
* Overhead documented and measured (< ~1–2s per device).

**Labels**: enhancement, perf
**Depends on**: #2

---

# 4) Configuration Builder: Safe Benchmark Config API

**Description**
Create a fluent builder that takes requested metrics and returns a validated/sanitized configuration + metadata.

**Tasks**

* `BenchmarkConfigBuilder(requestedMetrics)` -> `(sanitizedMetrics, capabilityReport)`.
* Preserve “requested vs available” metadata.
* Provide fallback strategies (e.g., drop `FrameTiming` if unsupported).
* Kotlin DSL for readability.

**Acceptance Criteria**

* Builder filters unsupported metrics deterministically.
* Unit tests: requested ⊇ available; reasons retained.

**Labels**: feature
**Depends on**: #2 (and #3 if used)

---

# 5) Base Test Class: Common Setup & Safe Execution

**Description**
Provide `BaseBenchmarkTest` that all benchmarks extend.

**Tasks**

* Package under test validation (installed? correct version?).
* Invoke capability detection + configuration builder.
* Execute macrobenchmark with sanitized metrics.
* Standardized try/skip semantics (skip rather than fail).
* Hook points for logging/reporting.

**Acceptance Criteria**

* Sample test can extend base and run with no boilerplate.
* Unsupported metrics are skipped; suite does not fail CI.

**Labels**: feature, testing
**Depends on**: #4

---

# 6) Reporting Layer: Logcat Reporter

**Description**
Emit human-readable summaries to Logcat during execution.

**Tasks**

* Pretty printer that lists available/skipped metrics with reasons.
* Include test name, package, device info.
* Mirror the example format in the spec.

**Acceptance Criteria**

* Logs match the “After Refactor” example (or better).
* Validated in emulator + one physical device.

**Labels**: DX, observability
**Depends on**: #5

---

# 7) Reporting Layer: JSON Metadata Writer

**Description**
Write structured JSON files per benchmark run for reproducibility.

**Tasks**

* Define JSON schema (testName, device, summary, requested vs available, reasons, timestamps, build SHA).
* Write to deterministic path (e.g., `/sdcard/Android/data/<pkg>/benchmark/availability/` or test artifacts dir).
* Ensure file I/O safe on CI devices.

**Acceptance Criteria**

* Files match schema; validated against example payload.
* Multiple runs don’t overwrite unless intended (timestamped).

**Labels**: DX, observability
**Depends on**: #5

---

# 8) Reporting Layer: Markdown Summary (Nice-to-have)

**Description**
Generate optional `.md` summaries for stakeholders/docs.

**Tasks**

* Convert capability map to a short MD table.
* Include quick counts (requested/available/unavailable).
* Link to JSON artifact location.

**Acceptance Criteria**

* `.md` generated alongside JSON when flag enabled.

**Labels**: docs, nice-to-have
**Depends on**: #7

---

# 9) Refactor Tests: coldStartup_compose → New Architecture

**Description**
Port `coldStartup_compose` to extend `BaseBenchmarkTest` and use the builder.

**Tasks**

* Remove try/catch in test.
* Request metrics via builder (include FrameTiming).
* Verify logs + JSON generated.

**Acceptance Criteria**

* Test method shrinks to ~3–5 lines.
* CI path passes; skipped metrics don’t fail job.

**Labels**: refactor, testing
**Depends on**: #5–#7

---

# 10) Refactor Tests: coldStartup_view → New Architecture

**Description**
Port `coldStartup_view` and *remove* manual metric exclusions.

**Tasks**

* Request same metrics as compose test (no hardcoded removal).
* Confirm capability filtering handles View case.
* Validate reporting.

**Acceptance Criteria**

* No hardcoded exclusions remain.
* Logs/JSON show reasoned skip for FrameTiming (if applicable).

**Labels**: refactor, testing
**Depends on**: #5–#7

---

# 11) CI Integration: Artifacts & Skipped-Not-Failed Semantics

**Description**
Ensure CI collects JSON/MD artifacts and treats skipped metrics as success.

**Tasks**

* Update Gradle tasks to upload artifacts.
* Configure CI (GitHub Actions/Circle/etc.) to archive `/benchmark/availability/**`.
* Add JUnit output mapping for “skipped” vs “failed.”

**Acceptance Criteria**

* CI job green when only metrics are skipped.
* Artifacts visible per build.

**Labels**: ci, infra
**Depends on**: #7, #9, #10

---

# 12) Documentation: Architecture & Usage Guide

**Description**
Add developer docs for the 4-layer architecture and how to add new benchmarks/metrics.

**Tasks**

* `docs/benchmark-capabilities.md` with diagrams, examples.
* “How to add a metric” and “How to add a test” quickstarts.
* Troubleshooting table mapping reasons → actions.

**Acceptance Criteria**

* New dev can create a 3-line test following docs.
* Linked from README.

**Labels**: docs
**Depends on**: #5–#7

---

# 13) Comparison Report Across Devices (Nice-to-have)

**Description**
Aggregate multiple JSON runs into a cross-device availability matrix.

**Tasks**

* Small CLI/Gradle task to read JSON artifacts and build a table (device × metric → available/reason).
* Output MD + CSV.

**Acceptance Criteria**

* Table enables quick fleet capability inspection.

**Labels**: analytics, nice-to-have
**Depends on**: #7, #8

---

# 14) Telemetry Schema Stability & Versioning

**Description**
Version the JSON schema to allow future evolution without breaking downstream tools.

**Tasks**

* Add `schemaVersion` to JSON.
* Create changelog in `docs/telemetry-schema.md`.
* Add validator test.

**Acceptance Criteria**

* Consumers can gate on `schemaVersion`.
* Validator test prevents accidental breaking changes.

**Labels**: DX, reliability
**Depends on**: #7

---

# 15) Guardrails: Unit & Integration Tests for Edge Cases

**Description**
Comprehensive tests to prevent regressions in detection, filtering, and reporting.

**Tasks**

* Fake devices: old SDK, no frame data, HW unsupported.
* Simulate runtime error → classify as `RUNTIME_ERROR`.
* Golden tests for JSON payload & log output.

**Acceptance Criteria**

* Coverage includes all `AvailabilityReason` cases.
* Failing classifications block the build.

**Labels**: testing
**Depends on**: #2–#7

---

## Suggested Milestones / Grouping

* **Milestone 1 (Foundations):** #1–#5
* **Milestone 2 (Reporting + Refactors):** #6–#10, #14–#15
* **Milestone 3 (CI + Nice-to-haves):** #11, #8, #13, #12 (can straddle)

## Definition of Done (Overall)

* Test methods reduced to ~3–5 lines (#9, #10).
* 100% runs produce availability JSON (#7).
* CI has zero failures solely due to metric incompatibilities (#11).
* Docs explain architecture and how to extend it (#12).

If you want, I can also format each issue in your team’s GitHub template (with checklists/assignees/labels) or generate a JSON/YAML import for bulk creation.
