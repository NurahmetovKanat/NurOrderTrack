package kz.nurkanat.nurordertrack

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.messaging.FirebaseMessaging
import kz.nurkanat.nurordertrack.data.model.UserRole
import kz.nurkanat.nurordertrack.di.AppContainer
import kz.nurkanat.nurordertrack.navigation.AppNavigation
import kz.nurkanat.nurordertrack.ui.theme.NurOrderTrackTheme
import kz.nurkanat.nurordertrack.utils.FcmHelper
import kz.nurkanat.nurordertrack.utils.LanguageHelper
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private fun askNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        LanguageHelper.restoreLocale(this)

        var keepSplashScreenOn = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreenOn }

        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings
            .Builder()
            .setPersistenceEnabled(true)
            .build()

        enableEdgeToEdge()
        askNotificationPermission()

        setContent {
            SideEffect {
                keepSplashScreenOn = false
            }

            NurOrderTrackTheme {
                val navController = rememberNavController()
                val userRepo = AppContainer.userRepository
                val scope = rememberCoroutineScope()

                var isLoggedIn by remember { mutableStateOf(userRepo.isLoggedIn()) }
                var userRole by remember { mutableStateOf(UserRole.EXECUTOR) }
                var isReady by remember { mutableStateOf(false) }

                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) {
                        val uid = userRepo.getCurrentUserId()
                        if (uid != null) {
                            FcmHelper.saveToken(uid)
                            userRepo.getCurrentUserFlow().collect { user ->
                                user?.let { userRole = it.role }
                                isReady = true
                            }
                        }
                    } else {
                        isReady = true
                    }
                }

                LaunchedEffect(Unit) {
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("FCM_TOKEN", "Token: ${task.result}")
                        }
                    }
                }

                if (isReady) {
                    AppNavigation(
                        navController = navController,
                        isLoggedIn = isLoggedIn,
                        userRole = userRole,
                        onLoginSuccess = { isLoggedIn = true }
                    )
                }
            }
        }
    }
}