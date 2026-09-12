package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps ORDER BY name ASC")
    fun getAllApps(): Flow<List<AppItem>>

    @Query("SELECT * FROM apps WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteApps(): Flow<List<AppItem>>

    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackage(packageName: String): AppItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppItem>)

    @Query("UPDATE apps SET isFavorite = :isFavorite WHERE packageName = :packageName")
    suspend fun updateFavorite(packageName: String, isFavorite: Boolean)

    @Query("SELECT * FROM apps WHERE name LIKE '%' || :query || '%' OR packageName LIKE '%' || :query || '%'")
    fun searchApps(query: String): Flow<List<AppItem>>

    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)

    @Query("DELETE FROM apps WHERE driveFileId NOT IN (:activeFileIds) AND driveFileId != ''")
    suspend fun deleteStaleApps(activeFileIds: List<String>)

    @Query("DELETE FROM apps")
    suspend fun clearAll()

    @Query("""
        UPDATE apps SET 
            name = :name, 
            versionName = :versionName, 
            versionCode = :versionCode, 
            size = :size, 
            downloadUrl = :downloadUrl, 
            iconUrl = :iconUrl, 
            description = :description, 
            category = :category,
            lastUpdated = :lastUpdated,
            driveModifiedTime = :driveModifiedTime,
            patchVersion = :patchVersion,
            updateCheck = :updateCheck,
            platform = :platform
        WHERE packageName = :packageName
    """)
    suspend fun updateAppDetails(
        packageName: String,
        name: String,
        versionName: String,
        versionCode: Int,
        size: Long,
        downloadUrl: String,
        iconUrl: String,
        description: String,
        category: String,
        lastUpdated: Long,
        driveModifiedTime: Long = 0L,
        patchVersion: String = "",
        updateCheck: Boolean = true,
        platform: String = "Android"
    )
}
