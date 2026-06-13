package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class YemenGuideRepository(context: Context) {
    
    private val db: YemenGuideDatabase = Room.databaseBuilder(
        context.applicationContext,
        YemenGuideDatabase::class.java,
        "yemen_guide_db"
    )
    .fallbackToDestructiveMigration()
    .build()

    private val dao = db.dao()

    init {
        // Pre-populate data on a IO Coroutine if database has no providers
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val existing = dao.getAllProvidersStream().first()
                if (existing.isEmpty()) {
                    prePopulateData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Stream Services ---
    val activeProviders: Flow<List<ServiceProvider>> = dao.getActiveProvidersStream()
    val allProviders: Flow<List<ServiceProvider>> = dao.getAllProvidersStream()
    val banners: Flow<List<AdBanner>> = dao.getAllBannersStream()
    val notifications: Flow<List<NotificationLog>> = dao.getAllNotificationsStream()
    val recentChats: Flow<List<RecentChatInfo>> = dao.getRecentChats()

    // --- Providers CRUD ---
    suspend fun getProviderById(id: Int): ServiceProvider? = withContext(Dispatchers.IO) {
        dao.getProviderById(id)
    }

    suspend fun insertProvider(provider: ServiceProvider): Long = withContext(Dispatchers.IO) {
        dao.insertProvider(provider)
    }

    suspend fun updateProvider(provider: ServiceProvider) = withContext(Dispatchers.IO) {
        dao.updateProvider(provider)
    }

    suspend fun deleteProvider(provider: ServiceProvider) = withContext(Dispatchers.IO) {
        dao.deleteProvider(provider)
    }

    // --- Banners ---
    suspend fun insertBanner(banner: AdBanner): Long = withContext(Dispatchers.IO) {
        dao.insertBanner(banner)
    }

    suspend fun deleteBannerById(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteBannerById(id)
    }

    // --- Chat Messages ---
    fun getChatMessagesFlow(providerId: Int): Flow<List<ChatMessage>> {
        return dao.getChatMessagesForProvider(providerId)
    }

    suspend fun sendChatMessage(message: ChatMessage): Long = withContext(Dispatchers.IO) {
        dao.insertChatMessage(message)
    }

    suspend fun deleteMessagesByProviderId(providerId: Int) = withContext(Dispatchers.IO) {
        dao.deleteMessagesByProviderId(providerId)
    }

    // --- Notifications Log ---
    suspend fun logNotification(title: String, body: String, target: String) = withContext(Dispatchers.IO) {
        dao.insertNotificationLog(NotificationLog(title = title, body = body, target = target))
    }

    // --- Database Population Helper ---
    private suspend fun prePopulateData() {
        // Core Yemeni service providers across cities
        val initialProviders = listOf(
            ServiceProvider(
                name = "ماهر الوصابي",
                phone = "777644670",
                category = "سباك",
                city = "صنعاء",
                rating = 4.9f,
                reviewCount = 37,
                isVip = true,
                isVerified = true,
                isRecommended = true,
                description = "خبرة أكثر من ١٢ عاماً في تأسيس وصيانة شبكات المياه والمجاري وكشف التسريبات بأحدث الأجهزة الإلكترونية في أمانة العاصمة.",
                imageUrl = "art_plumber",
                latitude = 15.3694,
                longitude = 44.1910,
                completionCount = 142,
                workHours = "على مدار الساعة",
                isAvailable247 = true
            ),
            ServiceProvider(
                name = "أحمد المطري",
                phone = "777123456",
                category = "كهربائي",
                city = "صنعاء",
                rating = 4.8f,
                reviewCount = 29,
                isVip = true,
                isVerified = true,
                isRecommended = true,
                description = "كهربائي منازل ومحلات تجارية متكامل، تمديد خطوط طاقة شمسية وصيانة لوحات التوزيع الذكية ومولدات الكهرباء.",
                imageUrl = "art_electrician",
                latitude = 15.3850,
                longitude = 44.2050,
                completionCount = 98,
                workHours = "7:00 ص - 11:00 م",
                isAvailable247 = false
            ),
            ServiceProvider(
                name = "عمر السنيدار",
                phone = "736462700",
                category = "تكييف",
                city = "عدن",
                rating = 4.7f,
                reviewCount = 52,
                isVip = true,
                isVerified = true,
                isRecommended = false,
                description = "صيانة وتنظيف وفك وتركيب جميع أنواع المكيفات (سبلت، دولابي، شباك) بأسعار مناسبة وضمانة عمل حقيقية في عدن وضواحيها.",
                imageUrl = "art_ac",
                latitude = 12.8000,
                longitude = 45.0333,
                completionCount = 210,
                workHours = "على مدار الساعة",
                isAvailable247 = true
            ),
            ServiceProvider(
                name = "خالد الهمداني",
                phone = "711223344",
                category = "دهان",
                city = "تعز",
                rating = 4.6f,
                reviewCount = 18,
                isVip = false,
                isVerified = true,
                isRecommended = true,
                description = "ديكورات داخلية وخارجية، دهان جدران حديث، ورق حائط، جبس بورد، صب كراكليه وتخطيط إبداعي بأرقى المواد.",
                imageUrl = "art_painter",
                latitude = 13.5833,
                longitude = 44.0167,
                completionCount = 45,
                workHours = "8:00 ص - 8:00 م",
                isAvailable247 = false
            ),
            ServiceProvider(
                name = "علي الشرفي",
                phone = "775566778",
                category = "نجار",
                city = "الحديدة",
                rating = 4.5f,
                reviewCount = 15,
                isVip = false,
                isVerified = true,
                isRecommended = false,
                description = "تفصيل وصيانة أبواب، مطابخ، غرف نوم، غرف أطفال، وتعديل الأثاث الخشبي بجودة يمنية أصيلة تضاهي المستورد.",
                imageUrl = "art_carpenter",
                latitude = 14.8000,
                longitude = 42.9500,
                completionCount = 31,
                workHours = "8:00 ص - 6:00 م",
                isAvailable247 = false
            ),
            ServiceProvider(
                name = "يحيى الكبسي",
                phone = "770011223",
                category = "نقل اثاث",
                city = "صنعاء",
                rating = 4.9f,
                reviewCount = 63,
                isVip = true,
                isVerified = true,
                isRecommended = true,
                description = "شركة الكبسي لنقل وتغليف الأثاث الخشبي والزجاجي بأيدي عمالة يمنية ماهرة وسيارات مخصصة مغلقة لجميع المحافظات.",
                imageUrl = "art_moving",
                latitude = 15.3500,
                longitude = 44.1800,
                completionCount = 180,
                workHours = "على مدار الساعة",
                isAvailable247 = true
            ),
            ServiceProvider(
                name = "فؤاد الآنسي",
                phone = "733445566",
                category = "تنظيف",
                city = "إب",
                rating = 4.4f,
                reviewCount = 21,
                isVip = false,
                isVerified = false,
                isRecommended = false,
                description = "تنظيف فلل وشقق ومكاتب وغسيل سجاد ومجالس بالبخار ومكافحة حشرات في إب الخضراء.",
                imageUrl = "art_cleaning",
                latitude = 13.9667,
                longitude = 44.1833,
                completionCount = 19,
                workHours = "9:00 ص - 9:00 م",
                isAvailable247 = false
            ),
            ServiceProvider(
                name = "صدام الحاوري",
                phone = "777443322",
                category = "حداد",
                city = "مأرب",
                rating = 4.7f,
                reviewCount = 12,
                isVip = false,
                isVerified = true,
                isRecommended = false,
                description = "تفصيل وتركيب أبواب حديد أمان، حمايات نوافذ، مظلات حدائق وهناجر ومقاولات حدادة بمواصفات عالية في مأرب.",
                imageUrl = "art_blacksmith",
                latitude = 15.4500,
                longitude = 45.3167,
                completionCount = 12,
                workHours = "8:00 ص - 10:00 م",
                isAvailable247 = false
            )
        )

        initialProviders.forEach { dao.insertProvider(it) }

        // Initial banners
        val initialBanners = listOf(
            AdBanner(
                title = "العرض الذهبي لصيف ٢٠٢٦",
                description = "خصومات تصل إلى ٢٥٪ على صيانة التكييف في عدن والحديدة عبر مقدمينا المعتمدين."
            ),
            AdBanner(
                title = "خدمات النخبة VIP",
                description = "ابحث عن الشعار الذهبي للتعامل مع الفنيين الأكثر كفاءة وتقييماً في اليمن."
            ),
            AdBanner(
                title = "سجل كمقدم خدمة الآن",
                description = "انضم إلى أكثر من ١٠,٠٠٠ فني متميز واجعل أعمالك تنتشر في كافة المحافظات اليمنية."
            )
        )

        initialBanners.forEach { dao.insertBanner(it) }

        // Initial notification log
        dao.insertNotificationLog(
            NotificationLog(
                title = "مرحباً بكم في الدليل اليمني",
                body = "تطبيقكم الشامل لتصفح وطلب جميع الخدمات المحلية والمهنية من أي مكان في اليمن.",
                target = "all"
            )
        )

        // Seed some first chats to make conversation history visible
        dao.insertChatMessage(
            ChatMessage(
                senderName = "ماهر الوصابي",
                senderRole = "provider",
                providerId = 1,
                messageText = "مرحباً بك يا غالي، أنا سباك ماهر الوصابي. كيف يمكنني خدمتك اليوم بخصوص صيانة السباكة؟",
                isSentByMe = false
            )
        )
    }
}
