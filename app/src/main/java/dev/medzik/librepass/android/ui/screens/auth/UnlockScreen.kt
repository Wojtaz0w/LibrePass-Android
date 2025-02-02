package dev.medzik.librepass.android.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import dev.medzik.android.composables.LoadingButton
import dev.medzik.android.composables.TextInputField
import dev.medzik.android.composables.TopBar
import dev.medzik.android.composables.res.Text
import dev.medzik.android.cryptoutils.KeyStore
import dev.medzik.libcrypto.Argon2
import dev.medzik.libcrypto.EncryptException
import dev.medzik.librepass.android.R
import dev.medzik.librepass.android.data.getRepository
import dev.medzik.librepass.android.ui.Screen
import dev.medzik.librepass.android.utils.Biometric
import dev.medzik.librepass.android.utils.Navigation.navigate
import dev.medzik.librepass.android.utils.Remember.rememberLoadingState
import dev.medzik.librepass.android.utils.Remember.rememberStringData
import dev.medzik.librepass.android.utils.SecretStore
import dev.medzik.librepass.android.utils.Toast.showToast
import dev.medzik.librepass.android.utils.UserSecrets
import dev.medzik.librepass.android.utils.runGC
import dev.medzik.librepass.client.utils.Cryptography
import dev.medzik.librepass.client.utils.Cryptography.computePasswordHash
import dev.medzik.librepass.client.utils.Cryptography.generateKeyPairFromPrivate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun UnlockScreen(navController: NavController) {
    // context must be FragmentActivity to show biometric prompt
    val context = LocalContext.current as FragmentActivity

    val scope = rememberCoroutineScope()

    var loading by rememberLoadingState()
    var password by rememberStringData()

    val credentials = context.getRepository().credentials.get()!!

    fun onUnlock(password: String) {
        // disable button
        loading = true

        scope.launch(Dispatchers.IO) {
            try {
                loading = true

                // compute base password hash
                val passwordHash = computePasswordHash(
                    password = password,
                    email = credentials.email,
                    argon2Function = Argon2(
                        32,
                        credentials.parallelism,
                        credentials.memory,
                        credentials.iterations
                    )
                )

                val keyPair = generateKeyPairFromPrivate(passwordHash)

                if (keyPair.publicKey != credentials.publicKey)
                    throw EncryptException("Invalid password")

                // run gc cycle after computing password hash
                runGC()

                val secretKey =
                    Cryptography.computeSharedKey(keyPair.privateKey, credentials.publicKey)

                SecretStore.save(
                    context,
                    UserSecrets(
                        privateKey = keyPair.privateKey,
                        secretKey = secretKey
                    )
                )

                // run only if loading is true (if no error occurred)
                if (loading) {
                    scope.launch(Dispatchers.Main) {
                        navController.navigate(
                            screen = Screen.Dashboard,
                            disableBack = true
                        )
                    }
                }
            } catch (e: EncryptException) {
                // if password is invalid
                loading = false
                context.showToast(R.string.Error_InvalidCredentials)
            }
        }
    }

    fun showBiometric() {
        Biometric.showBiometricPrompt(
            context = context,
            cipher = KeyStore.initCipherForDecryption(
                alias = Biometric.PrivateKeyAlias,
                initializationVector = credentials.biometricProtectedPrivateKeyIV!!,
                requireAuthentication = true
            ),
            onAuthenticationSucceeded = { cipher ->
                val privateKey =
                    KeyStore.decrypt(cipher, credentials.biometricProtectedPrivateKey!!)

                val secretKey = Cryptography.computeSharedKey(privateKey, credentials.publicKey)

                SecretStore.save(
                    context,
                    UserSecrets(
                        privateKey = privateKey,
                        secretKey = secretKey
                    )
                )

                navController.navigate(
                    screen = Screen.Dashboard,
                    disableBack = true
                )
            },
            onAuthenticationFailed = { }
        )
    }

    LaunchedEffect(scope) {
        if (credentials.biometricEnabled)
            showBiometric()
    }

    Scaffold(
        topBar = { TopBar(R.string.TopBar_Unlock) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            TextInputField(
                label = R.string.InputField_Password,
                value = password,
                onValueChange = { password = it },
                hidden = true,
                keyboardType = KeyboardType.Password
            )

            LoadingButton(
                loading = loading,
                onClick = { onUnlock(password) },
                enabled = password.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .padding(horizontal = 80.dp)
            ) {
                Text(R.string.Button_Unlock)
            }

            if (credentials.biometricEnabled) {
                OutlinedButton(
                    onClick = { showBiometric() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .padding(horizontal = 80.dp)
                ) {
                    Text(R.string.Button_UseBiometric)
                }
            }
        }
    }
}
