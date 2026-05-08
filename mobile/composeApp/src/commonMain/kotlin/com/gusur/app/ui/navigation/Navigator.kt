package com.gusur.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList

class Navigator internal constructor(initial: Screen) {
    private val stack: SnapshotStateList<Screen> = mutableStateListOf(initial)

    val current: Screen get() = stack.last()
    val canPop: Boolean get() = stack.size > 1

    fun push(screen: Screen) {
        if (stack.lastOrNull() != screen) stack.add(screen)
    }

    fun pop() {
        if (canPop) stack.removeAt(stack.lastIndex)
    }

    fun selectTab(tab: Screen) {
        stack.clear()
        stack.add(tab)
    }
}

@Composable
fun rememberNavigator(initial: Screen = Screen.Home): Navigator =
    remember { Navigator(initial) }

val LocalNavigator = compositionLocalOf<Navigator> { error("Navigator not provided") }
