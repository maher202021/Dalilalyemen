package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.YemenGuideViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderRegistrationScreen(
    viewModel: YemenGuideViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    val compressionStatus by viewModel.compressionStatus.collectAsStateWithLifecycle()
    val registrationSuccess by viewModel.registrationSuccess.collectAsStateWithLifecycle()
    val registrationConditions by viewModel.registrationConditions.collectAsStateWithLifecycle()

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("سباك") }
    var selectedCity by remember { mutableStateOf("صنعاء") }
    var serviceDescription by remember { mutableStateOf("") }
    var mockAttachedImage by remember { mutableStateOf<String?>(null) }

    // Upload & Capture Specs
    var uploadSourceType by remember { mutableStateOf("gallery") } // gallery or camera
    var isFemaleProvider by remember { mutableStateOf(false) } // females can upload profession-related replacement
    
    // Conditions checkable states map
    var acceptMap = remember { mutableStateMapOf<String, Boolean>() }

    val categories = listOf("سباك", "كهربائي", "دهان", "نجار", "حداد", "تكييف", "نقل اثاث", "تنظيف")
    val cities = listOf("صنعاء", "عدن", "تعز", "الحديدة", "حضرموت", "إب", "مأرب")

    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var isCityDropdownExpanded by remember { mutableStateOf(false) }

    // On clean composition, reset viewModel registration states
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetRegistrationState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تسجيل مقدم خدمة جديد",
                        fontWeight = FontWeight.Bold,
                        color = YemenGold
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateBg)
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.End
        ) {
            // Hero Intro
            Text(
                text = "انضم إلى نخبة الفنيين في اليمن 🇾🇪",
                color = YemenGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "سجل بياناتك الحقيقية لنوثق حسابك ونربطك بآلاف العملاء في منطقتك مجاناً وبدون أي عمولات في البداية.",
                color = MutedSlate,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 20.dp)
            )

            // Input Column Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Full name input
                    Text("الاسم الرباعي الكامل", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 14.dp)
                            .testTag("full_name_input"),
                        placeholder = { Text("مثال: ماهر علي أحمد الوصابي") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YemenGold,
                            unfocusedBorderColor = SlateBg,
                            focusedContainerColor = SlateBg,
                            unfocusedContainerColor = SlateBg,
                            focusedTextColor = OffWhite,
                            unfocusedTextColor = OffWhite
                        )
                    )

                    // Phone input
                    Text("رقم الهاتف اليمني النشط", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 14.dp)
                            .testTag("phone_input"),
                        placeholder = { Text("مثال: 777644670") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YemenGold,
                            unfocusedBorderColor = SlateBg,
                            focusedContainerColor = SlateBg,
                            unfocusedContainerColor = SlateBg,
                            focusedTextColor = OffWhite,
                            unfocusedTextColor = OffWhite
                        )
                    )

                    // Category Selector
                    Text("مجال التخصص الرئيسي", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp)) {
                        OutlinedButton(
                            onClick = { isCategoryDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, MutedSlate.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = SlateBg, contentColor = OffWhite)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = YemenGold)
                                Text(selectedCategory, color = YemenGold, fontWeight = FontWeight.Bold)
                            }
                        }
                        DropdownMenu(
                            expanded = isCategoryDropdownExpanded,
                            onDismissRequest = { isCategoryDropdownExpanded = false },
                            modifier = Modifier.background(SlateCard)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = OffWhite, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                    onClick = {
                                        selectedCategory = cat
                                        isCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // City Selector
                    Text("محافظة العمل الحالية", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp)) {
                        OutlinedButton(
                            onClick = { isCityDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, MutedSlate.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = SlateBg, contentColor = OffWhite)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = YemenGold)
                                Text(selectedCity, color = YemenGold, fontWeight = FontWeight.Bold)
                            }
                        }
                        DropdownMenu(
                            expanded = isCityDropdownExpanded,
                            onDismissRequest = { isCityDropdownExpanded = false },
                            modifier = Modifier.background(SlateCard)
                        ) {
                            cities.forEach { ct ->
                                DropdownMenuItem(
                                    text = { Text(ct, color = OffWhite, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                    onClick = {
                                        selectedCity = ct
                                        isCityDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Description of Services
                    Text("شرح مفصل عن خبرتك وأعمالك السابقة", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = serviceDescription,
                        onValueChange = { serviceDescription = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(top = 6.dp, bottom = 14.dp)
                            .testTag("description_input"),
                        placeholder = { Text("مثال: خبرة ١٠ سنوات في صيانة السباكة ومختلف كشوفات التسريبات...") },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YemenGold,
                            unfocusedBorderColor = SlateBg,
                            focusedContainerColor = SlateBg,
                            unfocusedContainerColor = SlateBg,
                            focusedTextColor = OffWhite,
                            unfocusedTextColor = OffWhite
                        )
                    )

                    HorizontalDivider(color = SlateBg, modifier = Modifier.padding(vertical = 12.dp))

                    // Female Provider Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { isFemaleProvider = !isFemaleProvider },
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تحميل صورة ترمز للمهنة مجهولة الوجه بدلاً من السيلفي\n(خيار مخصص للفنيات لمراعاة الخصوصية والسلامة 🛡️)",
                            color = YemenGold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Right,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Checkbox(
                            checked = isFemaleProvider,
                            onCheckedChange = { isFemaleProvider = it },
                            colors = CheckboxDefaults.colors(checkedColor = YemenGold)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Doc Source Selector
                    Text("اختر مصدر إدخال وتحميل الصورة 📸", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                uploadSourceType = "camera" 
                                mockAttachedImage = if (isFemaleProvider) "professional_workspace_photo.png" else "yemeni_selfie_captured.png"
                                Toast.makeText(context, "📷 تم تفعيل كاميرا الهاتف والتقاط الصورة مباشرة بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uploadSourceType == "camera") YemenGold else SlateBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = if (uploadSourceType == "camera") SlateBg else YemenGold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("سيلفي كاميرا", color = if (uploadSourceType == "camera") SlateBg else OffWhite, fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = { 
                                uploadSourceType = "gallery" 
                                mockAttachedImage = if (isFemaleProvider) "workspace_design_portfolio.png" else "my_gallery_selfie.png"
                                Toast.makeText(context, "📂 تم فتح المعرض الداخلي للهاتف وتحديد الصورة بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uploadSourceType == "gallery") YemenGold else SlateBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Image, contentDescription = null, tint = if (uploadSourceType == "gallery") SlateBg else YemenGold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ذاكرة الهاتف (الاستوديو)", color = if (uploadSourceType == "gallery") SlateBg else OffWhite, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Document/Photo Upload Emulator Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SlateBg)
                            .border(1.dp, YemenGold.copy(alpha = 0.2f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (mockAttachedImage != null) Icons.Filled.CheckCircle else Icons.Filled.CloudUpload,
                                contentDescription = "Upload Icon",
                                tint = if (mockAttachedImage != null) SoftEmerald else YemenGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (mockAttachedImage != null) "الملف الجاهز للرفع والضغط: $mockAttachedImage" else "الرجاء تحديد صورتك أو مستند رخصتك",
                                color = if (mockAttachedImage != null) SoftEmerald else OffWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section C: Terms and Conditions List loaded dynamically from Firestore Admin settings
            if (registrationConditions.isNotEmpty()) {
                Text(
                    "شروط وقواعد التسجيل المطلوبة من الإدارة ⚠️",
                    color = YemenGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        registrationConditions.forEach { cond ->
                            val isChecked = acceptMap[cond.id] ?: false
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .minimumInteractiveComponentSize()
                                    .clickable { acceptMap[cond.id] = !isChecked },
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${cond.text} ${if (cond.isRequired) "(إلزامي ⚠️)" else "(اختياري)"}",
                                    color = OffWhite,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { acceptMap[cond.id] = it },
                                    colors = CheckboxDefaults.colors(checkedColor = YemenGold)
                                )
                            }
                        }
                    }
                }
            }

            // Compression progress logs indicator (Live display of photo reduction!)
            AnimatedVisibility(
                visible = compressionStatus != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, YemenGold.copy(alpha = 0.3f)),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "محرك المعالجة والضغط الفوري",
                                color = YemenGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            iconImageVectorForStatus(compressionStatus).let { symbol ->
                                Icon(symbol, contentDescription = null, tint = YemenGold, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = compressionStatus ?: "",
                            color = OffWhite,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Register Submit Button
            Button(
                onClick = {
                    // Check required conditions are met
                    val missingRequired = registrationConditions.filter { it.isRequired }.any { cond ->
                        (acceptMap[cond.id] ?: false) == false
                    }

                    if (fullName.isEmpty() || phone.isEmpty() || serviceDescription.isEmpty()) {
                        Toast.makeText(context, "يرجى ملء جميع الحقول الإلزامية لتسجيل الطلب!", Toast.LENGTH_LONG).show()
                    } else if (phone.length < 9) {
                        Toast.makeText(context, "يرجى إدخال رقم هاتف يمني صحيح ومكتمل!", Toast.LENGTH_LONG).show()
                    } else if (missingRequired) {
                        Toast.makeText(context, "الرجاء الموافقة على جميع شروط تسجيل الإدارة الإلزامية للمتابعة!", Toast.LENGTH_LONG).show()
                    } else {
                        viewModel.registerNewProvider(
                            fullName, phone, selectedCategory, selectedCity, serviceDescription, mockAttachedImage
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_registration_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (registrationSuccess) SoftEmerald else YemenGold,
                    contentColor = SlateBg
                ),
                shape = RoundedCornerShape(10.dp),
                enabled = compressionStatus == null || registrationSuccess
            ) {
                Text(
                    text = if (registrationSuccess) "تم تسجيل طلبك بنجاح! للبيت" else "إرسال طلب الانضمام والتحقق",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (registrationSuccess) Color.White else SlateBg
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            if (registrationSuccess) {
                TextButton(
                    onClick = {
                        viewModel.resetRegistrationState()
                        onNavigateBack()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("الرجوع للقائمة الرئيسية", color = YemenGold, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun iconImageVectorForStatus(status: String?): androidx.compose.ui.graphics.vector.ImageVector {
    if (status == null) return Icons.Filled.HourglassEmpty
    return when {
        status.contains("نجاح") || status.contains("تم") -> Icons.Filled.CheckCircle
        status.contains("ضغط") -> Icons.Filled.SettingsBackupRestore
        status.contains("حفظ") -> Icons.Filled.CloudSync
        else -> Icons.Filled.HourglassBottom
    }
}
