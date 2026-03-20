package org.cakk.googlelogin.domain.repository

import kotlinx.coroutines.flow.Flow
import org.cakk.googlelogin.data.model.User

interface IAuthRepository {
    val currentUser: User?
    val authState: Flow<User?>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signOut(): Result<Unit>
}