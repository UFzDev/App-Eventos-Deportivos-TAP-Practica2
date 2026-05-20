package ufz.dev.eventosdeportivos.data.sports

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface FootballApiService {
    
    // Obtener todos los equipos de una liga específica para una temporada (ej: league = 140 para La Liga, season = 2025)
    @GET("teams")
    fun getTeamsByLeague(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): Call<FootballTeamResponse>

    // Obtener los partidos anteriores/resultados de una liga específica
    // Ejemplo: league = 140 para La Liga, season = 2025, last = 15 para obtener los últimos 15 partidos
    @GET("fixtures")
    fun getPastMatchesByLeague(
        @Query("league") leagueId: Int,
        @Query("season") season: Int,
        @Query("last") lastCount: Int
    ): Call<FootballFixtureResponse>

    // Obtener la plantilla de jugadores (roster/squad) de un equipo específico
    @GET("players/squads")
    fun getTeamSquad(
        @Query("team") teamId: Int
    ): Call<FootballSquadResponse>
}
