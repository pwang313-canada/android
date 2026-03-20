package org.cakk.googlelogin.domain.usecase

import org.cakk.googlelogin.data.model.User
import org.cakk.googlelogin.domain.repository.IAuthRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<User> {
        return authRepository.signInWithGoogle(idToken)
    }
}