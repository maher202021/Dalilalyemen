package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class YemenGuideViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = YemenGuideRepository(application)

    // --- Authentication & User States ---
    private val _currentUser = MutableStateFlow<UserSession>(UserSession.Guest)
    val currentUser: StateFlow<UserSession> = _currentUser.asStateFlow()

    // --- Search & Filters ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedCity = MutableStateFlow<String?>(null)
    val selectedCity: StateFlow<String?> = _selectedCity.asStateFlow()

    private val _searchEnabled = MutableStateFlow(true)
    val searchEnabled: StateFlow<Boolean> = _searchEnabled.asStateFlow()

    private val _isAutocompleteEnabled = MutableStateFlow(true)
    val isAutocompleteEnabled: StateFlow<Boolean> = _isAutocompleteEnabled.asStateFlow()

    private val _isVoiceSearchEnabled = MutableStateFlow(true)
    val isVoiceSearchEnabled: StateFlow<Boolean> = _isVoiceSearchEnabled.asStateFlow()

    private val _minRatingFilter = MutableStateFlow(0f)
    val minRatingFilter: StateFlow<Float> = _minRatingFilter.asStateFlow()

    private val _sortBy = MutableStateFlow("vip") // vip, rating, distance
    val sortBy: StateFlow<String> = _sortBy.asStateFlow()

    private val _maxDistanceFilter = MutableStateFlow<Double?>(null)
    val maxDistanceFilter: StateFlow<Double?> = _maxDistanceFilter.asStateFlow()

    // Map radius search range (control under Secret settings)
    private val _radiusSearchRange = MutableStateFlow(10) // default 10km
    val radiusSearchRange: StateFlow<Int> = _radiusSearchRange.asStateFlow()

    // Fixed User Location
    val currentUserLat = 15.3694
    val currentUserLon = 44.1910

    // --- Dynamic Identity & Icon Customizations (Synced via Simulated Firestore) ---
    private val _chatIconSize = MutableStateFlow(56)
    val chatIconSize: StateFlow<Int> = _chatIconSize.asStateFlow()

    private val _chatIconColorHex = MutableStateFlow("#CCA43B") // YemenGold
    val chatIconColorHex: StateFlow<String> = _chatIconColorHex.asStateFlow()

    private val _isChatIconHidden = MutableStateFlow(false)
    val isChatIconHidden: StateFlow<Boolean> = _isChatIconHidden.asStateFlow()

    private val _isChatIconDeleted = MutableStateFlow(false)
    val isChatIconDeleted: StateFlow<Boolean> = _isChatIconDeleted.asStateFlow()

    // Assistant customization
    private val _assistantIconSize = MutableStateFlow(56)
    val assistantIconSize: StateFlow<Int> = _assistantIconSize.asStateFlow()

    private val _assistantIconColorHex = MutableStateFlow("#10B981") // SoftEmerald
    val assistantIconColorHex: StateFlow<String> = _assistantIconColorHex.asStateFlow()

    private val _isAssistantIconHidden = MutableStateFlow(false)
    val isAssistantIconHidden: StateFlow<Boolean> = _isAssistantIconHidden.asStateFlow()

    // Booking specifications
    private val _isBookingsEnabled = MutableStateFlow(true)
    val isBookingsEnabled: StateFlow<Boolean> = _isBookingsEnabled.asStateFlow()

    private val _bookingVisibility = MutableStateFlow("الإدارة والفني") // "الإدارة والفني", "الفني فقط", "الإدارة فقط"
    val bookingVisibility: StateFlow<String> = _bookingVisibility.asStateFlow()

    // Global in-app custom notification for chat feature disablement
    private val _chatDisabledMessage = MutableStateFlow("مرحباً بك، الدردشة المباشرة متوقفة مؤقتاً بأمر الإدارة وتحديث الأنظمة. نأسف للإزعاج.")
    val chatDisabledMessage: StateFlow<String> = _chatDisabledMessage.asStateFlow()

    // App Visual Identity (Colors and Fonts)
    private val _themePrimaryColorHex = MutableStateFlow("#CCA43B") // YemenGold
    val themePrimaryColorHex: StateFlow<String> = _themePrimaryColorHex.asStateFlow()

    private val _themeSecondaryColorHex = MutableStateFlow("#10B981") // SoftEmerald
    val themeSecondaryColorHex: StateFlow<String> = _themeSecondaryColorHex.asStateFlow()

    private val _themeFontName = MutableStateFlow("Cairo") // Cairo, Amiri, Tajawal, Default
    val themeFontName: StateFlow<String> = _themeFontName.asStateFlow()

    // Temporary Database Cleanup settings
    private val _retentionDays = MutableStateFlow(30)
    val retentionDays: StateFlow<Int> = _retentionDays.asStateFlow()

    // App language State ("ar" or "en")
    private val _currentLanguage = MutableStateFlow("ar")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // Dynamic Top App Bar Configuration
    private val _topAppBarIcons = MutableStateFlow<List<TopAppBarIconConfig>>(emptyList())
    val topAppBarIcons: StateFlow<List<TopAppBarIconConfig>> = _topAppBarIcons.asStateFlow()

    // --- Streams ---
    val banners: StateFlow<List<AdBanner>> = repository.banners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationLog>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProviders: StateFlow<List<ServiceProvider>> = repository.allProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentChats: StateFlow<List<RecentChatInfo>> = repository.recentChats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookings: StateFlow<List<Booking>> = repository.bookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val registrationConditions: StateFlow<List<RegistrationCondition>> = repository.registrationConditions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filterPart1 = combine(_searchQuery, _selectedCategory, _selectedCity) { query, cat, city ->
        Triple(query, cat, city)
    }

    private val filterFlow = combine(
        filterPart1,
        _minRatingFilter,
        _maxDistanceFilter,
        _sortBy
    ) { part1, minRating, maxDistance, sort ->
        FilterState(
            query = part1.first,
            cat = part1.second,
            city = part1.third,
            minRating = minRating,
            maxDistance = maxDistance,
            sort = sort
        )
    }

    val activeProviders: StateFlow<List<ServiceProvider>> = combine(
        repository.activeProviders,
        filterFlow
    ) { providers, filter ->
        val filtered = providers.filter { provider ->
            val matchesQuery = filter.query.isEmpty() || 
                    provider.name.contains(filter.query, ignoreCase = true) ||
                    provider.description.contains(filter.query, ignoreCase = true)
            val matchesCat = filter.cat == null || provider.category == filter.cat
            val matchesCity = filter.city == null || provider.city == filter.city
            val matchesRating = provider.rating >= filter.minRating
            
            // Calculate distance
            val distance = calculateDistance(currentUserLat, currentUserLon, provider.latitude, provider.longitude)
            val matchesDistance = filter.maxDistance == null || distance <= filter.maxDistance
            
            matchesQuery && matchesCat && matchesCity && matchesRating && matchesDistance
        }
        
        when (filter.sort) {
            "rating" -> filtered.sortedByDescending { it.rating }
            "distance" -> filtered.sortedBy { calculateDistance(currentUserLat, currentUserLon, it.latitude, it.longitude) }
            else -> filtered.sortedWith(compareByDescending<ServiceProvider> { it.isVip }.thenByDescending { it.rating })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Chat Messages ---
    private val _activeChatProviderId = MutableStateFlow<Int?>(null)
    val activeChatProviderId: StateFlow<Int?> = _activeChatProviderId.asStateFlow()

    val activeChatMessages: StateFlow<List<ChatMessage>> = repository.activeChatMessages

    // --- Assistant Smart Conversation State ---
    private val _assistantChat = MutableStateFlow<List<AssistantMessage>>(listOf(
        AssistantMessage("مرحباً بك يا غالي في الدليل اليمني! أنا مساعدك الذكي 🤖، كيف بقدر أخدمك اليوم؟ يمكنك سؤالي عن السباكين، الكهربائيين، الفنيين أو كيف تسجل معنا في التطبيق.", false)
    ))
    val assistantChat: StateFlow<List<AssistantMessage>> = _assistantChat.asStateFlow()

    private val _assistantLoading = MutableStateFlow(false)
    val assistantLoading: StateFlow<Boolean> = _assistantLoading.asStateFlow()

    // --- Provider Registration Temporary State ---
    private val _compressionStatus = MutableStateFlow<String?>(null)
    val compressionStatus: StateFlow<String?> = _compressionStatus.asStateFlow()

    private val _registrationSuccess = MutableStateFlow(false)
    val registrationSuccess: StateFlow<Boolean> = _registrationSuccess.asStateFlow()

    init {
        // Fetch and load initial values for identity and configurations from Firestore
        loadIdentitySettingsFromFirestore()
    }

    private fun loadIdentitySettingsFromFirestore() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("settings").document("identity").get { doc, _ ->
            if (doc != null && doc.exists()) {
                _chatIconSize.value = doc.getLong("chatIconSize")?.toInt() ?: 56
                _chatIconColorHex.value = doc.getString("chatIconColorHex") ?: "#CCA43B"
                _isChatIconHidden.value = doc.getBoolean("isChatIconHidden") ?: false
                _isChatIconDeleted.value = doc.getBoolean("isChatIconDeleted") ?: false

                _assistantIconSize.value = doc.getLong("assistantIconSize")?.toInt() ?: 56
                _assistantIconColorHex.value = doc.getString("assistantIconColorHex") ?: "#10B981"
                _isAssistantIconHidden.value = doc.getBoolean("isAssistantIconHidden") ?: false

                _isBookingsEnabled.value = doc.getBoolean("isBookingsEnabled") ?: true
                _bookingVisibility.value = doc.getString("bookingVisibility") ?: "الإدارة والفني"

                _chatDisabledMessage.value = doc.getString("chatDisabledMessage") ?: "الدردشة المباشرة متوقفة مؤقتاً بأمر الإدارة."

                _themePrimaryColorHex.value = doc.getString("themePrimaryColorHex") ?: "#CCA43B"
                _themeSecondaryColorHex.value = doc.getString("themeSecondaryColorHex") ?: "#10B981"
                _themeFontName.value = doc.getString("themeFontName") ?: "Cairo"

                _radiusSearchRange.value = doc.getLong("radiusSearchRange")?.toInt() ?: 10
                _retentionDays.value = doc.getLong("retentionDays")?.toInt() ?: 30

                val iconsList = doc.getData()?.get("topAppBarIcons") as? List<Map<String, Any>>
                if (iconsList != null) {
                    _topAppBarIcons.value = iconsList.map { map ->
                        TopAppBarIconConfig(
                            id = map["id"] as? String ?: "",
                            nameAr = map["nameAr"] as? String ?: "",
                            nameEn = map["nameEn"] as? String ?: "",
                            defaultIcon = map["defaultIcon"] as? String ?: "🏠",
                            isVisible = map["isVisible"] as? Boolean ?: true,
                            order = (map["order"] as? Long)?.toInt() ?: 0
                        )
                    }.sortedBy { it.order }
                } else {
                    _topAppBarIcons.value = listOf(
                        TopAppBarIconConfig("home", "الرئيسية", "Home", "🏠", true, 0),
                        TopAppBarIconConfig("login", "تسجيل الدخول", "Login", "🔐", true, 1),
                        TopAppBarIconConfig("register", "إنشاء حساب مقدم خدمة", "Register Provider", "👤", true, 2),
                        TopAppBarIconConfig("language", "تبديل اللغة", "Switch Language", "🌐", true, 3),
                        TopAppBarIconConfig("refresh", "تحديث الصفحة والبيانات", "Refresh Screen & Data", "🔄", true, 4)
                    )
                }
            } else {
                _topAppBarIcons.value = listOf(
                    TopAppBarIconConfig("home", "الرئيسية", "Home", "🏠", true, 0),
                    TopAppBarIconConfig("login", "تسجيل الدخول", "Login", "🔐", true, 1),
                    TopAppBarIconConfig("register", "إنشاء حساب مقدم خدمة", "Register Provider", "👤", true, 2),
                    TopAppBarIconConfig("language", "تبديل اللغة", "Switch Language", "🌐", true, 3),
                    TopAppBarIconConfig("refresh", "تحديث الصفحة والبيانات", "Refresh Screen & Data", "🔄", true, 4)
                )
            }
        }
    }

    private fun saveIdentitySettingsToFirestore() {
        val firestore = FirebaseFirestore.getInstance()
        val data = mapOf(
            "chatIconSize" to _chatIconSize.value,
            "chatIconColorHex" to _chatIconColorHex.value,
            "isChatIconHidden" to _isChatIconHidden.value,
            "isChatIconDeleted" to _isChatIconDeleted.value,
            "assistantIconSize" to _assistantIconSize.value,
            "assistantIconColorHex" to _assistantIconColorHex.value,
            "isAssistantIconHidden" to _isAssistantIconHidden.value,
            "isBookingsEnabled" to _isBookingsEnabled.value,
            "bookingVisibility" to _bookingVisibility.value,
            "chatDisabledMessage" to _chatDisabledMessage.value,
            "themePrimaryColorHex" to _themePrimaryColorHex.value,
            "themeSecondaryColorHex" to _themeSecondaryColorHex.value,
            "themeFontName" to _themeFontName.value,
            "radiusSearchRange" to _radiusSearchRange.value,
            "retentionDays" to _retentionDays.value,
            "topAppBarIcons" to _topAppBarIcons.value.map { icon ->
                mapOf(
                    "id" to icon.id,
                    "nameAr" to icon.nameAr,
                    "nameEn" to icon.nameEn,
                    "defaultIcon" to icon.defaultIcon,
                    "isVisible" to icon.isVisible,
                    "order" to icon.order
                )
            }
        )
        firestore.collection("settings").document("identity").set(data)
    }

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == "ar") "en" else "ar"
    }

    fun updateTopAppBarIcons(icons: List<TopAppBarIconConfig>) {
        _topAppBarIcons.value = icons.sortedBy { it.order }
        saveIdentitySettingsToFirestore()
    }

    fun toggleTopAppBarIconVisibility(id: String) {
        val updated = _topAppBarIcons.value.map { icon ->
            if (icon.id == id) icon.copy(isVisible = !icon.isVisible) else icon
        }
        updateTopAppBarIcons(updated)
    }

    fun moveTopAppBarIconUp(id: String) {
        val list = _topAppBarIcons.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index > 0) {
            val current = list[index]
            val prev = list[index - 1]
            list[index] = prev.copy(order = current.order)
            list[index - 1] = current.copy(order = prev.order)
            updateTopAppBarIcons(list)
        }
    }

    fun moveTopAppBarIconDown(id: String) {
        val list = _topAppBarIcons.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0 && index < list.size - 1) {
            val current = list[index]
            val next = list[index + 1]
            list[index] = next.copy(order = current.order)
            list[index + 1] = current.copy(order = next.order)
            updateTopAppBarIcons(list)
        }
    }

    fun addTopAppBarIcon(id: String, nameAr: String, nameEn: String, icon: String, visible: Boolean) {
        val currentList = _topAppBarIcons.value
        val nextOrder = (currentList.maxOfOrNull { it.order } ?: -1) + 1
        val newIcon = TopAppBarIconConfig(id, nameAr, nameEn, icon, visible, nextOrder)
        val updated = currentList + newIcon
        updateTopAppBarIcons(updated)
    }

    fun deleteTopAppBarIcon(id: String) {
        val updated = _topAppBarIcons.value.filter { it.id != id }.mapIndexed { idx, icon ->
            icon.copy(order = idx)
        }
        updateTopAppBarIcons(updated)
    }

    // --- UPDATE SYSTEM SPECIFICATIONS CONTROLS ---

    fun updateChatIconSpecs(size: Int, colorHex: String, hidden: Boolean, deleted: Boolean) {
        _chatIconSize.value = size
        _chatIconColorHex.value = colorHex
        _isChatIconHidden.value = hidden
        _isChatIconDeleted.value = deleted
        saveIdentitySettingsToFirestore()
    }

    fun updateAssistantIconSpecs(size: Int, colorHex: String, hidden: Boolean) {
        _assistantIconSize.value = size
        _assistantIconColorHex.value = colorHex
        _isAssistantIconHidden.value = hidden
        saveIdentitySettingsToFirestore()
    }

    fun updateBookingSettings(enabled: Boolean, visibility: String) {
        _isBookingsEnabled.value = enabled
        _bookingVisibility.value = visibility
        saveIdentitySettingsToFirestore()
    }

    fun updateChatDisabledMessage(msg: String) {
        _chatDisabledMessage.value = msg
        saveIdentitySettingsToFirestore()
    }

    fun updateAppThemeSettings(primaryHex: String, secondaryHex: String, font: String) {
        _themePrimaryColorHex.value = primaryHex
        _themeSecondaryColorHex.value = secondaryHex
        _themeFontName.value = font
        saveIdentitySettingsToFirestore()
    }

    fun setRadiusSearchRange(km: Int) {
        _radiusSearchRange.value = km
        saveIdentitySettingsToFirestore()
    }

    fun setRetentionPeriod(days: Int) {
        _retentionDays.value = days
        saveIdentitySettingsToFirestore()
    }

    // --- Set Search Logic ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun setCityFilter(city: String?) {
        _selectedCity.value = if (_selectedCity.value == city) null else city
    }

    fun setSearchEnabled(enabled: Boolean) {
        _searchEnabled.value = enabled
    }

    fun setAutocompleteEnabled(enabled: Boolean) {
        _isAutocompleteEnabled.value = enabled
    }

    fun setVoiceSearchEnabled(enabled: Boolean) {
        _isVoiceSearchEnabled.value = enabled
    }

    fun setMinRatingFilter(rating: Float) {
        _minRatingFilter.value = rating
    }

    fun setSortBy(sort: String) {
        _sortBy.value = sort
    }

    fun setMaxDistanceFilter(distance: Double?) {
        _maxDistanceFilter.value = distance
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val theta = lon1 - lon2
        var dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta))
        dist = Math.acos(dist)
        dist = Math.toDegrees(dist)
        dist = dist * 60 * 1.1515 * 1.609344 // in kilometers
        return if (dist.isNaN()) 0.0 else dist
    }

    fun deleteConversation(providerId: Int) {
        viewModelScope.launch {
            repository.deleteMessagesByProviderId(providerId)
        }
    }

    fun clearAllChats() {
        viewModelScope.launch {
            repository.clearAllConversations()
        }
    }

    // --- User Management ---
    fun selectUserSession(session: UserSession) {
        _currentUser.value = session
    }

    // --- Send Chat with Provider ---
    fun sendChatMessage(providerId: Int, providerName: String, text: String) {
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            // Log user message
            repository.sendChatMessage(
                ChatMessage(
                    senderName = "مستخدم",
                    senderRole = "user",
                    providerId = providerId,
                    messageText = text,
                    isSentByMe = true
                )
            )

            // Simulate Provider Auto Answer after 1.5 seconds if not paused/suspended
            delay(1500)
            val mockReply = when {
                text.contains("السلام") || text.contains("مرحبا") -> {
                    "وعليكم السلام ورحمة الله وبركاته يا غالي، حياك الله في الدليل اليمني. كيف أقدر أساعدك اليوم؟"
                }
                text.contains("السعر") || text.contains("سعر") || text.contains("بكم") -> {
                    "أبشر بسعدك يا طيب، تختلف الأسعار بحسب حجم ونوع العمل. ارسل لي التفاصيل وبنتفق على سعر يرضيك."
                }
                text.contains("الوقت") || text.contains("متى") || text.contains("اليوم") -> {
                    "أنا جاهز ومستعد للبدء بالخدمة مباشرة. حدد التوقيت المناسب والأنسب لك وسأكون عندك بالموعد."
                }
                else -> {
                    "سأكون مسروراً جداً بخدمتك وتقديم أفضل جودة تليق بك! تواصل معي للاتفاق التام والمباشر."
                }
            }
            repository.sendChatMessage(
                ChatMessage(
                    senderName = providerName,
                    senderRole = "provider",
                    providerId = providerId,
                    messageText = mockReply,
                    isSentByMe = false
                )
            )
            NotificationHelper.showNotification(
                getApplication(),
                "رسالة جديدة من $providerName 💬",
                mockReply
            )
        }
    }

    fun setActiveChatProvider(providerId: Int?) {
        _activeChatProviderId.value = providerId
        if (providerId != null) {
            repository.activateChatSessionListener(providerId)
        } else {
            repository.deactivateChatSessionListener()
        }
    }

    // --- Send Message to Gemini AI Assistant ---
    fun sendMessageToAssistant(text: String) {
        if (text.trim().isEmpty()) return
        if (_assistantLoading.value) return // Guard: strictly allow only one active assistant execution at a time to prevent duplicate responses
        viewModelScope.launch {
            // Append user message
            val currentList = _assistantChat.value.toMutableList()
            currentList.add(AssistantMessage(text, true))
            _assistantChat.value = currentList
            
            _assistantLoading.value = true

            // Generate AI Response (either via cloud gemini or offline fallback)
            val responseText = GeminiService.generateResponse(text)
            
            _assistantLoading.value = false
            
            val updatedList = _assistantChat.value.toMutableList()
            updatedList.add(AssistantMessage(responseText, false))
            _assistantChat.value = updatedList
        }
    }

    fun clearAssistantChat() {
        _assistantChat.value = listOf(
            AssistantMessage("مرحباً بك يا غالي في الدليل اليمني! أنا مساعدك الذكي 🤖، كيف بقدر أخدمك اليوم؟ يمكنك سؤالي عن السباكين، الكهربائيين، الفنيين أو كيف تسجل معنا في التطبيق.", false)
        )
    }

    // --- Admin Dashboard logic ---
    fun approveProvider(provider: ServiceProvider) {
        viewModelScope.launch {
            val approved = provider.copy(status = "نشط", isVerified = true)
            repository.updateProvider(approved)
            repository.logNotification(
                title = "تم توثيق فني جديد! 🎉",
                body = "نرحب بـ ${provider.name} كمقدم خدمة معتمد في مجال ${provider.category} بمدينة ${provider.city}.",
                target = "all"
            )
            NotificationHelper.showNotification(
                getApplication(),
                "تم توثيق فني جديد! 🎉",
                "نرحب بـ ${provider.name} كمقدم خدمة معتمد في مجال ${provider.category} بمدينة ${provider.city}."
            )
        }
    }

    fun rejectProvider(provider: ServiceProvider) {
        viewModelScope.launch {
            val rejected = provider.copy(status = "مرفوض")
            repository.updateProvider(rejected)
        }
    }

    fun deleteProvider(provider: ServiceProvider) {
        viewModelScope.launch {
            repository.deleteProvider(provider)
        }
    }

    fun toggleVip(provider: ServiceProvider) {
        viewModelScope.launch {
            val updated = provider.copy(isVip = !provider.isVip)
            repository.updateProvider(updated)
        }
    }

    fun toggleRecommended(provider: ServiceProvider) {
        viewModelScope.launch {
            val updated = provider.copy(isRecommended = !provider.isRecommended)
            repository.updateProvider(updated)
        }
    }

    fun addLoyaltyPoints(provider: ServiceProvider, count: Int) {
        viewModelScope.launch {
            val updated = provider.copy(points = provider.points + count)
            repository.updateProvider(updated)
        }
    }

    fun sendAdminNotification(title: String, body: String, target: String) {
        viewModelScope.launch {
            repository.logNotification(title, body, target)
            NotificationHelper.showNotification(
                getApplication(),
                title,
                body
            )
        }
    }

    fun addNewBanner(title: String, description: String, mediaType: String = "صورة", catRedirect: String = "all", duration: Int = 5) {
        viewModelScope.launch {
            repository.insertBanner(
                AdBanner(
                    title = title,
                    description = description,
                    mediaType = mediaType,
                    categoryRedirect = catRedirect,
                    displayDuration = duration
                )
            )
        }
    }

    fun deleteBanner(banner: AdBanner) {
        viewModelScope.launch {
            repository.deleteBannerById(banner.id)
        }
    }

    // --- Bookings management ---
    fun submitBookingRequest(
        userName: String,
        providerId: Int,
        providerName: String,
        category: String,
        date: String,
        time: String,
        notes: String
    ) {
        viewModelScope.launch {
            val newBooking = Booking(
                userName = userName,
                providerId = providerId,
                providerName = providerName,
                serviceCategory = category,
                date = date,
                time = time,
                notes = notes,
                status = "قيد الانتظار"
            )
            repository.insertBooking(newBooking)

            // Auto log notification/alert reminding booking
            repository.logNotification(
                title = "تم جدولة موعد حجز جديد 📅",
                body = "تلقيت موعد حجز لـ $providerName في $date الساعة $time.",
                target = "users"
            )

            NotificationHelper.showNotification(
                getApplication(),
                "تأكيد موعد الحجز! 📅",
                "تم إرسال طلب الحجز لـ $providerName بتاريخ $date في تمام $time وهو قيد المعالجة."
            )
        }
    }

    fun changeBookingStatus(bookingId: String, newStatus: String) {
        viewModelScope.launch {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("bookings").document(bookingId).get { doc, _ ->
                doc?.let { d ->
                    val data = d.getData()?.toMutableMap() ?: return@get
                    data["status"] = newStatus
                    firestore.collection("bookings").document(bookingId).set(data)
                }
            }
        }
    }

    fun removeBooking(bookingId: String) {
        viewModelScope.launch {
            repository.deleteBooking(bookingId)
        }
    }

    // --- Conditions controls for Admin ---
    fun addCondition(text: String, isRequired: Boolean) {
        viewModelScope.launch {
            val current = registrationConditions.value.toMutableList()
            current.add(RegistrationCondition(text = text, isRequired = isRequired))
            repository.updateRegistrationConditions(current)
        }
    }

    fun deleteCondition(id: String) {
        viewModelScope.launch {
            val current = registrationConditions.value.filter { it.id != id }
            repository.updateRegistrationConditions(current)
        }
    }

    // --- Cleanup and Database Management Tasks ---
    private val _taskProgress = MutableStateFlow<String?>(null)
    val taskProgress: StateFlow<String?> = _taskProgress.asStateFlow()

    fun performCleanupTask() {
        viewModelScope.launch {
            _taskProgress.value = "البدء في فحص وتطهير التخزين المؤقت والملفات المؤقتة..."
            delay(1000)
            _taskProgress.value = "جاري تصفية سجلات الإشعارات التي تتخطى فترة بقاء ${_retentionDays.value} يوم..."
            delay(1200)
            
            // Perform actual database purge of logs
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("notification_logs").get { snapshot, _ ->
                snapshot?.let { snap ->
                    val borderTime = System.currentTimeMillis() - (_retentionDays.value * 24L * 3600L * 1000L)
                    val toDelete = snap.documents.filter { doc ->
                        val ts = doc.getLong("timestamp") ?: 0L
                        ts < borderTime
                    }
                    toDelete.forEach { doc ->
                        firestore.collection("notification_logs").document(doc.id).delete()
                    }
                }
            }

            _taskProgress.value = "تم إفراغ سجلات النظام وتفريغ الذاكرة الفارغة تماماً! 🧹"
            delay(1500)
            _taskProgress.value = null
        }
    }

    fun performDatabaseManualBackup(context: Context, destination: String) {
        viewModelScope.launch {
            _taskProgress.value = "جاري تهيئة خادم الفحص واسترجاع ملفات التخزين..."
            delay(1000)
            _taskProgress.value = "جاري تجميع الملفات وقاعدة البيانات وتنسيق محتواها كـ JSON..."
            delay(1200)
            _taskProgress.value = "جاري الضغط والمزامنة مع الحساب السحابي ($destination)..."
            delay(1500)
            
            // Backup action (simulated backup save file)
            _taskProgress.value = "اكتملت عملية الحفظ الاحتياطي بنجاح كملف مضغوط (yemen_guide_backup.zip) في: $destination!"
            delay(2500)
            _taskProgress.value = null
        }
    }

    // --- Service Provider Form Registration with simulated image compression ---
    fun registerNewProvider(
        name: String,
        phone: String,
        category: String,
        city: String,
        description: String,
        imageUri: String?
    ) {
        viewModelScope.launch {
            _registrationSuccess.value = false
            _compressionStatus.value = "جاري فحص الصور وتهيئتها..."
            delay(1200)
            _compressionStatus.value = "جاري تفعيل محرك الضغط الذكي (WebP)..."
            delay(1000)
            _compressionStatus.value = "تم ضغط الصور بنجاح! تم خفض الحجم بمقدار ٧٨٪ (من 5.4MB إلى 1.2MB)."
            delay(1200)
            _compressionStatus.value = "جاري حفظ بيانات مقدم الخدمة ومزامنتها مع الخادم الفوري..."
            delay(1000)

            // Insert into root firebase collection
            val starterId = (allProviders.value.maxOfOrNull { it.id } ?: 0) + 1
            val newProvider = ServiceProvider(
                id = starterId,
                name = name,
                phone = phone,
                category = category,
                city = city,
                rating = 5.0f,
                reviewCount = 0,
                isVip = false,
                isVerified = false,
                isRecommended = false,
                status = "قيد الانتظار",
                description = description,
                points = 10,
                lastActive = System.currentTimeMillis()
            )
            repository.insertProvider(newProvider)

            // Log notification for Admin
            repository.logNotification(
                title = "طلب انضمام جديد قيد المعالجة 📥",
                body = "قدم الفني $name طلباً للتمثيل في مجال $category بـ $city. يرجى المراجعة والقبول من لوحة التحكم.",
                target = "all"
            )
            NotificationHelper.showNotification(
                getApplication(),
                "طلب انضمام جديد قيد المعالجة 📥",
                "قدم الفني $name طلباً للتمثيل في مجال $category بـ $city."
            )

            _compressionStatus.value = "تم التسجيل بنجاح! طلبك الآن قيد المعالجة من قبل الإدارة وسنتواصل معك قريباً."
            _registrationSuccess.value = true
        }
    }

    fun resetRegistrationState() {
        _compressionStatus.value = null
        _registrationSuccess.value = false
    }
}

// Support Structures
data class FilterState(
    val query: String,
    val cat: String?,
    val city: String?,
    val minRating: Float,
    val maxDistance: Double?,
    val sort: String
)

sealed class UserSession {
    object Guest : UserSession()
    object Admin : UserSession()
    object Owner : UserSession()
}

data class AssistantMessage(
    val text: String,
    val isSentByMe: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class TopAppBarIconConfig(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val defaultIcon: String,
    val isVisible: Boolean = true,
    val order: Int
)
