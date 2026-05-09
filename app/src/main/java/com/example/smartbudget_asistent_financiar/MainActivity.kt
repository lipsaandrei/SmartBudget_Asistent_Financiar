package com.example.smartbudget_asistent_financiar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
                SmartBudgetNavGraph()
            }
        }
    }
}