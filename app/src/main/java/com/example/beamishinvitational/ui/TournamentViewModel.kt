package com.example.beamishinvitational.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.beamishinvitational.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TournamentViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).tournamentDao()

    val allTournaments = dao.getAllTournaments()

    fun createTournament(name: String, splitPoints: Boolean, playerNames: List<String>) {
        viewModelScope.launch {
            val tournamentId = dao.insertTournament(Tournament(name = name, splitPointsOnTie = splitPoints))
            
            playerNames.forEach { playerName ->
                dao.insertPlayer(Player(tournamentId = tournamentId, name = playerName))
            }

            // Create 10 games automatically
            for (i in 1..10) {
                dao.insertGame(Game(tournamentId = tournamentId, locationName = "Course $i", gameOrder = i))
            }
        }
    }

    fun updateTournament(tournament: Tournament) {
        viewModelScope.launch {
            dao.updateTournament(tournament)
        }
    }

    fun getPlayers(tournamentId: Long) = dao.getPlayersForTournament(tournamentId)
    
    fun getGames(tournamentId: Long) = dao.getGamesForTournament(tournamentId)

    fun updateGame(game: Game) {
        viewModelScope.launch {
            dao.updateGame(game)
        }
    }

    fun saveScore(gameId: Long, playerId: Long, hole: Int, strokes: Int) {
        viewModelScope.launch {
            dao.insertScore(Score(gameId = gameId, playerId = playerId, holeNumber = hole, strokes = strokes))
        }
    }

    fun getScores(gameId: Long) = dao.getScoresForGame(gameId)

    fun getTournamentScores(tournamentId: Long) = dao.getScoresForTournament(tournamentId)
}
