package ufz.dev.eventosdeportivos.ui.teams

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ListenerRegistration
import ufz.dev.eventosdeportivos.R
import ufz.dev.eventosdeportivos.data.sports.TeamDetails
import ufz.dev.eventosdeportivos.data.network.FavoritesManager

/**
 * Adaptador de Equipos con botón de favoritos en tiempo real respaldado por Firestore.
 */
class TeamAdapter(
    private var teamList: List<TeamDetails>,
    private val onTeamClick: (TeamDetails) -> Unit,
    private val onFavoriteClick: (TeamDetails) -> Unit
) : RecyclerView.Adapter<TeamAdapter.TeamViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_team, parent, false)
        return TeamViewHolder(view)
    }

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        holder.bind(teamList[position], onTeamClick, onFavoriteClick)
    }

    override fun getItemCount(): Int = teamList.size

    fun updateList(newList: List<TeamDetails>) {
        teamList = newList
        notifyDataSetChanged()
    }

    class TeamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgBadge: ImageView = itemView.findViewById(R.id.img_team_badge)
        private val tvName: TextView = itemView.findViewById(R.id.tv_team_name)
        private val tvFounded: TextView = itemView.findViewById(R.id.tv_team_founded)
        private val btnFavorite: ImageView = itemView.findViewById(R.id.btn_team_favorite)
        private val btnInfo: ImageView = itemView.findViewById(R.id.img_team_info)

        private var favoriteListener: ListenerRegistration? = null

        fun bind(
            team: TeamDetails,
            onClick: (TeamDetails) -> Unit,
            onFavClick: (TeamDetails) -> Unit
        ) {
            // Cancelar el escuchador anterior para evitar leaks
            favoriteListener?.remove()
            favoriteListener = null

            tvName.text = team.name
            tvFounded.text = if (team.founded != null) "FUNDADO EN ${team.founded}" else "CLUB HISTÓRICO"

            val secureBadgeUrl = team.logo?.replace("http://", "https://")
            val glideUrl = if (!secureBadgeUrl.isNullOrEmpty()) {
                com.bumptech.glide.load.model.GlideUrl(
                    secureBadgeUrl,
                    com.bumptech.glide.load.model.LazyHeaders.Builder()
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .build()
                )
            } else {
                null
            }

            Glide.with(itemView.context)
                .load(glideUrl)
                .placeholder(R.drawable.ic_refresh)
                .error(R.drawable.ic_teams)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        android.util.Log.e(
                            "GlideError",
                            "Falla al cargar escudo de ${team.name}. URL: $secureBadgeUrl. Razón: ${e?.message}"
                        )
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
                .fitCenter()
                .into(imgBadge)

            // Escuchar el estado de favorito en tiempo real
            val itemId = team.id.toString()
            if (FavoritesManager.isUserRegistered()) {
                favoriteListener = FavoritesManager.checkIsFavoritedRealTime(itemId) { isFav ->
                    btnFavorite.setImageResource(if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart)
                }
            } else {
                btnFavorite.setImageResource(R.drawable.ic_heart)
            }

            btnFavorite.setOnClickListener { onFavClick(team) }
            btnInfo.setOnClickListener { onClick(team) }
            itemView.setOnClickListener { onClick(team) }
        }
    }
}
