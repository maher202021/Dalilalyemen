package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.NotificationHelper
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

    // Fixed User Location
    val currentUserLat = 15.3694
    val currentUserLon = 44.1910

    // --- Streams ---
    val banners: StateFlow<List<AdBanner>> = repository.banners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationLog>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProviders: StateFlow<List<ServiceProvider>> = repository.allProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentChats: StateFlow<List<RecentChatInfo>> = repository.recentChats
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

    val activeChatMessages: StateFlow<List<ChatMessage>> = _activeChatProviderId
        .flatMapLatest { providerId ->
            if (providerId != null) {
                repository.getChatMessagesFlow(providerId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // --- User Management ---
    fun selectUserSession(session: UserSession) {
        _currentUser.value = session
    }

    // --- Send Chat with Provider ---
    fun sendChatMessage(providerId: Int, providerName: String, text: String) {
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            // Log sender message
            repository.sendChatMessage(
                ChatMessage(
                    senderName = "مستخدم",
                    senderRole = "user",
                    providerId = providerId,
                    messageText = text,
                    isSentByMe = true
                )
            )

            // Simulate Provider Auto Answer after 1.5 seconds
            delay(1500)
            val mockReply = when {
                text.contains("السلام") || text.contains("مرحبا") -> {
                    "وعليكم السلام ورحمة الله وبركاته يا غالي، حياك الله. كيف أقدر أساعدك؟"
                }
                text.contains("السعر") || text.contains("سعر") || text.contains("بكم") -> {
                    "أبشر بسعدك، السعر عادة يعتمد على نوع العمل المطلوب والتفاصيل. لو تشرح لي المشكلة بشكل أدق وبنتفق على سعر يرضيك إن شاء الله."
                }
                text.contains("متى") || text.contains("اليوم") || text.contains("الوقت") -> {
                    "أنا متواجد وجاهز للخدمة. حدد الوقت واليوم المناسب لك وبأكون عندك في الموعد بالضبط بعون الله."
                }
                else -> {
                    "تسلم يا غالي، أنا في خدمتك دائماً وسأحرص على إنجاز العمل بأعلى جودة تليق بك. هل تحب أن نتصل هاتفياً لتحديد بقية التفاصيل؟"
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
    }

    // --- Send Message to Gemini AI Assistant ---
    fun sendMessageToAssistant(text: String) {
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            // Append user message
            val currentList = _assistantChat.value.toMutableList()
            currentList.add(AssistantMessage(text, true))
            _assistantChat.value = currentList
            
            _assistantLoading.value = true

            // Generate AI Response (either via cloud gemini-3.5-flash or offline fallback)
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

    fun addNewBanner(title: String, description: String) {
        viewModelScope.launch {
            repository.insertBanner(AdBanner(title = title, description = description))
        }
    }

    fun deleteBanner(banner: AdBanner) {
        viewModelScope.launch {
            repository.deleteBannerById(banner.id)
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

            // Insert into room database with status 'قيد الانتظار' (Pending approval from Admin)
            val newProvider = ServiceProvider(
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
                points = 10 // starter loyalty points
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

// --- Supporting Sealed Classes & UI States ---

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
