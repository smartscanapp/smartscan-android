package com.fpf.smartscan.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // 1. Recreate media_metadata (TEXT type -> INTEGER type, single PK -> composite PK)
        db.execSQL(
            """
            CREATE TABLE media_metadata_new (
                id INTEGER NOT NULL,
                type INTEGER NOT NULL,
                dateAdded INTEGER NOT NULL,
                description TEXT,
                PRIMARY KEY(id, type)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO media_metadata_new (id, type, dateAdded, description)
            SELECT
                id,
                CASE
                    WHEN type = 'IMAGE' THEN 0
                    WHEN type = 'VIDEO' THEN 1
                END,
                dateAdded,
                NULL
            FROM media_metadata
            """.trimIndent()
        )

        db.execSQL("DROP TABLE media_metadata")

        db.execSQL(
            "ALTER TABLE media_metadata_new RENAME TO media_metadata"
        )

        db.execSQL(
            """
            CREATE INDEX index_media_metadata_dateAdded
            ON media_metadata(dateAdded)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX index_media_metadata_type_dateAdded
            ON media_metadata(type, dateAdded)
            """.trimIndent()
        )


        // 2. Recreate media_cluster_crossref
        db.execSQL(
            """
            CREATE TABLE media_cluster_crossref_new (
                mediaId INTEGER NOT NULL,
                mediaType INTEGER NOT NULL,
                clusterId INTEGER NOT NULL,
                PRIMARY KEY(mediaId, mediaType),
                FOREIGN KEY(clusterId)
                    REFERENCES cluster_metadata(clusterId)
                    ON DELETE CASCADE,
                FOREIGN KEY(mediaId, mediaType)
                    REFERENCES media_metadata(id, type)
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT OR IGNORE INTO media_cluster_crossref_new (
                mediaId,
                mediaType,
                clusterId
            )
            SELECT
                crc.mediaId,
                mm.type,
                crc.clusterId
            FROM media_cluster_crossref crc
            JOIN media_metadata mm
                ON crc.mediaId = mm.id
            """.trimIndent()
        )

        db.execSQL("DROP TABLE media_cluster_crossref")

        db.execSQL(
            """
            ALTER TABLE media_cluster_crossref_new
            RENAME TO media_cluster_crossref
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX index_media_cluster_crossref_clusterId
            ON media_cluster_crossref(clusterId)
            """.trimIndent()
        )


        // 3. Recreate tag_crossref
        db.execSQL(
            """
            CREATE TABLE tag_crossref_new (
                mediaId INTEGER NOT NULL,
                mediaType INTEGER NOT NULL,
                tagId INTEGER NOT NULL,
                PRIMARY KEY(mediaId, mediaType, tagId),
                FOREIGN KEY(tagId)
                    REFERENCES media_tag(id)
                    ON DELETE CASCADE,
                FOREIGN KEY(mediaId, mediaType)
                    REFERENCES media_metadata(id, type)
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO tag_crossref_new (
                mediaId,
                mediaType,
                tagId
            )
            SELECT
                tr.mediaId,
                mm.type,
                tr.tagId
            FROM tag_crossref tr
            JOIN media_metadata mm
                ON tr.mediaId = mm.id
            """.trimIndent()
        )

        db.execSQL("DROP TABLE tag_crossref")

        db.execSQL(
            """
            ALTER TABLE tag_crossref_new
            RENAME TO tag_crossref
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX index_tag_crossref_tagId
            ON tag_crossref(tagId)
            """.trimIndent()
        )
    }
}