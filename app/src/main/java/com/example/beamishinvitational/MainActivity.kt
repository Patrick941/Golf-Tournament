@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.beamishinvitational

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

sealed class Screen {
    object TournamentList : Screen()
    object CreateTournament : Screen()
    data class GameList(val tournament: Tournament) : Screen()
    data class ScoreEntry(val tournament: Tournament, val game: Game) : Screen()
}

class MainActivity : ComponentActivity() {
    companion object {
        private const val LOG_PREFIX = "myLog"
        fun logError(tag: String, message: String, throwable: Throwable? = null) {
            val fullTag = "$LOG_PREFIX:$tag"
            if (throwable != null) {
                Log.e(fullTag, message, throwable)
                io.sentry.Sentry.captureException(throwable)
            } else {
                Log.e(fullTag, message)
                io.sentry.Sentry.captureMessage("$fullTag: $message")
            }
        }
        fun logDebug(tag: String, message: String) {
            Log.d("$LOG_PREFIX:$tag", message)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase before the ViewModel starts using it
        try {
            FirebaseApp.initializeApp(this)
            logDebug("Firebase", "Firebase initialized successfully")
            
            // Sign in anonymously if not already signed in
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously()
                    .addOnSuccessListener {
                        logDebug("Firebase", "Anonymous sign-in successful")
                    }
                    .addOnFailureListener { e ->
                        logError("Firebase", "Anonymous sign-in failed", e)
                    }
            }
        } catch (e: Exception) {
            logError("Firebase", "Firebase initialization failed", e)
        }

        logDebug("Lifecycle", "MainActivity onCreate")
        enableEdgeToEdge()
        setContent {
            BeamishInvitationalTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                
                fun showError(message: String) {
                    MainActivity.logError("UI", "Displaying error to user: $message")
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = SnackbarDuration.Long
                        )
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            AppContent(onError = ::showError)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppContent(onError: (String) -> Unit) {
    val viewModel: TournamentViewModel = viewModel()
    val uiError by viewModel.uiError.collectAsState()
    
    LaunchedEffect(uiError) {
        uiError?.let {
            onError(it)
            viewModel.clearError()
        }
    }

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

    // Handle system back button
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    onCreated = { name, split, players, totalGames ->
                        try {
                            viewModel.createTournament(name, split, players, totalGames)
                            currentScreen = Screen.TournamentList
                        } catch (e: Exception) {
                            onError("Could not create tournament: ${e.localizedMessage}")
                        }
                    }
                )
                is Screen.GameList -> GameListScreen(
                    viewModel,
                    screen.tournament,
                    onGameClick = { currentScreen = Screen.ScoreEntry(screen.tournament, it) },
                    onError = onError
                )
                is Screen.ScoreEntry -> ScoreEntryScreen(
                    viewModel,
                    screen.tournament,
                    screen.game,
                    onError = onError
                )
            }
        }

        showSettingsFor?.let { tournament ->
            SettingsDialog(
                tournament = tournament,
                onDismiss = { showSettingsFor = null },
                onSave = { updatedTournament ->
                    try {
                        viewModel.updateTournament(updatedTournament)
                        if (currentScreen is Screen.GameList && (currentScreen as Screen.GameList).tournament.id == updatedTournament.id) {
                            currentScreen = Screen.GameList(updatedTournament)
                        }
                        showSettingsFor = null
                    } catch (e: Exception) {
                        onError("Failed to save settings: ${e.localizedMessage}")
                    }
                }
            )
        }
    }
}

@Composable
fun ErrorFallback(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("A problem occurred:", style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(24.dp))
        Text("Please try restarting the application.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun SettingsDialog(tournament: Tournament, onDismiss: () -> Unit, onSave: (Tournament) -> Unit) {
    var name by remember { mutableStateOf(tournament.name) }
    var splitPoints by remember { mutableStateOf(tournament.splitPointsOnTie) }
    var distribution by remember { mutableStateOf(tournament.pointsDistribution) }
    var totalGamesText by remember { mutableStateOf(tournament.totalGames.toString()) }
    var hioGameText by remember { mutableStateOf(tournament.holeInOneBonusGame.toString()) }
    var hioTournamentText by remember { mutableStateOf(tournament.holeInOneBonusTournament.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tournament Settings") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tournament Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = splitPoints, onCheckedChange = { splitPoints = it })
                    Text("Split points on ties (vs both get 1)")
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = totalGamesText,
                    onValueChange = { totalGamesText = it },
                    label = { Text("Total Games") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = distribution,
                    onValueChange = { distribution = it },
                    label = { Text("Rank Points Distribution") },
                    placeholder = { Text("10,8,6,4,2,1") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Comma separated points for 1st, 2nd, 3rd, etc.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Hole-in-One Bonuses", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = hioGameText,
                    onValueChange = { hioGameText = it },
                    label = { Text("Game Bonus Points") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Added to hole points before game rank is decided.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = hioTournamentText,
                    onValueChange = { hioTournamentText = it },
                    label = { Text("Tournament Bonus Points") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Added directly to the overall tournament standings.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                val totalGames = totalGamesText.toIntOrNull() ?: tournament.totalGames
                val hioGame = hioGameText.toDoubleOrNull() ?: tournament.holeInOneBonusGame
                val hioTournament = hioTournamentText.toDoubleOrNull() ?: tournament.holeInOneBonusTournament
                onSave(tournament.copy(
                    name = name, 
                    splitPointsOnTie = splitPoints, 
                    pointsDistribution = distribution,
                    totalGames = totalGames,
                    holeInOneBonusGame = hioGame,
                    holeInOneBonusTournament = hioTournament
                )) 
            }) {
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
fun CreateTournamentScreen(onCreated: (String, Boolean, List<String>, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var splitPoints by remember { mutableStateOf(false) }
    var playersText by remember { mutableStateOf("") }
    var totalGamesText by remember { mutableStateOf("10") }

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
            value = totalGamesText,
            onValueChange = { totalGamesText = it },
            label = { Text("Total Games") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
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
                val totalGames = totalGamesText.toIntOrNull() ?: 10
                if (name.isNotEmpty() && players.isNotEmpty()) {
                    onCreated(name, splitPoints, players, totalGames)
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Create Tournament")
        }
    }
}

@Composable
fun GameListScreen(
    viewModel: TournamentViewModel, 
    tournament: Tournament, 
    onGameClick: (Game) -> Unit,
    onError: (String) -> Unit
) {
    val games by viewModel.getGames(tournament.id).collectAsState(initial = emptyList())
    val players by viewModel.getPlayers(tournament.id).collectAsState(initial = emptyList())
    val allScores by viewModel.getTournamentScores(tournament.id).collectAsState(initial = emptyList())

    val overallResults = remember(players, games, allScores, tournament) {
        try {
            calculateOverallTournamentResults(players, games, allScores, tournament)
        } catch (e: Exception) {
            MainActivity.logError("Standings", "Error calculating overall tournament results", e)
            emptyMap<String, Double>()
        }
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
                "Games / Courses",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(games) { game ->
            var isEditingName by remember { mutableStateOf(false) }
            var editedName by remember { mutableStateOf(game.locationName) }
            var showManualWinners by remember { mutableStateOf(false) }
            
            val gameScores = allScores.filter { it.gameId == game.id }
            val hasResults = gameScores.any { it.strokes > 0 }
            
            val gameLeader = if (!game.manualRanks.isNullOrEmpty()) {
                val winnerId = game.manualRanks.split(",").firstOrNull()
                players.find { it.id == winnerId }?.name
            } else if (hasResults) {
                try {
                    val results = calculateHolePointsAccumulatedInternal(players, gameScores, tournament.splitPointsOnTie)
                    results.entries.maxByOrNull { it.value }?.key?.name
                } catch (e: Exception) {
                    MainActivity.logError("GameList", "Error determining game leader for ${game.locationName}", e)
                    null
                }
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
                                    try {
                                        viewModel.updateGame(game.copy(locationName = editedName))
                                        isEditingName = false
                                    } catch (e: Exception) {
                                        onError("Update failed: ${e.localizedMessage}")
                                    }
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
                        Text("Game ${game.gameOrder} of ${tournament.totalGames}")
                        if (gameLeader != null) {
                            val prefix = if (!game.manualRanks.isNullOrEmpty()) "Winner (Manual): " else "Current Leader: "
                            Text("$prefix$gameLeader", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        } else {
                             Text("No scores yet", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                trailingContent = {
                    Row {
                        if (!isEditingName) {
                            IconButton(onClick = { showManualWinners = true }) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "Set Manual Winners",
                                    tint = if (game.manualRanks.isNullOrEmpty()) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { isEditingName = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Name")
                            }
                        }
                    }
                },
                modifier = Modifier.clickable { if (!isEditingName) onGameClick(game) }
            )
            HorizontalDivider()

            if (showManualWinners) {
                ManualWinnersDialog(
                    players = players,
                    initialRanks = game.manualRanks,
                    onDismiss = { showManualWinners = false },
                    onSave = { newRanks ->
                        viewModel.updateGame(game.copy(manualRanks = newRanks))
                        showManualWinners = false
                    }
                )
            }
        }
    }
}

@Composable
fun ManualWinnersDialog(
    players: List<Player>,
    initialRanks: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    val initialSelectedIds = remember(initialRanks) {
        initialRanks?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }
    var selectedPlayerIds by remember { mutableStateOf(initialSelectedIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Manual Winners") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Tap players in order of rank (1st, 2nd, 3rd...)",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (selectedPlayerIds.isNotEmpty()) {
                    Text("Current Ranking:", fontWeight = FontWeight.Bold)
                    selectedPlayerIds.forEachIndexed { index, id ->
                        val name = players.find { it.id == id }?.name ?: "Unknown"
                        Text("${index + 1}. $name", style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { selectedPlayerIds = emptyList() }, modifier = Modifier.align(Alignment.End)) {
                        Text("Clear All")
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }

                Text("Available Players:", fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    val remainingPlayers = players.filter { it.id !in selectedPlayerIds }
                    items(remainingPlayers) { player ->
                        ListItem(
                            headlineContent = { Text(player.name) },
                            modifier = Modifier.clickable {
                                selectedPlayerIds = selectedPlayerIds + player.id
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                val ranks = if (selectedPlayerIds.isEmpty()) null else selectedPlayerIds.joinToString(",")
                onSave(ranks) 
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TournamentStandingsTable(players: List<Player>, results: Map<String, Double>) {
    val sortedResults = results.toList().sortedByDescending { it.second }
    val hasAnyPoints = sortedResults.any { it.second > 0 }

    Card(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("Pos", modifier = Modifier.width(40.dp), style = MaterialTheme.typography.labelLarge)
                Text("Player", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Text("Total Pts", style = MaterialTheme.typography.labelLarge)
            }
            HorizontalDivider()
            if (!hasAnyPoints) {
                Text("No tournament results yet.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            } else {
                sortedResults.forEachIndexed { index, (playerId, points) ->
                    val playerName = players.find { it.id == playerId }?.name ?: "Unknown"
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Text("${index + 1}", modifier = Modifier.width(40.dp))
                        Text(playerName, modifier = Modifier.weight(1f))
                        val pointsText = try {
                            "%.1f".format(points)
                        } catch (e: Exception) {
                            MainActivity.logError("Standings", "Error formatting points ($points) for player $playerName", e)
                            "-"
                        }
                        Text(pointsText)
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
): Map<String, Double> {
    val totalTournamentPoints = mutableMapOf<String, Double>()
    players.forEach { totalTournamentPoints[it.id] = 0.0 }

    val dist = tournament.pointsDistribution.split(",").mapNotNull { it.trim().toDoubleOrNull() }

    games.forEach { game ->
        val gameScores = allScores.filter { it.gameId == game.id }
        
        // Add Tournament-level Hole-in-One bonus (awarded directly to overall total)
        if (tournament.holeInOneBonusTournament > 0) {
            gameScores.filter { it.strokes == 1 }.forEach { hioScore ->
                totalTournamentPoints[hioScore.playerId] = (totalTournamentPoints[hioScore.playerId] ?: 0.0) + tournament.holeInOneBonusTournament
            }
        }

        val manualRankIds = game.manualRanks?.split(",")?.filter { it.isNotBlank() }
        
        if (!manualRankIds.isNullOrEmpty()) {
            manualRankIds.forEachIndexed { index, playerId ->
                val p = if (index < dist.size) dist[index] else 0.0
                totalTournamentPoints[playerId] = (totalTournamentPoints[playerId] ?: 0.0) + p
            }
        } else {
            if (gameScores.any { it.strokes > 0 }) {
                val playerHolePoints = mutableMapOf<String, Double>()
                for (h in 1..18) {
                    val holeScores = gameScores.filter { it.holeNumber == h }
                    if (holeScores.isNotEmpty()) {
                        val pts = ScoringEngine.calculateHolePoints(holeScores, tournament.splitPointsOnTie)
                        pts.forEach { (pid, p) -> playerHolePoints[pid] = (playerHolePoints[pid] ?: 0.0) + p }
                    }
                }
                
                // Add Game-level Hole-in-One bonus (awarded to game total points before rank calculation?)
                // Actually, the request says "give the bonus points for either the individual game or the overall tournament".
                // If it's for the individual game, it usually means it boosts your score for that specific game leader board.
                if (tournament.holeInOneBonusGame > 0) {
                    gameScores.filter { it.strokes == 1 }.forEach { hioScore ->
                        playerHolePoints[hioScore.playerId] = (playerHolePoints[hioScore.playerId] ?: 0.0) + tournament.holeInOneBonusGame
                    }
                }

                if (playerHolePoints.values.any { it > 0 }) {
                    val gamePoints = ScoringEngine.calculateGamePoints(playerHolePoints, players.map { it.id }, dist)
                    gamePoints.forEach { (pid, p) -> totalTournamentPoints[pid] = (totalTournamentPoints[pid] ?: 0.0) + p }
                }
            }
        }
    }
    return totalTournamentPoints
}

@Composable
fun ScoreEntryScreen(
    viewModel: TournamentViewModel, 
    tournament: Tournament, 
    game: Game,
    onError: (String) -> Unit
) {
    val players by viewModel.getPlayers(tournament.id).collectAsState(initial = emptyList())
    val scores by viewModel.getScores(tournament.id, game.id).collectAsState(initial = emptyList())
    
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
                                    try {
                                        viewModel.saveScore(tournament.id, game.id, player.id, selectedHole, strokes)
                                    } catch (e: Exception) {
                                        onError("Save failed: ${e.localizedMessage}")
                                    }
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
                        "Individual Hole Points Total (Game Leaderboard)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    val results = try {
                        calculateHolePointsAccumulatedInternal(players, scores, tournament.splitPointsOnTie)
                    } catch (e: Exception) {
                        MainActivity.logError("ScoreEntry", "Error calculating accumulated hole points for game ${game.locationName}", e)
                        emptyMap<Player, Double>()
                    }

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
    
    val playerHolePoints = mutableMapOf<String, Double>()
    
    // Sum up hole points across all 18 holes
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
