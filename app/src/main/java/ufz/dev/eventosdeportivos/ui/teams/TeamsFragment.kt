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

    private fun setupRecyclerView() {
        teamAdapter = TeamAdapter(teamList) { team ->
            showSquadBottomSheet(team)
        }
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

        // Cargar clubes de La Liga de España (ID 140, temporada 2024)
        RetrofitClient.footballInstance.getTeamsByLeague(leagueId = 140, season = 2024)
            .enqueue(object : Callback<FootballTeamResponse> {
                override fun onResponse(call: Call<FootballTeamResponse>, response: Response<FootballTeamResponse>) {
                    showLoading(false)
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        
                        // Diagnóstico de Errores nativos de API-Football (devueltos bajo HTTP 200)
                        val errorsObj = body.errors
                        if (errorsObj != null && !errorsObj.isJsonNull) {
                            if (errorsObj.isJsonObject) {
                                val obj = errorsObj.asJsonObject
                                if (obj.size() > 0) {
                                    val firstKey = obj.keySet().firstOrNull()
                                    val errorMsg = obj.get(firstKey)?.asString ?: "Error desconocido"
                                    android.util.Log.e("TeamsFragment", "Error de API-Football: $errorMsg")
                                    Toast.makeText(context, "API Error: $errorMsg", Toast.LENGTH_LONG).show()
                                    showError(true)
                                    return
                                }
                            } else if (errorsObj.isJsonPrimitive) {
                                val errorMsg = errorsObj.asString
                                if (errorMsg.isNotEmpty()) {
                                    android.util.Log.e("TeamsFragment", "Error de API-Football: $errorMsg")
                                    Toast.makeText(context, "API Error: $errorMsg", Toast.LENGTH_LONG).show()
                                    showError(true)
                                    return
                                }
                            }
                        }

                        val responseItems = body.response
                        if (!responseItems.isNullOrEmpty()) {
                            val teams = responseItems.map { it.team }
                            teamList.addAll(teams)
                        }
                    }

                    if (teamList.isNotEmpty()) {
                        // Mezclar los clubes de forma aleatoria para brindar una visualización dinámica
                        teamList.shuffle()
                        teamAdapter.updateList(teamList)
                        showError(false)
                    } else {
                        showError(true)
                        val errorDetail = response.errorBody()?.string() ?: "Respuesta vacía"
                        android.util.Log.e("TeamsFragment", "Error en API-Football: $errorDetail")
                        Toast.makeText(context, "No se encontraron equipos deportivos o tu API Key es inválida.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<FootballTeamResponse>, t: Throwable) {
                    showLoading(false)
                    showError(true)
                    Toast.makeText(
                        context,
                        "Error de red: ${t.localizedMessage ?: "Verifica tu conexión"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
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
