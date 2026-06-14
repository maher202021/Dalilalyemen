package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.YemenGuideViewModel
import com.example.ui.UserSession
import com.example.ui.TopAppBarIconConfig
import com.example.ui.theme.*

@Composable
fun YemenGuideTopAppBar(
    viewModel: YemenGuideViewModel,
    currentScreenRoute: String,
    onNavigateHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val rawIcons by viewModel.topAppBarIcons.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val primaryColorHex by viewModel.themePrimaryColorHex.collectAsStateWithLifecycle()

    val primaryAccent = remember(primaryColorHex) {
        try { Color(android.graphics.Color.parseColor(primaryColorHex)) } catch (e: Exception) { YemenGold }
    }

    var showLoginDialog by remember { mutableStateOf(false) }

    // Enforce RTL Layout Direction for the entire Top App Bar
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateCard)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right part: App Title & Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = if (language == "ar") "الدليل اليمني" else "Yemen Guide",
                        color = primaryAccent,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Role Indicator Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (currentUser) {
                                    is UserSession.Owner -> primaryAccent.copy(alpha = 0.2f)
                                    is UserSession.Admin -> SoftEmerald.copy(alpha = 0.2f)
                                    else -> MutedSlate.copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when (currentUser) {
                                is UserSession.Owner -> if (language == "ar") "المالك 👑" else "Owner"
                                is UserSession.Admin -> if (language == "ar") "مشرف 🛡️" else "Admin"
                                else -> if (language == "ar") "زائر" else "Guest"
                            },
                            color = when (currentUser) {
                                is UserSession.Owner -> primaryAccent
                                is UserSession.Admin -> SoftEmerald
                                else -> OffWhite
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Left part: Custom Ordered Icons List
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Iterate and draw each visible icon config in its sorted order
                    rawIcons.filter { it.isVisible }.forEach { icon ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isScreenActive(icon.id, currentScreenRoute)) {
                                        primaryAccent.copy(alpha = 0.2f)
                                    } else {
                                        SlateBg
                                    }
                                )
                                .clickable {
                                    handleTopBarIconClick(
                                        iconId = icon.id,
                                        viewModel = viewModel,
                                        onShowLogin = { showLoginDialog = true },
                                        onNavigateHome = onNavigateHome,
                                        onNavigateToRegister = onNavigateToRegister,
                                        onNavigateToAdmin = onNavigateToAdmin,
                                        currentScreenRoute = currentScreenRoute,
                                        language = language,
                                        toastContext = context
                                    )
                                }
                                .testTag("top_bar_btn_${icon.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = icon.defaultIcon,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = SlateBg, thickness = 1.dp)
        }
    }

    // Role switcher and login dialog overlay
    if (showLoginDialog) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showLoginDialog = false },
                title = {
                    Text(
                        text = if (language == "ar") "تسجيل الصلاحيات وبوابة المرور 🔐" else "Login & Passcode Gateway 🔐",
                        fontWeight = FontWeight.Bold,
                        color = primaryAccent,
                        fontSize = 16.sp
                    )
                },
                text = {
                    var passwordInput by remember { mutableStateOf("") }
                    var hasError by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = if (language == "ar") 
                                "اختر الحساب المناسب للتنقل السريع أو ادخل كلمة المرور لتفعيل صلاحيات المالك:" 
                                else "Select a quick identity toggle or type the administrator passcode to grant full access:",
                            color = OffWhite,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.selectUserSession(UserSession.Guest)
                                    Toast.makeText(context, if (language == "ar") "تم تفعيل وضع الزائر 👤" else "Switched to Guest Session 👤", Toast.LENGTH_SHORT).show()
                                    showLoginDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateBg),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (language == "ar") "زائر" else "Guest", color = OffWhite, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.selectUserSession(UserSession.Admin)
                                    Toast.makeText(context, if (language == "ar") "تم تفعيل صلاحيات المشرف 🛡️" else "Switched to Admin Session 🛡️", Toast.LENGTH_SHORT).show()
                                    showLoginDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (language == "ar") "مشرف" else "Admin", color = Color.White, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = SlateBg)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Passcode Input of Owner
                        Text(
                            text = if (language == "ar") "رمز المرور السري للمالك الرسمي" else "Secret Owner Passcode",
                            color = primaryAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it; hasError = false },
                            label = { Text(if (language == "ar") "رمز الأمان الخاص" else "Passcode") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_login_passcode_input"),
                            singleLine = true,
                            isError = hasError,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryAccent,
                                unfocusedBorderColor = SlateBg,
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite
                            )
                        )

                        if (hasError) {
                            Text(
                                text = if (language == "ar") "عذراً! الرمز المدخل غير صالح للعملية." else "Passcode is invalid!",
                                color = Color.Red,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (passwordInput == "maher--736462") {
                                    viewModel.selectUserSession(UserSession.Owner)
                                    Toast.makeText(context, if (language == "ar") "أهلاً بك يا غالي WAM2026 في لوحة المالك الأهم! 👑" else "Welcome Owner WAM2026! 👑", Toast.LENGTH_LONG).show()
                                    showLoginDialog = false
                                } else {
                                    hasError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (language == "ar") "تفعيل وتحويل كمالك رسمي" else "Authenticate as Owner", color = SlateBg, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showLoginDialog = false }) {
                        Text(if (language == "ar") "إغلاق" else "Close", color = OffWhite)
                    }
                }
            )
        }
    }
}

private fun isScreenActive(iconId: String, currentScreenRoute: String): Boolean {
    return when (iconId) {
        "home" -> currentScreenRoute == "home"
        "register" -> currentScreenRoute == "register"
        "admin" -> currentScreenRoute == "admin"
        else -> false
    }
}

private fun handleTopBarIconClick(
    iconId: String,
    viewModel: YemenGuideViewModel,
    onShowLogin: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    currentScreenRoute: String,
    language: String,
    toastContext: android.content.Context
) {
    when (iconId) {
        "home" -> {
            if (currentScreenRoute != "home") {
                onNavigateHome()
            } else {
                Toast.makeText(toastContext, if (language == "ar") "أنت حالياً في الصفحة الرئيسية 🏠" else "You are already on the Home screen 🏠", Toast.LENGTH_SHORT).show()
            }
        }
        "login" -> {
            onShowLogin()
        }
        "register" -> {
            if (currentScreenRoute != "register") {
                onNavigateToRegister()
            } else {
                Toast.makeText(toastContext, if (language == "ar") "أنت في صفحة تسجيل فني بالفعل 👤" else "Already on registering screen 👤", Toast.LENGTH_SHORT).show()
            }
        }
        "language" -> {
            viewModel.toggleLanguage()
            val newLang = if (language == "ar") "English" else "العربية"
            Toast.makeText(toastContext, "تم تبديل لغة الواجهة إلى $newLang 🌐", Toast.LENGTH_SHORT).show()
        }
        "refresh" -> {
            Toast.makeText(toastContext, if (language == "ar") "جاري مزامنة وتحديث بيانات الصفحة... 🔄" else "Refreshing screen data... 🔄", Toast.LENGTH_SHORT).show()
            viewModel.updateChatIconSpecs(
                viewModel.chatIconSize.value,
                viewModel.chatIconColorHex.value,
                viewModel.isChatIconHidden.value,
                viewModel.isChatIconDeleted.value
            )
        }
        else -> {
            if (iconId == "admin") {
                onNavigateToAdmin()
            } else {
                Toast.makeText(toastContext, "تم تنشيط الزر الإضافي من الإدارة ✔️ ($iconId)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
