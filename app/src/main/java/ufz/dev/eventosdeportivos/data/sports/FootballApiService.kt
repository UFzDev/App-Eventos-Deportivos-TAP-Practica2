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

    // Obtener los partidos de una liga específica para procesar en cliente
    // Ejemplo: league = 140 para La Liga, season = 2024
    @GET("fixtures")
    fun getPastMatchesByLeague(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): Call<FootballFixtureResponse>

    // Obtener todos los partidos mundiales de una fecha específica
    // Ejemplo: date = "2026-05-19"
    @GET("fixtures")
    fun getMatchesByDate(
        @Query("date") date: String
    ): Call<FootballFixtureResponse>

    // Obtener los partidos en vivo (tiempo real)
    // Ejemplo: live = "all" o "140" (La Liga)
    @GET("fixtures")
    fun getLiveMatches(
        @Query("live") live: String
    ): Call<FootballFixtureResponse>

    // Obtener la plantilla de jugadores (roster/squad) de un equipo específico
    @GET("players/squads")
    fun getTeamSquad(
        @Query("team") teamId: Int
    ): Call<FootballSquadResponse>
}
