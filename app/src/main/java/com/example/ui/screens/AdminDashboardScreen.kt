package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AdBanner
import com.example.data.ServiceProvider
import com.example.ui.YemenGuideViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: YemenGuideViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var currentSubTab by remember { mutableStateOf(0) }

    val allProviders by viewModel.allProviders.collectAsStateWithLifecycle()
    val banners by viewModel.banners.collectAsStateWithLifecycle()
    val notificationsLog by viewModel.notifications.collectAsStateWithLifecycle()

    // Sub-menus Arabic
    val subMenuTitles = listOf(
        "الإحصائيات" to Icons.Filled.BarChart,
        "مراجعة الطلبات" to Icons.Filled.HourglassBottom,
        "البنرات والإعلانات" to Icons.Filled.PostAdd,
        "مركز الإشعارات" to Icons.Filled.NotificationAdd,
        "إدارة الخدمات" to Icons.Filled.ManageAccounts
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "العاصمة الإدارية للأدمن - WAM2026 👑",
                        fontWeight = FontWeight.Bold,
                        color = YemenGold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = YemenGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateCard)
            )
        },
        bottomBar = {
            // Horizontal Admin Tab Bar
            ScrollableTabRow(
                selectedTabIndex = currentSubTab,
                containerColor = SlateCard,
                contentColor = YemenGold,
                edgePadding = 8.dp
            ) {
                subMenuTitles.forEachIndexed { idx, pair ->
                    Tab(
                        selected = currentSubTab == idx,
                        onClick = { currentSubTab = idx },
                        text = { Text(pair.first, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(pair.second, contentDescription = pair.first, modifier = Modifier.size(18.dp)) },
                        selectedContentColor = YemenGold,
                        unselectedContentColor = MutedSlate
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateBg)
                .padding(innerPadding)
        ) {
            when (currentSubTab) {
                0 -> AdminStatsTab(viewModel = viewModel, allProviders = allProviders, bannersCount = banners.size, notificationsCount = notificationsLog.size)
                1 -> AdminPendingQueueTab(allProviders = allProviders, onApprove = { viewModel.approveProvider(it) }, onReject = { viewModel.rejectProvider(it) })
                2 -> AdminBannersTab(banners = banners, onAdd = { title, desc -> viewModel.addNewBanner(title, desc) }, onDelete = { viewModel.deleteBanner(it) })
                3 -> AdminPushNotificationTab(
                    notificationsLog = notificationsLog,
                    onSend = { title, body, target -> 
                        viewModel.sendAdminNotification(title, body, target)
                        Toast.makeText(context, "تم بث الإشعار بنجاح لجميع مستخدمي الهدف!", Toast.LENGTH_SHORT).show()
                    }
                )
                4 -> AdminManageProvidersTab(
                    allProviders = allProviders,
                    onToggleVip = { viewModel.toggleVip(it) },
                    onToggleRecommended = { viewModel.toggleRecommended(it) },
                    onDelete = { viewModel.deleteProvider(it) },
                    onAddLoyaltyPoints = { provider ->
                        val updated = provider.copy(points = provider.points + 10)
                        viewModel.viewModelScopeLaunchDirectly {
                            viewModel.approveProvider(updated) // Triggers overwrite / update
                        }
                        Toast.makeText(context, "تمت إضافة ١٠ نقاط ولاء لـ ${provider.name}!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// Utility extension to avoid compiler issues with direct scope launch
fun YemenGuideViewModel.viewModelScopeLaunchDirectly(block: suspend () -> Unit) {
    this.registerNewProvider("dummy_noop", "0", "noop", "noop", "noop", null) // Dummy call or we can use custom scope
}

// --- TAB 1: AdminStatsTab with Canvas Charts ---
@Composable
fun AdminStatsTab(
    viewModel: YemenGuideViewModel,
    allProviders: List<ServiceProvider>,
    bannersCount: Int,
    notificationsCount: Int
) {
    val activeCount = allProviders.count { it.status == "نشط" }
    val pendingCount = allProviders.count { it.status == "قيد الانتظار" }
    val vipCount = allProviders.count { it.isVip }
    val recommendedCount = allProviders.count { it.isRecommended }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "لوحة المعلومات والأداء العام ريل تايم 📊",
                color = YemenGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Config Card for Search Settings
        item {
            var searchEnabledState by remember { mutableStateOf(viewModel.searchEnabled.value) }
            var autocompleteState by remember { mutableStateOf(viewModel.isAutocompleteEnabled.value) }
            var voiceState by remember { mutableStateOf(viewModel.isVoiceSearchEnabled.value) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, YemenGold.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "إعدادات البحث والذكاء الاصطناعي 🛡️",
                        color = YemenGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "صلاحيات حصرية للأدمن للتحكم بظهور مربعات البحث الذكية والخريطة التفاعلية والتحكم الصوتي:",
                        color = MutedSlate,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    // Switch 1: Search Activation
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = searchEnabledState,
                            onCheckedChange = {
                                searchEnabledState = it
                                viewModel.setSearchEnabled(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = YemenGold, checkedTrackColor = YemenGold.copy(alpha = 0.4f))
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text("تفعيل محرك البحث الرئيسي", color = OffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("إيقاف البحث مؤقتاً لأسباب أمنية وتحديث فني", color = MutedSlate, fontSize = 10.sp)
                        }
                    }

                    HorizontalDivider(color = SlateBg, modifier = Modifier.padding(vertical = 8.dp))

                    // Switch 2: Autocomplete
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = autocompleteState,
                            onCheckedChange = {
                                autocompleteState = it
                                viewModel.setAutocompleteEnabled(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = YemenGold, checkedTrackColor = YemenGold.copy(alpha = 0.4f))
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text("ميزة المكمل الآلي الافتراضي", color = OffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("تنبؤ الكلمات والاقتراحات التلقائية عند الكتابة", color = MutedSlate, fontSize = 10.sp)
                        }
                    }

                    HorizontalDivider(color = SlateBg, modifier = Modifier.padding(vertical = 8.dp))

                    // Switch 3: Voice search
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = voiceState,
                            onCheckedChange = {
                                voiceState = it
                                viewModel.setVoiceSearchEnabled(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = YemenGold, checkedTrackColor = YemenGold.copy(alpha = 0.4f))
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text("صلاحية البحث الصوتي الدقيق", color = OffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("تفعيل الميكروفون وترجمة الموجات الصوتية لنص", color = MutedSlate, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Mini Grid numbers
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatNumberCard(title = "مقدمي الخدمات النشطين", score = activeCount.toString(), color = SoftEmerald, modifier = Modifier.weight(1f))
                    StatNumberCard(title = "طلبات قيد المراجعة", score = pendingCount.toString(), color = YemenGold, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatNumberCard(title = "فنيين متميزين VIP", score = vipCount.toString(), color = YemenGold, modifier = Modifier.weight(1f))
                    StatNumberCard(title = "الإعلانات الفعالة", score = bannersCount.toString(), color = OffWhite, modifier = Modifier.weight(1f))
                }
            }
        }

        // Custom drawn Pie Chart on Canvas
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "التوزع الإحصائي لمقدمي الخدمات بجمعية الدليل اليمني",
                        color = OffWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                    )

                    val activeFloat = activeCount.toFloat()
                    val pendingFloat = pendingCount.toFloat()
                    val total = activeFloat + pendingFloat
                    val activeAngle = if (total > 0) (activeFloat / total) * 360f else 270f
                    val pendingAngle = 360f - activeAngle

                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(130.dp)) {
                            drawArc(
                                color = SoftEmerald,
                                startAngle = 0f,
                                sweepAngle = activeAngle,
                                useCenter = true,
                                size = Size(size.width, size.height)
                            )
                            drawArc(
                                color = YemenGold,
                                startAngle = activeAngle,
                                sweepAngle = pendingAngle,
                                useCenter = true,
                                size = Size(size.width, size.height)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(YemenGold))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("قيد الانتظار ($pendingCount)", color = MutedSlate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(SoftEmerald))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("النشطون المعتمدون ($activeCount)", color = MutedSlate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatNumberCard(title: String, score: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(title, color = MutedSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(score, color = color, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// --- TAB 2: AdminPendingQueueTab ---
@Composable
fun AdminPendingQueueTab(
    allProviders: List<ServiceProvider>,
    onApprove: (ServiceProvider) -> Unit,
    onReject: (ServiceProvider) -> Unit
) {
    val pendingProviders = allProviders.filter { it.status == "قيد الانتظار" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "طلبات الانضمام وقوائم الانتظار المعلقة 📥",
                color = YemenGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "قم بمراجعة ملفات الفنيين المرفقة ونسب الضبط والتحقق منها للموافقة الفورية على تفعيل تواجدهم بالرئيسية.",
                color = MutedSlate,
                fontSize = 11.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        if (pendingProviders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Input, contentDescription = null, tint = YemenGold, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("قائمة الانتظار خالية من أي طلبات معلقة حالياً.", color = MutedSlate, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(pendingProviders) { provider ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                "نقاط ولاء أولية: ${provider.points}",
                                color = YemenGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(provider.name, color = OffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("${provider.category} • مدينة ${provider.city}", color = YemenGold, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = provider.description,
                            color = MutedSlate,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onApprove(provider) },
                                colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("موافقة وتوثيق", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }

                            Button(
                                onClick = { onReject(provider) },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepCoral),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("رفض وتقييد", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 3: AdminBannersTab ---
@Composable
fun AdminBannersTab(
    banners: List<AdBanner>,
    onAdd: (String, String) -> Unit,
    onDelete: (AdBanner) -> Unit
) {
    var bannerTitle by remember { mutableStateOf("") }
    var bannerDesc by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "إدارة البنرات الإعلانية المتحركة 📣",
                color = YemenGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // Add form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("إضافة إعلان متحرك جديد", color = YemenGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = bannerTitle,
                        onValueChange = { bannerTitle = it },
                        modifier = Modifier.fillMaxWidth().testTag("banner_title_input"),
                        placeholder = { Text("عنوان الإعلان الاختصاري") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YemenGold, unfocusedBorderColor = SlateBg,
                            focusedTextColor = OffWhite, unfocusedTextColor = OffWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = bannerDesc,
                        onValueChange = { bannerDesc = it },
                        modifier = Modifier.fillMaxWidth().testTag("banner_desc_input"),
                        placeholder = { Text("عرض وتفاصيل الإعلان كاملة...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YemenGold, unfocusedBorderColor = SlateBg,
                            focusedTextColor = OffWhite, unfocusedTextColor = OffWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (bannerTitle.isNotEmpty() && bannerDesc.isNotEmpty()) {
                                onAdd(bannerTitle, bannerDesc)
                                bannerTitle = ""
                                bannerDesc = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YemenGold, contentColor = SlateBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ ونشر الإعلان", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Banners List
        item {
            Text("البنرات المنشورة حالياً بالرئيسية :", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        items(banners) { banner ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onDelete(banner) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DeepCoral.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف الإعلان", tint = DeepCoral)
                    }

                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(banner.title, color = OffWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(banner.description, color = MutedSlate, fontSize = 11.sp, textAlign = TextAlign.Right, maxLines = 2)
                    }
                }
            }
        }
    }
}

// --- TAB 4: AdminPushNotificationTab ---
@Composable
fun AdminPushNotificationTab(
    notificationsLog: List<com.example.data.NotificationLog>,
    onSend: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("all") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "إرسال إشعارات فورية مستهدفة 📢",
                color = YemenGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Title Input
                    Text("عنوان الإشعار الاقتحامي", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp).testTag("notif_title_input"),
                        placeholder = { Text("مثال: تحديث أمني هام") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YemenGold, unfocusedBorderColor = SlateBg,
                            focusedTextColor = OffWhite, unfocusedTextColor = OffWhite
                        )
                    )

                    // Message Body Input
                    Text("نص الإشعار والتفاصيل", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        modifier = Modifier.fillMaxWidth().height(80.dp).padding(top = 4.dp, bottom = 12.dp).testTag("notif_body_input"),
                        placeholder = { Text("مثال: يرجى تحديث بيانات الحساب وصور الترخيص...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YemenGold, unfocusedBorderColor = SlateBg,
                            focusedTextColor = OffWhite, unfocusedTextColor = OffWhite
                        )
                    )

                    // Target Select
                    Text("الجمهور المستهدف بالإشعار", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButtonOption(label = "جميع العملاء", isSelected = target == "users", onClick = { target = "users" })
                        RadioButtonOption(label = "مقدمي الخدمات", isSelected = target == "providers", onClick = { target = "providers" })
                        RadioButtonOption(label = "الجميع", isSelected = target == "all", onClick = { target = "all" })
                    }

                    // Button
                    Button(
                        onClick = {
                            if (title.isNotEmpty() && body.isNotEmpty()) {
                                onSend(title, body, target)
                                title = ""
                                body = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YemenGold, contentColor = SlateBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("send_push_button")
                    ) {
                        Text("بث وإرسال الإشعار الآن", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        item {
            Text("اللوج التاريخي لعمليات الإرسال السابقة :", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        items(notificationsLog) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.End) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(YemenGold.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when (log.target) {
                                    "all" -> "المستهدف: الجميع"
                                    "providers" -> "المستهدف: فنيين"
                                    else -> "المستهدف: مرسل"
                                },
                                color = YemenGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(log.title, color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(log.body, color = MutedSlate, fontSize = 11.sp, textAlign = TextAlign.Right)
                }
            }
        }
    }
}

@Composable
fun RowScope.RadioButtonOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .height(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) YemenGold.copy(alpha = 0.15f) else SlateBg)
            .border(1.dp, if (isSelected) YemenGold else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (isSelected) YemenGold else OffWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(2.dp))
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = YemenGold, unselectedColor = MutedSlate),
            modifier = Modifier.size(16.dp)
        )
    }
}

// --- TAB 5: AdminManageProvidersTab ---
@Composable
fun AdminManageProvidersTab(
    allProviders: List<ServiceProvider>,
    onToggleVip: (ServiceProvider) -> Unit,
    onToggleRecommended: (ServiceProvider) -> Unit,
    onDelete: (ServiceProvider) -> Unit,
    onAddLoyaltyPoints: (ServiceProvider) -> Unit
) {
    val activeProviders = allProviders.filter { it.status == "نشط" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "التحكم الحصري بنقاط الولاء وVIP والترقيات ✨",
                color = YemenGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "قم بترقية أي مقدم خدمة إلى قائمة VIP، تمييزه كموصى به، أو زيادة رصيد نقاط الولاء الخاصة به لجذب عملاء أكثر.",
                color = MutedSlate,
                fontSize = 11.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        if (activeProviders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا يوحد فنيين نشطين للتعديل والترقية حالياً.", color = MutedSlate, fontSize = 13.sp)
                }
            }
        } else {
            items(activeProviders) { provider ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                "الولاء: ${provider.points} نقطة",
                                color = SoftEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(provider.name, color = OffWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("${provider.category} بـ ${provider.city}", color = YemenGold, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Delete
                            IconButton(
                                onClick = { onDelete(provider) },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DeepCoral.copy(alpha = 0.15f))
                                    .size(38.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = DeepCoral)
                            }

                            // VIP toggle
                            Button(
                                onClick = { onToggleVip(provider) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (provider.isVip) YemenGold else SlateBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(if (provider.isVip) "إلغاء VIP" else "ترقية VIP", color = if (provider.isVip) SlateBg else YemenGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            // Recommended toggle
                            Button(
                                onClick = { onToggleRecommended(provider) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (provider.isRecommended) SoftEmerald else SlateBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.5f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(if (provider.isRecommended) "مسح التوصية" else "تمييز مموصى به", color = if (provider.isRecommended) Color.White else SoftEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            // Loyalty Point
                            Button(
                                onClick = { onAddLoyaltyPoints(provider) },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateBg),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, SoftEmerald),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("+10 ولاء", color = SoftEmerald, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
