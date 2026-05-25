package com.example.beamishinvitational.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {
    @Insert
    suspend fun insertTournament(tournament: Tournament): Long

    @Update
    suspend fun updateTournament(tournament: Tournament)

    @Query("SELECT * FROM tournaments")
    fun getAllTournaments(): Flow<List<Tournament>>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getTournamentById(id: Long): Tournament?

    @Insert
    suspend fun insertPlayer(player: Player): Long

    @Query("SELECT * FROM players WHERE tournamentId = :tournamentId")
    fun getPlayersForTournament(tournamentId: Long): Flow<List<Player>>

    @Insert
    suspend fun insertGame(game: Game): Long

    @Update
    suspend fun updateGame(game: Game)

    @Query("SELECT * FROM games WHERE tournamentId = :tournamentId ORDER BY gameOrder ASC")
    fun getGamesForTournament(tournamentId: Long): Flow<List<Game>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: Score)

    @Query("SELECT * FROM scores WHERE gameId = :gameId")
    fun getScoresForGame(gameId: Long): Flow<List<Score>>

    @Query("SELECT scores.* FROM scores INNER JOIN games ON scores.gameId = games.id WHERE games.tournamentId = :tournamentId")
    fun getScoresForTournament(tournamentId: Long): Flow<List<Score>>

    @Query("SELECT * FROM scores WHERE gameId = :gameId AND holeNumber = :holeNumber")
    suspend fun getScoresForHole(gameId: Long, holeNumber: Int): List<Score>
}
