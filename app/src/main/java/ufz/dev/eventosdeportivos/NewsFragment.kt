package ufz.dev.eventosdeportivos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ufz.dev.eventosdeportivos.data.model.News
import ufz.dev.eventosdeportivos.data.model.NewsResponse
import ufz.dev.eventosdeportivos.data.network.RetrofitClient
import ufz.dev.eventosdeportivos.BuildConfig
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NewsFragment : Fragment() {

    // Se obtiene de forma segura la API Key desde el archivo .env a través de BuildConfig
    private val currentsApiKey = BuildConfig.CURRENTS_API_KEY

    private lateinit var rvNews: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var layoutErrorState: LinearLayout
    private lateinit var btnRetry: Button
    
    private lateinit var newsAdapter: NewsAdapter
    private val newsList = ArrayList<News>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_news, container, false)

        rvNews = view.findViewById(R.id.rv_news)
        progressLoading = view.findViewById(R.id.progress_loading)
        layoutErrorState = view.findViewById(R.id.layout_error_state)
        btnRetry = view.findViewById(R.id.btn_retry)

        setupRecyclerView()
        loadNewsFeed()

        btnRetry.setOnClickListener {
            loadNewsFeed()
        }

        return view
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter(newsList) { newsItem ->
            // Open full article in browser on click
            if (!newsItem.url.isNullOrEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(newsItem.url))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No se pudo abrir el artículo", Toast.LENGTH_SHORT).show()
                }
            }
        }
        rvNews.layoutManager = LinearLayoutManager(context)
        rvNews.adapter = newsAdapter
    }

    private fun loadNewsFeed() {
        showLoading(true)
        showError(false)

        if (currentsApiKey.isEmpty()) {
            showLoading(false)
            showError(true)
            Toast.makeText(
                context, 
                "API Key no configurada. Por favor, define 'currentsApiKey' en NewsFragment.kt", 
                Toast.LENGTH_LONG
            ).show()
        } else {
            fetchRemoteNews()
        }
    }

    private fun fetchRemoteNews() {
        RetrofitClient.instance.getLatestSportsNews(
            category = "sports",
            language = "es",
            apiKey = currentsApiKey
        ).enqueue(object : Callback<NewsResponse> {
            override fun onResponse(call: Call<NewsResponse>, response: Response<NewsResponse>) {
                showLoading(false)
                if (response.isSuccessful && response.body() != null) {
                    val articles = response.body()?.news
                    if (!articles.isNullOrEmpty()) {
                        newsList.clear()
                        newsList.addAll(articles)
                        newsAdapter.updateList(newsList)
                        showError(false)
                    } else {
                        showError(true)
                        Toast.makeText(context, "No se encontraron noticias deportivas.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    showError(true)
                    val errorMsg = "Error en la petición: Código ${response.code()}"
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
                showLoading(false)
                showError(true)
                Toast.makeText(
                    context, 
                    "Error de conexión: ${t.localizedMessage ?: "Fallo de red"}", 
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        rvNews.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showError(isError: Boolean) {
        layoutErrorState.visibility = if (isError) View.VISIBLE else View.GONE
        rvNews.visibility = if (isError) View.GONE else View.VISIBLE
    }
}
