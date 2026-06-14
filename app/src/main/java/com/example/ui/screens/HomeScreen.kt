package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ServiceProvider
import com.example.ui.YemenGuideViewModel
import com.example.ui.UserSession
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: YemenGuideViewModel,
    onNavigateToChatRaw: (Int, String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showChatDisabledDialog by remember { mutableStateOf(false) }

    // Decorate chat clicks to intercept disabled chats
    val onNavigateToChat: (Int, String) -> Unit = { id, name ->
        if (viewModel.isChatIconDeleted.value) {
            showChatDisabledDialog = true
        } else {
            onNavigateToChatRaw(id, name)
        }
    }

    // Sub-modules state
    var currentMainTab by remember { mutableStateOf(0) } // 0 = دليل الفنيين, 1 = محادثاتي النشطة
    var isMapMode by remember { mutableStateOf(false) } // False = List View, True = Map View
    var isFilterExpanded by remember { mutableStateOf(false) }
    var showVoiceSheet by remember { mutableStateOf(false) }
    var chatToDeleteId by remember { mutableStateOf<Int?>(null) }

    // ViewModel collected flow states
    val activeProviders by viewModel.activeProviders.collectAsStateWithLifecycle()
    val banners by viewModel.banners.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val allProvidersList by viewModel.allProviders.collectAsStateWithLifecycle()

    // Custom identities collected from configurations database
    val chatIconSize by viewModel.chatIconSize.collectAsStateWithLifecycle()
    val chatIconColorHex by viewModel.chatIconColorHex.collectAsStateWithLifecycle()
    val isChatIconHidden by viewModel.isChatIconHidden.collectAsStateWithLifecycle()
    val isChatIconDeleted by viewModel.isChatIconDeleted.collectAsStateWithLifecycle()

    val assistantIconSize by viewModel.assistantIconSize.collectAsStateWithLifecycle()
    val assistantIconColorHex by viewModel.assistantIconColorHex.collectAsStateWithLifecycle()
    val isAssistantIconHidden by viewModel.isAssistantIconHidden.collectAsStateWithLifecycle()

    val chatDisabledMessage by viewModel.chatDisabledMessage.collectAsStateWithLifecycle()
    val isBookingsEnabled by viewModel.isBookingsEnabled.collectAsStateWithLifecycle()

    val themePrimaryColorHex by viewModel.themePrimaryColorHex.collectAsStateWithLifecycle()
    val themeSecondaryColorHex by viewModel.themeSecondaryColorHex.collectAsStateWithLifecycle()
    val themeFontName by viewModel.themeFontName.collectAsStateWithLifecycle()

    // Admin controlled search states
    val searchEnabled by viewModel.searchEnabled.collectAsStateWithLifecycle()
    val isAutocompleteEnabled by viewModel.isAutocompleteEnabled.collectAsStateWithLifecycle()
    val isVoiceSearchEnabled by viewModel.isVoiceSearchEnabled.collectAsStateWithLifecycle()
    val minRatingFilter by viewModel.minRatingFilter.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()
    val maxDistanceFilter by viewModel.maxDistanceFilter.collectAsStateWithLifecycle()
    val recentChats by viewModel.recentChats.collectAsStateWithLifecycle()

    // Backdoor secret layers
    var homeClickCount by remember { mutableStateOf(0) }
    var showSecretDialog by remember { mutableStateOf(false) }
    var secretPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    val states = listOf("صنعاء", "عدن", "تعز", "الحديدة", "حضرموت", "إب", "مأرب")
    val categories = listOf("سباك", "كهربائي", "دهان", "نجار", "حداد", "تكييف", "نقل اثاث", "تنظيف")

    // Autocomplete suggestions (filtering matching keywords)
    val autocompleteSuggestions = remember(searchQuery, isAutocompleteEnabled) {
        if (!isAutocompleteEnabled || searchQuery.isBlank()) {
            emptyList()
        } else {
            val suggestionsList = mutableListOf<String>()
            categories.forEach { if (it.contains(searchQuery)) suggestionsList.add(it) }
            allProvidersList.forEach { 
                if (it.name.contains(searchQuery)) suggestionsList.add(it.name) 
                if (it.category.contains(searchQuery) && !suggestionsList.contains(it.category)) suggestionsList.add(it.category)
            }
            suggestionsList.distinct().take(4)
        }
    }

    if (showSecretDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSecretDialog = false 
                homeClickCount = 0
                secretPassword = ""
                passwordError = false
            },
            title = {
                Text(
                    text = "البوابة الخلفية السرية 🔐",
                    fontWeight = FontWeight.Bold,
                    color = YemenGold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "تم فتح طبقة الأدمن المشفرة. يرجى إدخال كلمة مرور الإدارة لمتابعة التحكم والتشخيص المالي والفني:",
                        color = OffWhite,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = secretPassword,
                        onValueChange = { secretPassword = it },
                        label = { Text("رمز المرور السري") },
                        modifier = Modifier.fillMaxWidth().testTag("secret_password_input"),
                        singleLine = true,
                        isError = passwordError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YemenGold,
                            unfocusedBorderColor = MutedSlate
                        )
                    )
                    if (passwordError) {
                        Text(
                            text = "رمز المرور غير صحيح! يرجى التحقق وإعادة المحاولة.",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (secretPassword == "maher--736462") {
                            viewModel.selectUserSession(UserSession.Owner)
                            showSecretDialog = false
                            homeClickCount = 0
                            secretPassword = ""
                            onNavigateToAdmin()
                            Toast.makeText(context, "أهلاً بك يا غالي WAM2026 في لوحة الإدارة الأغلى!", Toast.LENGTH_LONG).show()
                        } else {
                            passwordError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YemenGold)
                ) {
                    Text("دخول مباشر", color = SlateBg)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecretDialog = false }) {
                    Text("إلغاء", color = OffWhite)
                }
            }
        )
    }

    if (showChatDisabledDialog) {
        AlertDialog(
            onDismissRequest = { showChatDisabledDialog = false },
            title = {
                Text(
                    "تنبيه من الإدارة 🚨",
                    fontWeight = FontWeight.Bold,
                    color = try { Color(android.graphics.Color.parseColor(themePrimaryColorHex)) } catch(e: Exception) { YemenGold },
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = chatDisabledMessage,
                    color = OffWhite,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { showChatDisabledDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = try { Color(android.graphics.Color.parseColor(themePrimaryColorHex)) } catch(e: Exception) { YemenGold }
                    )
                ) {
                    Text("مفهوم", color = SlateBg)
                }
            }
        )
    }

    // Chat deletion confirmation popup Dialogue to avoid accidental wipes
    if (chatToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { chatToDeleteId = null },
            title = {
                Text(
                    "تأكيد حذف المحادثة ⚠️",
                    fontWeight = FontWeight.Bold,
                    color = DeepCoral,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    "هل أنت متأكد تماماً من رغبتك في حذف هذه المحادثة وكافة الرسائل المرتبطة بها؟ لا يمكن استرجاع البيانات بعد إتمام الحذف.",
                    color = OffWhite,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        chatToDeleteId?.let { id ->
                            viewModel.deleteConversation(id)
                            Toast.makeText(context, "تم مسح المحادثة بنجاح.", Toast.LENGTH_SHORT).show()
                        }
                        chatToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepCoral)
                ) {
                    Text("نعم، احذف المحادثة", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { chatToDeleteId = null }) {
                    Text("تراجع وإلغاء", color = OffWhite)
                }
            }
        )
    }

    // Advanced Voice Search Listening Sheet
    if (showVoiceSheet) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val pulseRatio by transition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "vocal_pulse"
        )

        AlertDialog(
            onDismissRequest = { showVoiceSheet = false },
            title = {
                Text(
                    "مساعد البحث الصوتي الدقيق 🎙️",
                    fontWeight = FontWeight.ExtraBold,
                    color = YemenGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "جاري تحليل نبرة الصوت والموجات بالذكاء الاصطناعي الفوري...",
                        fontSize = 12.sp,
                        color = MutedSlate,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                    )

                    // Wave visualizer
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(YemenGold.copy(alpha = 0.08f), CircleShape)
                            .pointerInput(Unit) {},
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size((60 * pulseRatio).dp)
                                .border(2.dp, YemenGold, CircleShape)
                        )
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Listening",
                            tint = YemenGold,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "اقتراحات يمكنك نطقها وتفعيلها باللمس المباشر:",
                        fontSize = 11.sp,
                        color = YemenGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.End).padding(bottom = 8.dp)
                    )

                    // Clickable voice suggestions
                    val suggestions = listOf(
                        "سباك ممتاز لكشف تسريبات المياه بصنعاء",
                        "كهربائي تمديدات طاقة شمسية محترف وعملي",
                        "أفضل فني صيانة مكيفات سبليت بالحديدة ودولابي"
                    )
                    suggestions.forEach { suggestion ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.setSearchQuery(suggestion)
                                    showVoiceSheet = false
                                    Toast.makeText(context, "المدخل المكتوب: $suggestion", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateCard)
                        ) {
                            Text(
                                text = suggestion,
                                color = OffWhite,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth().padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showVoiceSheet = false }) {
                    Text("إغلاق الميكروفون", color = MutedSlate)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            com.example.ui.components.YemenGuideTopAppBar(
                viewModel = viewModel,
                currentScreenRoute = "home",
                onNavigateHome = { /* Do nothing - already on home */ },
                onNavigateToRegister = onNavigateToRegister,
                onNavigateToAdmin = onNavigateToAdmin
            )
        },
        floatingActionButton = {
            val parsedChatIconColor = remember(chatIconColorHex) { try { Color(android.graphics.Color.parseColor(chatIconColorHex)) } catch(e: Exception) { YemenGold } }
            val parsedAssistantColor = remember(assistantIconColorHex) { try { Color(android.graphics.Color.parseColor(assistantIconColorHex)) } catch(e: Exception) { SoftEmerald } }
            val primaryAccent = remember(themePrimaryColorHex) { try { Color(android.graphics.Color.parseColor(themePrimaryColorHex)) } catch(e: Exception) { YemenGold } }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Floating Chat Widget / FAB with full specs controls (custom size, color, visibility, disable alerts)
                val shouldShowChatIcon = !isChatIconHidden || (currentUser != UserSession.Guest)
                if (shouldShowChatIcon) {
                    FloatingActionButton(
                        onClick = {
                            if (isChatIconDeleted) {
                                showChatDisabledDialog = true
                            } else {
                                // Toggle active chats tab
                                currentMainTab = 1
                            }
                        },
                        containerColor = parsedChatIconColor,
                        contentColor = SlateBg,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(chatIconSize.dp)
                            .testTag("floating_direct_chat_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Chat,
                            contentDescription = "مراسلة الدعم والدردشة النشطة",
                            modifier = Modifier.size((chatIconSize * 0.46f).dp)
                        )
                    }
                }

                // Smart Assistant Floating Fab (custom size, colors, visibility)
                val shouldShowAssistant = !isAssistantIconHidden || (currentUser != UserSession.Guest)
                if (shouldShowAssistant) {
                    FloatingActionButton(
                        onClick = { onNavigateToAssistant() },
                        containerColor = parsedAssistantColor,
                        contentColor = SlateBg,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(assistantIconSize.dp)
                            .testTag("smart_assistant_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "المساعد الذكي",
                            modifier = Modifier.size((assistantIconSize * 0.46f).dp)
                        )
                    }
                }

                // Add Provider registration Fab (tinted matching primary color settings)
                FloatingActionButton(
                    onClick = { onNavigateToRegister() },
                    containerColor = try { Color(android.graphics.Color.parseColor(themeSecondaryColorHex)) } catch(e: Exception) { SoftEmerald },
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(56.dp).testTag("register_provider_fab")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "تسجيل مقدم خدمة"
                    )
                }
                
                if (currentUser != UserSession.Guest) {
                    // Floating button to Admin page
                    FloatingActionButton(
                        onClick = { onNavigateToAdmin() },
                        containerColor = Purple40,
                        contentColor = primaryAccent,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(56.dp).testTag("admin_dashboard_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = "لوحة التحكم"
                        )
                    }
                }
            }
        },

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateBg)
                .padding(innerPadding)
        ) {
            // Double dynamic Tab Switcher at the very top of content
            TabRow(
                selectedTabIndex = currentMainTab,
                containerColor = SlateCard,
                contentColor = YemenGold,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = currentMainTab == 1,
                    onClick = { currentMainTab = 1 },
                    text = { Text("محادثاتي النشطة 💬", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = currentMainTab == 0,
                    onClick = { currentMainTab = 0 },
                    text = { Text("تصفح فنيي الخدمات 🛠️", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // Scrollable view switcher based on active tab
            if (currentMainTab == 0) {
                // TAB 0: Service Directory view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Live Marquee Info Strip
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateCard)
                            .border(1.dp, YemenGold.copy(alpha = 0.15f))
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "marquee")
                        val scrollOffset by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = -0.1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(12000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "scrolling"
                        )

                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val width = constraints.maxWidth
                            val translationX = scrollOffset * width

                            Text(
                                text = "أهلاً ومرحباً بكم في الدليل اليمني لخدمات المنازل الموثوقة 🌟 كادر فني متكامل في صنعاء وعدن وتعز ومأرب ومختلف المحافظات 🌟 اتصل بنا فورا وسنلبي تطلعاتك 🌟 رقم المنسق الفني MAW 777644670 🌟",
                                color = YemenGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.offset(x = translationX.dp / 3)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Banners sliding rotator
                    if (banners.isNotEmpty()) {
                        val currentBannerIndex = remember { mutableStateOf(0) }
                        LaunchedEffect(banners) {
                            while (true) {
                                delay(5000)
                                currentBannerIndex.value = (currentBannerIndex.value + 1) % banners.size
                            }
                        }
                        
                        val currentBanner = banners[currentBannerIndex.value % banners.size]
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, YemenGold),
                            colors = CardDefaults.cardColors(containerColor = SlateCard)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(SlateCard, SlateBg.copy(alpha = 0.8f))
                                            )
                                        )
                                )
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(YemenGold.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("إعلان مميز VIP", color = YemenGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = currentBanner.title,
                                            color = OffWhite,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Right,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = currentBanner.description,
                                            color = MutedSlate,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            textAlign = TextAlign.Right,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(YemenGold.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = "Golden Shield",
                                            tint = YemenGold,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Advanced Search Bar (Hidable by Admin decision)
                    if (searchEnabled) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_bar"),
                                placeholder = { 
                                    Text(
                                        text = "ابحث عن فني ممتاز، سباك، مكيفات...", 
                                        color = MutedSlate,
                                        fontSize = 14.sp
                                    ) 
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (isVoiceSearchEnabled) {
                                                showVoiceSheet = true
                                            } else {
                                                Toast.makeText(context, "البحث الصوتي معطل من قبل المشرف 🚫", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Mic,
                                            contentDescription = "Voice Search Button",
                                            tint = if (showVoiceSheet) SoftEmerald else YemenGold
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Search Icon", tint = MutedSlate)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = YemenGold,
                                    unfocusedBorderColor = SlateCard,
                                    focusedContainerColor = SlateCard,
                                    unfocusedContainerColor = SlateCard,
                                    focusedTextColor = OffWhite,
                                    unfocusedTextColor = OffWhite
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { /* Execute */ })
                            )

                            // Autocomplete overlay
                            if (autocompleteSuggestions.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = SlateCard,
                                    border = BorderStroke(1.dp, YemenGold.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        autocompleteSuggestions.forEach { suggestion ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { 
                                                        viewModel.setSearchQuery(suggestion)
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Filled.History, contentDescription = null, tint = YemenGold, modifier = Modifier.size(14.dp))
                                                Text(
                                                    text = suggestion,
                                                    color = OffWhite,
                                                    fontSize = 13.sp,
                                                    textAlign = TextAlign.Right
                                                )
                                            }
                                            HorizontalDivider(color = SlateBg, thickness = 1.dp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Notice search disabled
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = BorderStroke(1.dp, DeepCoral.copy(alpha = 0.3f))
                        ) {
                            Text(
                                "البحث النصي والصوتي معطل حالياً من قبل الإدارة الموقرة 🛡️",
                                color = DeepCoral,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Expandable advanced properties filtering drawer
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, MutedSlate.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isFilterExpanded = !isFilterExpanded }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isFilterExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Expand Filters",
                                    tint = YemenGold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "تصفية متقدمة وترتيب دقيق ومحيط جرافي ⚙️",
                                        color = OffWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Filled.FilterList, contentDescription = null, tint = YemenGold, modifier = Modifier.size(16.dp))
                                }
                            }

                            AnimatedVisibility(visible = isFilterExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    HorizontalDivider(color = SlateBg, modifier = Modifier.padding(bottom = 12.dp))

                                    // Filter 1: Rating filter options
                                    Text("التقييم الأدنى للفني ⭐:", color = YemenGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                                    ) {
                                        val ratingOptions = listOf("الكل" to 0f, "★ 4.5+" to 4.5f, "★ 4.7+" to 4.7f, "★ 4.9+" to 4.9f)
                                        ratingOptions.forEach { pair ->
                                            val isChosen = minRatingFilter == pair.second
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isChosen) YemenGold else SlateBg)
                                                    .clickable { viewModel.setMinRatingFilter(pair.second) }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(pair.first, color = if (isChosen) SlateBg else OffWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Filter 2: Proximity geographical boundary
                                    Text("النطاق الجغرافي والجوار السكني 📍:", color = YemenGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                                    ) {
                                        val distanceOptions = listOf("الكل" to null, "5 كم" to 5.0, "20 كم" to 20.0, "100 كم" to 100.0)
                                        distanceOptions.forEach { pair ->
                                            val isChosen = maxDistanceFilter == pair.second
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isChosen) YemenGold else SlateBg)
                                                    .clickable { viewModel.setMaxDistanceFilter(pair.second) }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(pair.first ?: "الكل", color = if (isChosen) SlateBg else OffWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Filter 3: Sorting Options
                                    Text("ترتيب النتائج الفوري 📊:", color = YemenGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                                    ) {
                                        val sortOptions = listOf("نخبة VIP" to "vip", "الأعلى تقييماً" to "rating", "الأقرب سكنياً" to "distance")
                                        sortOptions.forEach { pair ->
                                            val isChosen = sortBy == pair.second
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isChosen) YemenGold else SlateBg)
                                                    .clickable { viewModel.setSortBy(pair.second) }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(pair.first, color = if (isChosen) SlateBg else OffWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Yemen Cities Horizontal Filters
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Text(
                            text = "تصفية حسب المدينة 📍",
                            color = OffWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.End).padding(bottom = 10.dp)
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            reverseLayout = true
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCity == null,
                                    onClick = { viewModel.setCityFilter(null) },
                                    label = { Text("المحافظات كافة") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = YemenGold,
                                        selectedLabelColor = SlateBg,
                                        containerColor = SlateCard,
                                        labelColor = OffWhite
                                    )
                                )
                            }
                            items(states) { st ->
                                FilterChip(
                                    selected = selectedCity == st,
                                    onClick = { viewModel.setCityFilter(st) },
                                    label = { Text(st) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = YemenGold,
                                        selectedLabelColor = SlateBg,
                                        containerColor = SlateCard,
                                        labelColor = OffWhite
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Categories Grid List
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Text(
                            text = "الأقسام والخدمات التخصصية 🛠️",
                            color = OffWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.End).padding(bottom = 12.dp)
                        )
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = 4
                        ) {
                            categories.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                val iconValue = when(cat) {
                                    "سباك" -> Icons.Filled.WaterDamage
                                    "كهربائي" -> Icons.Filled.ElectricBolt
                                    "دهان" -> Icons.Filled.FormatPaint
                                    "نجار" -> Icons.Filled.Handyman
                                    "حداد" -> Icons.Filled.Hardware
                                    "تكييف" -> Icons.Filled.SevereCold
                                    "نقل اثاث" -> Icons.Filled.LocalShipping
                                    else -> Icons.Filled.CleaningServices
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) YemenGold else SlateCard)
                                        .border(1.dp, if (isSelected) YemenGold else SlateCard.copy(alpha = 0.5f))
                                        .clickable { viewModel.setCategoryFilter(cat) }
                                        .padding(vertical = 12.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = iconValue,
                                            contentDescription = cat,
                                            tint = if (isSelected) SlateBg else YemenGold,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = cat,
                                            color = if (isSelected) SlateBg else OffWhite,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Slider Toggle between List View vs Vector Map View!
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Slider switcher
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SlateCard)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isMapMode) YemenGold else Color.Transparent)
                                    .clickable { isMapMode = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("خريطة دقيقة 🗺️", color = if (isMapMode) SlateBg else OffWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isMapMode) YemenGold else Color.Transparent)
                                    .clickable { isMapMode = false }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("قائمة الفنيين 📋", color = if (!isMapMode) SlateBg else OffWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text("نمط العرض ومكتشف المواقع:", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isMapMode) {
                        // --- Regular List Mode rendering (Elite rotative + normal vertical) ---
                        // Elite rotativeVIP
                        val eliteVips = activeProviders.filter { it.isVip || it.isRecommended }
                        if (eliteVips.isNotEmpty()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                                Text(
                                    text = "قسم النخبة والموصى بهم VIP ✨",
                                    color = YemenGold,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp).align(Alignment.End)
                                )

                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    reverseLayout = true
                                ) {
                                    items(eliteVips) { provider ->
                                        Card(
                                            modifier = Modifier
                                                .width(260.dp)
                                                .clickable { onNavigateToChat(provider.id, provider.name) },
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, YemenGold),
                                            colors = CardDefaults.cardColors(containerColor = SlateCard)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Points
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(YemenGold.copy(alpha = 0.15f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "${provider.points} نقطة ولاء",
                                                            color = YemenGold,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        val distance = viewModel.calculateDistance(
                                                            viewModel.currentUserLat, viewModel.currentUserLon,
                                                            provider.latitude, provider.longitude
                                                        )
                                                        Text(
                                                            text = "${provider.city} • ${String.format("%.1f", distance)}كم",
                                                            color = MutedSlate,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            imageVector = Icons.Filled.LocationOn,
                                                            contentDescription = "location",
                                                            tint = YemenGold,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (provider.isVerified) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.CheckCircle,
                                                                    contentDescription = "Verified Seal",
                                                                    tint = SoftEmerald,
                                                                    modifier = Modifier.size(15.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                            }
                                                            Text(
                                                                text = provider.name,
                                                                color = OffWhite,
                                                                fontSize = 15.sp,
                                                                fontWeight = FontWeight.ExtraBold
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "فني ${provider.category}",
                                                            color = YemenGold,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clip(CircleShape)
                                                            .background(YemenGold.copy(alpha = 0.1f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Star,
                                                            contentDescription = "VIP Emblem",
                                                            tint = YemenGold,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = provider.description,
                                                    color = MutedSlate,
                                                    fontSize = 11.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Right,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (provider.isAvailable247) {
                                                        Box(
                                                            modifier = Modifier
                                                                .padding(end = 4.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(SoftEmerald.copy(alpha = 0.15f))
                                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("متاح 24/7 🔌", color = SoftEmerald, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(end = 4.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(YemenGold.copy(alpha = 0.15f))
                                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("⏰ ${provider.workHours}", color = YemenGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    if (provider.completionCount > 0) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color.Cyan.copy(alpha = 0.15f))
                                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("🏆 أنجز ${provider.completionCount} خدمة", color = Color.Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = { onNavigateToChat(provider.id, provider.name) },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(38.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(YemenGold)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("دردشة فورية", color = SlateBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                                                contentDescription = "Chat",
                                                                tint = SlateBg,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                                                            context.startActivity(intent)
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(38.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(SlateCard)
                                                            .border(1.dp, MutedSlate.copy(alpha = 0.3f))
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("اتصال مباشر", color = OffWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Icon(
                                                                imageVector = Icons.Filled.Phone,
                                                                contentDescription = "Call",
                                                                tint = SoftEmerald,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Normal vertical providers list
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Text(
                                text = "مقدمي الخدمات النشطين والمتاحين 👷‍♂️",
                                color = OffWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp).align(Alignment.End)
                            )

                            if (activeProviders.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.SearchOff, contentDescription = null, tint = MutedSlate, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("لا يوجد مقدمي خدمات يطابقون خيارات البحث حالياً.", color = MutedSlate, fontSize = 13.sp, textAlign = TextAlign.Center)
                                    }
                                }
                            } else {
                                activeProviders.forEach { provider ->
                                    val distance = viewModel.calculateDistance(
                                        viewModel.currentUserLat, viewModel.currentUserLon,
                                        provider.latitude, provider.longitude
                                    )
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                            .clickable { onNavigateToChat(provider.id, provider.name) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = SlateCard)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "(${provider.reviewCount}) ${provider.rating}",
                                                        color = YemenGold,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(Icons.Filled.Star, contentDescription = "rating", tint = YemenGold, modifier = Modifier.size(14.dp))
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (provider.isVip) {
                                                                Icon(Icons.Filled.WorkspacePremium, contentDescription = "VIP", tint = YemenGold, modifier = Modifier.size(16.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                            }
                                                            Text(provider.name, color = OffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        Text("فني ${provider.category} بـ ${provider.city} • تبعد ${String.format("%.1f", distance)} كم 📍", color = YemenGold.copy(alpha = 0.8f), fontSize = 11.sp)
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(46.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(YemenGold.copy(alpha = 0.1f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        val symbol = when(provider.category) {
                                                            "سباك" -> Icons.Filled.WaterDamage
                                                            "كهربائي" -> Icons.Filled.ElectricBolt
                                                            "تكييف" -> Icons.Filled.SevereCold
                                                            "دهان" -> Icons.Filled.FormatPaint
                                                            else -> Icons.Filled.Handyman
                                                        }
                                                        Icon(symbol, contentDescription = "icon", tint = YemenGold, modifier = Modifier.size(22.dp))
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = provider.description,
                                                color = MutedSlate,
                                                fontSize = 12.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (provider.isAvailable247) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(end = 4.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(SoftEmerald.copy(alpha = 0.15f))
                                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("متاح 24/7 🔌", color = SoftEmerald, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .padding(end = 4.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(YemenGold.copy(alpha = 0.15f))
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Text("⏰ ${provider.workHours}", color = YemenGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                                if (provider.completionCount > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color.Cyan.copy(alpha = 0.15f))
                                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("🏆 أنجز ${provider.completionCount} خدمة", color = Color.Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val ageMin = (System.currentTimeMillis() - provider.lastActive) / 60000
                                                val activeDotText = if (ageMin < 2) "متصل الآن 🟢" else "نشط منذ $ageMin دقيقة 🟡"
                                                Text(activeDotText, color = SoftEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Button(
                                                        onClick = { onNavigateToChat(provider.id, provider.name) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = SlateBg),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("تحدث الآن", color = YemenGold, fontSize = 11.sp)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "chat", tint = YemenGold, modifier = Modifier.size(12.dp))
                                                        }
                                                    }

                                                    Button(
                                                        onClick = {
                                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                                                            context.startActivity(intent)
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("طلب اتصال", color = Color.White, fontSize = 11.sp)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Icon(Icons.Filled.Phone, contentDescription = "call", tint = Color.White, modifier = Modifier.size(12.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // --- CAN-MODE: Interactive Vector GPS Map Canvas ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            var mapScale by remember { mutableStateOf(1.0f) }
                            var mapOffsetX by remember { mutableStateOf(0f) }
                            var mapOffsetY by remember { mutableStateOf(0f) }
                            var selectedMapProvider by remember { mutableStateOf<ServiceProvider?>(null) }

                            // Action zooming helpers inside card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, YemenGold),
                                colors = CardDefaults.cardColors(containerColor = SlateBg)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // High Quality Map Canvas
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .pointerInput(Unit) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    mapOffsetX += dragAmount.x
                                                    mapOffsetY += dragAmount.y
                                                }
                                            }
                                            .pointerInput(activeProviders) {
                                                // Handle touching/clicking nearest provider nodes
                                                this.detectTapGestures { offset ->
                                                    // Math mapping solver
                                                    val factor = 80f * mapScale
                                                    val canvasW = size.width.toFloat()
                                                    val canvasH = size.height.toFloat()
                                                    val centerX = canvasW / 2f + mapOffsetX
                                                    val centerY = canvasH / 2f + mapOffsetY

                                                    var bestMatch: ServiceProvider? = null
                                                    var shortestSq = 1600f // 40px bounding box

                                                    activeProviders.forEach { p ->
                                                        val px = centerX + (p.longitude - viewModel.currentUserLon).toFloat() * factor
                                                        val py = centerY - (p.latitude - viewModel.currentUserLat).toFloat() * factor
                                                        val dx = offset.x - px
                                                        val dy = offset.y - py
                                                        val distanceSq = dx * dx + dy * dy
                                                        if (distanceSq < shortestSq) {
                                                            shortestSq = distanceSq
                                                            bestMatch = p
                                                        }
                                                    }
                                                    selectedMapProvider = bestMatch
                                                }
                                            }
                                    ) {
                                        val width = size.width
                                        val height = size.height
                                        val factor = 80f * mapScale
                                        val centerX = width / 2f + mapOffsetX
                                        val centerY = height / 2f + mapOffsetY

                                        // 1. Draw concentric distance rings (5km, 15km, 50km equivalent)
                                        val ringRadii = listOf(50f, 120f, 240f)
                                        ringRadii.forEach { radius ->
                                            drawCircle(
                                                color = YemenGold.copy(alpha = 0.15f),
                                                radius = radius * mapScale,
                                                center = Offset(centerX, centerY),
                                                style = Stroke(width = 1.dp.toPx())
                                            )
                                        }

                                        // 2. Draw geographical grid coordinates
                                        val xSpacing = 60f * mapScale
                                        var x = centerX % xSpacing
                                        while (x < width) {
                                            drawLine(
                                                color = MutedSlate.copy(alpha = 0.08f),
                                                start = Offset(x, 0f),
                                                end = Offset(x, height),
                                                strokeWidth = 1f
                                            )
                                            x += xSpacing
                                        }
                                        var y = centerY % xSpacing
                                        while (y < height) {
                                            drawLine(
                                                color = MutedSlate.copy(alpha = 0.08f),
                                                start = Offset(0f, y),
                                                end = Offset(width, y),
                                                strokeWidth = 1f
                                            )
                                            y += xSpacing
                                        }

                                        // 3. Draw User's fixed position (pulsing anchor)
                                        drawCircle(
                                            color = Color.Cyan.copy(alpha = 0.2f),
                                            radius = 16f,
                                            center = Offset(centerX, centerY)
                                        )
                                        drawCircle(
                                            color = Color.Cyan,
                                            radius = 6f,
                                            center = Offset(centerX, centerY)
                                        )

                                        // 4. Draw service provider nodes on vector coordinates
                                        activeProviders.forEach { p ->
                                            val px = centerX + (p.longitude - viewModel.currentUserLon).toFloat() * factor
                                            val py = centerY - (p.latitude - viewModel.currentUserLat).toFloat() * factor

                                            if (px >= 0 && px <= width && py >= 0 && py <= height) {
                                                val nodeColor = if (p.isVip) YemenGold else SoftEmerald
                                                val isSelected = selectedMapProvider?.id == p.id

                                                // Draw radar glow ring
                                                drawCircle(
                                                    color = nodeColor.copy(alpha = if (isSelected) 0.4f else 0.15f),
                                                    radius = if (isSelected) 24f else 14f,
                                                    center = Offset(px, py)
                                                )
                                                // Center pins
                                                drawCircle(
                                                    color = nodeColor,
                                                    radius = 7f,
                                                    center = Offset(px, py)
                                                )
                                            }
                                        }
                                    }

                                    // Touch map navigation controls on bottom right corner
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(12.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SlateCard.copy(alpha = 0.85f))
                                            .padding(4.dp)
                                    ) {
                                        IconButton(onClick = { mapScale = (mapScale + 0.2f).coerceAtMost(3.0f) }, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Filled.Add, contentDescription = "Zoom In", tint = YemenGold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        IconButton(onClick = { mapScale = (mapScale - 0.2f).coerceAtLeast(0.5f) }, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Filled.Remove, contentDescription = "Zoom Out", tint = YemenGold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        IconButton(onClick = { mapScale = 1.0f; mapOffsetX = 0f; mapOffsetY = 0f }, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Filled.MyLocation, contentDescription = "Recenter Map", tint = Color.Cyan)
                                        }
                                    }

                                    // Alert label header
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(10.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SlateCard)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("أمانة العاصمة - رادار الفنيين النشطين 🗺️", color = YemenGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Bottom responsive info popup details of Map Selection
                            if (selectedMapProvider != null) {
                                val provider = selectedMapProvider!!
                                val distance = viewModel.calculateDistance(
                                    viewModel.currentUserLat, viewModel.currentUserLon,
                                    provider.latitude, provider.longitude
                                )
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                                    border = BorderStroke(1.5.dp, YemenGold)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "(${provider.reviewCount}) ${provider.rating}",
                                                    color = YemenGold,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(Icons.Filled.Star, contentDescription = "Rating", tint = YemenGold, modifier = Modifier.size(14.dp))
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(provider.name, color = OffWhite, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                                Text("فني ${provider.category} • مدينة ${provider.city}", color = YemenGold, fontSize = 11.sp)
                                                Text("المرق الطبيعي: يبعد عنك حوالي ${String.format("%.1f", distance)} كيلو متر", color = MutedSlate, fontSize = 10.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = provider.description,
                                            color = MutedSlate,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Right,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { onNavigateToChat(provider.id, provider.name) },
                                                colors = ButtonDefaults.buttonColors(containerColor = YemenGold),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("دردشة سريعة 💬", color = SlateBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                                                    context.startActivity(intent)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("اتصال فوري 📞", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("المس أي نقطة أو فني على الخريطة لعرض تفاصيله والاتصال به فوراً.", color = MutedSlate, fontSize = 11.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                }
            } else {
                // TAB 1: Chats Board View (myle chats with providers)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "محادثاتك المستمرة مع فنيي الصيانة الدورية 💬",
                        color = YemenGold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "تدرج وتوثيق لحماية حقوق المستهلكين والفنيين. يمكنك مسح أي حوار من سلتك بالضغط على أيقونة الإزالة.",
                        color = MutedSlate,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (recentChats.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, tint = YemenGold, modifier = Modifier.size(54.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("سجل رسائلك فارغ في الوقت الحالي.", color = MutedSlate, fontSize = 13.sp)
                                Text("تصفح دليل الفنيين وابدأ محادثتك الأولى فوراً ترحيباً بك!", color = MutedSlate, fontSize = 11.sp)
                            }
                        }
                    } else {
                        recentChats.forEach { chatInfo ->
                            val linkedProvider = allProvidersList.find { it.id == chatInfo.providerId }
                            val providerName = linkedProvider?.name ?: chatInfo.senderName
                            val category = linkedProvider?.category ?: "عن التطبيق"

                            val diffMs = linkedProvider?.let { System.currentTimeMillis() - it.lastActive } ?: 9999999L
                            val dotColor = if (diffMs < 90_000) SoftEmerald else if (diffMs < 300_000) YemenGold else Color.DarkGray
                            val activeStateText = if (diffMs < 90_000) "متصل الآن" else "نشط مؤخراً"

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .clickable { onNavigateToChat(chatInfo.providerId, providerName) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SlateCard)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left side: Deletion prompt
                                    IconButton(
                                        onClick = { chatToDeleteId = chatInfo.providerId },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(DeepCoral.copy(alpha = 0.12f))
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "مسح", tint = DeepCoral, modifier = Modifier.size(18.dp))
                                    }

                                    // Right side: Name, Role, and Online state dot
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = activeStateText,
                                                    color = MutedSlate,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(providerName, color = OffWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text(
                                                text = "قسم الخدمة: فني $category",
                                                color = YemenGold,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Circular Avatar placeholder
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(YemenGold.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Person, contentDescription = null, tint = YemenGold, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
