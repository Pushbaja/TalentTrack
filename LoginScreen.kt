
package com.mad.talenttrack.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.mad.talenttrack.R
import com.mad.talenttrack.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }

    val loading by viewModel.loading
    val errorMessage by viewModel.errorMessage
    val successMessage by viewModel.successMessage

    val context = LocalContext.current

    // Google Sign-In Setup
    val webClientId = stringResource(R.string.default_web_client_id)

    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestEmail()
            .requestProfile()
            .requestIdToken(webClientId)
            .build()
    }

    val googleSignInClient = remember(gso) {
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->

            val intentData = result.data

            val task =
                GoogleSignIn.getSignedInAccountFromIntent(intentData)

            try {
                val account =
                    task.getResult(ApiException::class.java)

                val idToken = account.idToken

                if (!idToken.isNullOrEmpty()) {
                    viewModel.signInWithGoogle(
                        idToken,
                        onLoginSuccess
                    )
                } else {
                    Toast.makeText(
                        context,
                        "Google ID Token is missing. Check Web Client ID.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: ApiException) {

                val errorMsg = when (e.statusCode) {
                    10 ->
                        "Developer Error 10: Check Firebase SHA-1 and Web Client ID."

                    12500 ->
                        "Google Sign-In error 12500. Check Google configuration."

                    12501 ->
                        "Google Sign-In cancelled."

                    else ->
                        "Google Sign-In error: ${e.statusCode}"
                }

                if (e.statusCode != 12501) {
                    Toast.makeText(
                        context,
                        errorMsg,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotDialog = false
            },
            title = {
                Text("Forgot Password")
            },
            text = {
                Column {
                    Text(
                        "Enter your email address to receive a password reset link."
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = {
                            forgotEmail = it
                        },
                        label = {
                            Text("Email")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.forgotPassword(forgotEmail)
                        showForgotDialog = false
                    }
                ) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showForgotDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Login",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Center
        ) {

            Text(
                "Welcome to TalentTrack",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                },
                label = {
                    Text("Email")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                },
                label = {
                    Text("Password")
                },
                visualTransformation =
                    PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(
                    onClick = {
                        showForgotDialog = true
                    }
                ) {
                    Text("Forgot Password?")
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(Modifier.height(8.dp))
            }

            if (successMessage != null) {
                Text(
                    text = successMessage!!,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    viewModel.login(
                        email,
                        password,
                        onLoginSuccess
                    )
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Login")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Google Sign-In Button
            OutlinedButton(
                onClick = {
                    viewModel.clearMessages()

                    // Clear previous Google Sign-In session
                    googleSignInClient.signOut()
                        .addOnCompleteListener {

                            // Open Google account chooser
                            googleSignInLauncher.launch(
                                googleSignInClient.signInIntent
                            )
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue with Google")
            }

            Spacer(Modifier.height(16.dp))

            TextButton(
                onClick = onRegisterClick,
                enabled = !loading
            ) {
                Text("Don't have an account? Sign Up")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onLoginSuccess = {},
        onRegisterClick = {}
    )
}