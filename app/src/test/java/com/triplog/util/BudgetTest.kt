package com.triplog.util

import com.triplog.data.settings.BudgetSettings
import com.triplog.data.settings.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetTest {

    private class MemoryStore(initial: Map<String, String> = emptyMap()) : SettingsStore {
        val values = initial.toMutableMap()
        override fun getString(key: String): String? = values[key]
        override fun putString(key: String, value: String) {
            values[key] = value
        }
        override fun remove(key: String) {
            values.remove(key)
        }
    }

    @Test
    fun noBudget_givesNoStatus() {
        assertNull(budgetStatus(null, 500.0))
    }

    @Test
    fun underBudget_reportsRemainingAndFraction() {
        val status = budgetStatus(1000.0, 250.0)!!

        assertEquals(750.0, status.remainingRands, 0.0)
        assertEquals(0.25f, status.fractionUsed, 0.0001f)
        assertFalse(status.isOver)
        assertFalse(status.isNearLimit)
    }

    @Test
    fun atEightyPercent_isNearLimit() {
        val status = budgetStatus(1000.0, 800.0)!!

        assertTrue(status.isNearLimit)
        assertFalse(status.isOver)
    }

    @Test
    fun exactlyOnBudget_isNotOver() {
        val status = budgetStatus(1000.0, 1000.0)!!

        assertFalse(status.isOver)
        assertEquals(1f, status.fractionUsed, 0.0f)
    }

    @Test
    fun overBudget_capsTheBarAndShowsNegativeRemaining() {
        val status = budgetStatus(1000.0, 1250.0)!!

        assertTrue(status.isOver)
        assertFalse(status.isNearLimit)
        assertEquals(1f, status.fractionUsed, 0.0f)
        assertEquals(-250.0, status.remainingRands, 0.0)
    }

    @Test
    fun settings_defaultToNoBudget() {
        assertNull(BudgetSettings(MemoryStore()).monthlyBudget.value)
    }

    @Test
    fun settings_persistAndReload() {
        val store = MemoryStore()
        val settings = BudgetSettings(store)

        settings.setMonthlyBudget(1500.5)

        assertEquals(1500.5, settings.monthlyBudget.value!!, 0.0)
        assertEquals(1500.5, BudgetSettings(store).monthlyBudget.value!!, 0.0)
    }

    @Test
    fun settings_nullRemovesTheBudget() {
        val store = MemoryStore()
        val settings = BudgetSettings(store)
        settings.setMonthlyBudget(900.0)

        settings.setMonthlyBudget(null)

        assertNull(settings.monthlyBudget.value)
        assertNull(BudgetSettings(store).monthlyBudget.value)
    }

    @Test
    fun settings_ignoreInvalidValues() {
        val settings = BudgetSettings(MemoryStore())
        settings.setMonthlyBudget(500.0)

        settings.setMonthlyBudget(0.0)
        settings.setMonthlyBudget(-10.0)
        settings.setMonthlyBudget(Double.NaN)

        assertEquals(500.0, settings.monthlyBudget.value!!, 0.0)
    }

    @Test
    fun settings_corruptStoredValueMeansNoBudget() {
        val settings = BudgetSettings(MemoryStore(mapOf("monthly_budget" to "abc")))

        assertNull(settings.monthlyBudget.value)
    }
}
