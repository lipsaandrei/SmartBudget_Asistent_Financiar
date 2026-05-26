package com.example.smartbudget_asistent_financiar

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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
