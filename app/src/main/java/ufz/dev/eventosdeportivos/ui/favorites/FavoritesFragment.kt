package ufz.dev.eventosdeportivos.ui.favorites

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ufz.dev.eventosdeportivos.R
import ufz.dev.eventosdeportivos.data.network.FavoritesManager
import ufz.dev.eventosdeportivos.data.network.RetrofitClient
import ufz.dev.eventosdeportivos.data.news.News
import ufz.dev.eventosdeportivos.data.sports.FavoriteItem
import ufz.dev.eventosdeportivos.data.sports.FixtureResponseItem
import ufz.dev.eventosdeportivos.data.sports.FootballSquadResponse
import ufz.dev.eventosdeportivos.data.sports.SquadPlayerDetails
import ufz.dev.eventosdeportivos.data.sports.TeamDetails
import ufz.dev.eventosdeportivos.ui.auth.LoginFragment
import ufz.dev.eventosdeportivos.ui.matches.MatchAdapter
import ufz.dev.eventosdeportivos.ui.news.NewsAdapter
import ufz.dev.eventosdeportivos.ui.teams.PlayerAdapter
import ufz.dev.eventosdeportivos.ui.teams.TeamAdapter
import ufz.dev.eventosdeportivos.utils.clearBackStackAndNavigateTo

/**
 * Fragmento que gestiona y clasifica la visualización de los favoritos del usuario (Noticias, Equipos, Partidos).
 * Implementa una interfaz de invitado premium para anónimos, y sincronización en tiempo real para registrados.
 */
class FavoritesFragment : Fragment() {

    // Contenedores principales
    private lateinit var layoutGuest: View
    private lateinit var layoutContent: View

    // Botón de Login para invitados
    private lateinit var btnLockLogin: Button

    // RecyclerViews de Favoritos
    private lateinit var rvNews: RecyclerView
    private lateinit var rvTeams: RecyclerView
    private lateinit var rvMatches: RecyclerView

    // Textos de estado vacío
    private lateinit var tvEmptyNews: TextView
    private lateinit var tvEmptyTeams: TextView
    private lateinit var tvEmptyMatches: TextView

    // Adaptadores
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var teamAdapter: TeamAdapter
    private lateinit var matchAdapter: MatchAdapter

    // Listas de datos
    private val favNewsList = ArrayList<News>()
    private val favTeamsList = ArrayList<TeamDetails>()
    private val favMatchesList = ArrayList<FixtureResponseItem>()

    // Firestore listener
    private var favoritesListenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)

        // Referenciar contenedores y controles
        layoutGuest = view.findViewById(R.id.layout_favorites_guest)
        layoutContent = view.findViewById(R.id.layout_favorites_content)
        btnLockLogin = view.findViewById(R.id.btn_lock_login)

        rvNews = view.findViewById(R.id.rv_fav_news)
        rvTeams = view.findViewById(R.id.rv_fav_teams)
        rvMatches = view.findViewById(R.id.rv_fav_matches)

        tvEmptyNews = view.findViewById(R.id.tv_empty_news)
        tvEmptyTeams = view.findViewById(R.id.tv_empty_teams)
        tvEmptyMatches = view.findViewById(R.id.tv_empty_matches)

        setupRecyclerViews()
        setupAuthUI()

        return view
    }

    override fun onStart() {
        super.onStart()
        startFavoritesListening()
    }

    override fun onStop() {
        super.onStop()
        favoritesListenerRegistration?.remove()
        favoritesListenerRegistration = null
    }

    /**
     * Permite sincronizar manualmente los favoritos.
     */
    fun refresh() {
        if (FavoritesManager.isUserRegistered()) {
            Toast.makeText(context, "Sincronizando favoritos en tiempo real...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Inicia sesión para usar favoritos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAuthUI() {
        if (!FavoritesManager.isUserRegistered()) {
            // Mostrar panel de invitado bloqueado
            layoutGuest.visibility = View.VISIBLE
            layoutContent.visibility = View.GONE

            btnLockLogin.setOnClickListener {
                parentFragmentManager.clearBackStackAndNavigateTo(LoginFragment())
            }
        } else {
            // Mostrar contenido sincronizado en tiempo real
            layoutGuest.visibility = View.GONE
            layoutContent.visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerViews() {
        val context = context ?: return

        // 1. Noticias Guardadas (Carrusel Horizontal)
        newsAdapter = NewsAdapter(
            favNewsList,
            onNewsClick = { newsItem ->
                if (!newsItem.url.isNullOrEmpty()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(newsItem.url))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No se pudo abrir el artículo", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onFavoriteClick = { newsItem ->
                val itemId = FavoritesManager.getSafeId(newsItem.url ?: "")
                if (itemId.isNotEmpty()) {
                    FavoritesManager.removeFavorite(
                        itemId,
                        onSuccess = { Toast.makeText(context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show() },
                        onFailure = { Toast.makeText(context, "Error al eliminar favorito", Toast.LENGTH_SHORT).show() }
                    )
                }
            }
        )
        rvNews.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvNews.adapter = newsAdapter

        // 2. Mis Equipos (Carrusel Horizontal)
        teamAdapter = TeamAdapter(
            favTeamsList,
            onTeamClick = { team ->
                showSquadBottomSheet(team)
            },
            onFavoriteClick = { team ->
                val itemId = team.id.toString()
                FavoritesManager.removeFavorite(
                    itemId,
                    onSuccess = { Toast.makeText(context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show() },
                    onFailure = { Toast.makeText(context, "Error al eliminar favorito", Toast.LENGTH_SHORT).show() }
                )
            }
        )
        rvTeams.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvTeams.adapter = teamAdapter

        // 3. Partidos Guardados (Historial Vertical)
        matchAdapter = MatchAdapter(
            favMatchesList,
            onMatchClick = { match ->
                val detailMsg = "Resultado: ${match.teams.home.name} ${match.goals.home ?: 0} - ${match.goals.away ?: 0} ${match.teams.away.name}"
                Toast.makeText(context, detailMsg, Toast.LENGTH_LONG).show()
            },
            onFavoriteClick = { match ->
                val itemId = match.fixture.id.toString()
                FavoritesManager.removeFavorite(
                    itemId,
                    onSuccess = { Toast.makeText(context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show() },
                    onFailure = { Toast.makeText(context, "Error al eliminar favorito", Toast.LENGTH_SHORT).show() }
                )
            }
        )
        rvMatches.layoutManager = LinearLayoutManager(context)
        rvMatches.adapter = matchAdapter
    }

    private fun startFavoritesListening() {
        if (!FavoritesManager.isUserRegistered()) return

        favoritesListenerRegistration = FavoritesManager.listenToFavorites(
            onUpdate = { favorites ->
                classifyFavorites(favorites)
            },
            onError = { error ->
                android.util.Log.e("FavoritesFragment", "Error al escuchar favoritos de Firestore: ${error.message}")
            }
        )
    }

    private fun classifyFavorites(favorites: List<FavoriteItem>) {
        val gson = Gson()

        // 1. Filtrar y clasificar noticias
        favNewsList.clear()
        val newsFavs = favorites.filter { it.type == "news" }
        newsFavs.forEach {
            try {
                val newsItem = gson.fromJson(it.dataJson, News::class.java)
                favNewsList.add(newsItem)
            } catch (e: Exception) {
                android.util.Log.e("FavoritesFragment", "Error deserializando noticia: ${e.message}")
            }
        }
        newsAdapter.updateList(favNewsList)
        tvEmptyNews.visibility = if (favNewsList.isEmpty()) View.VISIBLE else View.GONE
        rvNews.visibility = if (favNewsList.isEmpty()) View.GONE else View.VISIBLE

        // 2. Filtrar y clasificar equipos
        favTeamsList.clear()
        val teamsFavs = favorites.filter { it.type == "team" }
        teamsFavs.forEach {
            try {
                val teamItem = gson.fromJson(it.dataJson, TeamDetails::class.java)
                favTeamsList.add(teamItem)
            } catch (e: Exception) {
                android.util.Log.e("FavoritesFragment", "Error deserializando equipo: ${e.message}")
            }
        }
        teamAdapter.updateList(favTeamsList)
        tvEmptyTeams.visibility = if (favTeamsList.isEmpty()) View.VISIBLE else View.GONE
        rvTeams.visibility = if (favTeamsList.isEmpty()) View.GONE else View.VISIBLE

        // 3. Filtrar y clasificar partidos
        favMatchesList.clear()
        val matchesFavs = favorites.filter { it.type == "match" }
        matchesFavs.forEach {
            try {
                val matchItem = gson.fromJson(it.dataJson, FixtureResponseItem::class.java)
                favMatchesList.add(matchItem)
            } catch (e: Exception) {
                android.util.Log.e("FavoritesFragment", "Error deserializando partido: ${e.message}")
            }
        }
        matchAdapter.updateList(favMatchesList)
        tvEmptyMatches.visibility = if (favMatchesList.isEmpty()) View.VISIBLE else View.GONE
        rvMatches.visibility = if (favMatchesList.isEmpty()) View.GONE else View.VISIBLE
    }

    /**
     * Muestra la plantilla oficial del equipo en un dialog BottomSheet deslizable,
     * reutilizando perfectamente el flujo nativo e interactivo del proyecto.
     */
    private fun showSquadBottomSheet(team: TeamDetails) {
        val context = context ?: return

        val dialog = BottomSheetDialog(context)
        val bottomSheetView = layoutInflater.inflate(R.layout.dialog_team_squad, null)
        dialog.setContentView(bottomSheetView)

        val imgLogo = bottomSheetView.findViewById<ImageView>(R.id.img_dialog_team_logo)
        val tvTitle = bottomSheetView.findViewById<TextView>(R.id.tv_dialog_team_title)
        val progressDialog = bottomSheetView.findViewById<ProgressBar>(R.id.progress_dialog_loading)
        val rvPlayers = bottomSheetView.findViewById<RecyclerView>(R.id.rv_dialog_players)
        val layoutError = bottomSheetView.findViewById<LinearLayout>(R.id.layout_dialog_error)
        val tvErrorMsg = bottomSheetView.findViewById<TextView>(R.id.tv_dialog_error_msg)

        tvTitle.text = team.name

        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        val secureLogoUrl = team.logo?.replace("http://", "https://")
        val logoGlideUrl = if (!secureLogoUrl.isNullOrEmpty()) {
            com.bumptech.glide.load.model.GlideUrl(
                secureLogoUrl,
                com.bumptech.glide.load.model.LazyHeaders.Builder()
                    .addHeader("User-Agent", userAgent)
                    .build()
            )
        } else {
            null
        }

        Glide.with(context)
            .load(logoGlideUrl)
            .placeholder(R.drawable.ic_teams)
            .error(R.drawable.ic_teams)
            .into(imgLogo)

        val squadPlayersList = ArrayList<SquadPlayerDetails>()
        val playerAdapter = PlayerAdapter(squadPlayersList)
        rvPlayers.layoutManager = LinearLayoutManager(context)
        rvPlayers.adapter = playerAdapter

        progressDialog.visibility = View.VISIBLE
        rvPlayers.visibility = View.GONE
        layoutError.visibility = View.GONE

        RetrofitClient.footballInstance.getTeamSquad(teamId = team.id)
            .enqueue(object : Callback<FootballSquadResponse> {
                override fun onResponse(
                    call: Call<FootballSquadResponse>,
                    response: Response<FootballSquadResponse>
                ) {
                    progressDialog.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!

                        val errorsObj = body.errors
                        if (errorsObj != null && !errorsObj.isJsonNull) {
                            if (errorsObj.isJsonObject) {
                                val obj = errorsObj.asJsonObject
                                if (obj.size() > 0) {
                                    val firstKey = obj.keySet().firstOrNull()
                                    val errorMsg = obj.get(firstKey)?.asString ?: "Error"
                                    tvErrorMsg.text = "API Error: $errorMsg"
                                    layoutError.visibility = View.VISIBLE
                                    rvPlayers.visibility = View.GONE
                                    return
                                }
                            } else if (errorsObj.isJsonPrimitive) {
                                val errorMsg = errorsObj.asString
                                if (errorMsg.isNotEmpty()) {
                                    tvErrorMsg.text = "API Error: $errorMsg"
                                    layoutError.visibility = View.VISIBLE
                                    rvPlayers.visibility = View.GONE
                                    return
                                }
                            }
                        }

                        val responseList = body.response
                        if (!responseList.isNullOrEmpty()) {
                            val players = responseList[0].players
                            if (!players.isNullOrEmpty()) {
                                squadPlayersList.addAll(players)
                                squadPlayersList.sortBy { it.number ?: Int.MAX_VALUE }
                                playerAdapter.updateList(squadPlayersList)
                                rvPlayers.visibility = View.VISIBLE
                                layoutError.visibility = View.GONE
                                return
                            }
                        }
                    }

                    tvErrorMsg.text = "No se encontraron jugadores en la plantilla oficial o tu suscripción de API Key no tiene permisos para este club."
                    layoutError.visibility = View.VISIBLE
                    rvPlayers.visibility = View.GONE
                }

                override fun onFailure(call: Call<FootballSquadResponse>, t: Throwable) {
                    progressDialog.visibility = View.GONE
                    tvErrorMsg.text = "Fallo de conexión: ${t.localizedMessage ?: "Comprueba tu red"}"
                    layoutError.visibility = View.VISIBLE
                    rvPlayers.visibility = View.GONE
                }
            })

        dialog.show()
    }
}
