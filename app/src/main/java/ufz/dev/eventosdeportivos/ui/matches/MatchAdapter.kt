package ufz.dev.eventosdeportivos.ui.matches

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ufz.dev.eventosdeportivos.R
import ufz.dev.eventosdeportivos.data.sports.FixtureResponseItem

class MatchAdapter(
    private var matchList: List<FixtureResponseItem>,
    private val onMatchClick: (FixtureResponseItem) -> Unit
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_match, parent, false)
        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        holder.bind(matchList[position], onMatchClick)
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

        fun bind(match: FixtureResponseItem, onClick: (FixtureResponseItem) -> Unit) {
            tvHomeName.text = match.teams.home.name
            tvAwayName.text = match.teams.away.name
            
            // Recortar la fecha del formato ISO-8601 (ej: 2026-05-20T14:00:00+00:00 -> 2026-05-20)
            tvDate.text = match.fixture.date?.take(10) ?: "Programado"
            
            // Mostrar puntuación o "VS"
            val homeScore = match.goals.home
            val awayScore = match.goals.away
            if (homeScore != null && awayScore != null) {
                tvScore.text = "$homeScore - $awayScore"
            } else {
                tvScore.text = "VS"
            }

            // Nombre de la liga
            tvLeague.text = match.league.name

            val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

            // Glide: Carga segura de escudo Local con User-Agent
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

            // Glide: Carga segura de escudo Visitante con User-Agent
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

            itemView.setOnClickListener { onClick(match) }
        }
    }
}
