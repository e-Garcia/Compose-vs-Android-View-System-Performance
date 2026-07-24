# Benchmark Results — Compose vs Android View Performance

> **Device:** sdk_gphone64_x86_64 (Android 16, API 36, Emulator)
> **CPU:** 4 cores @ 2GHz, 2GB RAM
> **Date:** 2026-07-23
> **Test harness:** AndroidX Benchmark Macrobenchmark (Jitpack)
> **Note:** Running on an emulator is not representative of real device performance. Results here are for development iteration — physical device benchmarks are required for production-grade conclusions.

---

## Setup

| Parameter | Value |
|-----------|-------|
| Target SDK | Android 16 (API 36) |
| Device | sdk_gphone64_x86_64 (x86_64 emulator) |
| UI Frameworks | Jetpack Compose (latest) vs Android Views (RecyclerView) |
| Benchmark library | androidx.benchmark:benchmark-macro-junit4 |
| Iterations | 3 (cold-start), 5 (fast-scroll) |
| Startup mode | COLD (process killed before each run) |
| Emulator | Generic AVD (Android 16, x86_64) |

### Test Matrix

| Benchmark | UI Framework | Startup | Scroll Items | Metrics |
|-----------|-------------|---------|-------------|---------|
| coldStartup | Compose (LazyColumn) | Cold | 1000 scrollable items | StartupTiming, FrameTiming |
| coldStartup | Android View (RecyclerView) | Cold | 1000 scrollable items | StartupTiming, FrameTiming |
| fastScroll | Compose (LazyColumn) | WARM | 1000 scrollable items | FrameTiming |
| fastScroll | Android View (RecyclerView) | WARM | 1000 scrollable items | FrameTiming |
| Smoke | Compose (LazyColumn) | Cold | 1000 scrollable items | StartupTiming (smoke) |

---

## Cold Start Comparison (Fixed)

### ⚠️ IMPORTANT: FrameTimingMetric Fix

**Before fix:** `ViewBenchmarks.coldStartup_view()` used **only** `StartupTimingMetric` (no frame timing).
**After fix:** Both frameworks now measure **`StartupTimingMetric` + `FrameTimingMetric`** for an apples-to-apples comparison.

---

### Cold Start: timeToInitialDisplayMs (ms)

| Benchmark | min | **median** | max |
|-----------|------|-----------|------|
| **View (RecyclerView)** | 302.8 | **380.0** | 426.7 |
| **Compose (LazyColumn)** | 499.2 | **669.9** | 1097.8 |
| **Smoke (Compose)** | 560.9 | 985.1 | 1196.7 |

> ✅ **View is ~35% faster** on cold-start median (380ms vs 670ms) on this emulator run.

---

### Cold Start: Frame Duration (P50 / P90 / P95 / P99) — CPU

| Benchmark | P50 | P90 | P95 | P99 |
|-----------|------|------|------|------|
| **View (RecyclerView)** | 198.3 | 207.6 | 208.8 | 209.7 |
| **Compose (LazyColumn)** | 222.5 | 337.6 | 352.0 | 363.6 |
| **Smoke (Compose)** | 108.2 | 282.5 | 294.7 | 304.5 |

> ✅ **View has lower P90/P95 frame durations** during cold start. Compose shows higher tail latencies (P90 = 337ms vs View's 208ms).

---

### Cold Start: Frame Overrun (P50 / P90 / P95 / P99) — CPU

| Benchmark | P50 | P90 | P95 | P99 |
|-----------|------|------|------|------|
| **View (RecyclerView)** | 253.1 | 270.2 | 272.3 | 274.0 |
| **Compose (LazyColumn)** | 256.6 | 329.9 | 344.6 | 356.4 |
| **Smoke (Compose)** | 147.7 | 373.8 | 400.7 | 422.3 |

> ✅ **View has lower tail frame overrun** during cold start (P50 = 253ms vs Compose's 257ms — but with lower P90/P95).

---

## Fast Scroll (Warm Start) — 500 Items

| Benchmark | Frames | Frame P50 (ms) | Frame P90 (ms) | Frame P95 (ms) | Frame P99 (ms) |
|-----------|--------|---------------|---------------|---------------|---------------|
| **Compose (LazyColumn)** | 481–492 | 7.5 | 17.6 | 22.7 | 40.6 |
| **View (RecyclerView)** | 467–483 | 6.9 | 18.8 | 22.6 | 42.5 |
| | | | | | |

> 📌 **Close call in fast scroll!** View edges out on P50 (6.9ms vs 7.5ms). P90/P95 are essentially identical. Both perform excellently at scroll — no meaningful differentiator.

---

## Smoke Test (Compose Only)

| Metric | min | median | max |
|--------|------|--------|------|
| Time to Initial Display | 560.9 | 985.1 | 1196.7 |
| Frame P50 (ms) | — | 108.2 | — |
| Frame Overrun P50 (ms) | — | 147.7 | — |

---

## Summary

| Scenario | View (RecyclerView) | Compose (LazyColumn) | Verdict |
|----------|--------------------|---------------------|---------|
| **Cold Start (TTI)** | **~380ms** | ~670ms | ✅ **View ~35% faster** |
| **Cold Start (P90 Frame)** | **~208ms** | ~338ms | ✅ **View smoother during cold** |
| **Fast Scroll (P50)** | 6.9ms | 7.5ms | 📌 ~Equal (View marginally better) |
| **Fast Scroll (P95)** | 22.6ms | 22.7ms | 📌 ~Identical |

---

## Changelog

### 2026-07-23 — FrameTimingMetric Fix (Issue #1)

**File modified:** `benchmark/src/main/java/dev/egarcia/andperf/benchmark/ViewBenchmarks.kt`

**Before:**
```kotlin
metrics = listOf(StartupTimingMetric()),
```

**After:**
```kotlin
metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
```

**Impact:**
- View benchmarks now include frame timing during cold start, matching Compose.
- Previous results comparing **only** time-to-initial-display were incomplete — they showed View was faster, but couldn't prove *why* (frame delivery).
- With frame metrics enabled: View shows both faster TTI **and** better P90/P95 frame durations during cold start, confirming the speed advantage.

**Emulator verification:** All 5 tests passed on a generic API 36 emulator (Android 16, x86_64).

---

## TODO / Next Steps

1. [ ] Run on **physical devices** (emulator results may not generalize to hardware)
2. [ ] Increase iterations from 3 → 10+ for cold-start stability
3. [ ] Add image-thumbnail scrolling benchmark (both frameworks)
4. [ ] Add JVM unit tests estimating LazyColumn virtualization vs RecyclerView pool (ground-truth baseline)
5. [ ] Implement capability-based metric routing (wiring `sanitizedMetricsForBenchmark()` into actual benchmark classes)
6. [ ] Switch View's MainActivity to `ComponentActivity` (parity with Compose)
7. [ ] Add power, thermal, memory, and network benchmarks (capability model already designed)
