package com.example.mediajournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = RepositoryProvider.get(this)
        val coverSearchRepository = CoverSearchRepository(this)

        setContent {
            MediaJournalTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MediaJournalApp(repository, coverSearchRepository)
                }
            }
        }
    }
}

@Composable
fun MediaJournalApp(repository: ContentRepository, coverSearchRepository: CoverSearchRepository) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route.orEmpty()
    val showBottomBar = currentRoute == Routes.Home ||
        currentRoute == Routes.Stats ||
        currentRoute == Routes.History

    LaunchedEffect(repository) {
        repository.seedIfEmpty()
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.Home,
                        onClick = {
                            navController.navigate(Routes.Home) {
                                popUpTo(Routes.Home) { inclusive = true }
                            }
                        },
                        icon = { Icon(Icons.Rounded.Home, contentDescription = "Inicio") },
                        label = { Text("Inicio") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.Stats,
                        onClick = { navController.navigate(Routes.Stats) },
                        icon = { Icon(Icons.Rounded.BarChart, contentDescription = "Estadísticas") },
                        label = { Text("Estadísticas") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.History,
                        onClick = { navController.navigate(Routes.History) },
                        icon = { Icon(Icons.Rounded.History, contentDescription = "Histórico") },
                        label = { Text("Histórico") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.Home) {
                val vm: HomeViewModel = viewModel(
                    factory = RepositoryViewModelFactory { HomeViewModel(repository) }
                )
                val state by vm.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    state = state,
                    onTypeSelected = vm::setType,
                    onStatusSelected = vm::setStatus,
                    onSearchChanged = vm::setSearchQuery,
                    onAdd = { navController.navigate(Routes.add(state.selectedType)) },
                    onMarkFinished = vm::markFinished,
                    onOpen = { id -> navController.navigate(Routes.detail(id)) }
                )
            }
            composable(
                route = Routes.Add,
                arguments = listOf(navArgument("type") {
                    type = NavType.StringType
                    defaultValue = ""
                })
            ) { entry ->
                val initialType = entry.arguments
                    ?.getString("type")
                    ?.takeIf { it.isNotBlank() }
                    ?.let(ContentType::valueOf)
                val vm: EditContentViewModel = viewModel(
                    key = "add-${initialType?.name ?: "any"}",
                    factory = RepositoryViewModelFactory {
                        EditContentViewModel(repository, null, initialType, coverSearchRepository)
                    }
                )
                val state by vm.uiState.collectAsStateWithLifecycle()
                EditContentScreen(
                    state = state,
                    onTitleChanged = vm::updateTitle,
                    onTypeChanged = vm::updateType,
                    onStatusChanged = vm::updateStatus,
                    onRatingChanged = vm::updateRating,
                    onGenreChanged = vm::updateGenre,
                    onCoverUrlChanged = vm::updateCoverUrl,
                    onSearchCover = vm::searchCover,
                    onCoverSelected = vm::selectCover,
                    onNotesChanged = vm::updateNotes,
                    onSave = vm::save,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.Edit,
                arguments = listOf(navArgument("contentId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("contentId") ?: 0L
                val vm: EditContentViewModel = viewModel(
                    key = "edit-$id",
                    factory = RepositoryViewModelFactory { EditContentViewModel(repository, id, coverSearchRepository = coverSearchRepository) }
                )
                val state by vm.uiState.collectAsStateWithLifecycle()
                EditContentScreen(
                    state = state,
                    onTitleChanged = vm::updateTitle,
                    onTypeChanged = vm::updateType,
                    onStatusChanged = vm::updateStatus,
                    onRatingChanged = vm::updateRating,
                    onGenreChanged = vm::updateGenre,
                    onCoverUrlChanged = vm::updateCoverUrl,
                    onSearchCover = vm::searchCover,
                    onCoverSelected = vm::selectCover,
                    onNotesChanged = vm::updateNotes,
                    onSave = vm::save,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.Detail,
                arguments = listOf(navArgument("contentId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("contentId") ?: 0L
                val vm: DetailViewModel = viewModel(
                    key = "detail-$id",
                    factory = RepositoryViewModelFactory { DetailViewModel(repository, id) }
                )
                val state by vm.uiState.collectAsStateWithLifecycle()
                DetailScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onEdit = { contentId -> navController.navigate(Routes.edit(contentId)) },
                    onDeleted = {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Home) { inclusive = true }
                        }
                    },
                    onDelete = vm::delete,
                    onStatusChanged = vm::updateStatus
                )
            }
            composable(Routes.Stats) {
                val vm: StatsViewModel = viewModel(
                    factory = RepositoryViewModelFactory { StatsViewModel(repository) }
                )
                val state by vm.uiState.collectAsStateWithLifecycle()
                StatsScreen(
                    state = state,
                    onPreviousMonth = vm::previousMonth,
                    onNextMonth = vm::nextMonth,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.History) {
                val vm: HistoricalViewModel = viewModel(
                    factory = RepositoryViewModelFactory { HistoricalViewModel(repository) }
                )
                val state by vm.uiState.collectAsStateWithLifecycle()
                HistoricalScreen(
                    state = state,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

object Routes {
    const val Home = "home"
    const val Add = "addContent?type={type}"
    const val Stats = "stats"
    const val History = "history"
    const val Detail = "contentDetail/{contentId}"
    const val Edit = "editContent/{contentId}"

    fun detail(id: Long) = "contentDetail/$id"
    fun edit(id: Long) = "editContent/$id"
    fun add(type: ContentType?) = "addContent?type=${type?.name.orEmpty()}"
}

@Composable
fun MediaJournalTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = Color(0xFF7B6FA8),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE8E1FF),
        onPrimaryContainer = Color(0xFF2D254D),
        secondary = Color(0xFFBA7C74),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDAD5),
        onSecondaryContainer = Color(0xFF4C241F),
        tertiary = Color(0xFF5F8F86),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFD4F3EB),
        onTertiaryContainer = Color(0xFF173B35),
        background = Color(0xFFFFFBF7),
        onBackground = Color(0xFF2E2A2F),
        surface = Color(0xFFFFFBF7),
        onSurface = Color(0xFF2E2A2F),
        surfaceVariant = Color(0xFFF3ECE7),
        onSurfaceVariant = Color(0xFF6D626B),
        surfaceContainer = Color(0xFFFFF0EC),
        outline = Color(0xFFD5C7D2)
    )
    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}

