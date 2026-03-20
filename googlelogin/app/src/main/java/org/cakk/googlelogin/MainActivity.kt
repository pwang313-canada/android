package org.cakk.googlelogin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import org.cakk.googlelogin.presentation.home.HomeScreen
import org.cakk.googlelogin.presentation.login.LoginScreen
import org.cakk.googlelogin.presentation.login.LoginViewModel
import org.cakk.googlelogin.ui.theme.GoogleLoginTheme

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var googleSignInClient: GoogleSignInClient

    // Create ViewModel instance that can be accessed from anywhere in the activity
    private lateinit var loginViewModel: LoginViewModel

    // Launcher for legacy Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleLegacySignInResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        credentialManager = CredentialManager.create(this)

        // Initialize ViewModel
        loginViewModel = LoginViewModel()

        // Configure legacy Google Sign-In as fallback
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            GoogleLoginTheme {
                val navController = rememberNavController()

                val startDestination = if (auth.currentUser != null) "home" else "login"

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable("login") {
                        LoginScreen(
                            viewModel = loginViewModel,
                            onSignInSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            launchGoogleSignIn = {
                                // Call the function that handles sign-in
                                startGoogleSignIn()
                            }
                        )
                    }

                    composable("home") {
                        HomeScreen(
                            onSignOut = {
                                signOut()
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun startGoogleSignIn() {
        lifecycleScope.launch {
            // First try with Credential Manager
            tryCredentialManagerSignIn()
        }
    }

    private suspend fun tryCredentialManagerSignIn() {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                this@MainActivity,
                request
            )

            when (val credential = result.credential) {
                is GoogleIdTokenCredential -> {
                    Log.d("GoogleSignIn", "Credential Manager success")
                    loginViewModel.handleGoogleSignInResult(credential.idToken)
                }
                else -> {
                    Log.e("GoogleSignIn", "Unexpected credential type")
                    // Fallback to legacy sign-in
                    startLegacySignIn()
                }
            }

        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            Log.d("GoogleSignIn", "User cancelled sign-in")
            loginViewModel.handleGoogleSignInResultError("Sign-in cancelled")
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            Log.d("GoogleSignIn", "No credentials available, falling back to legacy sign-in")
            // Fallback to legacy sign-in when no credentials are available
            startLegacySignIn()
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "Credential Manager failed", e)
            // Fallback to legacy sign-in for any other error
            startLegacySignIn()
        }
    }

    private fun startLegacySignIn() {
        Log.d("GoogleSignIn", "Using legacy Google Sign-In")
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleLegacySignInResult(result: androidx.activity.result.ActivityResult) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d("GoogleSignIn", "Legacy sign-in successful")
            account.idToken?.let { idToken ->
                loginViewModel.handleGoogleSignInResult(idToken)
            } ?: run {
                loginViewModel.handleGoogleSignInResultError("No ID token received")
            }
        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "Legacy sign-in failed", e)
            loginViewModel.handleGoogleSignInResultError(e.message ?: "Sign-in failed")
        }
    }

    private fun signOut() {
        // Sign out from both Firebase and Google
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            Log.d("GoogleSignIn", "Signed out from Google")
        }
    }
}