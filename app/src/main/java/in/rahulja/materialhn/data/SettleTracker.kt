package `in`.rahulja.materialhn.data

/**
 * Decides when a page has stopped growing so Readability can parse it.
 * A page settles after two equal non-zero text lengths, or at the time cap.
 */
class SettleTracker(
    private val intervalMs: Int = DEFAULT_INTERVAL_MS,
    private val maxMs: Int = DEFAULT_MAX_MS
) {
    private var lastLength = -1

    fun record(length: Int, elapsedMs: Long): Boolean {
        val settled = length > 0 && length == lastLength
        lastLength = length
        return settled || elapsedMs >= maxMs
    }

    companion object {
        const val DEFAULT_INTERVAL_MS = 400
        const val DEFAULT_MAX_MS = 4000
    }
}
