package com.developers.client

import com.developers.core.ui.PanAppGlassFilterChip

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore

data class CatalogRating(val average: Double?, val votes: Long)

internal enum class CatalogSort(val label: Int) {
    Default(R.string.client_catalog_default),
    Rating(R.string.client_catalog_top_rated),
    Orders(R.string.client_catalog_most_ordered)
}

internal fun rankProducts(products: List<Product>, sort: CatalogSort, ratings: Map<String, CatalogRating>): List<Product> =
    when (sort) {
        CatalogSort.Default -> products
        CatalogSort.Rating -> products.sortedWith(
            compareByDescending<Product> { ratings[it.id]?.average ?: -1.0 }
                .thenByDescending { ratings[it.id]?.votes ?: 0L }
        )
        CatalogSort.Orders -> products.sortedByDescending { it.pedidosRegistrados }
    }

internal data class CatalogProducts(
    val products: List<Product> = emptyList(),
    val loading: Boolean = true,
    val loaded: Boolean = false
)

@Composable
internal fun rememberCatalogProducts(): CatalogProducts {
    var catalog by remember { mutableStateOf(CatalogProducts()) }
    DisposableEffect(Unit) {
        val listener = FirebaseFirestore.getInstance().collection("productos")
            .addSnapshotListener { snapshot, error ->
                catalog = if (error == null && snapshot != null) {
                    CatalogProducts(
                        products = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Product::class.java)?.copy(id = doc.id)
                        },
                        loading = false,
                        loaded = true
                    )
                } else catalog.copy(loading = false)
            }
        onDispose { listener.remove() }
    }
    return catalog
}

@Composable
internal fun rememberCatalogRatings(products: List<Product>, uid: String, revision: Int): Map<String, CatalogRating> {
    val ids = remember(products) { products.map { it.id }.filter(String::isNotBlank).distinct().sorted() }
    val ratings = remember(ids, uid) { mutableStateMapOf<String, CatalogRating>() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, ids, uid, revision) {
        var generation = 0
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && ids.isNotEmpty() &&
                FirebaseAuth.getInstance().currentUser?.uid == uid
            ) {
                val request = ++generation
                val db = FirebaseFirestore.getInstance()
                val count = AggregateField.count()
                val average = AggregateField.average("estrellas")
                ids.forEach { id ->
                    db.collection("productos").document(id).collection("valoraciones")
                        .whereGreaterThanOrEqualTo("partida", 0)
                        .aggregate(count, average).get(AggregateSource.SERVER)
                        .addOnSuccessListener { result ->
                            if (request == generation) {
                                ratings[id] = CatalogRating(result.get(average), result.get(count))
                            }
                        }
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            generation++
            lifecycle.removeObserver(observer)
        }
    }
    return ratings
}

@Composable
internal fun CatalogSortChips(selected: CatalogSort, onSelect: (CatalogSort) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CatalogSort.entries.forEach { sort ->
            PanAppGlassFilterChip(
                selected = selected == sort,
                onClick = { onSelect(sort) },
                label = { Text(stringResource(sort.label)) }
            )
        }
    }
}

@Composable
internal fun CatalogPhotoRating(rating: CatalogRating?, modifier: Modifier = Modifier) {
    val average = rating?.average ?: return
    if (rating.votes <= 0) return
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            stringResource(R.string.client_catalog_score, average, rating.votes),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
internal fun CatalogRatingLabel(rating: CatalogRating?) {
    val average = rating?.average
    val label = when {
        rating == null -> stringResource(R.string.client_catalog_rating_unavailable)
        average == null || rating.votes == 0L -> stringResource(R.string.client_rating_none)
        else -> stringResource(R.string.client_catalog_score, average, rating.votes)
    }
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
