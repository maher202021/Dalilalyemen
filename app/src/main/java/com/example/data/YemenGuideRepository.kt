package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.NetworkState
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class YemenGuideRepository(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()

    // Stateful reactive flows with instant values
    val activeProviders = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val allProviders = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val banners = MutableStateFlow<List<AdBanner>>(emptyList())
    val notifications = MutableStateFlow<List<NotificationLog>>(emptyList())
    val recentChats = MutableStateFlow<List<RecentChatInfo>>(emptyList())
    val bookings = MutableStateFlow<List<Booking>>(emptyList())
    val registrationConditions = MutableStateFlow<List<RegistrationCondition>>(emptyList())

    // Direct active registrations for Firebase Firestore to strictly avoid listener leaks
    private var activeProvidersReg: ListenerRegistration? = null
    private var bannersReg: ListenerRegistration? = null
    private var notificationsReg: ListenerRegistration? = null
    private var bookingsReg: ListenerRegistration? = null
    private var conditionsReg: ListenerRegistration? = null
    private var activeChatReg: ListenerRegistration? = null

    // Stateful stream of current chat messages
    private val _activeChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val activeChatMessages: StateFlow<List<ChatMessage>> = _activeChatMessages.asStateFlow()

    init {
        // Load initial data seeded if cache file has not been built yet
        val docs = firestore.getDocuments("service_providers")
        if (docs.isEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                prePopulateData()
                subscribeToAll()
            }
        } else {
            subscribeToAll()
        }

        // Network connection listener to force re-subscribing safely
        NetworkState.addListener { isOnline ->
            if (isOnline) {
                Log.d("FirestoreRepository", "Network re-connected. Re-initializing listeners.")
                subscribeToAll()
            }
        }
    }

    // Explicitly unsubscribe old listeners and recreate snapshot listeners (as requested in Section 1.أ / 1.ب)
    fun subscribeToAll() {
        // A. Remove existing listeners explicitly
        activeProvidersReg?.remove()
        bannersReg?.remove()
        notificationsReg?.remove()
        bookingsReg?.remove()
        conditionsReg?.remove()

        Log.d("FirestoreRepository", "Removed old snapshot listeners.")

        // B. Load latest data using .get() for immediate page load (offline speed, section 1.ج)
        loadCachedData()

        // C. Re-establish snapshot listeners for real-time online updates
        activeProvidersReg = firestore.collection("service_providers").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                val list = parseProviders(it)
                allProviders.value = list
                activeProviders.value = list.filter { it.status == "نشط" }
                updateRecentChats(list)
                saveCache()
            }
        }

        bannersReg = firestore.collection("ad_banners").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                banners.value = parseBanners(it)
                saveCache()
            }
        }

        notificationsReg = firestore.collection("notification_logs").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                notifications.value = parseNotifications(it)
                saveCache()
            }
        }

        bookingsReg = firestore.collection("bookings").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                bookings.value = parseBookings(it)
                saveCache()
            }
        }

        conditionsReg = firestore.collection("registration_conditions").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                registrationConditions.value = parseConditions(it)
                saveCache()
            }
        }

        Log.d("FirestoreRepository", "Re-subscribed successfully to snapshot listeners.")
    }

    private fun loadCachedData() {
        // Immediate get for providers
        firestore.collection("service_providers").get { snapshot, _ ->
            snapshot?.let {
                val list = parseProviders(it)
                allProviders.value = list
                activeProviders.value = list.filter { it.status == "نشط" }
                updateRecentChats(list)
            }
        }

        // Immediate get for banners
        firestore.collection("ad_banners").get { snapshot, _ ->
            snapshot?.let { banners.value = parseBanners(it) }
        }

        // Immediate get for notifications logs
        firestore.collection("notification_logs").get { snapshot, _ ->
            snapshot?.let { notifications.value = parseNotifications(it) }
        }

        // Immediate get for bookings
        firestore.collection("bookings").get { snapshot, _ ->
            snapshot?.let { bookings.value = parseBookings(it) }
        }

        // Immediate get for registration conditions
        firestore.collection("registration_conditions").get { snapshot, _ ->
            snapshot?.let { registrationConditions.value = parseConditions(it) }
        }
    }

    private fun saveCache() {
        firestore.saveOfflinePersistence(context)
    }

    // Parse helpers
    private fun parseProviders(snapshot: QuerySnapshot): List<ServiceProvider> {
        return snapshot.documents.map { doc ->
            ServiceProvider(
                id = doc.id.toIntOrNull() ?: doc.getLong("id")?.toInt() ?: 0,
                name = doc.getString("name") ?: "",
                phone = doc.getString("phone") ?: "",
                category = doc.getString("category") ?: "",
                city = doc.getString("city") ?: "",
                rating = doc.getDouble("rating")?.toFloat() ?: doc.getDouble("rating")?.toFloat() ?: 5f,
                reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0,
                isVip = doc.getBoolean("isVip") ?: false,
                isVerified = doc.getBoolean("isVerified") ?: false,
                isRecommended = doc.getBoolean("isRecommended") ?: false,
                imageUrl = doc.getString("imageUrl") ?: "",
                points = doc.getLong("points")?.toInt() ?: 0,
                status = doc.getString("status") ?: "نشط",
                description = doc.getString("description") ?: "",
                latitude = doc.getDouble("latitude") ?: 15.3694,
                longitude = doc.getDouble("longitude") ?: 44.1910,
                lastActive = doc.getLong("lastActive") ?: System.currentTimeMillis(),
                completionCount = doc.getLong("completionCount")?.toInt() ?: 0,
                workHours = doc.getString("workHours") ?: "8:00 ص - 10:00 م",
                isAvailable247 = doc.getBoolean("isAvailable247") ?: false
            )
        }.sortedBy { it.id }
    }

    private fun parseBanners(snapshot: QuerySnapshot): List<AdBanner> {
        return snapshot.documents.map { doc ->
            AdBanner(
                id = doc.id.toIntOrNull() ?: doc.getLong("id")?.toInt() ?: 0,
                title = doc.getString("title") ?: "",
                imageUrl = doc.getString("imageUrl") ?: "",
                description = doc.getString("description") ?: "",
                categoryRedirect = doc.getString("categoryRedirect") ?: "",
                mediaType = doc.getString("mediaType") ?: "صورة",
                displayDuration = doc.getLong("displayDuration")?.toInt() ?: 5,
                adSize = doc.getLong("adSize")?.toInt() ?: 10
            )
        }.sortedBy { it.id }
    }

    private fun parseNotifications(snapshot: QuerySnapshot): List<NotificationLog> {
        return snapshot.documents.map { doc ->
            NotificationLog(
                id = doc.id.toIntOrNull() ?: doc.getLong("id")?.toInt() ?: 0,
                title = doc.getString("title") ?: "",
                body = doc.getString("body") ?: "",
                target = doc.getString("target") ?: "all",
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                category = doc.getString("category") ?: "عام"
            )
        }.sortedByDescending { it.timestamp }
    }

    private fun parseBookings(snapshot: QuerySnapshot): List<Booking> {
        return snapshot.documents.map { doc ->
            Booking(
                id = doc.id,
                userName = doc.getString("userName") ?: "",
                providerId = doc.getLong("providerId")?.toInt() ?: 0,
                providerName = doc.getString("providerName") ?: "",
                serviceCategory = doc.getString("serviceCategory") ?: "",
                date = doc.getString("date") ?: "",
                time = doc.getString("time") ?: "",
                notes = doc.getString("notes") ?: "",
                status = doc.getString("status") ?: "قيد الانتظار",
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
            )
        }.sortedByDescending { it.timestamp }
    }

    private fun parseConditions(snapshot: QuerySnapshot): List<RegistrationCondition> {
        return snapshot.documents.map { doc ->
            RegistrationCondition(
                id = doc.id,
                text = doc.getString("text") ?: "",
                isRequired = doc.getBoolean("isRequired") ?: true
            )
        }
    }

    // Listen to chat messages for an active provider/session
    fun activateChatSessionListener(providerId: Int) {
        activeChatReg?.remove()
        
        // Immediate load
        firestore.collection("chat_messages").get { snapshot, _ ->
            snapshot?.let { snap ->
                val allMsgs = parseMessages(snap)
                _activeChatMessages.value = allMsgs.filter { it.providerId == providerId }
            }
        }

        // Realtime update
        activeChatReg = firestore.collection("chat_messages").addSnapshotListener { snapshot, _ ->
            snapshot?.let { snap ->
                val allMsgs = parseMessages(snap)
                _activeChatMessages.value = allMsgs.filter { it.providerId == providerId }
                // Also update recent chats info
                updateRecentChats(allProviders.value)
            }
        }
    }

    fun deactivateChatSessionListener() {
        activeChatReg?.remove()
        activeChatReg = null
        _activeChatMessages.value = emptyList()
    }

    private fun parseMessages(snapshot: QuerySnapshot): List<ChatMessage> {
        return snapshot.documents.map { doc ->
            ChatMessage(
                id = doc.id,
                senderName = doc.getString("senderName") ?: "",
                senderRole = doc.getString("senderRole") ?: "user",
                providerId = doc.getLong("providerId")?.toInt() ?: 0,
                messageText = doc.getString("messageText") ?: "",
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                isSentByMe = doc.getBoolean("isSentByMe") ?: false
            )
        }.sortedBy { it.timestamp }
    }

    private fun updateRecentChats(providersList: List<ServiceProvider>) {
        firestore.collection("chat_messages").get { snapshot, _ ->
            snapshot?.let { snap ->
                val allMsgs = parseMessages(snap)
                val chatSessions = allMsgs.groupBy { it.providerId }
                val recent = chatSessions.mapNotNull { (pid, msgs) ->
                    val lastMsgObj = msgs.lastOrNull() ?: return@mapNotNull null
                    val providerObj = providersList.find { it.id == pid }
                    RecentChatInfo(
                        providerId = pid,
                        senderName = providerObj?.name ?: "رسالة عاجلة",
                        lastMessage = lastMsgObj.messageText,
                        timestamp = lastMsgObj.timestamp
                    )
                }.sortedByDescending { it.timestamp }
                recentChats.value = recent
            }
        }
    }

    // --- WRITE OPERATIONS ---

    // Service providers CRUD mapped to Firestore Documents
    suspend fun insertProvider(provider: ServiceProvider) {
        val idStr = if (provider.id == 0) (allProviders.value.maxOfOrNull { it.id } ?: 0 + 1).toString() else provider.id.toString()
        val data = mapOf(
            "id" to idStr.toInt(),
            "name" to provider.name,
            "phone" to provider.phone,
            "category" to provider.category,
            "city" to provider.city,
            "rating" to provider.rating.toDouble(),
            "reviewCount" to provider.reviewCount,
            "isVip" to provider.isVip,
            "isVerified" to provider.isVerified,
            "isRecommended" to provider.isRecommended,
            "imageUrl" to provider.imageUrl,
            "points" to provider.points,
            "status" to provider.status,
            "description" to provider.description,
            "latitude" to provider.latitude,
            "longitude" to provider.longitude,
            "lastActive" to provider.lastActive,
            "completionCount" to provider.completionCount,
            "workHours" to provider.workHours,
            "isAvailable247" to provider.isAvailable247
        )
        firestore.collection("service_providers").document(idStr).set(data)
    }

    suspend fun updateProvider(provider: ServiceProvider) {
        insertProvider(provider)
    }

    suspend fun deleteProvider(provider: ServiceProvider) {
        firestore.collection("service_providers").document(provider.id.toString()).delete()
    }

    // Banners CRUD
    suspend fun insertBanner(banner: AdBanner) {
        val idStr = if (banner.id == 0) (banners.value.maxOfOrNull { it.id } ?: 0 + 1).toString() else banner.id.toString()
        val data = mapOf(
            "id" to idStr.toInt(),
            "title" to banner.title,
            "imageUrl" to banner.imageUrl,
            "description" to banner.description,
            "categoryRedirect" to banner.categoryRedirect,
            "mediaType" to banner.mediaType,
            "displayDuration" to banner.displayDuration,
            "adSize" to banner.adSize
        )
        firestore.collection("ad_banners").document(idStr).set(data)
    }

    suspend fun deleteBannerById(id: Int) {
        firestore.collection("ad_banners").document(id.toString()).delete()
    }

    // Chat messages CRUD
    suspend fun sendChatMessage(message: ChatMessage) {
        val data = mapOf(
            "id" to message.id,
            "senderName" to message.senderName,
            "senderRole" to message.senderRole,
            "providerId" to message.providerId,
            "messageText" to message.messageText,
            "timestamp" to message.timestamp,
            "isSentByMe" to message.isSentByMe
        )
        firestore.collection("chat_messages").document(message.id).set(data)
    }

    suspend fun deleteMessagesByProviderId(providerId: Int) {
        // Query matching chat_messages and delete them
        firestore.collection("chat_messages").get { snapshot, _ ->
            snapshot?.let { snap ->
                val matchingDocs = snap.documents.filter { doc ->
                    doc.getLong("providerId")?.toInt() == providerId
                }
                matchingDocs.forEach { doc ->
                    firestore.collection("chat_messages").document(doc.id).delete()
                }
            }
        }
    }

    suspend fun clearAllConversations() {
        firestore.clearCollection("chat_messages")
    }

    // Notifications Logs
    suspend fun logNotification(title: String, body: String, target: String, category: String = "عام") {
        val idVal = System.currentTimeMillis().toInt()
        val data = mapOf(
            "id" to idVal,
            "title" to title,
            "body" to body,
            "target" to target,
            "timestamp" to System.currentTimeMillis(),
            "category" to category
        )
        firestore.collection("notification_logs").document(idVal.toString()).set(data)
    }

    // Bookings CRUD
    suspend fun insertBooking(booking: Booking) {
        val data = mapOf(
            "id" to booking.id,
            "userName" to booking.userName,
            "providerId" to booking.providerId,
            "providerName" to booking.providerName,
            "serviceCategory" to booking.serviceCategory,
            "date" to booking.date,
            "time" to booking.time,
            "notes" to booking.notes,
            "status" to booking.status,
            "timestamp" to booking.timestamp
        )
        firestore.collection("bookings").document(booking.id).set(data)
    }

    suspend fun deleteBooking(bookingId: String) {
        firestore.collection("bookings").document(bookingId).delete()
    }

    // Conditions CRUD
    suspend fun updateRegistrationConditions(list: List<RegistrationCondition>) {
        firestore.clearCollection("registration_conditions")
        list.forEach { condition ->
            val data = mapOf(
                "id" to condition.id,
                "text" to condition.text,
                "isRequired" to condition.isRequired
            )
            firestore.collection("registration_conditions").document(condition.id).set(data)
        }
    }

    // --- PRE-POPULATE DATA ---
    private suspend fun prePopulateData() {
        Log.d("FirestoreRepository", "Seeding initial mockup database data in Firestore.")
        
        // Initial providers list
        val seedProviders = listOf(
            ServiceProvider(
                id = 1,
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
                points = 150,
                lastActive = System.currentTimeMillis() - 30000,
                completionCount = 142,
                workHours = "على مدار الساعة",
                isAvailable247 = true
            ),
            ServiceProvider(
                id = 2,
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
                points = 90,
                lastActive = System.currentTimeMillis() - 120000,
                completionCount = 98,
                workHours = "7:00 ص - 11:00 م",
                isAvailable247 = false
            ),
            ServiceProvider(
                id = 3,
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
                points = 210,
                lastActive = System.currentTimeMillis() - 10000,
                completionCount = 210,
                workHours = "على مدار الساعة",
                isAvailable247 = true
            ),
            ServiceProvider(
                id = 4,
                name = "د. سليم غانم",
                phone = "777112233",
                category = "رعاية صحية",
                city = "صنعاء",
                rating = 4.9f,
                reviewCount = 44,
                isVip = true,
                isVerified = true,
                isRecommended = true,
                description = "طبيب ممارس عام منزلي لتقديم الرعاية الطبية الفورية المنزلية، الفحوصات الأساسية، ومتابعة الأمراض المزمنة طوال اليوم.",
                imageUrl = "art_doctor",
                points = 320,
                lastActive = System.currentTimeMillis() - 15000,
                completionCount = 88,
                workHours = "8:00 ص - 10:00 م",
                isAvailable247 = false
            ),
            ServiceProvider(
                id = 5,
                name = "المحامي عادل الجلال",
                phone = "733990011",
                category = "خدمات قانونية",
                city = "عدن",
                rating = 4.8f,
                reviewCount = 19,
                isVip = false,
                isVerified = true,
                isRecommended = true,
                description = "مستشار وقانوني متخصص في القضايا المدنية والتجارية، كتابة العقود والمرافعات وتقديم الاستشارات الصائبة.",
                imageUrl = "art_lawyer",
                points = 60,
                lastActive = System.currentTimeMillis() - 400000,
                completionCount = 22,
                workHours = "9:00 ص - 5:00 م",
                isAvailable247 = false
            ),
            ServiceProvider(
                id = 6,
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
                points = 40,
                lastActive = System.currentTimeMillis() - 80000,
                completionCount = 31,
                workHours = "8:00 ص - 6:00 م",
                isAvailable247 = false
            ),
            ServiceProvider(
                id = 7,
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
                points = 180,
                lastActive = System.currentTimeMillis() - 50000,
                completionCount = 180,
                workHours = "على مدار الساعة",
                isAvailable247 = true
            )
        )

        for (p in seedProviders) {
            insertProvider(p)
        }

        // Seeding banners
        val seedBanners = listOf(
            AdBanner(
                id = 1,
                title = "العرض الذهبي لصيف ٢٠٢٦",
                description = "خصومات تصل إلى ٢٥٪ على صيانة التكييف في عدن والحديدة عبر مقدمينا المعتمدين.",
                categoryRedirect = "tكييف",
                mediaType = "صورة",
                displayDuration = 8,
                adSize = 10
            ),
            AdBanner(
                id = 2,
                title = "خدمات النخبة VIP 👑",
                description = "ابحث عن الشعار الذهبي للتعامل مع الفنيين الأكثر كفاءة وتقييماً في اليمن.",
                categoryRedirect = "all",
                mediaType = "نص ترويجي",
                displayDuration = 5,
                adSize = 8
            )
        )
        for (b in seedBanners) {
            insertBanner(b)
        }

        // Seeding default alerts
        logNotification(
            title = "أهلاً ومرحباً بكم بالدليل اليمني المحترف 🎉",
            body = "الدليل الرقمي واللوجستي الأول في المحافظات اليمنية لطلب السباكة والكهرباء والخدمات المختلفة بكل سهولة.",
            target = "all"
        )

        // Seeding initial conditions
        val seedConditions = listOf(
            RegistrationCondition(text = "امتلاك هوية شخصية أو جواز سفر ساري المفعول في اليمن", isRequired = true),
            RegistrationCondition(text = "صورة سيلفي واضحة أو صورة معبرة عن المهنة للفتاة", isRequired = true),
            RegistrationCondition(text = "رقم هاتف مفعل واستقبال اتصالات العملاء بلطف وحسن معاملة", isRequired = true),
            RegistrationCondition(text = "خبرة لا تقل عن سنتين في المجال المهني المقدم", isRequired = false)
        )
        updateRegistrationConditions(seedConditions)

        // Seeding a simple initial message
        sendChatMessage(
            ChatMessage(
                senderName = "ماهر الوصابي",
                senderRole = "provider",
                providerId = 1,
                messageText = "مرحباً بك يا غالي! تفضل اطرح مشكلة السباكة وسأجيبك بأسرع وقت وأكون جاهز لزيارتك.",
                isSentByMe = false
            )
        )

        saveCache()
    }
}
