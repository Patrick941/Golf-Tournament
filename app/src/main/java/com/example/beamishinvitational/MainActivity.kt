@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.beamishinvitational

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.beamishinvitational.data.*
import com.example.beamishinvitational.ui.TournamentViewModel
import com.example.beamishinvitational.ui.theme.BeamishInvitationalTheme
import androidx.compose.foundation.text.KeyboardOptions

sealed class Screen {
    object TournamentList : Screen()
    object CreateTournament : Screen()
    data class GameList(val tournament: Tournament) : Screen()
    data class ScoreEntry(val tournament: Tournament, val game: Game) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeamishInvitationalTheme {
                val viewModel: TournamentViewModel = viewModel()
                var currentScreen by remember { mutableStateOf<Screen>(Screen.TournamentList) }
                var showSettingsFor by remember { mutableStateOf<Tournament?>(null) }

                fun goBack() {
                    currentScreen = when (val screen = currentScreen) {
                        is Screen.CreateTournament -> Screen.TournamentList
                        is Screen.GameList -> Screen.TournamentList
                        is Screen.ScoreEntry -> Screen.GameList(screen.tournament)
                        else -> Screen.TournamentList
                    }
                }

                BackHandler(enabled = currentScreen !is Screen.TournamentList) {
                    goBack()
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    when (val screen = currentScreen) {
                                        is Screen.TournamentList -> "Tournaments"
                                        is Screen.CreateTournament -> "New Tournament"
                                        is Screen.GameList -> screen.tournament.name
                                        is Screen.ScoreEntry -> screen.game.locationName
                                    }
                                )
                            },
                            navigationIcon = {
                                if (currentScreen !is Screen.TournamentList) {
                                    IconButton(onClick = { goBack() }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                    }
                                }
                            },
                            actions = {
                                if (currentScreen is Screen.GameList) {
                                    IconButton(onClick = { showSettingsFor = (currentScreen as Screen.GameList).tournament }) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                                    }
                                }
                            }
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                        when (val screen = currentScreen) {
                            is Screen.TournamentList -> TournamentListScreen(
                                viewModel,
                                onCreateClick = { currentScreen = Screen.CreateTournament },
                                onTournamentClick = { currentScreen = Screen.GameList(it) }
                            )
                            is Screen.CreateTournament -> CreateTournamentScreen(
                                onCreated = { name, split, players ->
                                    viewModel.createTournament(name, split, players)
                                    currentScreen = Screen.TournamentList
                                }
                            )
                            is Screen.GameList -> GameListScreen(
                                viewModel,
                                screen.tournament,
                                onGameClick = { currentScreen = Screen.ScoreEntry(screen.tournament, it) }
                            )
                            is Screen.ScoreEntry -> ScoreEntryScreen(
                                viewModel,
                                screen.tournament,
                                screen.game
                            )
                        }
                    }

                    showSettingsFor?.let { tournament ->
                        SettingsDialog(
                            tournament = tournament,
                            onDismiss = { showSettingsFor = null },
                            onSave = { updatedTournament ->
                                viewModel.updateTournament(updatedTournament)
                                if (currentScreen is Screen.GameList && (currentScreen as Screen.GameList).tournament.id == updatedTournament.id) {
                                    currentScreen = Screen.GameList(updatedTournament)
                                }
                                showSettingsFor = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(tournament: Tournament, onDismiss: () -> Unit, onSave: (Tournament) -> Unit) {
    var splitPoints by remember { mutableStateOf(tournament.splitPointsOnTie) }
    var distribution by remember { mutableStateOf(tournament.pointsDistribution) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tournament Settings") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = splitPoints, onCheckedChange = { splitPoints = it })
                    Text("Split points on ties (vs both get 1)")
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = distribution,
                    onValueChange = { distribution = it },
                    label = { Text("Rank Points Distribution") },
                    placeholder = { Text("10,8,6,4,2,1") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Points awarded for rank 1, 2, 3, etc. Comma separated.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(tournament.copy(splitPointsOnTie = splitPoints, pointsDistribution = distribution)) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TournamentListScreen(
    viewModel: TournamentViewModel,
    onCreateClick: () -> Unit,
    onTournamentClick: (Tournament) -> Unit
) {
    val tournaments by viewModel.allTournaments.collectAsState(initial = emptyList())

    Box(Modifier.fillMaxSize()) {
        if (tournaments.isEmpty()) {
            Text("No tournaments yet. Tap + to create one.", modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn {
                items(tournaments) { tournament ->
                    ListItem(
                        headlineContent = { Text(tournament.name) },
                        modifier = Modifier.clickable { onTournamentClick(tournament) }
                    )
                    HorizontalDivider()
                }
            }
        }
        FloatingActionButton(
            onClick = onCreateClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }
}

@Composable
fun CreateTournamentScreen(onCreated: (String, Boolean, List<String>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var splitPoints by remember { mutableStateOf(false) }
    var playersText by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Tournament Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = splitPoints, onCheckedChange = { splitPoints = it })
            Spacer(modifier = Modifier.width(8.dp))
            Text("Split points on ties (vs both get 1)")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = playersText,
            onValueChange = { playersText = it },
            label = { Text("Player Names (comma separated)") },
            placeholder = { Text("Alice, Bob, Charlie") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                val players = playersText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (name.isNotEmpty() && players.isNotEmpty()) {
                    onCreated(name, splitPoints, players)
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Create Tournament")
        }
    }
}

@Composable
fun GameListScreen(viewModel: TournamentViewModel, tournament: Tournament, onGameClick: (Game) -> Unit) {
    val games by viewModel.getGames(tournament.id).collectAsState(initial = emptyList())
    val players by viewModel.getPlayers(tournament.id).collectAsState(initial = emptyList())
    val allScores by viewModel.getTournamentScores(tournament.id).collectAsState(initial = emptyList())

    val overallResults = remember(players, games, allScores, tournament) {
        calculateOverallTournamentResults(players, games, allScores, tournament)
    }

    LazyColumn {
        item {
            Text(
                "Overall Tournament Standings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            TournamentStandingsTable(players, overallResults)
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text(
                "Course Results / Games",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(games) { game ->
            var isEditingName by remember { mutableStateOf(false) }
            var editedName by remember { mutableStateOf(game.locationName) }
            
            val gameScores = allScores.filter { it.gameId == game.id }
            val hasResults = gameScores.any { it.strokes > 0 }
            
            val gameLeader = if (hasResults) {
                val results = calculateHolePointsAccumulatedInternal(players, gameScores, tournament.splitPointsOnTie)
                results.entries.maxByOrNull { it.value }?.key?.name
            } else null

            ListItem(
                headlineContent = {
                    if (isEditingName) {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.updateGame(game.copy(locationName = editedName))
                                    isEditingName = false
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save")
                                }
                            }
                        )
                    } else {
                        Text(game.locationName, fontWeight = FontWeight.Bold)
                    }
                },
                supportingContent = {
                    Column {
                        Text("Game ${game.gameOrder} of 10")
                        if (gameLeader != null) {
                            Text("Current Leader: $gameLeader", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        } else {
                             Text("No scores yet", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                trailingContent = {
                    if (!isEditingName) {
                        IconButton(onClick = { isEditingName = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Name")
                        }
                    }
                },
                modifier = Modifier.clickable { if (!isEditingName) onGameClick(game) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun TournamentStandingsTable(players: List<Player>, results: Map<Long, Double>) {
    val sortedResults = results.toList().sortedByDescending { it.second }
    val hasAnyPoints = sortedResults.any { it.second > 0 }

    Card(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("Pos", modifier = Modifier.width(40.dp), style = MaterialTheme.typography.labelLarge)
                Text("Player", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Text("Rank Pts", style = MaterialTheme.typography.labelLarge)
            }
            HorizontalDivider()
            if (!hasAnyPoints) {
                Text("No scores entered yet.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            } else {
                sortedResults.forEachIndexed { index, (playerId, points) ->
                    val playerName = players.find { it.id == playerId }?.name ?: "Unknown"
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Text("${index + 1}", modifier = Modifier.width(40.dp))
                        Text(playerName, modifier = Modifier.weight(1f))
                        Text("%.1f".format(points))
                    }
                }
            }
        }
    }
}

fun calculateOverallTournamentResults(
    players: List<Player>,
    games: List<Game>,
    allScores: List<Score>,
    tournament: Tournament
): Map<Long, Double> {
    val totalTournamentPoints = mutableMapOf<Long, Double>()
    players.forEach { totalTournamentPoints[it.id] = 0.0 }

    val dist = tournament.pointsDistribution.split(",").mapNotNull { it.trim().toDoubleOrNull() }

    games.forEach { game ->
        val gameScores = allScores.filter { it.gameId == game.id }
        if (gameScores.any { it.strokes > 0 }) {
            val playerHolePoints = mutableMapOf<Long, Double>()
            for (h in 1..18) {
                val holeScores = gameScores.filter { it.holeNumber == h }
                if (holeScores.isNotEmpty()) {
                    val pts = ScoringEngine.calculateHolePoints(holeScores, tournament.splitPointsOnTie)
                    pts.forEach { (pid, p) -> playerHolePoints[pid] = (playerHolePoints[pid] ?: 0.0) + p }
                }
            }
            if (playerHolePoints.values.any { it > 0 }) {
                val gamePoints = ScoringEngine.calculateGamePoints(playerHolePoints, players.map { it.id }, dist)
                gamePoints.forEach { (pid, p) -> totalTournamentPoints[pid] = (totalTournamentPoints[pid] ?: 0.0) + p }
            }
        }
    }
    return totalTournamentPoints
}

@Composable
fun ScoreEntryScreen(viewModel: TournamentViewModel, tournament: Tournament, game: Game) {
    val players by viewModel.getPlayers(tournament.id).collectAsState(initial = emptyList())
    val scores by viewModel.getScores(game.id).collectAsState(initial = emptyList())
    
    var selectedHole by remember { mutableIntStateOf(1) }

    Column {
        ScrollableTabRow(selectedTabIndex = selectedHole - 1) {
            for (h in 1..18) {
                Tab(
                    selected = selectedHole == h,
                    onClick = { selectedHole = h },
                    text = { Text("$h") }
                )
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(players) { player ->
                val playerScore = scores.find { it.playerId == player.id && it.holeNumber == selectedHole }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(player.name, modifier = Modifier.weight(1f))
                    var scoreText by remember(player.id, selectedHole) { 
                        mutableStateOf(if ((playerScore?.strokes ?: 0) > 0) playerScore!!.strokes.toString() else "") 
                    }
                    OutlinedTextField(
                        value = scoreText,
                        onValueChange = { 
                            scoreText = it
                            it.toIntOrNull()?.let { strokes ->
                                if (strokes >= 0) {
                                    viewModel.saveScore(game.id, player.id, selectedHole, strokes)
                                }
                            }
                        },
                        modifier = Modifier.width(70.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
            
            if (scores.any { it.strokes > 0 }) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        "Individual Hole Points (Holes 1-18 Total)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    val results = calculateHolePointsAccumulatedInternal(players, scores, tournament.splitPointsOnTie)
                    results.entries.sortedByDescending { it.value }.forEach { (player, points) ->
                        ListItem(
                            headlineContent = { Text(player.name) },
                            trailingContent = { Text("%.1f pts".format(points)) }
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun calculateHolePointsAccumulatedInternal(players: List<Player>, scores: List<Score>, splitPoints: Boolean): Map<Player, Double> {
    if (players.isEmpty()) return emptyMap()
    
    val playerHolePoints = mutableMapOf<Long, Double>()
    
    for (h in 1..18) {
        val holeScores = scores.filter { it.holeNumber == h }
        if (holeScores.isNotEmpty()) {
            val pointsMap = ScoringEngine.calculateHolePoints(holeScores, splitPoints)
            pointsMap.forEach { (playerId, pt) ->
                playerHolePoints[playerId] = (playerHolePoints[playerId] ?: 0.0) + pt
            }
        }
    }
    
    return players.associateWith { playerHolePoints[it.id] ?: 0.0 }
}
