package com.example.smartbudget_asistent_financiar.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartbudget_asistent_financiar.data.sync.SyncManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Loading : AuthState()
    object SignedOut : AuthState()
    object SigningIn : AuthState()
    object SignedIn : AuthState()
    object Guest : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        _authState.value = if (auth.currentUser != null) AuthState.SignedIn else AuthState.SignedOut
    }

    fun onGoogleSignInResult(account: GoogleSignInAccount) {
        _authState.value = AuthState.SigningIn
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                viewModelScope.launch {
                    runCatching { syncManager.syncOnSignIn() }
                    _authState.value = AuthState.SignedIn
                }
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.message ?: "Sign-in failed")
            }
    }

    fun continueAsGuest() {
        _authState.value = AuthState.Guest
    }

    fun onSignInError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { syncManager.clearLocalData() }
            auth.signOut()
            _authState.value = AuthState.SignedOut
        }
    }
}
