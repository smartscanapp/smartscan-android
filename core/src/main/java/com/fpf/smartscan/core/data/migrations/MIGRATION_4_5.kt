package com.fpf.smartscan.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE media_metadata ADD COLUMN isDuplicate INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
        )
        db.execSQL(
            """
            ALTER TABLE media_metadata ADD COLUMN isTrashed INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX index_media_metadata_isDuplicate
            ON media_metadata(isDuplicate)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX index_media_metadata_isTrashed
            ON media_metadata(isTrashed)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE concept (
                id INTEGER NOT NULL,
                description TEXT NOT NULL,
                updatedAt INTEGER NOT NULL,
                isPinned INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE concept_crossref (
                mediaId INTEGER NOT NULL,
                conceptId INTEGER NOT NULL,
                mediaType INTEGER NOT NULL,
                similarity REAL NOT NULL,
                PRIMARY KEY(mediaId, mediaType, conceptId),
                FOREIGN KEY(conceptId) REFERENCES concept(id) ON DELETE CASCADE,
                FOREIGN KEY(mediaId, mediaType) REFERENCES media_metadata(id, type) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX index_concept_crossref_conceptId_similarity
            ON concept_crossref(conceptId, similarity)
            """.trimIndent()
        )
    }
}