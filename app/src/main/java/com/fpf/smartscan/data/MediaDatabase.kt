package com.fpf.smartscan.data

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fpf.smartscan.data.clusters.ClusterCrossRefEntity
import com.fpf.smartscan.data.clusters.ClusterCrossRefDao
import com.fpf.smartscan.data.clusters.ClusterMetadataEntity
import com.fpf.smartscan.data.clusters.ClusterMetadataDao
import com.fpf.smartscan.data.concepts.ConceptCrossRefDao
import com.fpf.smartscan.data.concepts.ConceptCrossRefEntity
import com.fpf.smartscan.data.concepts.ConceptDao
import com.fpf.smartscan.data.concepts.ConceptEntity
import com.fpf.smartscan.data.metadata.MediaMetadata
import com.fpf.smartscan.data.metadata.MediaMetadataDao
import com.fpf.smartscan.data.migrations.MIGRATION_1_2
import com.fpf.smartscan.data.migrations.MIGRATION_2_3
import com.fpf.smartscan.data.migrations.MIGRATION_3_4
import com.fpf.smartscan.data.migrations.MIGRATION_4_5
import com.fpf.smartscan.data.tags.TagEntity
import com.fpf.smartscan.data.tags.TagCrossRefEntity
import com.fpf.smartscan.data.tags.TagCrossRefDao
import com.fpf.smartscan.data.tags.TagDao


@Database(
    entities = [
        MediaMetadata::class,
        ClusterMetadataEntity::class,
        ClusterCrossRefEntity::class,
        TagEntity::class,
        TagCrossRefEntity::class,
        ConceptEntity::class,
        ConceptCrossRefEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(MediaTypeConverter::class)
abstract class MediaDatabase : RoomDatabase() {

    abstract fun clusterCrossRefDao(): ClusterCrossRefDao
    abstract fun clusterMetadataDao(): ClusterMetadataDao

    abstract fun metadataDao(): MediaMetadataDao

    abstract fun tagCrossRefDao(): TagCrossRefDao
    abstract fun tagDao(): TagDao

    abstract fun conceptCrossRefDao(): ConceptCrossRefDao
    abstract fun conceptDao(): ConceptDao

    companion object {
        @Volatile
        private var INSTANCE: MediaDatabase? = null
        const val DB_NAME = "media_database"

        const val TAG = "MediaDatabase"

        fun close() {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun getDatabase(application: Application): MediaDatabase {
            return INSTANCE ?: synchronized(this) {

                // Build DB first
                val instance = Room.databaseBuilder(
                    application,
                    MediaDatabase::class.java,
                    DB_NAME
                ).setJournalMode(JournalMode.TRUNCATE)
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}