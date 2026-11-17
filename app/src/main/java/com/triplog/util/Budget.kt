package com.triplog.util

/** How a month's fuel spending compares with the monthly budget. */
data class BudgetStatus(val budgetRands: Double, val spentRands: Double) {
    val remainingRands: Double get() = budgetRands - spentRands
    val isOver: Boolean get() = spentRands > budgetRands

    /** Share of the budget used, from 0 to 1 (capped, so the progress bar never overflows). */
    val fractionUsed: Float get() = (spentRands / budgetRands).toFloat().coerceIn(0f, 1f)

    /** True from 80% of the budget, so the card can warn before the budget is actually exceeded. */
    val isNearLimit: Boolean get() = !isOver && spentRands >= budgetRands * NEAR_LIMIT

    private companion object {
        const val NEAR_LIMIT = 0.8
    }
}

/** Returns null when no budget is set. */
fun budgetStatus(budgetRands: Double?, spentRands: Double): BudgetStatus? =
    budgetRands?.let { BudgetStatus(it, spentRands) }
