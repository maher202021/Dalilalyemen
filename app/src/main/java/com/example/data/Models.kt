package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Entities ---

@Entity(tableName = "service_providers")
data class ServiceProvider(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val category: String, // سباك, كهربائي, دهان, نجار, حداد, تكييف, نقل, تنظيف
    val city: String, // صنعاء, عدن, تعز, الحديدة, حضرموت, إب, مأرب
    val rating: Float,
    val reviewCount: Int,
    val isVip: Boolean,
    val isVerified: Boolean,
    val isRecommended: Boolean = false,
    val imageUrl: String = "",
    val points: Int = 0,
    val status: String = "نشط", // نشط, قيد الانتظار, مرفوض
    val description: String = "",
    val latitude: Double = 15.3694,
    val longitude: Double = 44.1910,
    val lastActive: Long = System.currentTimeMillis(),
    val completionCount: Int = 0,
    val workHours: String = "8:00 ص - 10:00 م",
    val isAvailable247: Boolean = false
)

@Entity(tableName = "ad_banners")
data class AdBanner(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val imageUrl: String = "",
    val description: String = ""
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderName: String,
    val senderRole: String, // user, provider, admin, gemini
    val providerId: Int, // Chat session with a specific provider
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSentByMe: Boolean = true
)

@Entity(tableName = "notification_logs")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String,
    val target: String, // all, providers, users
    val timestamp: Long = System.currentTimeMillis()
)

// --- Room Database Integration ---

@Dao
interface YemenGuideDao {
    @Query("SELECT * FROM service_providers WHERE status = 'نشط' ORDER BY isVip DESC, rating DESC")
    fun getActiveProvidersStream(): Flow<List<ServiceProvider>>

    @Query("SELECT * FROM service_providers ORDER BY id DESC")
    fun getAllProvidersStream(): Flow<List<ServiceProvider>>

    @Query("SELECT * FROM service_providers WHERE id = :id")
    suspend fun getProviderById(id: Int): ServiceProvider?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ServiceProvider): Long

    @Update
    suspend fun updateProvider(provider: ServiceProvider)

    @Delete
    suspend fun deleteProvider(provider: ServiceProvider)

    @Query("SELECT * FROM ad_banners ORDER BY id DESC")
    fun getAllBannersStream(): Flow<List<AdBanner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanner(banner: AdBanner): Long

    @Query("DELETE FROM ad_banners WHERE id = :id")
    suspend fun deleteBannerById(id: Int)

    @Query("SELECT * FROM chat_messages WHERE providerId = :providerId ORDER BY timestamp ASC")
    fun getChatMessagesForProvider(providerId: Int): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages WHERE providerId = :providerId")
    suspend fun deleteMessagesByProviderId(providerId: Int)

    @Query("SELECT DISTINCT providerId, senderName FROM chat_messages ORDER BY timestamp DESC")
    fun getRecentChats(): Flow<List<RecentChatInfo>>

    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC")
    fun getAllNotificationsStream(): Flow<List<NotificationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationLog(log: NotificationLog): Long
}

// Simple tuple class to show active discussions in admin pane
data class RecentChatInfo(
    val providerId: Int,
    val senderName: String
)

@Database(
    entities = [ServiceProvider::class, AdBanner::class, ChatMessage::class, NotificationLog::class],
    version = 3,
    exportSchema = false
)
abstract class YemenGuideDatabase : RoomDatabase() {
    abstract fun dao(): YemenGuideDao
}
