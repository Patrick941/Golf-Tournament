package com.example.beamishinvitational.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

@Entity(tableName = "tournaments")
data class Tournament(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val splitPointsOnTie: Boolean = false,
    val pointsDistribution: String = "10,8,6,4,2,1",
    val totalGames: Int = 10,
    val holeInOneBonusGame: Double = 0.0,
    val holeInOneBonusTournament: Double = 0.0
) {
    constructor() : this(UUID.randomUUID().toString(), "", false, "10,8,6,4,2,1", 10, 0.0, 0.0)
}

@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(
            entity = Tournament::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tournamentId")]
)
data class Player(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tournamentId: String,
    val name: String
) {
    constructor() : this(UUID.randomUUID().toString(), "", "")
}

@Entity(
    tableName = "games",
    foreignKeys = [
        ForeignKey(
            entity = Tournament::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tournamentId")]
)
data class Game(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tournamentId: String,
    val locationName: String,
    val gameOrder: Int,
    val manualRanks: String? = null
) {
    constructor() : this(UUID.randomUUID().toString(), "", "", 0, null)
}

@Entity(
    tableName = "scores",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gameId"), Index("playerId"), Index("tournamentId")]
)
data class Score(
    @PrimaryKey val id: String, // Predictable ID: gameId_playerId_hole
    val tournamentId: String,
    val gameId: String,
    val playerId: String,
    val holeNumber: Int,
    val strokes: Int
) {
    constructor() : this("", "", "", "", 0, 0)
}
