package dev.hridaya.kubenexus.data.source.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
        private const val DATABASE_NAME = "kubenexus.db"

        @Volatile
        private var INSTANCE: KubeNexusDatabase? = null

        fun getInstance(context: Context): KubeNexusDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KubeNexusDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
