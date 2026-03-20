package org.cakk.googlelogin.data.model

import android.net.Uri

data class User(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: Uri?,
    val isEmailVerified: Boolean = false
)