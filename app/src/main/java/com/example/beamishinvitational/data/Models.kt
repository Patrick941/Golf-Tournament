package com.example.beamishinvitational.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "tournaments")
data class Tournament(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val splitPointsOnTie: Boolean = false,
    val pointsDistribution: String = "10,8,6,4,2,1" // Default distribution
)

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
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tournamentId: Long,
    val name: String
)

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
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tournamentId: Long,
    val locationName: String,
    val gameOrder: Int // 1 to 10
)

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
    indices = [Index("gameId"), Index("playerId")]
)
data class Score(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val playerId: Long,
    val holeNumber: Int, // 1 to 18
    val strokes: Int
)
