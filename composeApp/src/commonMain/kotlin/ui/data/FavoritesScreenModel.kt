package ui.data

import api.Course
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import co.touchlab.kermit.Logger
import com.pras.Database
import ui.getSupabaseClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "FavoritesViewModel"

data class FavoritesUiState(
    val favoritesList: List<Course> = emptyList(),
    val dataLoaded: Boolean = false,
    val errorMessage: String = "",
    val favoriteMessage: String = "",
    val refreshing: Boolean = false,
)


class FavoritesScreenModel : ScreenModel {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    fun setRefresh(isRefreshing: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                refreshing = isRefreshing
            )
        }
    }

    fun getFavorites(database: Database) {
        screenModelScope.launch(Dispatchers.IO) {
            try {
                /*
                _uiState.update { currentState ->
                    currentState.copy(
                        refreshing = true
                    )
                }
                */
                val idList = database.favoritesQueries.selectAll().executeAsList()
                val result = favoritesQuery(supabase, idList)
                _uiState.update { currentState ->
                    currentState.copy(
                        favoritesList = result,
                        dataLoaded = true,
                        refreshing = false
                    )
                }
            }  catch (e: Exception) {
                Logger.d("Exception in favorites: $e", tag = TAG)
                if (e !is CancellationException) {

                    val errorMessage = when(e) {
                        is HttpRequestException -> "Failed to fetch favorites"
                        is BadRequestRestException -> "Bad request"
                        else -> "An error occurred: ${e.message}"
                    }
                    // cancellation exceptions are normal
                    _uiState.update { currentState ->
                        currentState.copy(
                            errorMessage = errorMessage
                        )
                    }
                }
            }
        }
    }

    suspend fun handleFavorite(course: Course, database: Database) {
        withContext(Dispatchers.IO) {
            if (database.favoritesQueries.select(course.id).executeAsOneOrNull().isNullOrEmpty()) {
                database.favoritesQueries.insert(course.id)
                setFavoritesMessage("Favorited ${course.short_name}")
            } else {
                database.favoritesQueries.delete(course.id)
                setFavoritesMessage("Unfavorited ${course.short_name}")
            }
        }
    }

    private fun setFavoritesMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            favoriteMessage = message
        )
    }

    suspend fun favoritesQuery(supabase: SupabaseClient, idList: List<String>): List<Course> {
        val courseList = supabase.from("courses").select {

            filter {
                isIn("id", idList)
            }

            order(column = "term", order = Order.DESCENDING)
            order(column = "department", order = Order.ASCENDING)
            order(column = "course_number", order = Order.ASCENDING)
            order(column = "course_letter", order = Order.ASCENDING)
            order(column = "section_number", order = Order.ASCENDING)

        }.decodeList<Course>()

        Logger.d(courseList.toString(), tag = TAG)
        return courseList
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = ""
        )
    }

    private companion object {
        val supabase = getSupabaseClient()
    }
}