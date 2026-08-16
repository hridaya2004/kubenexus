package dev.hridaya.kubenexus.data.source.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.hridaya.kubenexus.data.source.local.dao.ClusterDao
import dev.hridaya.kubenexus.data.source.local.entity.ClusterEntity

@Database(
    entities = [ClusterEntity::class],
    version = 1,
    exportSchema = false
)
abstract class KubeNexusDatabase : RoomDatabase() {

    abstract fun clusterDao(): ClusterDao

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
