package dev.hridaya.kubenexus.data.source.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.hridaya.kubenexus.data.source.local.dao.APIResourceDao
import dev.hridaya.kubenexus.data.source.local.dao.ClusterDao
import dev.hridaya.kubenexus.data.source.local.dao.ExplainedResourceDao
import dev.hridaya.kubenexus.data.source.local.dao.NamespaceDao
import dev.hridaya.kubenexus.data.source.local.dao.PodDao
import dev.hridaya.kubenexus.data.source.local.entity.APIResourceEntity
import dev.hridaya.kubenexus.data.source.local.entity.ClusterEntity
import dev.hridaya.kubenexus.data.source.local.entity.ExplainedResourceEntity
import dev.hridaya.kubenexus.data.source.local.entity.NamespaceEntity
import dev.hridaya.kubenexus.data.source.local.entity.PodEntity
import dev.hridaya.kubenexus.data.source.local.entity.SyncMetadataEntity

@Database(
    entities = [
        ClusterEntity::class,
        PodEntity::class,
        NamespaceEntity::class,
        SyncMetadataEntity::class,
        APIResourceEntity::class,
        ExplainedResourceEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class KubeNexusDatabase : RoomDatabase() {

    abstract fun clusterDao(): ClusterDao
    abstract fun podDao(): PodDao
    abstract fun namespaceDao(): NamespaceDao
    abstract fun apiResourceDao(): APIResourceDao
    abstract fun explainedResourceDao(): ExplainedResourceDao

    companion object {
        const val DATABASE_NAME = "kubenexus.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Preserves existing database structure across schema upgrades
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `api_resources` (
                        `id` TEXT NOT NULL,
                        `clusterId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `singularName` TEXT NOT NULL,
                        `namespaced` INTEGER NOT NULL,
                        `kind` TEXT NOT NULL,
                        `group` TEXT NOT NULL,
                        `version` TEXT NOT NULL,
                        `groupVersion` TEXT NOT NULL,
                        `verbs` TEXT NOT NULL,
                        `shortNames` TEXT NOT NULL,
                        `categories` TEXT NOT NULL,
                        `lastUpdated` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_api_resources_clusterId` ON `api_resources` (`clusterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_api_resources_clusterId_name` ON `api_resources` (`clusterId`, `name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_api_resources_clusterId_groupVersion_name` ON `api_resources` (`clusterId`, `groupVersion`, `name`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `explained_resources` (
                        `id` TEXT NOT NULL,
                        `clusterId` TEXT NOT NULL,
                        `resourceOrKind` TEXT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `group` TEXT NOT NULL,
                        `version` TEXT NOT NULL,
                        `groupVersion` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `fieldsJson` TEXT NOT NULL,
                        `lastUpdated` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_explained_resources_clusterId` ON `explained_resources` (`clusterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_explained_resources_clusterId_resourceOrKind` ON `explained_resources` (`clusterId`, `resourceOrKind`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_explained_resources_clusterId_resourceOrKind_groupVersion` ON `explained_resources` (`clusterId`, `resourceOrKind`, `groupVersion`)")
            }
        }

        @Volatile
        private var INSTANCE: KubeNexusDatabase? = null

        fun getInstance(context: Context): KubeNexusDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KubeNexusDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
