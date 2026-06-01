package com.example.beamishinvitational.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = try {
        Firebase.firestore
    } catch (e: Exception) {
        Log.e("BeamishDebug", "Error initializing Firestore", e)
        null
    }
    private val tournamentsCol = db?.collection("tournaments")

    fun getTournaments(): Flow<List<Tournament>> = callbackFlow {
        if (tournamentsCol == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = tournamentsCol.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirebaseDebug", "Error fetching tournaments", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                try {
                    val list = snapshot.toObjects(Tournament::class.java)
                    trySend(list)
                } catch (e: Exception) {
                    Log.e("FirebaseDebug", "Error parsing tournaments", e)
                }
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun saveTournament(tournament: Tournament) {
        try {
            tournamentsCol?.document(tournament.id)?.set(tournament)?.await()
        } catch (e: Exception) {
            Log.e("FirebaseDebug", "Error saving tournament", e)
            throw e
        }
    }

    fun getPlayers(tournamentId: String): Flow<List<Player>> = callbackFlow {
        if (tournamentsCol == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = tournamentsCol.document(tournamentId).collection("players")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseDebug", "Error fetching players", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        trySend(snapshot.toObjects(Player::class.java))
                    } catch (e: Exception) {
                        Log.e("FirebaseDebug", "Error parsing players", e)
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun savePlayer(player: Player) {
        try {
            tournamentsCol?.document(player.tournamentId)?.collection("players")
                ?.document(player.id)?.set(player)?.await()
        } catch (e: Exception) {
            Log.e("FirebaseDebug", "Error saving player", e)
            throw e
        }
    }

    fun getGames(tournamentId: String): Flow<List<Game>> = callbackFlow {
        if (tournamentsCol == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = tournamentsCol.document(tournamentId).collection("games")
            .orderBy("gameOrder")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseDebug", "Error fetching games", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        trySend(snapshot.toObjects(Game::class.java))
                    } catch (e: Exception) {
                        Log.e("FirebaseDebug", "Error parsing games", e)
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveGame(game: Game) {
        try {
            tournamentsCol?.document(game.tournamentId)?.collection("games")
                ?.document(game.id)?.set(game)?.await()
        } catch (e: Exception) {
            Log.e("FirebaseDebug", "Error saving game", e)
            throw e
        }
    }

    fun getScores(tournamentId: String, gameId: String): Flow<List<Score>> = callbackFlow {
        if (tournamentsCol == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = tournamentsCol.document(tournamentId).collection("games")
            .document(gameId).collection("scores")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseDebug", "Error fetching scores", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        trySend(snapshot.toObjects(Score::class.java))
                    } catch (e: Exception) {
                        Log.e("FirebaseDebug", "Error parsing scores", e)
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveScore(tournamentId: String, score: Score) {
        try {
            tournamentsCol?.document(tournamentId)?.collection("games")
                ?.document(score.gameId)?.collection("scores")
                ?.document(score.id)?.set(score)?.await()
        } catch (e: Exception) {
            Log.e("FirebaseDebug", "Error saving score", e)
            throw e
        }
    }

    fun getAllTournamentScores(tournamentId: String): Flow<List<Score>> = callbackFlow {
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = db.collectionGroup("scores")
            .whereEqualTo("tournamentId", tournamentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseDebug", "Error fetching all tournament scores", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        trySend(snapshot.toObjects(Score::class.java))
                    } catch (e: Exception) {
                        Log.e("FirebaseDebug", "Error parsing tournament scores", e)
                    }
                }
            }
        awaitClose { subscription.remove() }
    }
}
