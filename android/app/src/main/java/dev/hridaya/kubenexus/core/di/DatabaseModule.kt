package dev.hridaya.kubenexus.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.hridaya.kubenexus.data.source.local.KubeNexusDatabase
import dev.hridaya.kubenexus.data.source.local.dao.APIResourceDao
import dev.hridaya.kubenexus.data.source.local.dao.ClusterDao
import dev.hridaya.kubenexus.data.source.local.dao.DeploymentDao
import dev.hridaya.kubenexus.data.source.local.dao.ExplainedResourceDao
import dev.hridaya.kubenexus.data.source.local.dao.NamespaceDao
import dev.hridaya.kubenexus.data.source.local.dao.OpenApiSchemaDao
import dev.hridaya.kubenexus.data.source.local.dao.PodDao
import dev.hridaya.kubenexus.data.source.local.dao.ServiceDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): KubeNexusDatabase = KubeNexusDatabase.getInstance(context)

    @Provides
    fun provideClusterDao(
        database: KubeNexusDatabase
    ): ClusterDao = database.clusterDao()

    @Provides
    fun providePodDao(
        database: KubeNexusDatabase
    ): PodDao = database.podDao()

    @Provides
    fun provideNamespaceDao(
        database: KubeNexusDatabase
    ): NamespaceDao = database.namespaceDao()

    @Provides
    fun provideApiResourceDao(
        database: KubeNexusDatabase
    ): APIResourceDao = database.apiResourceDao()

    @Provides
    fun provideExplainedResourceDao(
        database: KubeNexusDatabase
    ): ExplainedResourceDao = database.explainedResourceDao()

    @Provides
    fun provideOpenApiSchemaDao(
        database: KubeNexusDatabase
    ): OpenApiSchemaDao = database.openApiSchemaDao()

    @Provides
    fun provideDeploymentDao(
        database: KubeNexusDatabase
    ): DeploymentDao = database.deploymentDao()

    @Provides
    fun provideServiceDao(
        database: KubeNexusDatabase
    ): ServiceDao = database.serviceDao()
}
