package com.shadman.emojiarena

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shadman.emojiarena.game.GameScreen
import com.shadman.emojiarena.ui.ChatScreen
import com.shadman.emojiarena.ui.HomeScreen
import com.shadman.emojiarena.ui.PlaceholderScreen
import com.shadman.emojiarena.ui.theme.EmojiArenaTheme

private object Routes {
    const val HOME_TAB = "home_tab"
    const val CHATS_TAB = "chats_tab"
    const val CALLS_TAB = "calls_tab"
    const val CONTACTS_TAB = "contacts_tab"
    const val MARKETPLACE_TAB = "marketplace_tab"
    const val SETTINGS_TAB = "settings_tab"
    const val CHAT = "chat/{contactId}"
    // The emoji arrives URL-encoded — a raw emoji can contain characters
    // (ZWJ, variation selectors) that have no business sitting unescaped
    // in a route path. Decoded back on the way out, in the composable below.
    // contactId is the thread the game was launched from — "Send score"
    // needs to know where to post the card back to.
    const val GAME = "game/{emoji}/{contactId}"
    fun chat(contactId: String) = "chat/$contactId"
    fun game(emoji: String, contactId: String) = "game/${Uri.encode(emoji)}/$contactId"
}

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

// The app's 6 top-level destinations. Chats is the only one with real
// content (the contact list built in earlier sections); the rest are
// PlaceholderScreen so the app reads as a full product shell.
private val bottomTabs = listOf(
    BottomTab(Routes.HOME_TAB, "Home", Icons.Filled.Home),
    BottomTab(Routes.CHATS_TAB, "Chats", Icons.AutoMirrored.Filled.Chat),
    BottomTab(Routes.CALLS_TAB, "Calls", Icons.Filled.Call),
    BottomTab(Routes.CONTACTS_TAB, "Contacts", Icons.Filled.Contacts),
    BottomTab(Routes.MARKETPLACE_TAB, "Marketplace", Icons.Filled.Storefront),
    BottomTab(Routes.SETTINGS_TAB, "Settings", Icons.Filled.Settings)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EmojiArenaTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                // The bottom bar belongs to the 6 top-level tabs, not to a
                // contact's chat or the full-screen game — hidden there,
                // same as any real messenger.
                val showBottomBar = currentRoute != Routes.CHAT && currentRoute != Routes.GAME

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                bottomTabs.forEach { tab ->
                                    NavigationBarItem(
                                        selected = currentRoute == tab.route,
                                        onClick = {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(tab.icon, contentDescription = tab.label) }
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = Routes.CHATS_TAB,
                        modifier = Modifier.padding(padding)
                    ) {
                        composable(Routes.HOME_TAB) {
                            PlaceholderScreen(title = "Home", icon = Icons.Filled.Home)
                        }
                        composable(Routes.CHATS_TAB) {
                            // The bottom NavigationBar above already accounts
                            // for the system nav bar inset; without this,
                            // HomeScreen's own .navigationBarsPadding() (kept
                            // as-is, since Chats is wired in unchanged) would
                            // pad for it a second time and leave a dead gap
                            // above the tab bar.
                            Box(modifier = Modifier.consumeWindowInsets(WindowInsets.navigationBars)) {
                                HomeScreen(
                                    onContactClick = { contactId ->
                                        navController.navigate(Routes.chat(contactId))
                                    }
                                )
                            }
                        }
                        composable(Routes.CALLS_TAB) {
                            PlaceholderScreen(title = "Calls", icon = Icons.Filled.Call)
                        }
                        composable(Routes.CONTACTS_TAB) {
                            PlaceholderScreen(title = "Contacts", icon = Icons.Filled.Contacts)
                        }
                        composable(Routes.MARKETPLACE_TAB) {
                            PlaceholderScreen(title = "Marketplace", icon = Icons.Filled.Storefront)
                        }
                        composable(Routes.SETTINGS_TAB) {
                            PlaceholderScreen(title = "Settings", icon = Icons.Filled.Settings)
                        }
                        composable(
                            Routes.GAME,
                            arguments = listOf(
                                navArgument("emoji") { type = NavType.StringType },
                                navArgument("contactId") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val emoji = entry.arguments?.getString("emoji")
                                ?.let(Uri::decode)
                                ?.takeIf(String::isNotBlank)
                                ?: Constants.DEFAULT_BALL_EMOJI
                            val originContactId = entry.arguments?.getString("contactId").orEmpty()
                            GameScreen(ballEmoji = emoji, originContactId = originContactId)
                        }
                        composable(Routes.CHAT) { entry ->
                            val contactId = entry.arguments?.getString("contactId").orEmpty()
                            ChatScreen(
                                contactId = contactId,
                                onBack = { navController.popBackStack() },
                                onPlayEmoji = { emoji -> navController.navigate(Routes.game(emoji, contactId)) }
                            )
                        }
                    }
                }
            }
        }
    }
}
