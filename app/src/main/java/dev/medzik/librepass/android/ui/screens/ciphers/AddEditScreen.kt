package dev.medzik.librepass.android.ui.screens.ciphers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import dev.medzik.android.composables.LoadingButton
import dev.medzik.android.composables.TextInputFieldBase
import dev.medzik.android.composables.TopBar
import dev.medzik.android.composables.TopBarBackIcon
import dev.medzik.android.composables.res.Text
import dev.medzik.android.composables.settings.SettingsGroup
import dev.medzik.librepass.android.R
import dev.medzik.librepass.android.data.CipherTable
import dev.medzik.librepass.android.data.getRepository
import dev.medzik.librepass.android.ui.Screen
import dev.medzik.librepass.android.utils.Navigation.navigate
import dev.medzik.librepass.android.utils.Remember.rememberLoadingState
import dev.medzik.librepass.android.utils.SHORTEN_NAME_LENGTH
import dev.medzik.librepass.android.utils.SecretStore.getUserSecrets
import dev.medzik.librepass.android.utils.exception.handle
import dev.medzik.librepass.android.utils.shortenName
import dev.medzik.librepass.client.api.CipherClient
import dev.medzik.librepass.types.cipher.Cipher
import dev.medzik.librepass.types.cipher.CipherType
import dev.medzik.librepass.types.cipher.EncryptedCipher
import dev.medzik.librepass.types.cipher.data.CipherLoginData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun CipherAddEditView(
    navController: NavController,
    baseCipher: Cipher? = null
) {
    val context = LocalContext.current

    val userSecrets = context.getUserSecrets()
        ?: return

    val scope = rememberCoroutineScope()
    var loading by rememberLoadingState()
    var cipherData by remember {
        mutableStateOf(baseCipher?.loginData ?: CipherLoginData(name = ""))
    }

    val repository = context.getRepository()
    val credentials = repository.credentials.get()!!
    val cipherRepository = repository.cipher

    val cipherClient = CipherClient(credentials.apiKey)

    // observe username and password from navController
    // used to get password from password generator
    navController
        .currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<String>("password")?.observeForever {
            cipherData = cipherData.copy(password = it)
        }
    // observe for cipher from backstack
    navController
        .currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<String>("cipher")?.observeForever {
            val currentPassword = cipherData.password

            cipherData = Gson().fromJson(it, CipherLoginData::class.java)
            cipherData = cipherData.copy(password = currentPassword)
        }

    // Insert or update cipher
    fun submit() {
        // set loading indicator
        loading = true

        // update existing cipher or create new one
        val cipher = baseCipher?.copy(loginData = cipherData)
            ?: Cipher(
                id = UUID.randomUUID(),
                owner = credentials.userId,
                type = CipherType.Login,
                loginData = cipherData
            )

        scope.launch(Dispatchers.IO) {
            // encrypt cipher
            val encryptedCipher = EncryptedCipher(cipher, userSecrets.secretKey)

            try {
                // insert or update cipher on server
                if (baseCipher == null)
                    cipherClient.insert(encryptedCipher)
                else
                    cipherClient.update(encryptedCipher)

                // insert or update cipher in local database
                val cipherTable = CipherTable(encryptedCipher)
                if (baseCipher == null)
                    cipherRepository.insert(cipherTable)
                else
                    cipherRepository.update(cipherTable)

                scope.launch(Dispatchers.Main) { navController.popBackStack() }
            } catch (e: Exception) {
                loading = false
                e.handle(context)
            }
        }
    }

    @Composable
    fun topBarTitle(): String {
        if (baseCipher == null) return stringResource(R.string.TopBar_AddNewCipher)

        return shortenName(baseCipher.loginData!!.name, SHORTEN_NAME_LENGTH)
    }

    Scaffold(
        topBar = {
            TopBar(
                title = topBarTitle(),
                navigationIcon = { TopBarBackIcon(navController) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TextInputFieldBase(
                label = R.string.CipherField_Name,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                value = cipherData.name,
                onValueChange = { cipherData = cipherData.copy(name = it) }
            )

            SettingsGroup(R.string.CipherField_Group_Login) {
                TextInputFieldBase(
                    label = R.string.CipherField_Username,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    value = cipherData.username,
                    onValueChange = { cipherData = cipherData.copy(username = it) }
                )

                TextInputFieldBase(
                    label = R.string.CipherField_Password,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    value = cipherData.password,
                    onValueChange = { cipherData = cipherData.copy(password = it) },
                    hidden = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            // save cipher data as json to navController
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                "cipher",
                                Gson().toJson(cipherData)
                            )

                            navController.navigate(Screen.PasswordGenerator)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null
                            )
                        }
                    }
                )
            }

            SettingsGroup(R.string.CipherField_Group_Website) {
                // show field for each uri
                cipherData.uris?.forEachIndexed { index, uri ->
                    TextInputFieldBase(
                        label = stringResource(R.string.CipherField_URL) + " ${index + 1}",
                        modifier = Modifier.fillMaxWidth(),
                        value = uri,
                        onValueChange = {
                            cipherData = cipherData.copy(
                                uris = cipherData.uris.orEmpty().toMutableList().apply {
                                    this[index] = it
                                }
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                cipherData = cipherData.copy(
                                    uris = cipherData.uris.orEmpty().toMutableList().apply {
                                        this.removeAt(index)
                                    }
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }

                // button for adding more fields
                Button(
                    onClick = {
                        cipherData = cipherData.copy(
                            uris = cipherData.uris.orEmpty() + ""
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 60.dp)
                        .padding(top = 8.dp)
                ) {
                    Text(R.string.Button_AddField)
                }
            }

            SettingsGroup(R.string.CipherField_Group_Other) {
                TextInputFieldBase(
                    label = R.string.CipherField_Notes,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    value = cipherData.notes,
                    onValueChange = { cipherData = cipherData.copy(notes = it) }
                )
            }

            LoadingButton(
                loading = loading,
                onClick = { submit() },
                enabled = cipherData.name.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .padding(horizontal = 40.dp)
            ) {
                Text(baseCipher?.let { R.string.Button_Save } ?: R.string.Button_Add)
            }
        }
    }
}
