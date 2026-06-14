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
import com.example.data.*
import com.example.ui.YemenGuideViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: YemenGuideViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    var currentSubTab by remember { mutableStateOf(0) }

    val allProviders by viewModel.allProviders.collectAsStateWithLifecycle()
    val banners by viewModel.banners.collectAsStateWithLifecycle()
    val notificationsLog by viewModel.notifications.collectAsStateWithLifecycle()
    val bookingsList by viewModel.bookings.collectAsStateWithLifecycle()
    val registrationConditions by viewModel.registrationConditions.collectAsStateWithLifecycle()
    val taskProgress by viewModel.taskProgress.collectAsStateWithLifecycle()

    // Sub-menus Arabic (increased to 7 items to fit all specs)
    val subMenuTitles = listOf(
        "الإحصائيات" to Icons.Filled.BarChart,
        "مراجعة الطلبات" to Icons.Filled.HourglassBottom,
        "البنرات والإعلانات" to Icons.Filled.PostAdd,
        "مركز الإشعارات" to Icons.Filled.NotificationAdd,
        "إدارة الفنيين" to Icons.Filled.ManageAccounts,
        "الحجوزات المعلقة" to Icons.Filled.DateRange,
        "التحكم والهوية" to Icons.Filled.SettingsSuggest
    )

    Scaffold(
        topBar = {
            com.example.ui.components.YemenGuideTopAppBar(
                viewModel = viewModel,
                currentScreenRoute = "admin",
                onNavigateHome = onNavigateHome,
                onNavigateToRegister = onNavigateToRegister,
                onNavigateToAdmin = { /* already on admin */ }
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
                        Toast.makeText(context, "تم بث الإشعار بنجاح لجميع أجهزة الهدف!", Toast.LENGTH_SHORT).show()
                    }
                )
                4 -> AdminManageProvidersTab(
                    allProviders = allProviders,
                    onToggleVip = { viewModel.toggleVip(it) },
                    onToggleRecommended = { viewModel.toggleRecommended(it) },
                    onDelete = { viewModel.deleteProvider(it) },
                    onAddLoyaltyPoints = { provider ->
                        viewModel.addLoyaltyPoints(provider, 10)
                        Toast.makeText(context, "تمت إضافة ١٠ نقاط ولاء لـ ${provider.name}!", Toast.LENGTH_SHORT).show()
                    }
                )
                5 -> AdminBookingsTab(
                    viewModel = viewModel,
                    bookingsList = bookingsList
                )
                6 -> AdminAppIdentityTab(
                    viewModel = viewModel,
                    registrationConditions = registrationConditions,
                    taskProgress = taskProgress
                )
            }
        }
    }
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
                                onClick = { onReject(provider) },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepCoral.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("رفض الطلب ❌", color = DeepCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onApprove(provider) },
                                colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("الموافقة الفورية وعرض الفني ✅", color = SlateBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "رعاية وبناء بنرات الإعلانات الترويجية 📢",
                color = YemenGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "أضف بنرات مخصصة للظهور كشريط علوي للمستخدمين بجميع المحافظات لترويج المناسبات أو الخصومات الموسمية.",
                color = MutedSlate,
                fontSize = 11.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("إضافة بنر إعلاني جديد 🏷️", color = YemenGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان الإعلان الترويجي") },
                        modifier = Modifier.fillMaxWidth().testTag("banner_title_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YemenGold, unfocusedBorderColor = MutedSlate)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("محتوى أو تفاصيل الإعلان") },
                        modifier = Modifier.fillMaxWidth().testTag("banner_desc_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YemenGold, unfocusedBorderColor = MutedSlate)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (title.isNotEmpty() && desc.isNotEmpty()) {
                                onAdd(title, desc)
                                title = ""
                                desc = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YemenGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ ونشر الإعلان فوراً لقاعدة البيانات 🚀", color = SlateBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("البنرات المنشورة والنشطة حالياً 📱", color = OffWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        if (banners.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا يوجد بنرات ترويجية منشورة حالياً.", color = MutedSlate, fontSize = 12.sp)
                }
            }
        } else {
            items(banners) { banner ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, YemenGold.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onDelete(banner) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = DeepCoral)
                        }
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                            Text(banner.title, color = YemenGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(banner.description, color = OffWhite, fontSize = 11.sp, textAlign = TextAlign.Right)
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 4: AdminPushNotificationTab / NotificationsTab ---
@Composable
fun AdminPushNotificationTab(
    notificationsLog: List<NotificationLog>,
    onSend: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var selectedTarget by remember { mutableStateOf("all") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "مركز بث الرسائل والإشعارات المباشرة 🔔",
                color = YemenGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "أرسل إشعارات عاجلة لشرائح المستخدمين ومزودي الخدمات في اليمن لمتابعة التحديثات الأمنية أو عروض التوظيف الكبرى.",
                color = MutedSlate,
                fontSize = 11.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("صياغة تنبيه وبث إشعار فوري 💬", color = YemenGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان التنبيه") },
                        modifier = Modifier.fillMaxWidth().testTag("push_title_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YemenGold, unfocusedBorderColor = MutedSlate)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("محتوى وتفصيل التنبيه") },
                        modifier = Modifier.fillMaxWidth().testTag("push_body_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YemenGold, unfocusedBorderColor = MutedSlate)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("حدد الفئة المستهدفة بالتنبيه:", color = OffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Target segment selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedTarget == "providers", onClick = { selectedTarget = "providers" })
                            Text("الفنيين فقط", color = OffWhite, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedTarget == "users", onClick = { selectedTarget = "users" })
                            Text("العملاء فقط", color = OffWhite, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedTarget == "all", onClick = { selectedTarget = "all" })
                            Text("الجميع", color = OffWhite, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (title.isNotEmpty() && body.isNotEmpty()) {
                                onSend(title, body, selectedTarget)
                                title = ""
                                body = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YemenGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("بث إشعار فوري لجميع الأجهزة 🚀", color = SlateBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("تاريخ وسجل الإشعارات المرسلة مؤخراً 🎞️", color = OffWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        if (notificationsLog.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("سجل الإشعارات خالي حالياً.", color = MutedSlate, fontSize = 12.sp)
                }
            }
        } else {
            items(notificationsLog) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            val targetLabel = when(log.target) {
                                "all" -> "عام"
                                "providers" -> "الفنيين"
                                else -> "العملاء"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(YemenGold.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(targetLabel, color = YemenGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(log.title, color = OffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(log.body, color = MutedSlate, fontSize = 11.sp, textAlign = TextAlign.Right, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "إدارة والتحكم في مقدمي الخدمات الفعالين 👷‍♂️",
                color = YemenGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "تحكم في مستويات الفنيين النشطين كترقيتهم لعلامات VIP المتميزة أو التوصية التلقائية أو منح نقاط الولاء لشجيعهم، أو إلغاء تفعيل حساباتهم نهائياً.",
                color = MutedSlate,
                fontSize = 11.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        if (activeProviders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا يوجد فنيين نشطين للتعديل والترقية حالياً.", color = MutedSlate, fontSize = 13.sp)
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
                                "الولاء: ${provider.points} نقطة 🏆",
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
                                Text(if (provider.isRecommended) "مسح التوصية" else "تمييز موصى به", color = if (provider.isRecommended) Color.White else SoftEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                                Text("+10 ولاء", color = SoftEmerald, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 6: AdminBookingsTab (BRAND NEW!) ---
@Composable
fun AdminBookingsTab(
    viewModel: YemenGuideViewModel,
    bookingsList: List<Booking>
) {
    val isBookingsEnabled by viewModel.isBookingsEnabled.collectAsStateWithLifecycle()
    val bookingVisibility by viewModel.bookingVisibility.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "إدارة حجوزات ومواعيد الفنيين 📅",
                color = YemenGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "تحكم في إمكانية حجز مواعيد الصيانة، واختيار مستويات رؤية المواعيد لزيادة تواصل الأطراف.",
                color = MutedSlate,
                fontSize = 11.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        // Toggles & Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, YemenGold.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isBookingsEnabled,
                            onCheckedChange = { viewModel.updateBookingSettings(it, bookingVisibility) },
                            colors = SwitchDefaults.colors(checkedThumbColor = YemenGold, checkedTrackColor = YemenGold.copy(alpha = 0.4f))
                        )
                        Text("ميزة جدولة الحجوزات نشطة", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = SlateBg, modifier = Modifier.padding(vertical = 12.dp))

                    Text("مستويات رؤية ومتابعة المواعيد:", color = YemenGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val visibilityOptions = listOf("الإدارة والفني", "الفني فقط", "الإدارة فقط")
                    visibilityOptions.forEach { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable { viewModel.updateBookingSettings(isBookingsEnabled, opt) },
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(opt, color = OffWhite, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            RadioButton(
                                selected = bookingVisibility == opt,
                                onClick = { viewModel.updateBookingSettings(isBookingsEnabled, opt) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("قائمة المواعيد والحجوزات الحالية 🗓️", color = OffWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        if (bookingsList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا يوجد مواعيد صيانة مجدولة بالقائمة حتى الآن.", color = MutedSlate, fontSize = 12.sp)
                }
            }
        } else {
            items(bookingsList) { booking ->
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
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when(booking.status) {
                                            "مقبول" -> SoftEmerald.copy(alpha = 0.15f)
                                            "مرفوض" -> DeepCoral.copy(alpha = 0.15f)
                                            "مكتمل" -> Color.Cyan.copy(alpha = 0.15f)
                                            else -> YemenGold.copy(alpha = 0.15f)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = booking.status,
                                    color = when(booking.status) {
                                        "مقبول" -> SoftEmerald
                                        "مرفوض" -> DeepCoral
                                        "مكتمل" -> Color.Cyan
                                        else -> YemenGold
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("العميل: ${booking.userName}", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("الفني المطلوب: ${booking.providerName} (${booking.serviceCategory})", color = YemenGold, fontSize = 11.sp)
                                Text("التاريخ: ${booking.date} • الوقت: ${booking.time}", color = MutedSlate, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                if (booking.notes.isNotEmpty()) {
                                    Text("ملاحظات: ${booking.notes}", color = MutedSlate, fontSize = 11.sp, textAlign = TextAlign.Right, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.removeBooking(booking.id) },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DeepCoral.copy(alpha = 0.15f))
                                    .size(36.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = DeepCoral)
                            }

                            if (booking.status == "قيد الانتظار") {
                                Button(
                                    onClick = { viewModel.changeBookingStatus(booking.id, "مرفوض") },
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateBg),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("رفض الحجز", color = DeepCoral, fontSize = 10.sp)
                                }

                                Button(
                                    onClick = { viewModel.changeBookingStatus(booking.id, "مقبول") },
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("قبول الموعد", color = SlateBg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (booking.status == "مقبول") {
                                Button(
                                    onClick = { viewModel.changeBookingStatus(booking.id, "مكتمل") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("بث واكتمال الخدمة بنجاح 🏆", color = SlateBg, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 7: AdminAppIdentityTab (BRAND NEW!) ---
@Composable
fun AdminAppIdentityTab(
    viewModel: YemenGuideViewModel,
    registrationConditions: List<RegistrationCondition>,
    taskProgress: String?
) {
    val chatIconSize by viewModel.chatIconSize.collectAsStateWithLifecycle()
    val chatIconColorHex by viewModel.chatIconColorHex.collectAsStateWithLifecycle()
    val isChatIconHidden by viewModel.isChatIconHidden.collectAsStateWithLifecycle()
    val isChatIconDeleted by viewModel.isChatIconDeleted.collectAsStateWithLifecycle()

    val assistantIconSize by viewModel.assistantIconSize.collectAsStateWithLifecycle()
    val assistantIconColorHex by viewModel.assistantIconColorHex.collectAsStateWithLifecycle()
    val isAssistantIconHidden by viewModel.isAssistantIconHidden.collectAsStateWithLifecycle()

    val themePrimaryColorHex by viewModel.themePrimaryColorHex.collectAsStateWithLifecycle()
    val themeSecondaryColorHex by viewModel.themeSecondaryColorHex.collectAsStateWithLifecycle()
    val themeFontName by viewModel.themeFontName.collectAsStateWithLifecycle()

    val radiusSearchRange by viewModel.radiusSearchRange.collectAsStateWithLifecycle()
    val retentionDays by viewModel.retentionDays.collectAsStateWithLifecycle()

    var newConditionText by remember { mutableStateOf("") }
    var context = LocalContext.current

    // Sample color list for choose
    val colorPalettes = listOf(
        "#CCA43B" to "الذهبي اليمني",
        "#10B981" to "الزمردي الصحي",
        "#EF4444" to "المرجاني الدافئ",
        "#8B5CF6" to "الأرجواني الهادئ",
        "#06B6D4" to "السيان المائي",
        "#F59E0B" to "الكهرمان البرتقالي"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "التحكم المركزي، هوية التطبيق والنسخ السحابي 🛠️",
                color = YemenGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // Active Tasks Loading Banner
        if (taskProgress != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = YemenGold.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, YemenGold)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = YemenGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(taskProgress, color = OffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // Section A: Floating Chat Icon customization
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("أيقونة الدعم والدردشة العائمة 💬", color = YemenGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("حجم الأيقونة (بيكسل): $chatIconSize بكسل", color = OffWhite, fontSize = 11.sp)
                    Slider(
                        value = chatIconSize.toFloat(),
                        onValueChange = { viewModel.updateChatIconSpecs(it.toInt(), chatIconColorHex, isChatIconHidden, isChatIconDeleted) },
                        valueRange = 36f..96f,
                        colors = SliderDefaults.colors(thumbColor = YemenGold, activeTrackColor = YemenGold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("اختر لون أيقونة الدردشة:", color = OffWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Color Palettes Row Choice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colorPalettes.forEach { item ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(item.first)))
                                    .border(
                                        width = if (chatIconColorHex == item.first) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.updateChatIconSpecs(
                                            chatIconSize,
                                            item.first,
                                            isChatIconHidden,
                                            isChatIconDeleted
                                        )
                                    }
                            )
                        }
                    }

                    HorizontalDivider(color = SlateBg, modifier = Modifier.padding(vertical = 12.dp))

                    // Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isChatIconHidden,
                            onCheckedChange = { viewModel.updateChatIconSpecs(chatIconSize, chatIconColorHex, it, isChatIconDeleted) },
                            colors = SwitchDefaults.colors(checkedThumbColor = YemenGold)
                        )
                        Text("إخفاء الأيقونة العائمة مؤقتاً", color = OffWhite, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isChatIconDeleted,
                            onCheckedChange = { viewModel.updateChatIconSpecs(chatIconSize, chatIconColorHex, isChatIconHidden, it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = YemenGold)
                        )
                        Text("حذف (إيقاف) ميزة الدردشة بالكامل", color = OffWhite, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section B: Simulated Smart Assistant Floating customize
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("أيقونة المساعد الذكي AI 🤖", color = YemenGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("حجم أيقونة المساعد: $assistantIconSize بكسل", color = OffWhite, fontSize = 11.sp)
                    Slider(
                        value = assistantIconSize.toFloat(),
                        onValueChange = { viewModel.updateAssistantIconSpecs(it.toInt(), assistantIconColorHex, isAssistantIconHidden) },
                        valueRange = 36f..96f,
                        colors = SliderDefaults.colors(thumbColor = SoftEmerald, activeTrackColor = SoftEmerald)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("رؤية وتألق المساعد الذكي بالرئيسية:", color = OffWhite, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isAssistantIconHidden,
                            onCheckedChange = { viewModel.updateAssistantIconSpecs(assistantIconSize, assistantIconColorHex, it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = SoftEmerald)
                        )
                        Text("إخفاء مساعد الذكاء الاصطناعي", color = OffWhite, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section C: General Identity Theme colors and fonts
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("تخصيص الهوية الضوئية وخطوط العرض 🎨", color = YemenGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("الألوان والخطوط يتم مزامنتها فورياً عبر Snapshot Listeners لتعديل المظهر بجميع أجهزة المستخدمين.", color = MutedSlate, fontSize = 10.sp, textAlign = TextAlign.Right)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("أختر اللون الرئيسي للهوية (أعلى التطبيق والعناصر):", color = OffWhite, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        colorPalettes.forEach { cp ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(android.graphics.Color.parseColor(cp.first)))
                                    .border(
                                        width = if (themePrimaryColorHex == cp.first) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        viewModel.updateAppThemeSettings(cp.first, themeSecondaryColorHex, themeFontName)
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("اختر خط التصفير والعرض:", color = OffWhite, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    val fontOptions = listOf("Cairo" to "خط القاهرة الحديث", "Amiri" to "الخط الأميري التقليدي", "Tajawal" to "خط تجوال الأنيق")
                    fontOptions.forEach { fo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable { viewModel.updateAppThemeSettings(themePrimaryColorHex, themeSecondaryColorHex, fo.first) },
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(fo.second, color = if (themeFontName == fo.first) YemenGold else OffWhite, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(
                                selected = themeFontName == fo.first,
                                onClick = { viewModel.updateAppThemeSettings(themePrimaryColorHex, themeSecondaryColorHex, fo.first) }
                            )
                        }
                    }
                }
            }
        }

        // Section D: Registration Terms / Conditions Management
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("شروط وبنود تسجيل مقدمي الخدمات 📋", color = YemenGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Text("حدد الشروط الإلزامية التي تظهر للفنيين عند تعبئة طلب التسجيل لتصفية المدخلات والتحقق من الجدية.", color = MutedSlate, fontSize = 10.sp, textAlign = TextAlign.Right)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Existing condition terms
                    registrationConditions.forEach { condition ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.deleteCondition(condition.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = DeepCoral, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "${condition.text} ${if (condition.isRequired) "(إلزامي ⚠️)" else "(اختياري)"}",
                                color = OffWhite,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(color = SlateBg, modifier = Modifier.padding(vertical = 10.dp))

                    // Add new condition form
                    OutlinedTextField(
                        value = newConditionText,
                        onValueChange = { newConditionText = it },
                        label = { Text("صياغة شرط تسجيل جديد") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YemenGold)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (newConditionText.isNotEmpty()) {
                                viewModel.addCondition(newConditionText, true)
                                newConditionText = ""
                                Toast.makeText(context, "تم حفظ البند وإضافته للقائمة بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YemenGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إضافة شرط ملزم جديد ➕", color = SlateBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section E: Sound and Radius scope
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("نطاق البحث ومحادثة الزوار 📍", color = YemenGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("نطاق فلترة الخريطة الحالي لجميع الفنيين: $radiusSearchRange كم", color = OffWhite, fontSize = 11.sp)
                    Slider(
                        value = radiusSearchRange.toFloat(),
                        onValueChange = { viewModel.setRadiusSearchRange(it.toInt()) },
                        valueRange = 5f..50f,
                        colors = SliderDefaults.colors(thumbColor = YemenGold)
                    )

                    HorizontalDivider(color = SlateBg, modifier = Modifier.padding(vertical = 12.dp))

                    Text("إمبراطورية ورسالة عجز الدردشة الموقوفة:", color = OffWhite, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    var txtMsg by remember { mutableStateOf(viewModel.chatDisabledMessage.value) }
                    OutlinedTextField(
                        value = txtMsg,
                        onValueChange = { 
                            txtMsg = it
                            viewModel.updateChatDisabledMessage(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = OffWhite)
                    )
                }
            }
        }

        // Section F: Retention clean and Backup database
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, DeepCoral.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("تنظيف وتطهير البيانات والنسخ الاحتياطي ⚙️", color = DeepCoral, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("فترة بقاء سجل الإشعارات والنظام: $retentionDays يوم", color = OffWhite, fontSize = 11.sp)
                    Slider(
                        value = retentionDays.toFloat(),
                        onValueChange = { viewModel.setRetentionPeriod(it.toInt()) },
                        valueRange = 7f..90f,
                        colors = SliderDefaults.colors(thumbColor = DeepCoral)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.performCleanupTask() },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepCoral.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, DeepCoral)
                    ) {
                        Text("تشغيل التنظيف التلقائي للأرشيف واللوغات 🧹", color = DeepCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("النسخ الاحتياطي اليدوي لقاعدة البيانات والملفات:", color = OffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.performDatabaseManualBackup(context, "Google Drive") },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = SoftEmerald, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("جوجل درايف", color = OffWhite, fontSize = 10.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.performDatabaseManualBackup(context, "الذاكرة المحلية للهاتف") },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Storage, contentDescription = null, tint = YemenGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ذاكرة الهاتف", color = OffWhite, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section G: Customizing the Top App Bar
        item {
            val topBarIcons by viewModel.topAppBarIcons.collectAsStateWithLifecycle()
            var newIconId by remember { mutableStateOf("") }
            var newIconNameAr by remember { mutableStateOf("") }
            var newIconNameEn by remember { mutableStateOf("") }
            var newIconSymbol by remember { mutableStateOf("") }

            Card(
                modifier = Modifier.fillMaxWidth().testTag("admin_topbar_customizer_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("إعدادات وتخصيص شريط الأيقونات العلوي 🌐", color = YemenGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("يمكنك إعادة ترتيب، حذف، إخفاء/إظهار أيقونات الشريط العلوي أو إضافة مفاتيح اختصار جديدة تماماً:", color = MutedSlate, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    topBarIcons.forEachIndexed { index, icon ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(SlateBg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Order Buttons (Up/Down) & Delete
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.moveTopAppBarIconUp(icon.id) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(24.dp).testTag("move_up_${icon.id}")
                                ) {
                                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Up", tint = if (index > 0) YemenGold else MutedSlate, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.moveTopAppBarIconDown(icon.id) },
                                    enabled = index < topBarIcons.size - 1,
                                    modifier = Modifier.size(24.dp).testTag("move_down_${icon.id}")
                                ) {
                                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Down", tint = if (index < topBarIcons.size - 1) YemenGold else MutedSlate, modifier = Modifier.size(16.dp))
                                }
                                if (icon.id != "home" && icon.id != "login" && icon.id != "register" && icon.id != "language" && icon.id != "refresh") {
                                    IconButton(
                                        onClick = { viewModel.deleteTopAppBarIcon(icon.id) },
                                        modifier = Modifier.size(24.dp).testTag("delete_${icon.id}")
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = DeepCoral, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Info name & status switch
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(icon.defaultIcon, fontSize = 16.sp, modifier = Modifier.padding(end = 4.dp))
                                        Text(icon.nameAr, color = OffWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("ID: ${icon.id} | Order: ${icon.order}", color = MutedSlate, fontSize = 9.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = icon.isVisible,
                                    onCheckedChange = { viewModel.toggleTopAppBarIconVisibility(icon.id) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = YemenGold),
                                    modifier = Modifier.testTag("toggle_visible_${icon.id}")
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = SlateBg, modifier = Modifier.padding(vertical = 12.dp))

                    Text("إضافة أيقونة اختصار جديدة للشريط العلوي ➕", color = YemenGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newIconId,
                            onValueChange = { newIconId = it },
                            label = { Text("معرّف الأيقونة", fontSize = 9.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("new_icon_id_input"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YemenGold, unfocusedBorderColor = SlateBg, focusedTextColor = OffWhite, unfocusedTextColor = OffWhite)
                        )
                        OutlinedTextField(
                            value = newIconSymbol,
                            onValueChange = { newIconSymbol = it },
                            label = { Text("الرمز/الإيموجي", fontSize = 9.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("new_icon_emoji_input"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YemenGold, unfocusedBorderColor = SlateBg, focusedTextColor = OffWhite, unfocusedTextColor = OffWhite)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newIconNameAr,
                            onValueChange = { newIconNameAr = it },
                            label = { Text("الاسم بالعربية", fontSize = 9.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("new_icon_name_ar_input"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YemenGold, unfocusedBorderColor = SlateBg, focusedTextColor = OffWhite, unfocusedTextColor = OffWhite)
                        )
                        OutlinedTextField(
                            value = newIconNameEn,
                            onValueChange = { newIconNameEn = it },
                            label = { Text("الاسم بالإنجليزية", fontSize = 9.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("new_icon_name_en_input"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YemenGold, unfocusedBorderColor = SlateBg, focusedTextColor = OffWhite, unfocusedTextColor = OffWhite)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (newIconId.isNotBlank() && newIconSymbol.isNotBlank() && newIconNameAr.isNotBlank()) {
                                viewModel.addTopAppBarIcon(newIconId, newIconNameAr, newIconNameEn, newIconSymbol, true)
                                Toast.makeText(context, "تمت إضافة الأيقونة بنجاح للشريط العلوي!", Toast.LENGTH_SHORT).show()
                                newIconId = ""
                                newIconNameAr = ""
                                newIconNameEn = ""
                                newIconSymbol = ""
                            } else {
                                Toast.makeText(context, "يرجى تعبئة كافة الحقول المطلوبة للإضافة!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YemenGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_custom_icon_btn")
                    ) {
                        Text("حفظ وإضافة للشريط العلوي 🛡️", color = SlateBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
