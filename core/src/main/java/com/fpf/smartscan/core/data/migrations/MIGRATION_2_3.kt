package com.fpf.smartscan.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=OFF")

        db.execSQL("DELETE FROM media_cluster_crossref")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cluster_metadata_new` (
                `clusterId` INTEGER NOT NULL,
                `prototypeSize` INTEGER NOT NULL,
                `meanSimilarity` REAL NOT NULL,
                `stdSimilarity` REAL NOT NULL,
                `label` TEXT,
                PRIMARY KEY(`clusterId`)
            )
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `cluster_metadata`")
        db.execSQL("ALTER TABLE `cluster_metadata_new` RENAME TO `cluster_metadata`")

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_cluster_metadata_label` ON `cluster_metadata` (`label`)"
        )

        db.execSQL("PRAGMA foreign_keys=ON")
    }
}