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

    private lateinit var rvMatches: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var layoutErrorState: LinearLayout
    private lateinit var btnRetry: Button

    private lateinit var matchAdapter: MatchAdapter
    private val matchList = ArrayList<FixtureResponseItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_matches, container, false)

        rvMatches = view.findViewById(R.id.rv_matches)
        progressLoading = view.findViewById(R.id.progress_loading)
        layoutErrorState = view.findViewById(R.id.layout_error_state)
        btnRetry = view.findViewById(R.id.btn_retry)

        setupRecyclerView()
        loadMatches()

        btnRetry.setOnClickListener {
            loadMatches()
        }

        return view
    }

    private fun setupRecyclerView() {
        matchAdapter = MatchAdapter(matchList) { match ->
            Toast.makeText(context, "${match.teams.home.name} vs ${match.teams.away.name} - ¡Gran encuentro futbolístico!", Toast.LENGTH_SHORT).show()
        }
        rvMatches.layoutManager = LinearLayoutManager(context)
        rvMatches.adapter = matchAdapter
    }

    fun refresh() {
        matchList.clear()
        loadMatches()
        Toast.makeText(context, "Sincronizando marcadores deportivos...", Toast.LENGTH_SHORT).show()
    }

    private fun loadMatches() {
        // Evitar redundancia HTTP si ya tenemos marcadores en memoria
        if (matchList.isNotEmpty()) {
            showLoading(false)
            showError(false)
            matchAdapter.updateList(matchList)
            return
        }

        showLoading(true)
        showError(false)
        matchList.clear()

        // Cargar últimos 15 marcadores de La Liga de España (ID 140, temporada 2024)
        RetrofitClient.footballInstance.getPastMatchesByLeague(leagueId = 140, season = 2024, lastCount = 15)
            .enqueue(object : Callback<FootballFixtureResponse> {
                override fun onResponse(call: Call<FootballFixtureResponse>, response: Response<FootballFixtureResponse>) {
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
                                    android.util.Log.e("MatchesFragment", "Error de API-Football: $errorMsg")
                                    Toast.makeText(context, "API Error: $errorMsg", Toast.LENGTH_LONG).show()
                                    showError(true)
                                    return
                                }
                            } else if (errorsObj.isJsonPrimitive) {
                                val errorMsg = errorsObj.asString
                                if (errorMsg.isNotEmpty()) {
                                    android.util.Log.e("MatchesFragment", "Error de API-Football: $errorMsg")
                                    Toast.makeText(context, "API Error: $errorMsg", Toast.LENGTH_LONG).show()
                                    showError(true)
                                    return
                                }
                            }
                        }

                        val events = body.response
                        if (!events.isNullOrEmpty()) {
                            matchList.addAll(events)
                        }
                    }

                    if (matchList.isNotEmpty()) {
                        // Ordenar por fecha cronológica descendente
                        matchList.sortByDescending { it.fixture.date ?: "" }
                        matchAdapter.updateList(matchList)
                        showError(false)
                    } else {
                        showError(true)
                        val errorDetail = response.errorBody()?.string() ?: "Respuesta vacía"
                        android.util.Log.e("MatchesFragment", "Error en API-Football (Fixtures): $errorDetail")
                        Toast.makeText(context, "No se encontraron marcadores deportivos globales o tu API Key es inválida.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<FootballFixtureResponse>, t: Throwable) {
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
        rvMatches.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showError(isError: Boolean) {
        layoutErrorState.visibility = if (isError) View.VISIBLE else View.GONE
        rvMatches.visibility = if (isError) View.GONE else View.VISIBLE
    }
}
