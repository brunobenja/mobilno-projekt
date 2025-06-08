package hr.ferit.brunobenja.musicplayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import hr.ferit.brunobenja.musicplayer.ui.theme.Gray2
import hr.ferit.brunobenja.musicplayer.ui.theme.Gray3
import hr.ferit.brunobenja.musicplayer.ui.theme.SecondaryButtonSize
import hr.ferit.brunobenja.musicplayer.ui.theme.White2
import java.net.URLDecoder

@Composable
fun MusicPlayerApp(vm: PlayerViewModel) {
    var hasPermission by remember { mutableStateOf(false) }

//    if (!hasPermission) {
//        RequestAudioPermission { hasPermission = true }
//    }

    if (hasPermission) {
        val navController = rememberNavController()
        LaunchedEffect(Unit) {
            vm.loadAudioFiles()
            vm.restoreLastPlayedOrFirst()
        }
        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                Column {
                    if (navBackStackEntry?.destination?.route != "playing") {
                        CurrentlyPlayingBar(vm = vm, navController = navController)
                    }
                    BottomNavigationBar(navController)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "playing",
                modifier = Modifier.padding(innerPadding)

            ) {
                composable("search") { SearchScreen(navController, vm) }
                composable("playing") { PlayingScreen(navController, vm) }
                composable("playlists") { PlaylistsScreen(navController, vm) }
                composable("queue") { QueueScreen(navController, vm) }
                composable(
                    "playlistDetails/{playlistName}",
                    arguments = listOf(navArgument("playlistName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val playlistName = backStackEntry.arguments?.getString("playlistName")?.let { URLDecoder.decode(it, "utf-8") }
                    val playlist = vm.playlists.find { it.name == playlistName }
                    if (playlist != null) {
                        PlaylistDetailsScreen(
                            navController = navController,
                            playlist = playlist,
                            vm = vm,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    } else {
        RequestAudioPermission { hasPermission = true }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    BottomNavigation(
        backgroundColor = Gray3,
        modifier = Modifier.navigationBarsPadding()
    ) {
        // SEARCH
        BottomNavigationItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.search),
                    "Prev",
                    Modifier
                        .size(SecondaryButtonSize),
                    tint = White2
                )
            },
            selected = currentRoute == "search",
            onClick = {
                if (currentRoute != "search") {
                    navController.navigate("search")
                }
            },
        )

        // PLAYING / QUEUE
        if (currentRoute == "playing") {
            BottomNavigationItem(
                icon = { Icon(
                    painter = painterResource(id = R.drawable.queue),
                    "Queue",
                    Modifier
                        .size(SecondaryButtonSize),
                    tint = White2
                ) },
                selected = false,
                onClick = {
                    navController.navigate("queue")
                }
            )
        } else {
            BottomNavigationItem(
                icon = { Icon(
                    painter = painterResource(id = R.drawable.play),
                    "Playing",
                    Modifier
                        .size(SecondaryButtonSize),
                    tint = White2
                ) },
                selected = currentRoute == "playing",
                onClick = {
                    if (currentRoute != "playing") {
                        navController.navigate("playing")
                    }
                }
            )
        }

        // PLAYLISTS
        BottomNavigationItem(
            icon = { Icon(
                painter = painterResource(id = R.drawable.playlist),
                "Playlists",
                Modifier
                    .size(SecondaryButtonSize),
                tint = White2
            ) },
            selected = currentRoute == "playlists",
            onClick = {
                if (currentRoute != "playlists") {
                    navController.navigate("playlists")
                }
            }
        )
    }
}