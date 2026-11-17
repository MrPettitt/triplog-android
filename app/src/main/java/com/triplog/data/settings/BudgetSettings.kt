package com.triplog.data.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The monthly fuel budget in rands, or null when none is set. Remembered across launches. */
@Singleton
class BudgetSettings @Inject constructor(private val store: SettingsStore) {

    private val _monthlyBudget = MutableStateFlow(read())
    val monthlyBudget: StateFlow<Double?> = _monthlyBudget.asStateFlow()

    /** Pass null to remove the budget. Values that are not a positive number are ignored. */
    fun setMonthlyBudget(rands: Double?) {
        if (rands == null) {
            store.remove(KEY_MONTHLY_BUDGET)
            _monthlyBudget.value = null
        } else if (rands.isFinite() && rands > 0.0) {
            store.putString(KEY_MONTHLY_BUDGET, rands.toString())
            _monthlyBudget.value = rands
        }
    }

    private fun read(): Double? =
        store.getString(KEY_MONTHLY_BUDGET)?.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }

    private companion object {
        const val KEY_MONTHLY_BUDGET = "monthly_budget"
    }
}
