package com.hayoonjae.sciencetoday

import android.content.Context
import android.content.Intent
import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

class MainActivity : ComponentActivity() {
    /** 위젯을 눌러 들어오면 그 상식을 바로 연다. */
    private val openFact = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AColor.WHITE, AColor.WHITE),
        )
        openFact.value = intent.getStringExtra(EXTRA_FACT)
        Widgets.updateAll(this)
        val lib = Library.get(this)
        val progress = Progress(this)
        val ref = RefData.get(this)
        setContent { AppTheme { App(lib, ref, progress, openFact) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_FACT)?.let { openFact.value = it }
    }

    companion object {
        const val EXTRA_FACT = "fact"
    }
}

private var cached: Library? = null

fun Library.Companion.get(ctx: Context): Library =
    cached ?: parse(ctx.assets.open("facts.json").bufferedReader().use { it.readText() }).also { cached = it }

private var cachedRef: RefData? = null

fun RefData.Companion.get(ctx: Context): RefData = cachedRef ?: parse(
    ctx.assets.open("elements.json").bufferedReader().use { it.readText() },
    ctx.assets.open("tables.json").bufferedReader().use { it.readText() },
).also { cachedRef = it }

enum class QuizMode { DAILY, FACT, FIELD, RANDOM, WRONG }

sealed interface Route {
    data class FactR(val id: String) : Route
    data class TermR(val id: String) : Route
    class QuizR(val session: QuizSession) : Route
    data object PeriodicR : Route
    data class ElementR(val z: Int) : Route
    data class TableR(val id: String) : Route
}

class Nav(private val stack: SnapshotStateList<Route>) {
    fun fact(id: String) { stack.add(Route.FactR(id)) }
    fun term(id: String) { stack.add(Route.TermR(id)) }
    fun quiz(items: List<QItem>, mode: QuizMode, day: Long = Today.day()) {
        if (items.isNotEmpty()) stack.add(Route.QuizR(QuizSession(items, mode, day)))
    }
    fun back() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    fun periodic() { stack.add(Route.PeriodicR) }
    fun element(z: Int) { stack.add(Route.ElementR(z)) }
    /** 원소 화면에서 이전/다음으로 넘길 때는 뒤로 가기 기록을 쌓지 않는다. */
    fun replaceElement(z: Int) { if (stack.isNotEmpty()) stack[stack.lastIndex] = Route.ElementR(z) }
    fun table(id: String) { stack.add(Route.TableR(id)) }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("오늘", Icons.Filled.Home),
    QUIZ("퀴즈", Icons.Filled.CheckCircle),
    DICT("사전", Icons.Filled.Search),
    REF("자료", Icons.Filled.Info),
    RECORD("기록", Icons.Filled.List),
}

@Composable
fun App(lib: Library, ref: RefData, progress: Progress, openFact: MutableState<String?>) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val stack = remember { mutableStateListOf<Route>() }
    val nav = remember { Nav(stack) }

    LaunchedEffect(openFact.value) {
        val id = openFact.value ?: return@LaunchedEffect
        if (id in lib.factById) {
            stack.clear()
            stack.add(Route.FactR(id))
        }
        openFact.value = null
    }
    BackHandler(stack.isNotEmpty()) { nav.back() }

    Box(Modifier.fillMaxSize().background(Palette.bg)) {
        when (val top = stack.lastOrNull()) {
            is Route.FactR -> lib.factById[top.id]?.let { FactScreen(it, lib, nav) }
            is Route.TermR -> lib.termById[top.id]?.let { TermScreen(it, lib, progress, nav) }
            is Route.QuizR -> QuizScreen(top.session, progress, nav)
            Route.PeriodicR -> PeriodicScreen(ref, nav)
            is Route.ElementR -> ref.byZ[top.z]?.let { ElementScreen(it, ref, nav) }
            is Route.TableR -> ref.tableById[top.id]?.let { TableScreen(it, nav) }
            null -> Scaffold(
                containerColor = Palette.bg,
                bottomBar = {
                    NavigationBar(containerColor = Palette.card) {
                        Tab.entries.forEachIndexed { i, t ->
                            NavigationBarItem(
                                selected = tab == i,
                                onClick = { tab = i },
                                icon = { Icon(t.icon, contentDescription = null) },
                                label = { Text(t.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Palette.purple,
                                    selectedTextColor = Palette.purple,
                                    indicatorColor = Palette.purple.copy(alpha = 0.12f),
                                    unselectedIconColor = Palette.dim,
                                    unselectedTextColor = Palette.dim,
                                ),
                            )
                        }
                    }
                },
            ) { pad ->
                Box(Modifier.padding(pad)) {
                    when (Tab.entries[tab]) {
                        Tab.TODAY -> TodayScreen(lib, progress, nav)
                        Tab.QUIZ -> QuizHomeScreen(lib, progress, nav)
                        Tab.DICT -> DictScreen(lib, progress, nav)
                        Tab.REF -> RefHomeScreen(ref, nav)
                        Tab.RECORD -> RecordScreen(lib, progress, nav)
                    }
                }
            }
        }
    }
}
