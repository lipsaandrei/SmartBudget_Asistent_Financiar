package com.example.smartbudget_asistent_financiar

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartbudget_asistent_financiar.ui.auth.AuthScreen
import com.example.smartbudget_asistent_financiar.ui.auth.AuthState
import com.example.smartbudget_asistent_financiar.ui.auth.AuthViewModel
import com.example.smartbudget_asistent_financiar.ui.navigation.SmartBudgetNavGraph
import com.example.smartbudget_asistent_financiar.ui.theme.SmartBudget_Asistent_FinanciarTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("smartbudget_prefs", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "English") ?: "English"
        val locale = if (language == "Romanian") Locale("ro") else Locale("en")
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        enableEdgeToEdge()
        setContent {
            SmartBudget_Asistent_FinanciarTheme {
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.authState.collectAsState()

                when (authState) {
                    AuthState.SignedIn, AuthState.Guest -> SmartBudgetNavGraph(
                        onSignOut = { authViewModel.signOut() }
                    )
                    else -> AuthScreen(viewModel = authViewModel)
                }
            }
        }
    }
}
