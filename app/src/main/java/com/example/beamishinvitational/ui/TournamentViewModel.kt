package com.example.beamishinvitational.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.beamishinvitational.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TournamentViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = try {
        AppDatabase.getDatabase(application).tournamentDao()
    } catch (e: Exception) {
        Log.e("BeamishDebug", "Failed to initialize local database", e)
        null
    }
    private val firestore = FirestoreRepository()

    private val _uiError = MutableStateFlow<String?>(null)
    val uiError: StateFlow<String?> = _uiError.asStateFlow()

    fun clearError() {
        _uiError.value = null
    }

    init {
        Log.d("BeamishDebug", "TournamentViewModel initialized")
        if (dao == null) {
            _uiError.value = "Local database error. App may not work correctly offline."
        }
    }

    // Source of truth is Firestore for multi-user sync
    val allTournaments: Flow<List<Tournament>> = firestore.getTournaments()
        .catch { e ->
            Log.e("BeamishDebug", "Error in allTournaments flow", e)
            _uiError.emit("Tournament sync error: ${e.localizedMessage}")
            emit(emptyList())
        }
        .onEach { tournaments ->
            if (dao != null) {
                viewModelScope.launch {
                    try {
                        tournaments.forEach { dao.insertTournament(it) }
                    } catch (e: Exception) {
                        Log.e("BeamishDebug", "Error syncing to local Room", e)
                    }
                }
            }
        }

    fun createTournament(name: String, splitPoints: Boolean, playerNames: List<String>, totalGames: Int) {
        viewModelScope.launch {
            try {
                val tournament = Tournament(name = name, splitPointsOnTie = splitPoints, totalGames = totalGames)
                Log.d("BeamishDebug", "Creating tournament: ${tournament.id}")
                firestore.saveTournament(tournament)
                
                playerNames.forEach { playerName ->
                    val player = Player(tournamentId = tournament.id, name = playerName)
                    firestore.savePlayer(player)
                }

                // Create games automatically based on totalGames
                for (i in 1..totalGames) {
                    val game = Game(tournamentId = tournament.id, locationName = "Course $i", gameOrder = i)
                    firestore.saveGame(game)
                }
            } catch (e: Exception) {
                Log.e("BeamishDebug", "Error creating tournament", e)
                _uiError.emit("Failed to create tournament: ${e.localizedMessage}")
            }
        }
    }

    fun updateTournament(tournament: Tournament) {
        viewModelScope.launch {
            try {
                Log.d("BeamishDebug", "Updating tournament: ${tournament.id} - ${tournament.name}")
                firestore.saveTournament(tournament)
            } catch (e: Exception) {
                Log.e("BeamishDebug", "Error updating tournament", e)
                _uiError.emit("Failed to update tournament: ${e.localizedMessage}")
            }
        }
    }

    fun getPlayers(tournamentId: String): Flow<List<Player>> = firestore.getPlayers(tournamentId)
        .catch { e ->
            Log.e("BeamishDebug", "Error fetching players", e)
            _uiError.emit("Error fetching players: ${e.localizedMessage}")
            emit(emptyList())
        }
    
    fun getGames(tournamentId: String): Flow<List<Game>> = firestore.getGames(tournamentId)
        .catch { e ->
            Log.e("BeamishDebug", "Error fetching games", e)
            _uiError.emit("Error fetching games: ${e.localizedMessage}")
            emit(emptyList())
        }

    fun updateGame(game: Game) {
        viewModelScope.launch {
            try {
                firestore.saveGame(game)
            } catch (e: Exception) {
                Log.e("BeamishDebug", "Error updating game", e)
                _uiError.emit("Failed to update course: ${e.localizedMessage}")
            }
        }
    }

    fun saveScore(tournamentId: String, gameId: String, playerId: String, hole: Int, strokes: Int) {
        viewModelScope.launch {
            try {
                // Use a predictable ID to avoid duplicates and allow overwriting
                val scoreId = "${gameId}_${playerId}_$hole"
                val score = Score(
                    id = scoreId,
                    tournamentId = tournamentId,
                    gameId = gameId,
                    playerId = playerId,
                    holeNumber = hole,
                    strokes = strokes
                )
                firestore.saveScore(tournamentId, score)
            } catch (e: Exception) {
                Log.e("BeamishDebug", "Error saving score", e)
                _uiError.emit("Failed to save score: ${e.localizedMessage}")
            }
        }
    }

    fun getScores(tournamentId: String, gameId: String): Flow<List<Score>> = firestore.getScores(tournamentId, gameId)
        .catch { e ->
            Log.e("BeamishDebug", "Error fetching scores", e)
            _uiError.emit("Error fetching scores: ${e.localizedMessage}")
            emit(emptyList())
        }

    fun getTournamentScores(tournamentId: String): Flow<List<Score>> = firestore.getAllTournamentScores(tournamentId)
        .catch { e ->
            Log.e("BeamishDebug", "Error fetching tournament scores", e)
            _uiError.emit("Error fetching standings: ${e.localizedMessage}")
            emit(emptyList())
        }
}
