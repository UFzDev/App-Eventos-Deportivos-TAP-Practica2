package ufz.dev.eventosdeportivos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ufz.dev.eventosdeportivos.data.model.News

class NewsAdapter(
    private var newsList: List<News>,
    private val onNewsClick: (News) -> Unit
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
            holder.bind(newsItem, onNewsClick)
        } else if (holder is StandardViewHolder) {
            holder.bind(newsItem, onNewsClick)
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

        fun bind(news: News, onClick: (News) -> Unit) {
            tvTitle.text = news.title ?: ""
            
            // Format time display
            tvTime.text = news.published?.take(16) ?: "Hace un momento"
            
            // Get category
            val cat = news.category?.firstOrNull()?.uppercase() ?: "EN VIVO"
            tvTag.text = "PARTIDO $cat"

            // Glide with high-quality loading and premium fallbacks
            val imageUrl = if (!news.image.isNullOrEmpty() && news.image != "None") news.image else "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=600&auto=format&fit=crop"
            
            Glide.with(itemView.context)
                .load(imageUrl)
                .placeholder(R.color.outline_border)
                .error(R.color.outline_border)
                .centerCrop()
                .into(imgHero)

            // Setup alpha/blink animation for live dot to feel dynamic
            liveDot.animate().alpha(0.3f).setDuration(800).withEndAction {
                liveDot.animate().alpha(1.0f).setDuration(800).start()
            }.start()

            itemView.setOnClickListener { onClick(news) }
        }
    }

    // ViewHolder for rest of the items (Standard Cards)
    class StandardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgStandard: ImageView = itemView.findViewById(R.id.img_standard_news)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_standard_category)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_standard_title)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_standard_time)

        fun bind(news: News, onClick: (News) -> Unit) {
            tvTitle.text = news.title ?: ""
            
            // Format time display
            tvTime.text = news.published?.take(16) ?: "Reciente"
            
            // Category tag
            tvCategory.text = news.category?.firstOrNull()?.uppercase() ?: "DEPORTES"

            // Glide image loading with placeholder
            val imageUrl = if (!news.image.isNullOrEmpty() && news.image != "None") news.image else "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?q=80&w=600&auto=format&fit=crop"

            Glide.with(itemView.context)
                .load(imageUrl)
                .placeholder(R.color.outline_border)
                .error(R.color.outline_border)
                .centerCrop()
                .into(imgStandard)

            itemView.setOnClickListener { onClick(news) }
        }
    }
}
