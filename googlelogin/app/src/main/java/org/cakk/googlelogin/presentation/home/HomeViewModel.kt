package org.cakk.googlelogin.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cakk.googlelogin.data.model.User
import org.cakk.googlelogin.domain.repository.IAuthRepository
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val _user = MutableStateFlow(authRepository.currentUser)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun signOut(onSignOutComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.signOut()
                .onSuccess {
                    onSignOutComplete()
                }
            _isLoading.value = false
        }
    }
}