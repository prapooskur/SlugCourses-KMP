import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import api.Type
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.pras.Database
import com.pras.slugcourses.ui.elements.BoringNormalTopBar
import kotlinx.coroutines.launch
import ui.data.ResultsScreenModel
import ui.elements.CourseCard

private const val TAG = "results"

data class ResultsScreen(
    val term: Int,
    val query: String,
    val type: List<Type>,
    val genEd: List<String>,
    val searchType: String,
    val database: Database
) : Screen {
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { ResultsScreenModel() }
        val uiState by screenModel.uiState.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                BoringNormalTopBar(
                    titleText = if (query.isNotBlank()) "Results for \"${query.trim()}\"" else "Results",
                    navigator = navigator,
                )
            },
            content = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.dataLoaded) {
                        val response = uiState.resultsList
                        // val favorites: Set<String> = database.favoritesQueries.selectAll().executeAsList().toSet()
                        if (response.isNotEmpty()) {
                            items(response.size) { id ->
                                val course = response[id]
                                CourseCard(
                                    course = course,
                                    navigator = navigator,
                                    // isFavorited = false,
                                    // fixme
                                    isFavorited = uiState.favoritesList.contains(course.id),
                                    onFavorite = {
                                        coroutineScope.launch {
                                            screenModel.handleFavorite(course, database)
                                        }
                                    }
                                )
                            }
                        } else {
                            item {
                                Text(
                                    text = "No classes found.",
                                    fontSize = 24.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.offset(y = (240).dp)
                                )
                            }
                        }

                    } else {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        )

        val splitQuery = query.contains(" ")

        val department = if (splitQuery) { query.substringBefore(" ") } else { query }
        val courseNumber = if (splitQuery) { query.substringAfter(" ") } else { query }

        LaunchedEffect(Unit) {
            screenModel.getCourses(
                term,
                department,
                courseNumber,
                query,
                type,
                genEd,
                searchType
            )
            screenModel.getFavorites(database)
        }
    }
}