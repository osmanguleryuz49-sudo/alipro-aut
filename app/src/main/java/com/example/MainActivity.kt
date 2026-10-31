package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color
import com.example.data.repository.ThemeMode
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.components.LockScreen
import com.example.ui.theme.DogrulaTheme

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDynamicColor by viewModel.isDynamicColorEnabled.collectAsStateWithLifecycle()
            val isSecureScreen by viewModel.isSecureScreenEnabled.collectAsStateWithLifecycle()
            val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
            val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()

            // Dynamic FLAG_SECURE enforcement
            LaunchedEffect(isSecureScreen) {
                if (isSecureScreen) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            // Lock app on launch if biometric is enabled
            LaunchedEffect(isBiometricEnabled) {
                if (isBiometricEnabled && isAppLocked) {
                    showBiometricPrompt()
                }
            }

            DogrulaTheme(
                themeMode = themeMode,
                dynamicColor = isDynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (themeMode == ThemeMode.LIQUID_GLASS) Color.Transparent else MaterialTheme.colorScheme.background
                ) {
                    if (isAppLocked) {
                        LockScreen(
                            onUnlockClick = { showBiometricPrompt() }
                        )
                    } else {
                        MainScreen(
                            viewModel = viewModel,
                            onTriggerBiometricPrompt = {
                                showBiometricPrompt(forEnabling = true)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (viewModel.isBiometricEnabled.value) {
            viewModel.setAppLocked(true)
            showBiometricPrompt()
        }
    }

    override fun onStop() {
        super.onStop()
        if (viewModel.isBiometricEnabled.value) {
            viewModel.setAppLocked(true)
        }
    }

    private fun showBiometricPrompt(forEnabling: Boolean = false) {
        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    if (forEnabling) {
                        viewModel.setBiometricEnabled(true)
                    }
                    viewModel.setAppLocked(false)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If user cancelled enabling, keep it disabled
                    if (forEnabling) {
                        viewModel.setBiometricEnabled(false)
                    }
                }
            }
        )

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (_: Exception) {
            // In environments without biometric or keyguard, unlock safely
            viewModel.setAppLocked(false)
        }
    }
}
