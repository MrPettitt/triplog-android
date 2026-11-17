package com.triplog.util

import com.triplog.sync.SyncConfig
import com.triplog.sync.SyncOutcome
import com.triplog.sync.SyncResult
import com.triplog.sync.outcomeForHttpCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncResultTest {

    @Test
    fun httpCodes_areClassified() {
        assertEquals(SyncOutcome.Success, outcomeForHttpCode(200))
        assertEquals(SyncOutcome.Success, outcomeForHttpCode(201))
        assertEquals(SyncOutcome.Rejected, outcomeForHttpCode(400))
        assertEquals(SyncOutcome.Rejected, outcomeForHttpCode(401))
        assertEquals(SyncOutcome.Rejected, outcomeForHttpCode(422))
        assertEquals(SyncOutcome.Retryable, outcomeForHttpCode(408))
        assertEquals(SyncOutcome.Retryable, outcomeForHttpCode(429))
        assertEquals(SyncOutcome.Retryable, outcomeForHttpCode(500))
        assertEquals(SyncOutcome.Retryable, outcomeForHttpCode(503))
    }

    @Test
    fun results_addUp_andOnlyRetryableTriggersRetry() {
        val total = SyncResult(synced = 1, rejected = 2) + SyncResult(retryable = 1)

        assertEquals(SyncResult(synced = 1, retryable = 1, rejected = 2), total)
        assertTrue(total.shouldRetry)
        assertFalse(SyncResult(synced = 3, rejected = 1).shouldRetry)
    }

    @Test
    fun syncConfig_treatsThePlaceholderUrlAsNotConfigured() {
        assertFalse(SyncConfig("https://your-api-url.com/api/v1/").isConfigured)
        assertFalse(SyncConfig("").isConfigured)
        assertTrue(SyncConfig("https://api.example.org/api/v1/").isConfigured)
    }
}
