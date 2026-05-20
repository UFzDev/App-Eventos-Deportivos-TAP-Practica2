package ufz.dev.eventosdeportivos.ui.news

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ListenerRegistration
import ufz.dev.eventosdeportivos.R
import ufz.dev.eventosdeportivos.data.news.News
import ufz.dev.eventosdeportivos.data.network.FavoritesManager

/**
 * Adaptador de noticias con soporte para destacar el primer elemento
 * y gestión de favoritos en tiempo real respaldado por Firestore.
 */
class NewsAdapter(
    private var newsList: List<News>,
    private val onNewsClick: (News) -> Unit,
    private val onFavoriteClick: (News) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_FEATURED = 0
        private const val VIEW_TYPE_STANDARD = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_FEATURED else VIEW_TYPE_STANDARD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_FEATURED) {
            val view = inflater.inflate(R.layout.item_featured_news, parent, false)
            FeaturedViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_standard_news, parent, false)
            StandardViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val newsItem = newsList[position]
        if (holder is FeaturedViewHolder) {
            holder.bind(newsItem, onNewsClick, onFavoriteClick)
        } else if (holder is StandardViewHolder) {
            holder.bind(newsItem, onNewsClick, onFavoriteClick)
        }
    }

    override fun getItemCount(): Int = newsList.size

    fun updateList(newList: List<News>) {
        newsList = newList
        notifyDataSetChanged()
    }

    // ViewHolder for first item (Hero / Featured)
    class FeaturedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgHero: ImageView = itemView.findViewById(R.id.img_hero_news)
        private val tvTag: TextView = itemView.findViewById(R.id.tv_hero_tag)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_hero_title)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_hero_time)
        private val liveDot: View = itemView.findViewById(R.id.view_live_dot)
        private val btnFavorite: ImageView = itemView.findViewById(R.id.btn_news_favorite)

        private var favoriteListener: ListenerRegistration? = null

        fun bind(news: News, onClick: (News) -> Unit, onFavClick: (News) -> Unit) {
            // Cancelar el escuchador anterior antes de re-vincular para evitar leaks de memoria
            favoriteListener?.remove()
            favoriteListener = null

            tvTitle.text = news.title ?: ""
            tvTime.text = news.published?.take(16) ?: "Hace un momento"
            
            val cat = news.category?.firstOrNull()?.uppercase() ?: "EN VIVO"
            tvTag.text = "PARTIDO $cat"

            val imageUrl = if (!news.image.isNullOrEmpty() && news.image != "None") news.image else "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=600&auto=format&fit=crop"
            
            Glide.with(itemView.context)
                .load(imageUrl)
                .placeholder(R.color.outline_border)
                .error(R.color.outline_border)
                .centerCrop()
                .into(imgHero)

            liveDot.animate().alpha(0.3f).setDuration(800).withEndAction {
                liveDot.animate().alpha(1.0f).setDuration(800).start()
            }.start()

            // Vincular estado de favorito en tiempo real
            val itemId = FavoritesManager.getSafeId(news.url ?: "")
            if (itemId.isNotEmpty() && FavoritesManager.isUserRegistered()) {
                favoriteListener = FavoritesManager.checkIsFavoritedRealTime(itemId) { isFav ->
                    btnFavorite.setImageResource(if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart)
                }
            } else {
                btnFavorite.setImageResource(R.drawable.ic_heart)
            }

            btnFavorite.setOnClickListener { onFavClick(news) }
            itemView.setOnClickListener { onClick(news) }
        }
    }

    // ViewHolder for rest of the items (Standard Cards)
    class StandardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgStandard: ImageView = itemView.findViewById(R.id.img_standard_news)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_standard_category)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_standard_title)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_standard_time)
        private val btnFavorite: ImageView = itemView.findViewById(R.id.btn_news_favorite)

        private var favoriteListener: ListenerRegistration? = null

        fun bind(news: News, onClick: (News) -> Unit, onFavClick: (News) -> Unit) {
            // Cancelar el escuchador anterior antes de re-vincular
            favoriteListener?.remove()
            favoriteListener = null

            tvTitle.text = news.title ?: ""
            tvTime.text = news.published?.take(16) ?: "Reciente"
            tvCategory.text = news.category?.firstOrNull()?.uppercase() ?: "DEPORTES"

            val imageUrl = if (!news.image.isNullOrEmpty() && news.image != "None") news.image else "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?q=80&w=600&auto=format&fit=crop"

            Glide.with(itemView.context)
                .load(imageUrl)
                .placeholder(R.color.outline_border)
                .error(R.color.outline_border)
                .centerCrop()
                .into(imgStandard)

            // Vincular estado de favorito en tiempo real
            val itemId = FavoritesManager.getSafeId(news.url ?: "")
            if (itemId.isNotEmpty() && FavoritesManager.isUserRegistered()) {
                favoriteListener = FavoritesManager.checkIsFavoritedRealTime(itemId) { isFav ->
                    btnFavorite.setImageResource(if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart)
                }
            } else {
                btnFavorite.setImageResource(R.drawable.ic_heart)
            }

            btnFavorite.setOnClickListener { onFavClick(news) }
            itemView.setOnClickListener { onClick(news) }
        }
    }
}
