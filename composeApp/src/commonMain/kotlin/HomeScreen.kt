import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import api.Status
import api.Type
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import cafe.adriel.voyager.core.model.rememberScreenModel
import com.pras.Database
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import slugcourses.composeapp.generated.resources.Res
import slugcourses.composeapp.generated.resources.slug
import ui.data.HomeScreenModel
import ui.elements.LargeDropdownMenu
import ui.elements.LargeDropdownMenuMultiSelect

data class HomeScreen(val database: Database) : Screen {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { HomeScreenModel() }
        val uiState by screenModel.uiState.collectAsState()

        val termMap = mapOf(
            "Summer 2024" to 2244,
            "Spring 2024" to 2242,
            "Winter 2024" to 2240,
            "Fall 2023" to 2238,
            "Summer 2023" to 2234,
            "Spring 2023" to 2232,
            "Winter 2023" to 2230,
            "Fall 2022" to 2228,
            "Summer 2022" to 2224
        )

        val genEdList = listOf("CC", "ER", "IM", "MF", "SI", "SR", "TA", "PE", "PR", "C")
        val selectedGenEdList = remember { mutableStateListOf<String>() }

        val termList = termMap.keys.toList()
        var selectedTermIndex by rememberSaveable { mutableIntStateOf(0) }

        val typeList = listOf("Hybrid", "Async Online", "Sync Online", "In Person")
        val selectedTypeList = remember { mutableStateListOf("Async Online", "Hybrid", "Sync Online", "In Person") }

        var selectedStatusIndex by rememberSaveable { mutableIntStateOf(1) }

        fun searchHandler() {


            val term = termMap.values.toList()[selectedTermIndex]

            val status = Json.encodeToString(Status.ALL)
            val classType: List<Type> = selectedTypeList.map { Type.valueOf(it.replace(" ","_").uppercase()) }
            val encodedType = Json.encodeToString(classType)
            val geList = Json.encodeToString(selectedGenEdList.toList())
            val searchType = when (selectedStatusIndex) {
                0 -> "Open"
                else -> "All"
            }

            // todo fix
//            val navPath = if (uiState.searchQuery.isBlank()) {
//                "results/${term}/${status}/${encodedType}/${geList}/${searchType}"
//            } else {
//                "results/${term}/${uiState.searchQuery}/${status}/${encodedType}/${geList}/${searchType}"
//            }
//
//
            navigator.push(ResultsScreen(term, uiState.searchQuery, classType, selectedGenEdList, searchType, database))

        }


        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .offset(y = 15.dp)
        ) {
            item {
                //Text("Slug Courses", fontSize = 42.sp, fontWeight = FontWeight.SemiBold)
                Image(
                    painterResource(Res.drawable.slug),
                    contentDescription = "Slug Courses",
                    contentScale = ContentScale.Inside,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight(0.25f)
                )
                SearchBar(
                    modifier = Modifier.padding(16.dp),
                    query = uiState.searchQuery,
                    onQueryChange = { newQuery->
                        screenModel.setQuery(newQuery)
                    },
                    active = false,
                    onActiveChange = { /* do nothing */ },
                    placeholder = { Text("Search for classes...") },
                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", modifier = Modifier.clickable {
                        searchHandler()
                    }) },
                    onSearch = {
                        searchHandler()
                    }
                ) { /* do nothing */ }

                Row(Modifier.padding(horizontal = 32.dp, vertical = 16.dp)) {
                    LargeDropdownMenu(
                        modifier = Modifier
                            .weight(.5f)
                            .padding(end = 8.dp),
                        label = "Term",
                        items = termList,
                        selectedIndex = selectedTermIndex,
                        onItemSelected = { index, _ -> selectedTermIndex = index },
                    )
                    LargeDropdownMenuMultiSelect(
                        modifier = Modifier
                            .weight(.35f)
                            .padding(start = 8.dp),
                        label = "GE",
                        items = genEdList,
                        displayLabel = when (selectedGenEdList.size) {
                            0 -> ""
                            1 -> selectedGenEdList[0]
                            genEdList.size -> "All"
                            else -> "Multi"
                        },
                        selectedItems = selectedGenEdList,
                        onItemSelected = { index, _ ->
                            selectedGenEdList.add(genEdList[index])
                        },
                        onItemRemoved = { _, itemName ->
                            if (itemName in selectedGenEdList) {
                                selectedGenEdList.remove(itemName)
                            }
                        }
                    )
                }

                Row(Modifier.padding(horizontal = 32.dp, vertical = 4.dp)) {
                    LargeDropdownMenuMultiSelect(
                        modifier = Modifier
                            .weight(.5f)
                            .padding(end = 8.dp),
                        label = "Type",
                        items = typeList,
                        displayLabel = when (selectedTypeList.size) {
                            0 -> ""
                            // without cutting out "Online", async online is too long
                            1 -> selectedTypeList[0].replace(" Online", "")
                            typeList.size -> "All"
                            else -> "Multiple"
                        },
                        selectedItems = selectedTypeList,
                        onItemSelected = { index, _ ->
                            selectedTypeList.add(typeList[index])
                        },
                        onItemRemoved = { _, itemName ->
                            if (itemName in selectedTypeList) {
                                selectedTypeList.remove(itemName)
                            }
                        }
                    )

                    LargeDropdownMenu(
                        modifier = Modifier
                            .weight(.35f)
                            .padding(start = 8.dp),
                        label = "Status",
                        items = listOf("Open", "All"),
                        selectedIndex = selectedStatusIndex,
                        onItemSelected = { index, _ -> selectedStatusIndex = index },
                    )
                }
            }
        }
    }
}
