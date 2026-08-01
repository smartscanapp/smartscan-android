package com.fpf.smartscan.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // media_metadata
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS media_metadata (
                id INTEGER NOT NULL PRIMARY KEY,
                type TEXT NOT NULL,
                dateAdded INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_media_metadata_dateAdded ON media_metadata(dateAdded)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_media_metadata_type ON media_metadata(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_media_metadata_type_dateAdded ON media_metadata(type, dateAdded)")

        // tag_crossref
        db.execSQL(
            """
            CREATE TABLE tag_crossref_new (
                mediaId INTEGER NOT NULL,
                tagId INTEGER NOT NULL,
                PRIMARY KEY(mediaId, tagId),
                FOREIGN KEY(tagId) REFERENCES media_tag(id) ON DELETE CASCADE,
                FOREIGN KEY(mediaId) REFERENCES media_metadata(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO tag_crossref_new (mediaId, tagId)
            SELECT mediaId, tagId FROM tag_crossref
            """.trimIndent()
        )

        db.execSQL("DROP TABLE tag_crossref")
        db.execSQL("ALTER TABLE tag_crossref_new RENAME TO tag_crossref")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_tag_crossref_tagId ON tag_crossref(tagId)")

        // media_cluster_crossref
        db.execSQL(
            """
            CREATE TABLE media_cluster_crossref_new (
                clusterId INTEGER NOT NULL,
                mediaId INTEGER NOT NULL,
                PRIMARY KEY(clusterId, mediaId),
                FOREIGN KEY(clusterId) REFERENCES cluster_metadata(clusterId) ON DELETE CASCADE,
                FOREIGN KEY(mediaId) REFERENCES media_metadata(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO media_cluster_crossref_new (clusterId, mediaId)
            SELECT clusterId, mediaId FROM media_cluster_crossref
            """.trimIndent()
        )

        db.execSQL("DROP TABLE media_cluster_crossref")
        db.execSQL("ALTER TABLE media_cluster_crossref_new RENAME TO media_cluster_crossref")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_media_cluster_crossref_mediaId ON media_cluster_crossref(mediaId)")
    }
}