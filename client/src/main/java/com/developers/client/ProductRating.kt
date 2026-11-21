package com.developers.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

@Composable
internal fun ProductRating(productId: String, orderId: String, lineIndex: Int, onSaved: () -> Unit) {
    if (productId.isBlank()) return
    val auth = remember { FirebaseAuth.getInstance() }
    var uid by remember {
        mutableStateOf(auth.currentUser?.takeUnless { it.isAnonymous }?.uid)
    }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            uid = it.currentUser?.takeUnless { user -> user.isAnonymous }?.uid
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.client_rating_title), style = MaterialTheme.typography.titleMedium)
        val customer = uid
        if (customer == null) {
            Text(stringResource(R.string.client_rating_sign_in))
        } else {
            key(productId, customer) { CustomerRating(productId, customer, auth, orderId, lineIndex, onSaved) }
        }
    }
}

@Composable
private fun CustomerRating(productId: String, uid: String, auth: FirebaseAuth, orderId: String, lineIndex: Int, onSaved: () -> Unit) {
    val ratings = remember(productId) {
        FirebaseFirestore.getInstance().collection("productos").document(productId).collection("valoraciones")
    }
    val vote = remember(ratings, uid) { ratings.document(uid) }
    var stored by remember { mutableStateOf<Int?>(null) }
    var draft by remember { mutableIntStateOf(0) }
    var voteLoaded by remember { mutableStateOf(false) }
    var voteError by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var voteRetry by remember { mutableIntStateOf(0) }
    var statsRefresh by remember { mutableIntStateOf(0) }
    var stats by remember { mutableStateOf<Pair<Long, Double?>>(0L to null) }
    var statsLoading by remember { mutableStateOf(true) }
    var statsError by remember { mutableStateOf(false) }

    DisposableEffect(vote, voteRetry) {
        var active = true
        voteError = false
        vote.get(Source.SERVER).addOnCompleteListener { task ->
            if (active) {
                if (task.isSuccessful) {
                    val previous = task.result.getLong("estrellas")?.takeIf { it in 1..5 }?.toInt()
                    stored = previous.takeIf { task.result.getString("pedidoId") != null }
                    draft = previous ?: 0
                    voteLoaded = true
                } else voteError = true
            }
        }
        onDispose { active = false }
    }
    DisposableEffect(ratings, statsRefresh) {
        var active = true
        statsLoading = true
        statsError = false
        val count = AggregateField.count()
        val average = AggregateField.average("estrellas")
        ratings.whereGreaterThanOrEqualTo("partida", 0)
            .aggregate(count, average).get(AggregateSource.SERVER).addOnCompleteListener { task ->
            if (active) {
                statsLoading = false
                if (task.isSuccessful) {
                    stats = task.result.get(count) to task.result.get(average)
                } else statsError = true
            }
        }
        onDispose { active = false }
    }

    val (count, average) = stats
    when {
        statsLoading && count == 0L -> LinearProgressIndicator(Modifier.fillMaxWidth())
        statsError -> {
            Text(stringResource(R.string.client_rating_stats_error), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { statsRefresh++ }) { Text(stringResource(R.string.client_rating_retry)) }
        }
        count == 0L || average == null -> Text(stringResource(R.string.client_rating_none))
        else -> {
            Text(stringResource(R.string.client_rating_average, average))
            Text(pluralStringResource(R.plurals.client_rating_votes, if (count == 1L) 1 else 2, count))
        }
    }

    when {
        voteError -> {
            Text(stringResource(R.string.client_rating_load_error), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { voteRetry++ }) { Text(stringResource(R.string.client_rating_retry)) }
        }
        voteLoaded -> {
            Text(stringResource(R.string.client_rating_yours), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.widthIn(max = 260.dp).fillMaxWidth().selectableGroup()) {
                for (stars in 1..5) {
                    val description = stringResource(R.string.client_rating_star, stars)
                    Box(
                        Modifier.weight(1f).height(48.dp)
                            .selectable(
                                selected = draft == stars, enabled = !saving, role = Role.RadioButton,
                                onClick = { draft = stars; saved = false; saveError = false }
                            )
                            .semantics { contentDescription = description },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (stars <= draft) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (stars <= draft) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            OutlinedButton(
                enabled = draft in 1..5 && draft != stored && !saving,
                onClick = {
                    if (auth.currentUser?.uid != uid) return@OutlinedButton
                    val stars = draft
                    saving = true
                    saveError = false
                    vote.set(mapOf("estrellas" to stars, "pedidoId" to orderId, "partida" to lineIndex)).addOnCompleteListener { task ->
                        saving = false
                        if (task.isSuccessful) {
                            stored = stars
                            saved = true
                            statsRefresh++
                            onSaved()
                        } else saveError = true
                    }
                }
            ) {
                Text(stringResource(if (saving) R.string.client_rating_saving else R.string.client_rating_save))
            }
            if (saved) Text(stringResource(R.string.client_rating_saved))
            if (saveError) Text(stringResource(R.string.client_rating_save_error), color = MaterialTheme.colorScheme.error)
        }
    }
}
