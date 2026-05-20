package ufz.dev.eventosdeportivos.data.sports

import com.google.gson.annotations.SerializedName
import com.google.gson.JsonElement

// --- Modelos de Respuesta para Equipos ---
data class FootballTeamResponse(
    @SerializedName("response") val response: List<TeamResponseItem>?,
    @SerializedName("errors") val errors: JsonElement?
)

data class TeamResponseItem(
    @SerializedName("team") val team: TeamDetails
)

data class TeamDetails(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("logo") val logo: String?,
    @SerializedName("founded") val founded: Int?
)

// --- Modelos de Respuesta para Partidos (Fixtures) ---
data class FootballFixtureResponse(
    @SerializedName("response") val response: List<FixtureResponseItem>?,
    @SerializedName("errors") val errors: JsonElement?
)

data class FixtureResponseItem(
    @SerializedName("fixture") val fixture: FixtureDetails,
    @SerializedName("league") val league: LeagueDetails,
    @SerializedName("teams") val teams: FixtureTeams,
    @SerializedName("goals") val goals: FixtureGoals
)

data class FixtureDetails(
    @SerializedName("id") val id: Int,
    @SerializedName("date") val date: String?, // Formato ISO 8601: "2026-05-20T14:00:00+00:00"
    @SerializedName("status") val status: FixtureStatus
)

data class FixtureStatus(
    @SerializedName("short") val short: String?, // e.g. "FT" (Finished), "NS" (Not Started), "1H", "2H", "LIVE"
    @SerializedName("elapsed") val elapsed: Int? // Minutos transcurridos
)

data class LeagueDetails(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class FixtureTeams(
    @SerializedName("home") val home: FixtureTeamDetails,
    @SerializedName("away") val away: FixtureTeamDetails
)

data class FixtureTeamDetails(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("logo") val logo: String?
)

data class FixtureGoals(
    @SerializedName("home") val home: Int?,
    @SerializedName("away") val away: Int?
)

// --- Modelos de Respuesta para Plantilla (Squads) ---
data class FootballSquadResponse(
    @SerializedName("response") val response: List<SquadResponseItem>?,
    @SerializedName("errors") val errors: JsonElement?
)

data class SquadResponseItem(
    @SerializedName("team") val team: SquadTeamDetails,
    @SerializedName("players") val players: List<SquadPlayerDetails>?
)

data class SquadTeamDetails(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("logo") val logo: String?
)

data class SquadPlayerDetails(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("age") val age: Int?,
    @SerializedName("number") val number: Int?,
    @SerializedName("position") val position: String?,
    @SerializedName("photo") val photo: String?
)

