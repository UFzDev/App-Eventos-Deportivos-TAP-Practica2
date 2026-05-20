package ufz.dev.eventosdeportivos.ui.matches

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ListenerRegistration
import ufz.dev.eventosdeportivos.R
import ufz.dev.eventosdeportivos.data.sports.FixtureResponseItem
import ufz.dev.eventosdeportivos.data.network.FavoritesManager

/**
 * Adaptador de Partidos Históricos con soporte de Favoritos en tiempo real en Firestore.
 */
class MatchAdapter(
    private var matchList: List<FixtureResponseItem>,
    private val onMatchClick: (FixtureResponseItem) -> Unit,
    private val onFavoriteClick: (FixtureResponseItem) -> Unit
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_match, parent, false)
        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        holder.bind(matchList[position], onMatchClick, onFavoriteClick)
    }

    override fun getItemCount(): Int = matchList.size

    fun updateList(newList: List<FixtureResponseItem>) {
        matchList = newList
        notifyDataSetChanged()
    }

    class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgHomeBadge: ImageView = itemView.findViewById(R.id.img_home_badge)
        private val imgAwayBadge: ImageView = itemView.findViewById(R.id.img_away_badge)
        private val tvHomeName: TextView = itemView.findViewById(R.id.tv_home_name)
        private val tvAwayName: TextView = itemView.findViewById(R.id.tv_away_name)
        private val tvScore: TextView = itemView.findViewById(R.id.tv_match_score)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_match_date)
        private val tvLeague: TextView = itemView.findViewById(R.id.tv_match_league)
        private val btnFavorite: ImageView = itemView.findViewById(R.id.btn_match_favorite)

        private var favoriteListener: ListenerRegistration? = null

        fun bind(
            match: FixtureResponseItem,
            onClick: (FixtureResponseItem) -> Unit,
            onFavClick: (FixtureResponseItem) -> Unit
        ) {
            // Cancelar el escuchador anterior para evitar leaks de memoria
            favoriteListener?.remove()
            favoriteListener = null

            tvHomeName.text = match.teams.home.name
            tvAwayName.text = match.teams.away.name
            
            tvDate.text = match.fixture.date?.take(10) ?: "Programado"
            
            val homeScore = match.goals.home
            val awayScore = match.goals.away
            if (homeScore != null && awayScore != null) {
                tvScore.text = "$homeScore - $awayScore"
            } else {
                tvScore.text = "VS"
            }

            tvLeague.text = match.league.name

            val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

            val homeLogoUrl = match.teams.home.logo?.replace("http://", "https://")
            val homeGlideUrl = if (!homeLogoUrl.isNullOrEmpty()) {
                com.bumptech.glide.load.model.GlideUrl(
                    homeLogoUrl,
                    com.bumptech.glide.load.model.LazyHeaders.Builder()
                        .addHeader("User-Agent", userAgent)
                        .build()
                )
            } else {
                null
            }

            Glide.with(itemView.context)
                .load(homeGlideUrl)
                .placeholder(R.color.outline_border)
                .error(R.drawable.ic_refresh)
                .fitCenter()
                .into(imgHomeBadge)

            val awayLogoUrl = match.teams.away.logo?.replace("http://", "https://")
            val awayGlideUrl = if (!awayLogoUrl.isNullOrEmpty()) {
                com.bumptech.glide.load.model.GlideUrl(
                    awayLogoUrl,
                    com.bumptech.glide.load.model.LazyHeaders.Builder()
                        .addHeader("User-Agent", userAgent)
                        .build()
                )
            } else {
                null
            }

            Glide.with(itemView.context)
                .load(awayGlideUrl)
                .placeholder(R.color.outline_border)
                .error(R.drawable.ic_refresh)
                .fitCenter()
                .into(imgAwayBadge)

            // Escuchar el estado de favorito en tiempo real
            val itemId = match.fixture.id.toString()
            if (FavoritesManager.isUserRegistered()) {
                favoriteListener = FavoritesManager.checkIsFavoritedRealTime(itemId) { isFav ->
                    btnFavorite.setImageResource(if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart)
                }
            } else {
                btnFavorite.setImageResource(R.drawable.ic_heart)
            }

            btnFavorite.setOnClickListener { onFavClick(match) }
            itemView.setOnClickListener { onClick(match) }
        }
    }
}
