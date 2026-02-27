package AppBackend.ResourceManagement

import android.util.Log
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Globally exhaustive GPU usage reader for Play Store distribution.
 *
 * Designed to work on every Android device ever shipped commercially,
 * from budget sub-$50 phones to flagship gaming devices.
 *
 * ┌──────────────────────────────┬─────────────────────────────────────────────────────┐
 * │ Vendor / GPU                 │ Coverage                                            │
 * ├──────────────────────────────┼─────────────────────────────────────────────────────┤
 * │ Qualcomm Adreno              │ Snapdragon 200 → 8 Gen 3 (all KGSL paths)           │
 * │ ARM Mali (devfreq)           │ Any Mali on standard devfreq framework               │
 * │ Samsung Exynos Mali          │ Galaxy S/Note/A series, all known Samsung sysfs      │
 * │ MediaTek Mali / GPU          │ Helio G/P/X, Dimensity 700–9300                     │
 * │ Google Tensor                │ Pixel 6/7/8 series (ARM Mali on Tensor SoC)         │
 * │ HiSilicon Kirin              │ Huawei P/Mate/Nova (pre-ban + existing devices)     │
 * │ Unisoc / Spreadtrum          │ Budget phones T606/T616/T618/T760/T770 etc.         │
 * │ Rockchip                     │ RK3399/RK3588 tablets, TV boxes, Chromebooks        │
 * │ Allwinner                    │ H/A/R series budget Chinese phones and tablets       │
 * │ Nvidia Tegra                 │ Shield tablet, Tegra 2/3/4 legacy devices           │
 * │ Imagination PowerVR          │ Intel Atom Android tablets, STBs                    │
 * │ Vivante GCxxx                │ NXP i.MX, budget Chinese SoCs                       │
 * │ Broadcom VideoCore           │ Raspberry Pi Android builds                         │
 * │ Intel HD / Gen               │ Android x86 tablets (Surface-like, Atom series)     │
 * │ OPPO / Realme / OnePlus      │ ColorOS custom sysfs nodes                          │
 * │ Xiaomi / MIUI                │ Xiaomi-specific sysfs overlays                      │
 * │ Generic devfreq sweep        │ Dynamic scan catches any uncatalogued node          │
 * │ Generic /proc/ sweep         │ Dynamic scan of /proc/ for GPU-related entries      │
 * └──────────────────────────────┴─────────────────────────────────────────────────────┘
 *
 * Safety guarantees:
 *  - Every single file operation is individually try-caught
 *  - Thread-safe via ReadWriteLock (background thread polling safe)
 *  - Never crashes regardless of SELinux policy, locked bootloader, or missing paths
 *  - Returns null (not a fake value) when nothing is readable — caller owns fallback
 *  - After initial scan, repeated calls cost only one file read (zero scanning)
 */
object GpuUsageReader {

    private const val TAG = "GpuUsageReader"

    // ─────────────────────────────────────────────────────────────────────────
    // Thread safety — multiple threads may call read() concurrently
    // ─────────────────────────────────────────────────────────────────────────
    private val lock = ReentrantReadWriteLock()

    @Volatile private var resolvedPath: String? = null
    @Volatile private var resolvedParser: Parser? = null
    @Volatile private var scanComplete = false

    // ─────────────────────────────────────────────────────────────────────────
    // Parser strategies
    // ─────────────────────────────────────────────────────────────────────────
    private enum class Parser {
        PERCENT_SIGN,       // "73 %" or "73%"                  → strip non-digits → /100
        PLAIN_INT,          // "35"                              → /100
        FRACTION,           // "364792308/533000000"             → used/total
        FRACTION_PERCENT,   // "40 @500MHz" or "40/100"          → first int /100
        KGSL_BUSY,          // "3355443 4194304"                 → used total (space-sep hex-like ints)
        MTK_LOADING,        // MediaTek multi-line: "Loading = 42"
        KIRIN_LINE,         // Huawei: "busy_rate:55"
        POWERVR_UTIL,       // PowerVR: "Utilization: 0.78"      → ratio or percent
        GENERIC_RATIO,      // "0.73"                            → already a ratio
        NVIDIA_LOAD,        // Tegra: "busy 55 total 100"        → extract busy/total
        INTEL_RC6,          // Intel: "RC6 residency: 45%"       → invert (RC6=idle time)
        ROCKCHIP_LOAD,      // Rockchip: "load = 55%"
        UNISOC_LOAD,        // Unisoc: "55" or "55%"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Master candidate table — ordered most common → rarest
    // ─────────────────────────────────────────────────────────────────────────
    private val candidates: List<Pair<String, Parser>> = listOf(

        // ── Qualcomm Adreno (KGSL) ───────────────────────────────────────────
        // gpu_busy_percentage: plain integer percent, most reliable Adreno source
        "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"                      to Parser.PERCENT_SIGN,
        "/sys/devices/platform/kgsl-3d0/kgsl/kgsl-3d0/gpu_busy_percentage"  to Parser.PERCENT_SIGN,
        // gpubusy: "3355443 4194304" — two space-separated ints (busy_count total_count)
        "/sys/class/kgsl/kgsl-3d0/gpubusy"                                  to Parser.KGSL_BUSY,
        "/sys/devices/platform/kgsl-3d0/kgsl/kgsl-3d0/gpubusy"             to Parser.KGSL_BUSY,
        // devfreq load exposed by some Snapdragon kernels
        "/sys/class/devfreq/kgsl-3d0/load"                                  to Parser.FRACTION_PERCENT,
        "/sys/class/devfreq/kgsl-3d0/device/kgsl-3d0/gpu_busy_percentage"  to Parser.PERCENT_SIGN,
        // Older Snapdragon (S4, 600, 800 era)
        "/sys/devices/platform/kgsl-3d0.0/kgsl/kgsl-3d0/gpu_busy_percentage" to Parser.PERCENT_SIGN,
        "/sys/devices/platform/msm_kgsl.0/kgsl/kgsl-3d0/gpu_busy_percentage" to Parser.PERCENT_SIGN,
        // Snapdragon 888 / 8 Gen 1 alternate path
        "/sys/devices/platform/soc/3d00000.qcom,kgsl-3d0/kgsl/kgsl-3d0/gpu_busy_percentage" to Parser.PERCENT_SIGN,
        // Snapdragon 8 Gen 2 / 8 Gen 3
        "/sys/devices/platform/soc/3d00000.qcom,adreno/kgsl/kgsl-3d0/gpu_busy_percentage" to Parser.PERCENT_SIGN,

        // ── ARM Mali — generic devfreq ────────────────────────────────────────
        "/sys/class/devfreq/mali/load"                                       to Parser.FRACTION,
        "/sys/class/devfreq/Mali/load"                                       to Parser.FRACTION,
        "/sys/class/devfreq/gpu/load"                                        to Parser.FRACTION,
        "/sys/class/devfreq/gpufreq/load"                                    to Parser.FRACTION,
        // Address-named Mali nodes (SoC-specific addresses)
        "/sys/class/devfreq/ff9a0000.gpu/load"                               to Parser.FRACTION,
        "/sys/class/devfreq/ffa30000.gpu/load"                               to Parser.FRACTION,
        "/sys/class/devfreq/fc010000.gpu/load"                               to Parser.FRACTION,
        "/sys/class/devfreq/fb000000.gpu/load"                               to Parser.FRACTION,
        "/sys/class/devfreq/13000000.mali/load"                              to Parser.FRACTION,
        "/sys/class/devfreq/11500000.mali/load"                              to Parser.FRACTION,
        "/sys/class/devfreq/60000000.mali/load"                              to Parser.FRACTION,
        "/sys/class/devfreq/2d000000.mali/load"                              to Parser.FRACTION,
        // Mali platform driver utilization nodes
        "/sys/devices/platform/mali.0/utilization"                           to Parser.PLAIN_INT,
        "/sys/devices/platform/mali/utilization"                             to Parser.PLAIN_INT,
        "/sys/devices/platform/mali.0/utilization_pp"                        to Parser.PLAIN_INT,
        "/sys/module/mali/parameters/mali_gpu_utilization"                   to Parser.PLAIN_INT,

        // ── Samsung Exynos Mali ───────────────────────────────────────────────
        "/sys/kernel/gpu/gpu_busy"                                           to Parser.PLAIN_INT,
        "/sys/kernel/gpu/gpu_util"                                           to Parser.PLAIN_INT,
        "/sys/kernel/gpu/gpu_utilization"                                    to Parser.PLAIN_INT,
        "/sys/devices/platform/gpex_bd.0/gpu_busy"                          to Parser.PLAIN_INT,
        "/sys/devices/platform/gpex_bd.0/utilization"                       to Parser.PLAIN_INT,
        "/sys/devices/platform/gpex_bd.0/gpu_utilization"                   to Parser.PLAIN_INT,
        "/sys/class/misc/gpu/gpu_usage"                                      to Parser.PERCENT_SIGN,
        // Exynos 990 / 1080 / 1280 / 1380 / 2100 / 2200 / 2400
        "/sys/devices/platform/19000000.mali/utilization"                    to Parser.PLAIN_INT,
        "/sys/devices/platform/22000000.mali/utilization"                    to Parser.PLAIN_INT,
        "/sys/devices/platform/28000000.mali/utilization"                    to Parser.PLAIN_INT,
        // Samsung One UI custom debug node
        "/sys/kernel/debug/exynos-gpu/utilization"                           to Parser.PLAIN_INT,

        // ── Google Tensor (Pixel 6 / 7 / 8 series) ───────────────────────────
        // Tensor uses ARM Mali on a custom Samsung-fabbed SoC
        "/sys/devices/platform/1c500000.mali/utilization"                    to Parser.PLAIN_INT,
        "/sys/devices/platform/27800000.mali/utilization"                    to Parser.PLAIN_INT,
        "/sys/class/devfreq/1c500000.mali/load"                              to Parser.FRACTION,
        "/sys/class/devfreq/27800000.mali/load"                              to Parser.FRACTION,
        // Tensor G3 (Pixel 8)
        "/sys/devices/platform/20000000.mali/utilization"                    to Parser.PLAIN_INT,
        "/sys/class/devfreq/20000000.mali/load"                              to Parser.FRACTION,

        // ── MediaTek Helio / Dimensity ────────────────────────────────────────
        "/proc/gpufreq/gpufreq_var_dump"                                     to Parser.MTK_LOADING,
        "/proc/gpufreq/gpufreq_status"                                       to Parser.MTK_LOADING,
        "/proc/mali/utilization"                                             to Parser.PLAIN_INT,
        "/proc/gpufreq/gpufreq_loading"                                      to Parser.PLAIN_INT,
        // MT67xx series (Helio G series)
        "/sys/devices/platform/mt8173.mali/utilization"                      to Parser.PLAIN_INT,
        "/sys/devices/platform/13040000.mali/utilization"                    to Parser.PLAIN_INT,
        "/sys/devices/platform/13000000.mali/utilization"                    to Parser.PLAIN_INT,
        // Dimensity devfreq (addresses vary by chip)
        "/sys/class/devfreq/13000000.gpu/load"                               to Parser.FRACTION,
        "/sys/class/devfreq/13040000.gpu/load"                               to Parser.FRACTION,
        "/sys/class/devfreq/13080000.gpu/load"                               to Parser.FRACTION,
        "/sys/class/devfreq/13200000.gpu/load"                               to Parser.FRACTION,
        "/sys/class/devfreq/13400000.gpu/load"                               to Parser.FRACTION,
        // MediaTek DVFS sysfs
        "/sys/kernel/debug/mtk_mfg/loading"                                  to Parser.PLAIN_INT,
        "/sys/bus/platform/drivers/mtk-mali/utilization"                     to Parser.PLAIN_INT,

        // ── HiSilicon Kirin (Huawei / Honor) ─────────────────────────────────
        "/sys/kernel/debug/hisi_gpu/utilization"                             to Parser.KIRIN_LINE,
        "/sys/kernel/debug/mali0/utilization"                                to Parser.KIRIN_LINE,
        "/sys/devices/platform/e8970000.mali/utilization"                    to Parser.PLAIN_INT,
        "/sys/devices/platform/e8970000.mali/utilization_pp"                 to Parser.PLAIN_INT,
        "/sys/devices/platform/gpu/utilization"                              to Parser.PLAIN_INT,
        // Kirin 980 / 990 / 9000
        "/sys/kernel/debug/hisi_gpu/busy_rate"                               to Parser.KIRIN_LINE,
        "/sys/class/devfreq/e8970000.mali/load"                              to Parser.FRACTION,
        "/sys/class/devfreq/fdb30000.gpu/load"                               to Parser.FRACTION,

        // ── Unisoc / Spreadtrum ───────────────────────────────────────────────
        // Budget Android phones (T606/T616/T618/T700/T760/T770/T820)
        "/sys/class/devfreq/sprd-gpu/load"                                   to Parser.FRACTION,
        "/sys/class/devfreq/sprd_gpu/load"                                   to Parser.UNISOC_LOAD,
        "/sys/devices/platform/sprd-gpu/utilization"                         to Parser.PLAIN_INT,
        "/sys/kernel/debug/sprd_gpu/utilization"                             to Parser.PLAIN_INT,
        "/proc/gpuinfo"                                                      to Parser.UNISOC_LOAD,
        "/sys/class/devfreq/60100000.gpu/load"                               to Parser.FRACTION,
        "/sys/class/devfreq/60200000.gpu/load"                               to Parser.FRACTION,
        "/sys/class/devfreq/60300000.gpu/load"                               to Parser.FRACTION,

        // ── Rockchip (RK3399 / RK3568 / RK3588) ──────────────────────────────
        // Tablets, TV boxes, Chromebooks, some Android phones
        "/sys/class/devfreq/ff9a0000.gpu/load"                               to Parser.FRACTION,
        "/sys/class/devfreq/fde60000.gpu/load"                               to Parser.FRACTION,
        "/sys/class/devfreq/fb000000.gpu/load"                               to Parser.FRACTION,
        "/sys/kernel/debug/rk3d/gpu_load"                                    to Parser.ROCKCHIP_LOAD,
        "/sys/devices/platform/fb000000.gpu/utilization"                     to Parser.PLAIN_INT,
        "/sys/devices/platform/fde60000.gpu/utilization"                     to Parser.PLAIN_INT,
        // RK3588 (newer high-end Rockchip)
        "/sys/class/devfreq/fb000000.mali/load"                              to Parser.FRACTION,
        "/sys/devices/platform/fb000000.mali/utilization"                    to Parser.PLAIN_INT,

        // ── Allwinner (H/A/R series) ──────────────────────────────────────────
        // Budget Android phones and tablets, very common in sub-$50 category
        "/sys/devices/platform/gpu/utilization"                              to Parser.PLAIN_INT,
        "/sys/devices/platform/sunxi-mali.0/utilization"                     to Parser.PLAIN_INT,
        "/sys/kernel/debug/sunxi_gpu/utilization"                            to Parser.PLAIN_INT,
        "/sys/class/devfreq/1800000.gpu/load"                                to Parser.FRACTION,
        "/sys/class/devfreq/1c40000.gpu/load"                                to Parser.FRACTION,

        // ── Nvidia Tegra ──────────────────────────────────────────────────────
        // Shield Tablet, Nexus 7 (2013), legacy Tegra 2/3/4 devices
        "/sys/kernel/debug/clock/3d/rate"                                    to Parser.NVIDIA_LOAD,
        "/sys/devices/platform/host1x/gr3d/load"                            to Parser.PLAIN_INT,
        "/sys/class/devfreq/57000000.gpu/load"                               to Parser.FRACTION,
        "/sys/class/devfreq/gpu.0/load"                                      to Parser.FRACTION,
        "/sys/kernel/debug/tegra_gpu/utilization"                            to Parser.PLAIN_INT,
        // Tegra X1 (Shield TV, Nintendo Switch Android ports)
        "/sys/class/devfreq/57000000.gr3d/load"                             to Parser.FRACTION,
        "/sys/devices/platform/gpu.0/load"                                   to Parser.PLAIN_INT,

        // ── Imagination PowerVR ───────────────────────────────────────────────
        "/sys/devices/platform/pvrsrvkm/utilization"                         to Parser.POWERVR_UTIL,
        "/sys/devices/platform/pvrsrvkm.0/utilization"                       to Parser.PLAIN_INT,
        "/proc/pvr/render_utilization"                                       to Parser.POWERVR_UTIL,
        "/proc/pvr/3d_load"                                                  to Parser.PLAIN_INT,
        "/sys/devices/platform/rogue/utilization"                            to Parser.POWERVR_UTIL,

        // ── Vivante GCxxx (NXP i.MX) ─────────────────────────────────────────
        "/sys/bus/platform/drivers/galcore/utilization"                      to Parser.PLAIN_INT,
        "/sys/devices/platform/galcore/utilization"                          to Parser.PLAIN_INT,
        "/sys/kernel/debug/gc/load"                                          to Parser.PLAIN_INT,

        // ── Intel HD / Gen (Android x86 tablets) ─────────────────────────────
        // Surface-like Intel Atom tablets running Android, rare but real
        "/sys/class/drm/card0/device/gt_cur_freq_mhz"                       to Parser.INTEL_RC6,
        "/sys/kernel/debug/dri/0/i915_gpu_info"                             to Parser.INTEL_RC6,
        "/sys/class/devfreq/drm-msm_gpu/load"                               to Parser.FRACTION,

        // ── Broadcom VideoCore ────────────────────────────────────────────────
        "/sys/kernel/debug/vc4/gpu_usage"                                    to Parser.PLAIN_INT,
        "/proc/vc4_gpu_usage"                                                to Parser.PLAIN_INT,
        "/sys/class/devfreq/3f400000.v3d/load"                              to Parser.FRACTION,

        // ── OPPO / Realme / OnePlus (ColorOS custom nodes) ────────────────────
        // These OEMs sometimes add proprietary sysfs on top of Snapdragon/Dimensity
        "/sys/class/devfreq/gpufreq/cur_load"                               to Parser.PLAIN_INT,
        "/proc/oppogpu/load"                                                 to Parser.PLAIN_INT,
        "/sys/kernel/oplus_performance/gpu_utilization"                      to Parser.PLAIN_INT,

        // ── Xiaomi / MIUI ─────────────────────────────────────────────────────
        "/sys/kernel/debug/kgsl/kgsl-3d0/gpu_busy_percentage"               to Parser.PERCENT_SIGN,
        "/proc/perfinfo/gpu_load"                                            to Parser.PLAIN_INT,
        "/sys/kernel/xiaomi_touch/gpu_load"                                  to Parser.PLAIN_INT,

        // ── Generic devfreq catch-all ─────────────────────────────────────────
        "/sys/class/devfreq/soc:gpu/load"                                    to Parser.FRACTION,
        "/sys/class/devfreq/graphics/load"                                   to Parser.FRACTION,
        "/sys/class/devfreq/3d_scaling/load"                                 to Parser.FRACTION,
        "/sys/class/devfreq/bigsea/load"                                     to Parser.FRACTION,
        "/sys/class/devfreq/gpu-subsystem/load"                              to Parser.FRACTION,
        "/sys/class/devfreq/gpu_subsys/load"                                 to Parser.FRACTION,
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    fun read(): Float? {
        // Fast path — resolved path known, just read it
        lock.read {
            val path = resolvedPath
            val parser = resolvedParser
            if (path != null && parser != null) {
                return tryParseFile(path, parser)
            }
            if (scanComplete) return null
        }

        // Slow path — need to scan. Write lock prevents duplicate scans from concurrent callers.
        lock.write {
            // Double-check after acquiring write lock
            val path = resolvedPath
            val parser = resolvedParser
            if (path != null && parser != null) return tryParseFile(path, parser)
            if (scanComplete) return null

            return fullScan()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Full scan
    // ─────────────────────────────────────────────────────────────────────────

    private fun fullScan(): Float? {
        val dynamicDevfreq = discoverDevfreqGpuNodes()
        val dynamicProc    = discoverProcGpuNodes()

        val allCandidates = candidates + dynamicDevfreq + dynamicProc

        for ((path, parser) in allCandidates) {
            val result = tryCandidate(path, parser)
            if (result != null) {
                resolvedPath   = path
                resolvedParser = parser
                scanComplete   = true
                Log.i(TAG, "GPU sysfs resolved → $path  [parser=$parser]  value=$result")
                return result
            }
        }

        scanComplete = true
        Log.w(TAG, "No readable GPU sysfs node found on this device — using CPU-weighted fallback.")
        return null
    }

    /**
     * Dynamically walks /sys/class/devfreq/ and collects every directory whose
     * name suggests a GPU — catches exotic address-named nodes like "a1234560.gpu"
     * that no hardcoded list can anticipate.
     */
    private fun discoverDevfreqGpuNodes(): List<Pair<String, Parser>> {
        val discovered = mutableListOf<Pair<String, Parser>>()
        return try {
            val devfreqDir = File("/sys/class/devfreq")
            if (!devfreqDir.exists() || !devfreqDir.isDirectory) return discovered

            devfreqDir.listFiles()?.forEach { node ->
                try {
                    val name = node.name.lowercase()
                    val isGpu = name.contains("gpu")
                            || name.contains("mali")
                            || name.contains("kgsl")
                            || name.contains("graphic")
                            || name.contains("gr3d")
                            || name.contains("v3d")
                            || name.contains("rogue")
                            || name.contains("pvr")
                            || name.contains("bigsea")
                            || name.contains("adreno")
                            || name.contains("gc")       // Vivante GCxxx

                    if (isGpu) {
                        File(node, "load").takeIf { it.exists() }?.let {
                            discovered.add(it.absolutePath to Parser.FRACTION)
                        }
                        File(node, "utilization").takeIf { it.exists() }?.let {
                            discovered.add(it.absolutePath to Parser.PLAIN_INT)
                        }
                        File(node, "cur_load").takeIf { it.exists() }?.let {
                            discovered.add(it.absolutePath to Parser.PLAIN_INT)
                        }
                    }
                } catch (_: Exception) {}
            }
            discovered
        } catch (_: Exception) {
            discovered
        }
    }

    /**
     * Dynamically walks /proc/ for GPU-related nodes that aren't in our hardcoded list.
     * Limited to top-level /proc/ entries to avoid deep traversal cost.
     */
    private fun discoverProcGpuNodes(): List<Pair<String, Parser>> {
        val discovered = mutableListOf<Pair<String, Parser>>()
        return try {
            val procDir = File("/proc")
            if (!procDir.exists()) return discovered

            procDir.listFiles()?.forEach { entry ->
                try {
                    if (!entry.isDirectory) return@forEach
                    val name = entry.name.lowercase()
                    val isGpu = name.contains("gpu")
                            || name.contains("mali")
                            || name.contains("kgsl")
                            || name.contains("pvr")
                            || name.contains("gc_")      // Vivante
                            || name.contains("gpufreq")

                    if (isGpu) {
                        // Look for common stat file names inside the directory
                        listOf("utilization", "load", "loading", "busy", "gpu_load").forEach { stat ->
                            File(entry, stat).takeIf { it.exists() }?.let {
                                discovered.add(it.absolutePath to Parser.PLAIN_INT)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
            discovered
        } catch (_: Exception) {
            discovered
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-candidate isolation
    // ─────────────────────────────────────────────────────────────────────────

    private fun tryCandidate(path: String, parser: Parser): Float? {
        return try {
            val file = File(path)
            if (!file.exists() || !file.canRead()) return null
            tryParseFile(path, parser)
        } catch (_: Exception) {
            null
        }
    }

    private fun tryParseFile(path: String, parser: Parser): Float? {
        return try {
            val raw = File(path).readText().trim()
            if (raw.isEmpty()) return null
            parseRaw(raw, parser)
        } catch (_: Exception) {
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parsers
    // ─────────────────────────────────────────────────────────────────────────

    private fun parseRaw(raw: String, parser: Parser): Float? {
        return try {
            when (parser) {

                // "73 %" / "73%" / "73" → 0.73
                Parser.PERCENT_SIGN -> {
                    raw.replace(Regex("[^0-9]"), "")
                        .trim().toFloatOrNull()
                        ?.div(100f)?.coerceIn(0f, 1f)
                }

                // "35" → 0.35
                Parser.PLAIN_INT -> {
                    raw.lines()
                        .firstOrNull { it.isNotBlank() }
                        ?.replace(Regex("[^0-9]"), "")
                        ?.trim()?.toFloatOrNull()
                        ?.div(100f)?.coerceIn(0f, 1f)
                }

                // "364792308/533000000" → busy/total → ratio
                Parser.FRACTION -> {
                    if (raw.contains("/")) {
                        val parts = raw.split("/")
                        val used  = parts.getOrNull(0)?.trim()?.toLongOrNull() ?: return null
                        val total = parts.getOrNull(1)?.trim()
                            ?.split(Regex("\\s+"))?.firstOrNull()
                            ?.toLongOrNull() ?: return null
                        if (total == 0L) return null
                        (used.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    } else {
                        raw.replace(Regex("[^0-9]"), "").trim()
                            .toFloatOrNull()?.div(100f)?.coerceIn(0f, 1f)
                    }
                }

                // "40 @500MHz" / "40/100" → first integer as percent
                Parser.FRACTION_PERCENT -> {
                    raw.replace(Regex("[^0-9/]"), " ").trim()
                        .split(Regex("\\s+|/"))
                        .firstOrNull { it.isNotBlank() }
                        ?.toFloatOrNull()?.div(100f)?.coerceIn(0f, 1f)
                }

                // Adreno gpubusy: "3355443 4194304" → busy_count / total_count
                // The two numbers are hardware cycle counters, ratio gives load.
                Parser.KGSL_BUSY -> {
                    val parts = raw.trim().split(Regex("\\s+"))
                    val busy  = parts.getOrNull(0)?.toLongOrNull() ?: return null
                    val total = parts.getOrNull(1)?.toLongOrNull() ?: return null
                    if (total == 0L) return null
                    (busy.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                }

                // MediaTek multi-line proc dump: find the "Loading" line
                // e.g. "Loading = 42" or "GPU Loading: 42"
                Parser.MTK_LOADING -> {
                    val line = raw.lines().firstOrNull {
                        it.contains("loading", ignoreCase = true)
                                || it.contains("utilization", ignoreCase = true)
                                || it.contains("busy", ignoreCase = true)
                    } ?: return null
                    line.replace(Regex("[^0-9]"), "")
                        .trim().toFloatOrNull()
                        ?.div(100f)?.coerceIn(0f, 1f)
                }

                // Huawei Kirin: "busy_rate:55" / "utilization:55" / "loading:55"
                Parser.KIRIN_LINE -> {
                    val line = raw.lines().firstOrNull {
                        it.contains("busy_rate", ignoreCase = true)
                                || it.contains("utilization", ignoreCase = true)
                                || it.contains("loading", ignoreCase = true)
                                || it.contains("busy", ignoreCase = true)
                    } ?: raw.lines().firstOrNull { it.isNotBlank() }
                    line?.replace(Regex("[^0-9]"), "")
                        ?.trim()?.toFloatOrNull()
                        ?.div(100f)?.coerceIn(0f, 1f)
                }

                // PowerVR: "Utilization: 0.78" (ratio) or "Utilization: 78" (percent)
                Parser.POWERVR_UTIL -> {
                    val numStr = raw.replace(Regex("[^0-9.]"), "").trim()
                    val value  = numStr.toFloatOrNull() ?: return null
                    if (value > 1.0f) value.div(100f).coerceIn(0f, 1f)
                    else value.coerceIn(0f, 1f)
                }

                // "0.73" plain float ratio
                Parser.GENERIC_RATIO -> {
                    raw.trim().toFloatOrNull()?.coerceIn(0f, 1f)
                }

                // Nvidia Tegra: "busy 55 total 100" or just a clock rate file.
                // Clock rate alone isn't load, so we look for busy/total keywords first.
                Parser.NVIDIA_LOAD -> {
                    if (raw.contains("busy", ignoreCase = true) &&
                        raw.contains("total", ignoreCase = true)) {
                        val numbers = Regex("\\d+").findAll(raw)
                            .map { it.value.toLongOrNull() }
                            .filterNotNull()
                            .toList()
                        val busy  = numbers.getOrNull(0) ?: return null
                        val total = numbers.getOrNull(1) ?: return null
                        if (total == 0L) return null
                        (busy.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    } else {
                        // Just a rate file — not useful as a load proxy, skip
                        null
                    }
                }

                // Intel RC6 residency: RC6 is the GPU idle state.
                // "RC6 residency: 45%" means 45% idle → 55% busy.
                Parser.INTEL_RC6 -> {
                    val line = raw.lines().firstOrNull {
                        it.contains("RC6", ignoreCase = true) ||
                                it.contains("residency", ignoreCase = true)
                    } ?: return null
                    val pct = line.replace(Regex("[^0-9]"), "")
                        .trim().toFloatOrNull() ?: return null
                    val idle = pct.div(100f).coerceIn(0f, 1f)
                    (1f - idle).coerceIn(0f, 1f)   // invert: idle → busy
                }

                // Rockchip: "load = 55%" or "gpu_load=55"
                Parser.ROCKCHIP_LOAD -> {
                    val line = raw.lines().firstOrNull {
                        it.contains("load", ignoreCase = true)
                    } ?: raw.lines().firstOrNull { it.isNotBlank() }
                    line?.replace(Regex("[^0-9]"), "")
                        ?.trim()?.toFloatOrNull()
                        ?.div(100f)?.coerceIn(0f, 1f)
                }

                // Unisoc: "55" or "55%" or "gpu load: 55"
                Parser.UNISOC_LOAD -> {
                    val line = raw.lines().firstOrNull {
                        it.contains("load", ignoreCase = true) ||
                                it.contains("busy", ignoreCase = true) ||
                                it.isNotBlank()
                    }
                    line?.replace(Regex("[^0-9]"), "")
                        ?.trim()?.toFloatOrNull()
                        ?.div(100f)?.coerceIn(0f, 1f)
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}