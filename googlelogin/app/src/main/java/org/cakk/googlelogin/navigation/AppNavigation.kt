package org.cakk.googlelogin.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.cakk.googlelogin.presentation.home.HomeScreen
import org.cakk.googlelogin.presentation.login.LoginScreen
import org.cakk.googlelogin.presentation.login.LoginViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    launchGoogleSignIn: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel()
            val loginState by loginViewModel.state.collectAsStateWithLifecycle()

            LoginScreen(
                viewModel = loginViewModel,
                onSignInSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                launchGoogleSignIn = launchGoogleSignIn
            )

            // Handle Google Sign-In result
            LaunchedEffect(loginState.user) {
                loginState.user?.let { user ->
                    // Handle successful sign in if needed
                }
            }
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}