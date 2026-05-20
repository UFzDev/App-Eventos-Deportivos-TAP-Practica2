package ufz.dev.eventosdeportivos.ui.matches

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ufz.dev.eventosdeportivos.R
import ufz.dev.eventosdeportivos.data.sports.FixtureResponseItem

class LiveMatchAdapter(
    private var matchList: List<FixtureResponseItem>,
    private val onMatchClick: (FixtureResponseItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_EMPTY = 0
        private const val VIEW_TYPE_LIVE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (matchList.isEmpty()) VIEW_TYPE_EMPTY else VIEW_TYPE_LIVE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_EMPTY) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_live_empty, parent, false)
            EmptyViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_live_match, parent, false)
            LiveMatchViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is LiveMatchViewHolder) {
            holder.bind(matchList[position], onMatchClick)
        }
    }

    override fun getItemCount(): Int {
        // Retornamos 1 si la lista está vacía para mostrar la tarjeta de estado vacío premium
        return if (matchList.isEmpty()) 1 else matchList.size
    }

    fun updateList(newList: List<FixtureResponseItem>) {
        matchList = newList
        notifyDataSetChanged()
    }

    class LiveMatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewPulseDot: View = itemView.findViewById(R.id.view_pulse_dot)
        private val tvLiveTime: TextView = itemView.findViewById(R.id.tv_live_time)
        private val tvLiveLeague: TextView = itemView.findViewById(R.id.tv_live_league)
        private val imgHomeBadge: ImageView = itemView.findViewById(R.id.img_home_badge)
        private val imgAwayBadge: ImageView = itemView.findViewById(R.id.img_away_badge)
        private val tvHomeName: TextView = itemView.findViewById(R.id.tv_home_name)
        private val tvAwayName: TextView = itemView.findViewById(R.id.tv_away_name)
        private val tvScore: TextView = itemView.findViewById(R.id.tv_match_score)

        fun bind(match: FixtureResponseItem, onClick: (FixtureResponseItem) -> Unit) {
            tvHomeName.text = match.teams.home.name
            tvAwayName.text = match.teams.away.name
            tvLiveLeague.text = match.league.name

            // Minuto transcurrido
            val elapsed = match.fixture.status.elapsed
            tvLiveTime.text = if (elapsed != null) "$elapsed'" else ""

            // Marcador en tiempo real
            val homeScore = match.goals.home
            val awayScore = match.goals.away
            if (homeScore != null && awayScore != null) {
                tvScore.text = "$homeScore - $awayScore"
            } else {
                tvScore.text = "0 - 0"
            }

            // Iniciar animación infinita de latido en el punto rojo de directo
            val pulseAnim = AnimationUtils.loadAnimation(itemView.context, R.anim.anim_pulse)
            viewPulseDot.startAnimation(pulseAnim)

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

    class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
