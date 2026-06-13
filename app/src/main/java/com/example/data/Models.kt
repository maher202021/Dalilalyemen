package com.example.data

// Clean data models (fully migrated off Room Database as requested)

data class ServiceProvider(
    val id: Int = 0,
    val name: String,
    val phone: String,
    val category: String, // سباك, كهربائي, تكييف, دهان, نجار, حداد, نقل اثاث, تنظيف, رعاية صحية, خدمات قانونية
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

data class AdBanner(
    val id: Int = 0,
    val title: String,
    val imageUrl: String = "",
    val description: String = "",
    val categoryRedirect: String = "", // Department or Category to redirect to (e.g. ac, plumbing)
    val mediaType: String = "صورة", // صورة, فيديو, نص ترويجي
    val displayDuration: Int = 5, // Display duration in seconds
    val adSize: Int = 10
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val senderName: String,
    val senderRole: String, // user, provider, admin, gemini
    val providerId: Int, // Chat session with a specific provider (0 for Admin)
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSentByMe: Boolean = true
)

data class NotificationLog(
    val id: Int = 0,
    val title: String,
    val body: String,
    val target: String, // all, providers, users
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "عام" // عام, تنبيه, عرض ترويجي
)

data class RecentChatInfo(
    val providerId: Int,
    val senderName: String,
    val lastMessage: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Booking(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userName: String,
    val providerId: Int,
    val providerName: String,
    val serviceCategory: String,
    val date: String,
    val time: String,
    val notes: String = "",
    val status: String = "قيد الانتظار", // قيد الانتظار, مقبول, مرفوض, مكتمل
    val timestamp: Long = System.currentTimeMillis()
)

data class RegistrationCondition(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isRequired: Boolean = true
)
