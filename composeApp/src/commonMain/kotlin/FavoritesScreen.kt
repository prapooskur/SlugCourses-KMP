package com.pras.slugcourses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.pras.Database
import kotlinx.coroutines.launch
import ui.data.FavoritesScreenModel
import ui.elements.CourseCard


private const val TAG = "favorites"

data class FavoritesScreen(val database: Database) : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { FavoritesScreenModel() }
        val uiState by screenModel.uiState.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Favorites",
                            overflow = TextOverflow.Ellipsis
                        )
                    },
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
                        val response = uiState.favoritesList
                        val favorites: Set<String> = database.favoritesQueries.selectAll().executeAsList().toSet()
                        if (response.isNotEmpty()) {
                            items(response.size) { id ->
                                val course = response[id]
                                CourseCard(
                                    course = course,
                                    navigator = navigator,
                                    isFavorited = favorites.contains(course.id),
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
                                    text = "No favorite classes.",
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

        LaunchedEffect(key1 = uiState.errorMessage) {
            if (uiState.errorMessage.isNotEmpty()) {
                //shorttoast("An error occurred: ${uiState.errorMessage}", context)
            }
        }

        LaunchedEffect(Unit) {
            screenModel.getFavorites(database)
        }
    }
    
}