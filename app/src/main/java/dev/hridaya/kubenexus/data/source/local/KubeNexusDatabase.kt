package dev.hridaya.kubenexus.data.source.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.hridaya.kubenexus.data.source.local.dao.ClusterDao
import dev.hridaya.kubenexus.data.source.local.dao.NamespaceDao
import dev.hridaya.kubenexus.data.source.local.dao.PodDao
import dev.hridaya.kubenexus.data.source.local.entity.ClusterEntity
import dev.hridaya.kubenexus.data.source.local.entity.NamespaceEntity
import dev.hridaya.kubenexus.data.source.local.entity.PodEntity
import dev.hridaya.kubenexus.data.source.local.entity.SyncMetadataEntity

@Database(
    entities = [
        ClusterEntity::class,
        PodEntity::class,
        NamespaceEntity::class,
        SyncMetadataEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class KubeNexusDatabase : RoomDatabase() {

    abstract fun clusterDao(): ClusterDao
    abstract fun podDao(): PodDao
    abstract fun namespaceDao(): NamespaceDao

    companion object {
        const val DATABASE_NAME = "kubenexus.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Preserves existing database structure across schema upgrades
            }
        }

        @Volatile
        private var INSTANCE: KubeNexusDatabase? = null

        fun getInstance(context: Context): KubeNexusDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KubeNexusDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
