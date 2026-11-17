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

/**
 * Version 3 to 4: the app no longer syncs, so the sync columns (client id, synced flag, remote
 * id, revision, deletion tombstone, receipt uploaded flag) are dropped. Records that were only
 * marked as deleted are removed for good. SQLite cannot drop columns on older Android versions,
 * so each table is rebuilt.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM trips WHERE isDeleted = 1")
        db.execSQL("DELETE FROM fuel_expenses WHERE isDeleted = 1")

        db.execSQL(
            "CREATE TABLE trips_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "fromLocation TEXT NOT NULL, toLocation TEXT NOT NULL, dateTime TEXT NOT NULL, " +
                "distanceKm REAL NOT NULL, purpose TEXT NOT NULL, notes TEXT NOT NULL, " +
                "createdAt TEXT NOT NULL)"
        )
        db.execSQL(
            "INSERT INTO trips_new (id, fromLocation, toLocation, dateTime, distanceKm, purpose, notes, createdAt) " +
                "SELECT id, fromLocation, toLocation, dateTime, distanceKm, purpose, notes, createdAt FROM trips"
        )
        db.execSQL("DROP TABLE trips")
        db.execSQL("ALTER TABLE trips_new RENAME TO trips")

        db.execSQL(
            "CREATE TABLE fuel_expenses_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "date TEXT NOT NULL, litres REAL NOT NULL, amountRands REAL NOT NULL, " +
                "odometerKm INTEGER NOT NULL, fuelStation TEXT NOT NULL, receiptImagePath TEXT, " +
                "createdAt TEXT NOT NULL)"
        )
        db.execSQL(
            "INSERT INTO fuel_expenses_new (id, date, litres, amountRands, odometerKm, fuelStation, receiptImagePath, createdAt) " +
                "SELECT id, date, litres, amountRands, odometerKm, fuelStation, receiptImagePath, createdAt FROM fuel_expenses"
        )
        db.execSQL("DROP TABLE fuel_expenses")
        db.execSQL("ALTER TABLE fuel_expenses_new RENAME TO fuel_expenses")
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
