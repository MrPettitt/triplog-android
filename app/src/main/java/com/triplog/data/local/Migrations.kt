package com.triplog.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 1 to 2: adds a client generated id to every trip and fuel expense so the
 * server can de-duplicate retried uploads. Existing rows get a random UUID.
 * Unsynced data is preserved.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trips ADD COLUMN clientId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE fuel_expenses ADD COLUMN clientId TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE trips SET clientId = $RANDOM_UUID_SQL")
        db.execSQL("UPDATE fuel_expenses SET clientId = $RANDOM_UUID_SQL")
    }
}

/** SQLite expression producing a random version 4 UUID, evaluated once per row. */
internal const val RANDOM_UUID_SQL =
    "lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || " +
        "substr(hex(randomblob(2)), 2) || '-' || substr('89ab', abs(random()) % 4 + 1, 1) || " +
        "substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6)))"

/**
 * Version 2 to 3: adds a revision counter and a deletion tombstone to both tables, and tracks
 * whether the receipt photo has been uploaded. Receipts of already synced expenses count as
 * uploaded; unsynced ones are retried.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trips ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE trips ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE fuel_expenses ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE fuel_expenses ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE fuel_expenses ADD COLUMN receiptUploaded INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE fuel_expenses SET receiptUploaded = isSynced")
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
