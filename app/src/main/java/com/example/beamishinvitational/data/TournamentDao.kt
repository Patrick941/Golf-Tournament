package com.example.beamishinvitational.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: Tournament)

    @Update
    suspend fun updateTournament(tournament: Tournament)

    @Query("SELECT * FROM tournaments")
    fun getAllTournaments(): Flow<List<Tournament>>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getTournamentById(id: String): Tournament?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: Player)

    @Query("SELECT * FROM players WHERE tournamentId = :tournamentId")
    fun getPlayersForTournament(tournamentId: String): Flow<List<Player>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: Game)

    @Update
    suspend fun updateGame(game: Game)

    @Query("SELECT * FROM games WHERE tournamentId = :tournamentId ORDER BY gameOrder ASC")
    fun getGamesForTournament(tournamentId: String): Flow<List<Game>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: Score)

    @Query("SELECT * FROM scores WHERE gameId = :gameId")
    fun getScoresForGame(gameId: String): Flow<List<Score>>

    @Query("SELECT * FROM scores WHERE tournamentId = :tournamentId")
    fun getScoresForTournament(tournamentId: String): Flow<List<Score>>
}
