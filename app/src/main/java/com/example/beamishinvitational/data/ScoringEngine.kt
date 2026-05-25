package com.example.beamishinvitational.data

object ScoringEngine {

    /**
     * Calculates hole points for each player in a single hole.
     * The winner of the hole gets 1 point.
     * Ties are handled based on tournament settings (split or both get 1).
     */
    fun calculateHolePoints(
        scores: List<Score>,
        splitPoints: Boolean
    ): Map<Long, Double> {
        // Filter out non-positive stroke entries (initial state or incomplete)
        val validScores = scores.filter { it.strokes > 0 }
        if (validScores.isEmpty()) return emptyMap()

        val minStrokes = validScores.minOf { it.strokes }
        val winners = validScores.filter { it.strokes == minStrokes }
        
        val pointsPerWinner = if (splitPoints) {
            1.0 / winners.size
        } else {
            1.0
        }

        val results = mutableMapOf<Long, Double>()
        winners.forEach { results[it.playerId] = pointsPerWinner }
        
        return results
    }

    /**
     * Calculates game points based on total hole points accumulated in a game.
     * distribution: List of points for rank 1, 2, 3... 
     * If there are more players than distribution entries, the rest get 0.
     */
    fun calculateGamePoints(
        playerHolePoints: Map<Long, Double>,
        allPlayerIds: List<Long>,
        distribution: List<Double>
    ): Map<Long, Double> {
        if (allPlayerIds.isEmpty()) return emptyMap()

        // If no one has any hole points yet (points > 0), everyone gets 0 game points
        if (playerHolePoints.values.all { it <= 0.0 }) {
            return allPlayerIds.associateWith { 0.0 }
        }

        // Ensure all players are represented
        val fullPointsMap = allPlayerIds.associateWith { playerHolePoints[it] ?: 0.0 }
        
        // Sort players by their hole points (higher points = better rank)
        val sortedPlayers = fullPointsMap.toList().sortedByDescending { it.second }
        
        val gamePointsMap = mutableMapOf<Long, Double>()
        
        sortedPlayers.forEachIndexed { index, (playerId, _) ->
            val points = if (index < distribution.size) distribution[index] else 0.0
            gamePointsMap[playerId] = points
        }

        return gamePointsMap
    }
}
