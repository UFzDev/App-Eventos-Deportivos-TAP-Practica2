package ufz.dev.eventosdeportivos.ui.matches

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
import ufz.dev.eventosdeportivos.data.sports.FootballFixtureResponse
import ufz.dev.eventosdeportivos.data.sports.FixtureResponseItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MatchesFragment : Fragment() {

    private lateinit var rvLiveMatches: RecyclerView
    private lateinit var rvMatches: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var layoutErrorState: LinearLayout
    private lateinit var btnRetry: Button

    private lateinit var liveAdapter: LiveMatchAdapter
    private lateinit var matchAdapter: MatchAdapter

    private val liveList = ArrayList<FixtureResponseItem>()
    private val matchList = ArrayList<FixtureResponseItem>()

    // Banderas de coordinación de red
    private var isLiveLoaded = false
    private var isPastLoaded = false
    private var liveRequestFailed = false
    private var pastRequestFailed = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_matches, container, false)

        rvLiveMatches = view.findViewById(R.id.rv_live_matches)
        rvMatches = view.findViewById(R.id.rv_matches)
        progressLoading = view.findViewById(R.id.progress_loading)
        layoutErrorState = view.findViewById(R.id.layout_error_state)
        btnRetry = view.findViewById(R.id.btn_retry)

        setupRecyclerViews()
        loadMatches()

        btnRetry.setOnClickListener {
            loadMatches()
        }

        return view
    }

    private fun setupRecyclerViews() {
        // 1. Carrusel Horizontal en Vivo
        liveAdapter = LiveMatchAdapter(liveList) { match ->
            Toast.makeText(context, "¡Marcador en vivo! ${match.teams.home.name} ${match.goals.home ?: 0} - ${match.goals.away ?: 0} ${match.teams.away.name}", Toast.LENGTH_SHORT).show()
        }
        rvLiveMatches.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvLiveMatches.adapter = liveAdapter

        // 2. Historial Vertical Normal
        matchAdapter = MatchAdapter(matchList) { match ->
            // Acción de clic silenciosa en el historial de partidos
        }
        rvMatches.layoutManager = LinearLayoutManager(context)
        rvMatches.adapter = matchAdapter
    }

    fun refresh() {
        liveList.clear()
        matchList.clear()
        loadMatches()
        Toast.makeText(context, "Sincronizando marcadores deportivos...", Toast.LENGTH_SHORT).show()
    }

    private fun loadMatches() {
        // Evitar redundancia HTTP si ya tenemos marcadores en memoria
        if (matchList.isNotEmpty() || liveList.isNotEmpty()) {
            showLoading(false)
            showError(false)
            liveAdapter.updateList(liveList)
            matchAdapter.updateList(matchList)
            return
        }

        showLoading(true)
        showError(false)
        isLiveLoaded = false
        isPastLoaded = false
        liveRequestFailed = false
        pastRequestFailed = false

        liveList.clear()
        matchList.clear()

        fetchLiveMatches()
        fetchPastMatches()
    }

    private fun checkAllRequestsCompleted() {
        if (isLiveLoaded && isPastLoaded) {
            showLoading(false)
            if (pastRequestFailed && matchList.isEmpty()) {
                showError(true)
            } else {
                showError(false)
                liveAdapter.updateList(liveList)
                matchAdapter.updateList(matchList)
            }
        }
    }

    private fun fetchLiveMatches() {
        // Cargar todos los partidos en vivo en tiempo real
        RetrofitClient.footballInstance.getLiveMatches("all")
            .enqueue(object : Callback<FootballFixtureResponse> {
                override fun onResponse(call: Call<FootballFixtureResponse>, response: Response<FootballFixtureResponse>) {
                    isLiveLoaded = true
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        
                        // Diagnóstico de Errores nativos
                        val errorsObj = body.errors
                        if (errorsObj != null && !errorsObj.isJsonNull) {
                            var errorMsg = ""
                            if (errorsObj.isJsonObject) {
                                val obj = errorsObj.asJsonObject
                                if (obj.size() > 0) {
                                    val firstKey = obj.keySet().firstOrNull()
                                    errorMsg = obj.get(firstKey)?.asString ?: "Error desconocido"
                                }
                            } else if (errorsObj.isJsonPrimitive) {
                                errorMsg = errorsObj.asString
                            }
                            if (errorMsg.isNotEmpty()) {
                                android.util.Log.e("MatchesFragment", "Error de API-Football (Live): $errorMsg")
                                liveRequestFailed = true
                            }
                        }

                        val events = body.response
                        if (!events.isNullOrEmpty()) {
                            // Agregar todos los partidos en vivo a nivel mundial
                            liveList.addAll(events)
                        }
                    } else {
                        android.util.Log.e("MatchesFragment", "Fallo HTTP en vivo: ${response.code()}")
                        liveRequestFailed = true
                    }
                    checkAllRequestsCompleted()
                }

                override fun onFailure(call: Call<FootballFixtureResponse>, t: Throwable) {
                    isLiveLoaded = true
                    liveRequestFailed = true
                    android.util.Log.e("MatchesFragment", "Fallo de conexión en vivo: ${t.localizedMessage}")
                    checkAllRequestsCompleted()
                }
            })
    }

    private fun fetchPastMatches() {
        // Calcular dinámicamente la fecha del día anterior (ayer)
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DATE, -1)
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)

        // Cargar marcadores de todo el mundo de la fecha de ayer
        RetrofitClient.footballInstance.getMatchesByDate(dateStr)
            .enqueue(object : Callback<FootballFixtureResponse> {
                override fun onResponse(call: Call<FootballFixtureResponse>, response: Response<FootballFixtureResponse>) {
                    isPastLoaded = true
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        
                        // Diagnóstico de Errores nativos de API-Football
                        val errorsObj = body.errors
                        if (errorsObj != null && !errorsObj.isJsonNull) {
                            var errorMsg = ""
                            if (errorsObj.isJsonObject) {
                                val obj = errorsObj.asJsonObject
                                if (obj.size() > 0) {
                                    val firstKey = obj.keySet().firstOrNull()
                                    errorMsg = obj.get(firstKey)?.asString ?: "Error"
                                }
                            } else if (errorsObj.isJsonPrimitive) {
                                errorMsg = errorsObj.asString
                            }
                            if (errorMsg.isNotEmpty()) {
                                android.util.Log.e("MatchesFragment", "Error de API-Football (Historial): $errorMsg")
                                Toast.makeText(context, "API Error: $errorMsg", Toast.LENGTH_LONG).show()
                                pastRequestFailed = true
                            }
                        }

                        val events = body.response
                        if (!events.isNullOrEmpty()) {
                            // Filtrar por partidos finalizados (FT) y ordenar por fecha descendente
                            val sortedCompleted = events
                                .filter { it.fixture.status.short == "FT" }
                                .sortedByDescending { it.fixture.date ?: "" }
                                .take(25)
                            matchList.addAll(sortedCompleted)
                        }
                    } else {
                        android.util.Log.e("MatchesFragment", "Fallo HTTP historial: ${response.code()}")
                        pastRequestFailed = true
                    }
                    checkAllRequestsCompleted()
                }

                override fun onFailure(call: Call<FootballFixtureResponse>, t: Throwable) {
                    isPastLoaded = true
                    pastRequestFailed = true
                    android.util.Log.e("MatchesFragment", "Fallo de conexión historial: ${t.localizedMessage}")
                    checkAllRequestsCompleted()
                }
            })
    }

    private fun showLoading(isLoading: Boolean) {
        progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        
        val visibility = if (isLoading) View.GONE else View.VISIBLE
        rvLiveMatches.visibility = visibility
        rvMatches.visibility = visibility
        view?.findViewById<View>(R.id.layout_live_header)?.visibility = visibility
        view?.findViewById<View>(R.id.layout_past_header)?.visibility = visibility
    }

    private fun showError(isError: Boolean) {
        layoutErrorState.visibility = if (isError) View.VISIBLE else View.GONE
        
        val visibility = if (isError) View.GONE else View.VISIBLE
        rvLiveMatches.visibility = visibility
        rvMatches.visibility = visibility
        view?.findViewById<View>(R.id.layout_live_header)?.visibility = visibility
        view?.findViewById<View>(R.id.layout_past_header)?.visibility = visibility
    }
}
