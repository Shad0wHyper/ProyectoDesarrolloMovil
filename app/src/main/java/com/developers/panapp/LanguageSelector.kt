package com.developers.panapp

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.core.os.LocaleListCompat
import com.developers.core.R as TextR

/** Available before sign-in and in every role, without adding navigation destinations. */
@Composable
fun LanguageSelector() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val tags = listOf("", "es-MX", "en", "pt-PT")
    val labels = listOf(
        stringResource(TextR.string.language_system),
        stringResource(TextR.string.language_spanish_mexico),
        stringResource(TextR.string.language_english),
        stringResource(TextR.string.language_portuguese_portugal)
    )
    val selected = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val selectedIndex = tags.indexOf(selected).takeIf { it >= 0 } ?: 0
    Row(Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Language, contentDescription = null)
            Text(stringResource(TextR.string.language_selection, labels[selectedIndex]))
        }
    }
    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text(stringResource(TextR.string.language_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    tags.forEachIndexed { index, tag ->
                        Row(
                            modifier = Modifier.fillMaxWidth().selectable(
                                selected = selected == tag,
                                role = Role.RadioButton,
                                onClick = {
                                    expanded = false
                                    // Empty means follow the system; do not persist a snapshot of it.
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(tag)
                                    )
                                }
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected == tag, onClick = null)
                            Text(labels[index])
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { expanded = false }) {
                    Text(stringResource(TextR.string.pan_close))
                }
            }
        )
    }
}
