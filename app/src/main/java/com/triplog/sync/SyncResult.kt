package com.triplog.sync

/** Outcome of one sync pass over the pending records. */
data class SyncResult(
    val synced: Int = 0,
    /** Records that failed for a reason that may clear up (offline, timeout, 5xx). */
    val retryable: Int = 0,
    /** Records the server rejected (4xx) or sent back an unusable answer for. */
    val rejected: Int = 0
) {
    val shouldRetry: Boolean get() = retryable > 0

    operator fun plus(other: SyncResult) = SyncResult(
        synced = synced + other.synced,
        retryable = retryable + other.retryable,
        rejected = rejected + other.rejected
    )
}

/** What happened to a single request. */
enum class SyncOutcome { Success, Retryable, Rejected }

/** 4xx means the server will not accept this record as sent; 408 and 429 are worth retrying. */
fun outcomeForHttpCode(code: Int): SyncOutcome = when {
    code in 200..299 -> SyncOutcome.Success
    code == 408 || code == 429 -> SyncOutcome.Retryable
    code in 400..499 -> SyncOutcome.Rejected
    else -> SyncOutcome.Retryable
}

fun SyncOutcome.toResult(): SyncResult = when (this) {
    SyncOutcome.Success -> SyncResult(synced = 1)
    SyncOutcome.Retryable -> SyncResult(retryable = 1)
    SyncOutcome.Rejected -> SyncResult(rejected = 1)
}
