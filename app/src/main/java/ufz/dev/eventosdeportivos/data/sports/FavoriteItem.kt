package ufz.dev.eventosdeportivos.data.sports

/**
 * Clase de datos que representa un favorito guardado en Firestore de forma polimórfica.
 */
data class FavoriteItem(
    val id: String = "",
    val type: String = "", // "news", "team", "match"
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val dataJson: String = "", // Representación JSON completa del objeto para deserialización instantánea
    val timestamp: Long = System.currentTimeMillis()
)
