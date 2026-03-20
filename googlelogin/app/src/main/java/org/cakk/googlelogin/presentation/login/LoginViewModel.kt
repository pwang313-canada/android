package org.cakk.googlelogin.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.cakk.googlelogin.data.model.User

data class LoginState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val errorMessage: String? = null,
    val isSignInSuccessful: Boolean = false
)

class LoginViewModel : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun handleGoogleSignInResult(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(credential).await()

                authResult.user?.let { firebaseUser ->
                    val user = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName,
                        photoUrl = firebaseUser.photoUrl,
                        isEmailVerified = firebaseUser.isEmailVerified
                    )

                    _state.update {
                        it.copy(
                            isLoading = false,
                            user = user,
                            isSignInSuccessful = true,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Sign in failed",
                        isSignInSuccessful = false
                    )
                }
            }
        }
    }

    fun handleGoogleSignInResultError(message: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = message,
                    isSignInSuccessful = false
                )
            }
        }
    }

    fun resetState() {
        _state.update { LoginState() }
    }
}