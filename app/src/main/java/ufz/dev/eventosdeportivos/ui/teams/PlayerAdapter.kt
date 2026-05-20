package ufz.dev.eventosdeportivos.ui.teams

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ufz.dev.eventosdeportivos.R
import ufz.dev.eventosdeportivos.data.sports.SquadPlayerDetails

class PlayerAdapter(
    private var playerList: List<SquadPlayerDetails>
) : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_player, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(playerList[position])
    }

    override fun getItemCount(): Int = playerList.size

    fun updateList(newList: List<SquadPlayerDetails>) {
        playerList = newList
        notifyDataSetChanged()
    }

    class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgPhoto: ImageView = itemView.findViewById(R.id.img_player_photo)
        private val tvName: TextView = itemView.findViewById(R.id.tv_player_name)
        private val tvPosition: TextView = itemView.findViewById(R.id.tv_player_position)
        private val tvNumber: TextView = itemView.findViewById(R.id.tv_player_number)

        fun bind(player: SquadPlayerDetails) {
            tvName.text = player.name
            
            // Traducir o formatear posición en español para una visualización premium
            tvPosition.text = when (player.position?.lowercase()) {
                "goalkeeper" -> "Guardameta"
                "defender" -> "Defensa"
                "midfielder" -> "Mediocampista"
                "attacker" -> "Delantero"
                else -> player.position ?: "Jugador"
            }

            // Mostrar el dorsal o "-" si no tiene
            tvNumber.text = player.number?.toString() ?: "-"
            tvNumber.visibility = if (player.number != null) View.VISIBLE else View.GONE

            // Forzar HTTPS en las URLs de imágenes de API-Football para evitar bloqueo de Cleartext Traffic
            val securePhotoUrl = player.photo?.replace("http://", "https://")

            // Usar GlideUrl con User-Agent estándar para evitar bloqueos HTTP 403 del CDN de API-Football
            val glideUrl = if (!securePhotoUrl.isNullOrEmpty()) {
                com.bumptech.glide.load.model.GlideUrl(
                    securePhotoUrl,
                    com.bumptech.glide.load.model.LazyHeaders.Builder()
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .build()
                )
            } else {
                null
            }

            // Glide: Cargar la Foto en círculo con placeholders
            Glide.with(itemView.context)
                .load(glideUrl)
                .placeholder(R.drawable.ic_teams)
                .error(R.drawable.ic_teams)
                .circleCrop()
                .into(imgPhoto)
        }
    }
}
