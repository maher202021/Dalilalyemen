package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun SmartAssistantScreen(
    viewModel: YemenGuideViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    var textMessage by remember { mutableStateOf("") }
    val chatTimeline by viewModel.assistantChat.collectAsStateWithLifecycle()
    val isLoading by viewModel.assistantLoading.collectAsStateWithLifecycle()
    val scrollState = rememberLazyListState()

    // Suggestions chips to help native arabic users query AI instantly
    val suggestions = listOf("سباك في صنعاء 🚰", "البوابة السرية للأدمن 🔑", "كيف يمكنني الانضمام كفني؟ 👷‍♂️")

    // Force scroll to bottom when chat updates
    LaunchedEffect(chatTimeline.size) {
        if (chatTimeline.isNotEmpty()) {
            scrollState.animateScrollToItem(chatTimeline.size - 1)
        }
    }

    Scaffold(
        topBar = {
            com.example.ui.components.YemenGuideTopAppBar(
                viewModel = viewModel,
                currentScreenRoute = "assistant",
                onNavigateHome = onNavigateHome,
                onNavigateToRegister = onNavigateToRegister,
                onNavigateToAdmin = onNavigateToAdmin
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateBg)
                .padding(innerPadding)
        ) {
            // Screen Info bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateCard.copy(alpha = 0.4f))
                    .padding(vertical = 8.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = "سيقوم المساعد بالربط الفوري واستنتاج الفنيين والمدن تلقائياً ومساعدتك في كيفية استخدام الدليل.",
                    color = MutedSlate,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Chat View
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatTimeline) { msg ->
                    val isMe = msg.isSentByMe
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        if (!isMe) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(YemenGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "Ai Indicator",
                                    tint = YemenGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

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
                                text = msg.text,
                                color = if (isMe) SlateBg else OffWhite,
                                fontSize = 14.sp,
                                textAlign = if (isMe) TextAlign.Right else TextAlign.Left,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                // Show loading spinner
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(YemenGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "AI Generating Loading",
                                    tint = YemenGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SlateCard)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = YemenGold,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "جاري الحوسبة الذكية للرد الطبيعي الفاخر...",
                                        color = YemenGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Arabic Suggestion Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = true
            ) {
                items(suggestions) { sugg ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(SlateCard)
                            .clickable {
                                viewModel.sendMessageToAssistant(sugg.replace(" 🚰", "").replace(" 🔑", "").replace(" 👷‍♂️", ""))
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(sugg, color = YemenGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Chat Input Console
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
                                viewModel.sendMessageToAssistant(textMessage)
                                textMessage = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(YemenGold)
                            .testTag("assistant_send_button"),
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
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
                            .testTag("assistant_chat_input"),
                        placeholder = { 
                            Text(
                                "تحدث فوراً مع الذكاء الاصطناعي باليمنية...", 
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
