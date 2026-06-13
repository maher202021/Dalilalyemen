package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.YemenGuideViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize Firestore settings as mandated
    val settings = FirebaseFirestoreSettings.Builder()
        .setPersistenceEnabled(true)
        .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
        .build()
    FirebaseFirestore.getInstance().firestoreSettings = settings
    FirebaseFirestore.getInstance().configureOfflinePersistence(this)

    // Initialize notification channel for real system alerts
    NotificationHelper.createNotificationChannel(this)

    // Proactively request POST_NOTIFICATIONS runtime permission on Android 13+
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
      if (androidx.core.content.ContextCompat.checkSelfPermission(
          this,
          android.Manifest.permission.POST_NOTIFICATIONS
        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
      ) {
        val requestPermissionLauncher = registerForActivityResult(
          androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { _ -> }
        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val navController = rememberNavController()
          val viewModel: YemenGuideViewModel = viewModel()

          NavHost(
            navController = navController,
            startDestination = "home"
          ) {
            // Home screen destination
            composable("home") {
              HomeScreen(
                viewModel = viewModel,
                onNavigateToChatRaw = { id, name ->
                  navController.navigate("chat/$id/$name")
                },
                onNavigateToRegister = {
                  navController.navigate("register")
                },
                onNavigateToAssistant = {
                  navController.navigate("assistant")
                },
                onNavigateToAdmin = {
                  navController.navigate("admin")
                }
              )
            }

            // Register provider destination
            composable("register") {
              ProviderRegistrationScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
              )
            }

            // Chat with specific provider destination
            composable(
              route = "chat/{id}/{name}",
              arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType }
              )
            ) { backStackEntry ->
              val id = backStackEntry.arguments?.getInt("id") ?: 0
              val name = backStackEntry.arguments?.getString("name") ?: ""
              ChatScreen(
                viewModel = viewModel,
                providerId = id,
                providerName = name,
                onNavigateBack = { navController.popBackStack() }
              )
            }

            // Smart Gemini assistant destination
            composable("assistant") {
              SmartAssistantScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
              )
            }

            // Admin Dashboard workspace destination
            composable("admin") {
              AdminDashboardScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
              )
            }
          }
        }
      }
    }
  }
}

