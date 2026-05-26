package com.example.smartbudget_asistent_financiar.ui.screens.home

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(private val auth: FirebaseAuth) : ViewModel() {
    val displayName: String get() = auth.currentUser?.displayName ?: "Guest"
    val email: String get() = auth.currentUser?.email ?: "Not signed in"
    val isGuest: Boolean get() = auth.currentUser == null
    val initials: String get() = auth.currentUser?.displayName
        ?.split(" ")
        ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
        ?.take(2)
        ?.joinToString("")
        ?: "G"
}
