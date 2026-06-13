package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.YemenGuideViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: YemenGuideViewModel,
    providerId: Int,
    providerName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var textMessage by remember { mutableStateOf("") }
    val currentMessages by viewModel.activeChatMessages.collectAsStateWithLifecycle()
    val scrollState = rememberLazyListState()
    val allProviders by viewModel.allProviders.collectAsStateWithLifecycle()

    val currentProvider = remember(allProviders, providerId) {
        allProviders.find { it.id == providerId }
    }

    // Dynamic Online status based on lastActive
    val statusText = remember(currentProvider) {
        if (currentProvider == null) {
            "فني معتمد نشط الآن 🟢"
        } else {
            val diffMs = System.currentTimeMillis() - currentProvider.lastActive
            if (diffMs < 90_000) {
                "نشط الآن 🟢"
            } else if (diffMs < 300_000) {
                "نشط منذ قليل 🟡"
            } else if (diffMs < 3_600_000) {
                "آخر ظهور منذ ${diffMs / 60000} دقيقة 🔴"
            } else {
                "غير متصل 🔴"
            }
        }
    }

    val statusColor = remember(currentProvider) {
        if (currentProvider == null) {
            SoftEmerald
        } else {
            val diffMs = System.currentTimeMillis() - currentProvider.lastActive
            if (diffMs < 90_000) SoftEmerald else if (diffMs < 300_000) YemenGold else DeepCoral
        }
    }

    // Initialize provider session in viewModel
    LaunchedEffect(providerId) {
        viewModel.setActiveChatProvider(providerId)
    }

    // Scroll to bottom when messages list count adjusts
    LaunchedEffect(currentMessages.size) {
        if (currentMessages.isNotEmpty()) {
            scrollState.animateScrollToItem(currentMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = providerName,
                            fontWeight = FontWeight.Bold,
                            color = YemenGold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.setActiveChatProvider(null)
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = YemenGold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:777644670"))
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = "Contact Phone",
                            tint = YemenGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateCard)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateBg)
                .padding(innerPadding)
        ) {
            // Chat background header tip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateCard.copy(alpha = 0.5f))
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                Text(
                    text = "جميع المحادثات موثقة ومحمية بموجب وثيقة شروط وأحكام الدليل اليمني ٢٠٢٦.",
                    color = MutedSlate,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Messages Board list
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(currentMessages) { msg ->
                    val isMe = msg.isSentByMe
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isMe) 12.dp else 2.dp,
                                        bottomEnd = if (isMe) 2.dp else 12.dp
                                    )
                                )
                                .background(if (isMe) YemenGold else SlateCard)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.messageText,
                                color = if (isMe) SlateBg else OffWhite,
                                fontSize = 14.sp,
                                textAlign = if (isMe) TextAlign.Right else TextAlign.Left
                            )
                        }
                        
                        // Sender Name & tag
                        Text(
                            text = if (isMe) "أنا" else msg.senderName,
                            color = MutedSlate,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                        )
                    }
                }
            }

            // Input Send Console Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (textMessage.trim().isNotEmpty()) {
                                viewModel.sendChatMessage(providerId, providerName, textMessage)
                                textMessage = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(YemenGold)
                            .testTag("send_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = SlateBg,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    TextField(
                        value = textMessage,
                        onValueChange = { textMessage = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input"),
                        placeholder = { 
                            Text(
                                "تحدث فوراً مع مقدم الخدمة هنا...", 
                                color = MutedSlate, 
                                fontSize = 13.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            ) 
                        },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = OffWhite,
                            unfocusedTextColor = OffWhite,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }
            }
        }
    }
}
