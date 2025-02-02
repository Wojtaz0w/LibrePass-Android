package dev.medzik.librepass.android.ui.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.medzik.android.composables.res.Text
import dev.medzik.android.composables.sheet.BottomSheetState
import dev.medzik.android.composables.sheet.PickerBottomSheet
import dev.medzik.android.composables.sheet.rememberBottomSheetState
import dev.medzik.librepass.android.R
import dev.medzik.librepass.android.utils.SHORTEN_NAME_LENGTH
import dev.medzik.librepass.android.utils.SHORTEN_USERNAME_LENGTH
import dev.medzik.librepass.android.utils.shortenName
import dev.medzik.librepass.client.api.CipherClient
import dev.medzik.librepass.types.cipher.Cipher
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CipherCard(
    cipher: Cipher,
    onClick: (Cipher) -> Unit,
    onEdit: (Cipher) -> Unit,
    onDelete: (Cipher) -> Unit
) {
    val scope = rememberCoroutineScope()

    val sheetState = rememberBottomSheetState()

    fun showMoreOptions() {
        scope.launch { sheetState.expand() }
    }

    fun getDomain(): String? {
        val uris = cipher.loginData?.uris
        return if (!uris.isNullOrEmpty()) uris[0] else null
    }

    val cipherData = cipher.loginData!!

    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick(cipher) },
                    onLongClick = { showMoreOptions() }
                )
                .padding(vertical = 8.dp, horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val domain = getDomain()
            if (domain != null) {
                AsyncImage(
                    model = CipherClient.getFavicon(domain = domain),
                    contentDescription = null,
                    error = rememberVectorPainter(Icons.Default.Person),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Image(
                    Icons.Default.Person,
                    contentDescription = null,
                )
            }

            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .fillMaxSize()
                    .weight(1f)
            ) {
                Text(
                    text = shortenName(cipherData.name, SHORTEN_NAME_LENGTH),
                    style = MaterialTheme.typography.titleMedium
                )

                val username = cipherData.username
                if (username != null) {
                    Text(
                        text = shortenName(username, SHORTEN_USERNAME_LENGTH),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            IconButton(onClick = { showMoreOptions() }) {
                Icon(Icons.Default.MoreHoriz, contentDescription = null)
            }

            CipherActionsSheet(
                state = sheetState,
                onClick = { onClick(cipher) },
                onEdit = { onEdit(cipher) },
                onDelete = { onDelete(cipher) }
            )
        }
    }
}

@Composable
fun CipherActionsSheet(
    state: BottomSheetState,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    PickerBottomSheet(
        state = state,
        items = listOf(
            R.string.CipherBottomSheet_View,
            R.string.CipherBottomSheet_Edit,
            R.string.CipherBottomSheet_Delete
        ),
        onSelected = {
            when (it) {
                R.string.CipherBottomSheet_View -> {
                    onClick()
                }

                R.string.CipherBottomSheet_Edit -> onEdit()
                R.string.CipherBottomSheet_Delete -> onDelete()
            }
        }
    ) {
        Text(
            text = it,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .fillMaxWidth()
        )
    }
}
