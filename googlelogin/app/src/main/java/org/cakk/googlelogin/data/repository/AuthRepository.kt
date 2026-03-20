package org.cakk.googlelogin.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.cakk.googlelogin.data.model.User
import org.cakk.googlelogin.domain.repository.IAuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : IAuthRepository {

    override val currentUser: User?
        get() = firebaseAuth.currentUser?.toUser()

    override val authState: Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toUser())
        }

        firebaseAuth.addAuthStateListener(authStateListener)

        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = firebaseAuth.signInWithCredential(credential).await()
        authResult.user?.toUser()?.let { user ->
            Result.success(user)
        } ?: Result.failure(Exception("User not found"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun signOut(): Result<Unit> = try {
        firebaseAuth.signOut()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun FirebaseUser.toUser(): User = User(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl,
        isEmailVerified = isEmailVerified
    )
}