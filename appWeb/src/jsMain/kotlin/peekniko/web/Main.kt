package peekniko.web

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import peekniko.AppModule
import peekniko.Peekniko
import peekniko.SampleApp
import peekniko.SampleAppTheme
import peekniko.SampleViewModel
import peekniko.initialize
import peekniko.ui.Screen

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    AppModule.initialize()
    val sampleViewModel = SampleViewModel()
    ComposeViewport {
        SampleAppTheme {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "sample",
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween())
                },
                exitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween())
                },
                popEnterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween())
                },
                popExitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween())
                },
            ) {
                composable("sample") {
                    SampleApp(
                        viewModel = sampleViewModel,
                        onOpenPeekniko = { navController.navigate("calls") },
                    )
                }
                composable("calls") {
                    Peekniko.Screen(
                        modifier = Modifier.fillMaxSize(),
                        onClose = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
