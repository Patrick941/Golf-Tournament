package com.example.beamishinvitational.data

import kotlin.math.pow

object ScoringEngine {

    /**
     * Calculates hole points for each player in a single hole.
     */
    fun calculateHolePoints(
        scores: List<Score>,
        splitPoints: Boolean
    ): Map<Long, Double> {
        if (scores.isEmpty()) return emptyMap()

        val minStrokes = scores.minOf { it.strokes }
        val winners = scores.filter { it.strokes == minStrokes }
        
        val pointsPerWinner = if (splitPoints) {
            1.0 / winners.size
        } else {
            1.0
        }

        val results = mutableMapOf<Long, Double>()
        // Initialize everyone with 0
        scores.forEach { results[it.playerId] = 0.0 }
        // Assign points to winners
        winners.forEach { results[it.playerId] = pointsPerWinner }
        
        return results
    }

    /**
     * Calculates game points based on total hole points accumulated.
     * The points distribution is non-linear, emphasizing higher ranks.
     * Winner gets max, last gets 0.
     */
    fun calculateGamePoints(
        playerHolePoints: Map<Long, Double>
    ): Map<Long, Double> {
        if (playerHolePoints.isEmpty()) return emptyMap()

        val sortedPlayers = playerHolePoints.toList().sortedByDescending { it.second }
        val numPlayers = sortedPlayers.size
        
        if (numPlayers == 1) return mapOf(sortedPlayers[0].first to 10.0) // Arbitrary max for solo

        val gamePointsMap = mutableMapOf<Long, Double>()
        
        // We want a distribution where rank 1 (index 0) is highest and rank N (index numPlayers - 1) is 0.
        // Using a power function for non-linearity.
        // Formula: Points = MaxPoints * ((N - Rank) / (N - 1))^power
        val maxPoints = 10.0 // Base max points for a game
        val power = 1.5 // Emphasis on higher ranks

        sortedPlayers.forEachIndexed { index, (playerId, _) ->
            val rank = index.toDouble()
            val points = maxPoints * ((numPlayers - 1 - rank) / (numPlayers - 1)).pow(power)
            
            // Round to 1 decimal place for readability
            gamePointsMap[playerId] = Math.round(points * 10.0) / 10.0
        }

        return gamePointsMap
    }
}
