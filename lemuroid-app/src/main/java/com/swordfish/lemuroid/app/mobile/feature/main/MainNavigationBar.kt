package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/** Wide windows, such as an unfolded foldable in landscape, navigate from a collapsed rail. */
@Composable
fun MainNavigationRail(
    currentRoute: MainRoute?,
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = currentRoute?.showBottomNavigation != false,
        enter = expandHorizontally(),
        exit = shrinkHorizontally(),
        modifier = modifier,
    ) {
        WideNavigationRail(
            windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Start),
            colors = WideNavigationRailDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        ) {
            MainNavigationRoutes.values().forEach { destination ->
                val isSelected = currentRoute?.root == destination.route
                WideNavigationRailItem(
                    selected = isSelected,
                    onClick = { navController.navigateToDestination(destination) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = stringResource(destination.titleId),
                        )
                    },
                    label = { Text(stringResource(destination.titleId)) },
                    railExpanded = false,
                )
            }
        }
    }
}

@Composable
fun MainNavigationBar(
    currentRoute: MainRoute?,
    navController: NavHostController,
) {
    AnimatedVisibility(
        visible = currentRoute?.showBottomNavigation != false,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        LemuroidNavigationBar(currentRoute, navController)
    }
}

/**
 * Material 3 Expressive's short navigation bar. On medium and wider windows the items sit
 * side by side with their labels and gather in the centre, as the expressive spec asks.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LemuroidNavigationBar(
    currentRoute: MainRoute?,
    navController: NavHostController,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 600.dp
        ShortNavigationBar(
            modifier = Modifier.fillMaxWidth(),
            arrangement =
                if (wide) ShortNavigationBarArrangement.Centered else ShortNavigationBarArrangement.EqualWeight,
        ) {
            MainNavigationRoutes.values().forEach { destination ->
                val isSelected = currentRoute?.root == destination.route
                val iconDrawable = if (isSelected) destination.selectedIcon else destination.unselectedIcon

                ShortNavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = iconDrawable,
                            contentDescription = stringResource(destination.titleId),
                        )
                    },
                    label = { Text(stringResource(destination.titleId)) },
                    selected = isSelected,
                    iconPosition = if (wide) NavigationItemIconPosition.Start else NavigationItemIconPosition.Top,
                    onClick = { navController.navigateToDestination(destination) },
                )
            }
        }
    }
}

private fun NavHostController.navigateToDestination(destination: MainNavigationRoutes) {
    navigate(destination.route.route) {
        // Pop up to the start destination of the graph to
        // avoid building up a large stack of destinations
        // on the back stack as users select items
        popUpTo(graph.findStartDestination().id) {
            saveState = false
        }
        // Avoid multiple copies of the same destination when
        // reselecting the same item
        launchSingleTop = true
        // Restore state when reselecting a previously selected item
        restoreState = false
    }
}
