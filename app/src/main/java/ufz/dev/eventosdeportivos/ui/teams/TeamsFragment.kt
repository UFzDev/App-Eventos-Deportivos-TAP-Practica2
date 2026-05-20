package ufz.dev.eventosdeportivos.ui.teams

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
import ufz.dev.eventosdeportivos.R
import ufz.dev.eventosdeportivos.data.network.RetrofitClient
import ufz.dev.eventosdeportivos.data.sports.FootballTeamResponse
import ufz.dev.eventosdeportivos.data.sports.TeamDetails
import ufz.dev.eventosdeportivos.data.sports.FootballSquadResponse
import ufz.dev.eventosdeportivos.data.sports.SquadPlayerDetails
import ufz.dev.eventosdeportivos.data.sports.FavoriteItem
import ufz.dev.eventosdeportivos.data.network.FavoritesManager
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TeamsFragment : Fragment() {

    private lateinit var rvTeams: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var layoutErrorState: LinearLayout
    private lateinit var btnRetry: Button

    private lateinit var teamAdapter: TeamAdapter
    private val teamList = ArrayList<TeamDetails>()

    private val favoritesSet = HashSet<String>()
    private var favoritesListenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_teams, container, false)

        rvTeams = view.findViewById(R.id.rv_teams)
        progressLoading = view.findViewById(R.id.progress_loading)
        layoutErrorState = view.findViewById(R.id.layout_error_state)
        btnRetry = view.findViewById(R.id.btn_retry)

        setupRecyclerView()
        loadTeams()

        btnRetry.setOnClickListener {
            loadTeams()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        if (FavoritesManager.isUserRegistered()) {
            favoritesListenerRegistration = FavoritesManager.listenToFavorites(
                onUpdate = { favorites ->
                    favoritesSet.clear()
                    favorites.forEach {
                        if (it.type == "team") {
                            favoritesSet.add(it.id)
                        }
                    }
                },
                onError = {
                    android.util.Log.e("TeamsFragment", "Error al cargar favoritos: ${it.message}")
                }
            )
        }
    }

    override fun onStop() {
        super.onStop()
        favoritesListenerRegistration?.remove()
        favoritesListenerRegistration = null
    }

    private fun setupRecyclerView() {
        teamAdapter = TeamAdapter(
            teamList,
            onTeamClick = { team ->
                showSquadBottomSheet(team)
            },
            onFavoriteClick = { team ->
                val itemId = team.id.toString()
                if (!FavoritesManager.isUserRegistered()) {
                    Toast.makeText(context, "Inicia sesión para guardar favoritos.", Toast.LENGTH_SHORT).show()
                    return@TeamAdapter
                }

                if (favoritesSet.contains(itemId)) {
                    FavoritesManager.removeFavorite(
                        itemId,
                        onSuccess = {
                            Toast.makeText(context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { e ->
                            val msg = e.localizedMessage ?: "Error desconocido"
                            Toast.makeText(context, "Error al eliminar de favoritos: $msg", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    val favItem = FavoriteItem(
                        id = itemId,
                        type = "team",
                        title = team.name ?: "Sin nombre",
                        subtitle = if (team.founded != null) "Fundado en ${team.founded}" else "Club Histórico",
                        imageUrl = team.logo ?: "",
                        dataJson = Gson().toJson(team)
                    )
                    FavoritesManager.addFavorite(
                        favItem,
                        onSuccess = {
                            Toast.makeText(context, "Guardado en favoritos", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { e ->
                            val msg = e.localizedMessage ?: "Error desconocido"
                            Toast.makeText(context, "Error al guardar favorito: $msg", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        )
        rvTeams.layoutManager = LinearLayoutManager(context)
        rvTeams.adapter = teamAdapter
    }

    private fun showSquadBottomSheet(team: TeamDetails) {
        val context = context ?: return
        
        // Instanciar BottomSheetDialog
        val dialog = BottomSheetDialog(context)
        val bottomSheetView = layoutInflater.inflate(R.layout.dialog_team_squad, null)
        dialog.setContentView(bottomSheetView)

        // Referenciar elementos visuales del BottomSheet
        val imgLogo = bottomSheetView.findViewById<ImageView>(R.id.img_dialog_team_logo)
        val tvTitle = bottomSheetView.findViewById<TextView>(R.id.tv_dialog_team_title)
        val progressDialog = bottomSheetView.findViewById<ProgressBar>(R.id.progress_dialog_loading)
        val rvPlayers = bottomSheetView.findViewById<RecyclerView>(R.id.rv_dialog_players)
        val layoutError = bottomSheetView.findViewById<LinearLayout>(R.id.layout_dialog_error)
        val tvErrorMsg = bottomSheetView.findViewById<TextView>(R.id.tv_dialog_error_msg)

        // Cargar los datos del equipo en la cabecera
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

        // Configurar RecyclerView de jugadores
        val squadPlayersList = ArrayList<SquadPlayerDetails>()
        val playerAdapter = PlayerAdapter(squadPlayersList)
        rvPlayers.layoutManager = LinearLayoutManager(context)
        rvPlayers.adapter = playerAdapter

        // Mostrar loading, ocultar lista y error
        progressDialog.visibility = View.VISIBLE
        rvPlayers.visibility = View.GONE
        layoutError.visibility = View.GONE

        // Ejecutar llamada a API-Football para obtener la plantilla
        RetrofitClient.footballInstance.getTeamSquad(teamId = team.id)
            .enqueue(object : Callback<FootballSquadResponse> {
                override fun onResponse(
                    call: Call<FootballSquadResponse>,
                    response: Response<FootballSquadResponse>
                ) {
                    progressDialog.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        
                        // Validar errores devueltos en el JSON bajo HTTP 200
                        val errorsObj = body.errors
                        if (errorsObj != null && !errorsObj.isJsonNull) {
                            if (errorsObj.isJsonObject) {
                                val obj = errorsObj.asJsonObject
                                if (obj.size() > 0) {
                                    val firstKey = obj.keySet().firstOrNull()
                                    val errorMsg = obj.get(firstKey)?.asString ?: "Error de suscripción"
                                    
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
                                // Ordenar jugadores opcionalmente por número (dorsal) si está disponible
                                squadPlayersList.sortBy { it.number ?: Int.MAX_VALUE }
                                playerAdapter.updateList(squadPlayersList)
                                rvPlayers.visibility = View.VISIBLE
                                layoutError.visibility = View.GONE
                                return
                            }
                        }
                    }

                    // Si llega aquí es que falló o vino vacío
                    tvErrorMsg.text = "No se encontraron jugadores en la plantilla oficial o tu suscripción de API Key no tiene permisos para este club."
                    layoutError.visibility = View.VISIBLE
                    rvPlayers.visibility = View.GONE
                }

                override fun onFailure(
                    call: Call<FootballSquadResponse>,
                    t: Throwable
                ) {
                    progressDialog.visibility = View.GONE
                    tvErrorMsg.text = "Fallo de conexión: ${t.localizedMessage ?: "Comprueba tu red"}"
                    layoutError.visibility = View.VISIBLE
                    rvPlayers.visibility = View.GONE
                }
            })

        dialog.show()
    }

    fun refresh() {
        teamList.clear()
        loadTeams()
        Toast.makeText(context, "Sincronizando equipos de fútbol...", Toast.LENGTH_SHORT).show()
    }

    private fun loadTeams() {
        // Evitar consulta HTTP redundante si los datos ya están en memoria
        if (teamList.isNotEmpty()) {
            showLoading(false)
            showError(false)
            teamAdapter.updateList(teamList)
            return
        }

        showLoading(true)
        showError(false)
        teamList.clear()

        // Ligas premium a consultar en paralelo (Mundial: España, Inglaterra, Italia, Alemania)
        val leagues = listOf(140, 39, 135, 78)
        var completedRequests = 0
        var successfulRequests = 0
        var lastErrorMessage = ""

        for (leagueId in leagues) {
            RetrofitClient.footballInstance.getTeamsByLeague(leagueId = leagueId, season = 2024)
                .enqueue(object : Callback<FootballTeamResponse> {
                    override fun onResponse(call: Call<FootballTeamResponse>, response: Response<FootballTeamResponse>) {
                        synchronized(this@TeamsFragment) {
                            completedRequests++
                            if (response.isSuccessful && response.body() != null) {
                                val body = response.body()!!
                                
                                // Diagnóstico de Errores nativos de API-Football (devueltos bajo HTTP 200)
                                val errorsObj = body.errors
                                if (errorsObj != null && !errorsObj.isJsonNull) {
                                    if (errorsObj.isJsonObject) {
                                        val obj = errorsObj.asJsonObject
                                        if (obj.size() > 0) {
                                            val firstKey = obj.keySet().firstOrNull()
                                            lastErrorMessage = obj.get(firstKey)?.asString ?: "Error"
                                        }
                                    } else if (errorsObj.isJsonPrimitive) {
                                        lastErrorMessage = errorsObj.asString
                                    }
                                }

                                val responseItems = body.response
                                if (!responseItems.isNullOrEmpty()) {
                                    val teams = responseItems.map { it.team }
                                    teamList.addAll(teams)
                                    successfulRequests++
                                }
                            } else {
                                lastErrorMessage = "HTTP ${response.code()}"
                            }

                            if (completedRequests == leagues.size) {
                                onAllTeamsRequestsCompleted(successfulRequests, lastErrorMessage)
                            }
                        }
                    }

                    override fun onFailure(call: Call<FootballTeamResponse>, t: Throwable) {
                        synchronized(this@TeamsFragment) {
                            completedRequests++
                            lastErrorMessage = t.localizedMessage ?: "Fallo de conexión"
                            if (completedRequests == leagues.size) {
                                onAllTeamsRequestsCompleted(successfulRequests, lastErrorMessage)
                            }
                        }
                    }
                })
        }
    }

    private fun onAllTeamsRequestsCompleted(successfulRequests: Int, lastError: String) {
        showLoading(false)
        if (teamList.isNotEmpty()) {
            // Mezclar todos los clubes unificados para máxima dinamización visual de equipos mundiales
            teamList.shuffle()
            teamAdapter.updateList(teamList)
            showError(false)
            if (successfulRequests < 4 && lastError.isNotEmpty()) {
                android.util.Log.w("TeamsFragment", "Carga parcial: algunas ligas no se cargaron. Detalle: $lastError")
            }
        } else {
            showError(true)
            val errorDisplay = if (lastError.isNotEmpty()) lastError else "API Key inválida o límite superado"
            Toast.makeText(context, "No se encontraron equipos ($errorDisplay).", Toast.LENGTH_LONG).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        rvTeams.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showError(isError: Boolean) {
        layoutErrorState.visibility = if (isError) View.VISIBLE else View.GONE
        rvTeams.visibility = if (isError) View.GONE else View.VISIBLE
    }
}
