package com.echonow.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import echo_now.composeapp.generated.resources.Res
import echo_now.composeapp.generated.resources.echo_tv_mark
import echo_now.composeapp.generated.resources.echo_now_logo_text_only
import org.jetbrains.compose.resources.painterResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.echonow.di.appModule
import com.echonow.domain.model.MediaType
import com.echonow.domain.repository.ProfileRepository
import com.echonow.domain.repository.ProfileState
import com.echonow.data.remote.streaming.streamingModule
import com.echonow.presentation.screens.CatalogScreen
import com.echonow.presentation.screens.DetailsScreen
import com.echonow.presentation.screens.HomeScreen
import com.echonow.presentation.screens.PlayerScreen
import com.echonow.presentation.screens.SearchScreen
import com.echonow.presentation.screens.SettingsScreen
import com.echonow.presentation.screens.ProfileAvatar
import com.echonow.presentation.screens.ProfileSetupScreen
import com.echonow.presentation.screens.ProfileSwitcherDialog
import com.echonow.presentation.components.LocalEchoImageLoader
import com.echonow.presentation.components.BootAnimation
import com.echonow.theme.LocalButtonCustomization
import com.echonow.presentation.viewmodel.SettingsViewModel
import com.echonow.platform.configuredTmdbApiKey
import com.echonow.platform.configuredTmdbReadAccessToken
import com.echonow.platform.configuredSubdlApiKey
import com.echonow.platform.isAndroidTvPlatform
import com.echonow.platform.isDesktopPlatform
import com.echonow.platform.currentTimeMillis
import com.echonow.platform.formatConfiguredDateTime
import com.echonow.platform.observeIncomingPlayback
import com.echonow.platform.updateLocalDeviceName
import com.echonow.theme.EchoTheme
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel

private data class Destination(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: Any,
)

private val destinations = listOf(
    Destination("Home", Icons.Rounded.Home, HomeRoute),
    Destination("Movies", Icons.Rounded.Movie, MoviesRoute),
    Destination("Series", Icons.Rounded.Tv, SeriesRoute),
    Destination("Search", Icons.Rounded.Search, SearchRoute),
    Destination("Settings", Icons.Rounded.Settings, SettingsRoute),
)

@Composable
fun EchoNowApp() {
    KoinApplication(application = {
        modules(
            appModule(
                tmdbApiKey = configuredTmdbApiKey(),
                tmdbReadAccessToken = configuredTmdbReadAccessToken(),
                subdlApiKey = configuredSubdlApiKey(),
            ),
            streamingModule,
        )
    }) {
        val profiles: ProfileRepository = org.koin.compose.koinInject()
        val profileState by profiles.observe().collectAsState(initial = ProfileState())
        val settings: SettingsViewModel = koinViewModel()
        val preferences by settings.state.collectAsState()
        LaunchedEffect(preferences.deviceName) { updateLocalDeviceName(preferences.deviceName) }
        val platformContext = LocalPlatformContext.current
        val imageLoader = remember(platformContext) {
            ImageLoader.Builder(platformContext)
                .components { add(KtorNetworkFetcherFactory()) }
                .build()
        }
        CompositionLocalProvider(LocalEchoImageLoader provides imageLoader) {
            EchoTheme(preferences) {
                var booting by rememberSaveable { mutableStateOf(true) }
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                    if (profileState.isLoading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            Modifier.align(androidx.compose.ui.Alignment.Center),
                        )
                    } else if (profileState.activeProfile == null) {
                        ProfileSetupScreen()
                    } else {
                        EchoNowNavigation()
                    }
                    if (booting) {
                        BootAnimation(onFinished = { booting = false })
                    }
                }
            }
            }
        }
    }

@Composable
private fun EchoNowNavigation() {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val availableHeight = maxHeight
        val navController = rememberNavController()
        LaunchedEffect(navController) {
            observeIncomingPlayback().collect { request ->
                navController.navigate(
                    PlayerRoute(
                        id = request.mediaId,
                        season = request.season,
                        episode = request.episode,
                    ),
                ) {
                    launchSingleTop = true
                }
            }
        }
        val entry by navController.currentBackStackEntryAsState()
        val destination = entry?.destination
        val settings: SettingsViewModel = koinViewModel()
        val preferences by settings.state.collectAsState()
        val profileRepository: ProfileRepository = org.koin.compose.koinInject()
        val profileState by profileRepository.observe().collectAsState(initial = ProfileState())
        val activeProfile = profileState.activeProfile
        var showProfiles by remember { mutableStateOf(false) }
        val buttonCustomization = LocalButtonCustomization.current
        val selectedNavigationColor = buttonCustomization.fillColor ?: MaterialTheme.colorScheme.primary
        val selectedNavigationContentColor = buttonCustomization.textColor ?: MaterialTheme.colorScheme.onPrimary
        val navigationFocusColor = buttonCustomization.borderColor ?: MaterialTheme.colorScheme.tertiary
        val navigationSurface = Color(0xFF0F0F0F)
        val navigationItemSurface = Color(0xFF1B1B22)
        val selectedNavigationSurface = buttonCustomization.fillColor?.copy(alpha = .28f)
            ?: selectedNavigationColor.copy(alpha = .22f)
        val landscape = maxWidth > maxHeight
        val wide = when (preferences.interfaceMode) {
            "PHONE" -> false
            "TV" -> true
            else -> landscape || isAndroidTvPlatform() || (isDesktopPlatform() && maxWidth >= 840.dp)
        }
        var navigationVisible by remember(wide) { mutableStateOf(true) }
        var navigationFocused by remember { mutableStateOf(false) }
        var navigationHadFocus by remember { mutableStateOf(false) }
        var requestNavigationFocus by remember { mutableStateOf(false) }
        val navigationFocusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        val selectedRoute = when {
            destination?.hasRoute<HomeRoute>() == true -> HomeRoute
            destination?.hasRoute<MoviesRoute>() == true -> MoviesRoute
            destination?.hasRoute<SeriesRoute>() == true -> SeriesRoute
            destination?.hasRoute<SearchRoute>() == true -> SearchRoute
            else -> SettingsRoute
        }
        val current = destinations.firstOrNull { it.route == selectedRoute } ?: destinations.first()
        val isPlayer = destination?.hasRoute<PlayerRoute>() == true
        val showClock = !isPlayer && if (destination?.hasRoute<HomeRoute>() == true) {
            preferences.showClockOnHome
        } else {
            preferences.showClockOnOtherPages
        }
        var clockEpoch by remember { mutableLongStateOf(currentTimeMillis()) }
        LaunchedEffect(showClock, preferences.clockMode) {
            while (showClock && preferences.clockMode != "MANUAL") {
                clockEpoch = currentTimeMillis()
                kotlinx.coroutines.delay(1_000)
            }
        }
        val showNavigation = navigationVisible &&
            destination?.hasRoute<DetailsRoute>() != true && destination?.hasRoute<PlayerRoute>() != true
        LaunchedEffect(navigationVisible, showNavigation, navigationFocused, navigationHadFocus) {
            if (navigationVisible && showNavigation && navigationHadFocus && !navigationFocused) {
                kotlinx.coroutines.delay(120)
                if (!navigationFocused) navigationVisible = false
            }
        }
        LaunchedEffect(requestNavigationFocus, showNavigation) {
            if (requestNavigationFocus && showNavigation) {
                navigationFocusRequester.requestFocus()
                requestNavigationFocus = false
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    !navigationVisible &&
                    ((wide && event.key == Key.DirectionLeft) || (!wide && event.key == Key.DirectionDown))
                ) {
                    val direction = if (wide) FocusDirection.Left else FocusDirection.Down
                    if (!focusManager.moveFocus(direction)) {
                        navigationVisible = true
                        requestNavigationFocus = true
                    }
                    true
                } else false
            }.pointerInput(wide) {
                if (wide) {
                    var revealFromEdge = false
                    detectHorizontalDragGestures(
                        onDragStart = { revealFromEdge = it.x < 48.dp.toPx() },
                        onHorizontalDrag = { change, dragAmount ->
                            if (revealFromEdge && dragAmount > 0f) {
                                navigationVisible = true
                                change.consume()
                            }
                        },
                    )
                } else {
                    var revealFromEdge = false
                    var upwardDistance = 0f
                    detectVerticalDragGestures(
                        onDragStart = {
                            revealFromEdge = it.y >= size.height - 96.dp.toPx()
                            upwardDistance = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (revealFromEdge && dragAmount < 0f) {
                                upwardDistance += -dragAmount
                                if (upwardDistance >= 40.dp.toPx()) navigationVisible = true
                                change.consume()
                            }
                        },
                    )
                }
            }.pointerInput(wide, showNavigation, preferences.navigationBarCollapsed) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val start = down.position
                    var distance = 0f
                    var finished = false
                    while (!finished) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.pressed) {
                            distance = maxOf(distance, (change.position - start).getDistance())
                        } else {
                            finished = true
                        }
                    }
                    if (showNavigation && distance < viewConfiguration.touchSlop) {
                        val withinNavigation = if (wide) {
                            start.x <= (if (preferences.navigationBarCollapsed) 84.dp else 256.dp).toPx()
                        } else {
                            start.y >= size.height - 96.dp.toPx()
                        }
                        if (!withinNavigation) navigationVisible = false
                    }
                }
            },
            bottomBar = {
                if (showNavigation && !wide) {
                    NavigationBar(
                        modifier = Modifier.border(1.dp, Color.White.copy(alpha = .06f)),
                        containerColor = navigationSurface,
                        tonalElevation = 0.dp,
                    ) {
                        NavigationBarItem(
                            modifier = Modifier.weight(1f),
                            selected = false,
                            onClick = {},
                            enabled = false,
                            icon = {
                                Image(
                                    painter = painterResource(Res.drawable.echo_tv_mark),
                                    contentDescription = "ECHO TV",
                                    contentScale = ContentScale.Fit,
                                    colorFilter = ColorFilter.tint(Color.White),
                                    modifier = Modifier.size(26.dp),
                                )
                            },
                            label = null,
                        )
                        destinations.forEach { item ->
                            var itemFocused by remember(item) { mutableStateOf(false) }
                            NavigationBarItem(
                                modifier = Modifier.weight(1f)
                                    .border(
                                        if (itemFocused) 2.dp else 0.dp,
                                        if (itemFocused) navigationFocusColor else Color.Transparent,
                                        buttonCustomization.shape,
                                    )
                                    .then(if (item == current) Modifier.focusRequester(navigationFocusRequester) else Modifier)
                                    .onFocusChanged {
                                        itemFocused = it.isFocused
                                        navigationFocused = it.isFocused
                                        if (it.isFocused) navigationHadFocus = true
                                    },
                                selected = current == item,
                                onClick = { navController.navigateTop(item.route) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = if (preferences.showMenuLabels) ({ Text(item.label) }) else null,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = selectedNavigationContentColor,
                                    selectedTextColor = selectedNavigationContentColor,
                                    indicatorColor = selectedNavigationColor,
                                    unselectedIconColor = Color.White.copy(alpha = .68f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                        var profileItemFocused by remember { mutableStateOf(false) }
                        NavigationBarItem(
                            modifier = Modifier.weight(1f)
                                .border(
                                    if (profileItemFocused) 2.dp else 0.dp,
                                    if (profileItemFocused) navigationFocusColor else Color.Transparent,
                                    buttonCustomization.shape,
                                )
                                .onFocusChanged {
                                    profileItemFocused = it.isFocused
                                    navigationFocused = it.isFocused
                                    if (it.isFocused) navigationHadFocus = true
                                },
                            selected = showProfiles,
                            onClick = { showProfiles = true },
                            icon = {
                                if (activeProfile != null) {
                                    ProfileAvatar(activeProfile.avatarId, selected = showProfiles)
                                } else {
                                    Icon(Icons.Rounded.Person, contentDescription = "Profile")
                                }
                            },
                            label = if (preferences.showMenuLabels) {
                                ({ Text(activeProfile?.nickname ?: "Profile", maxLines = 1) })
                            } else null,
                        )
                    }
                }
            },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(if (wide) androidx.compose.foundation.layout.PaddingValues() else padding)) {
                if (showNavigation && wide) {
                    val compactNavigation = preferences.navigationBarCollapsed
                    Column(
                        modifier = Modifier.width(if (compactNavigation) 84.dp else 256.dp).fillMaxHeight()
                            .background(navigationSurface)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = .06f),
                            ),
                    ) {
                        val compactMenu = availableHeight < 520.dp
                        val menuItemCount = 2 + destinations.count { it.route != SearchRoute }
                        val menuItemHeight = ((availableHeight - if (compactMenu) 66.dp else 76.dp) / menuItemCount)
                            .coerceIn(if (compactMenu) 34.dp else 40.dp, 56.dp)
                        val menuItemVerticalPadding = if (compactMenu) 4.dp else 6.dp
                        Row(
                            modifier = Modifier.fillMaxWidth().height(if (compactMenu) 58.dp else 68.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = if (compactNavigation) Arrangement.Center else Arrangement.Start,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.echo_tv_mark),
                                contentDescription = "ECHO TV",
                                contentScale = ContentScale.Fit,
                                colorFilter = ColorFilter.tint(Color.White),
                                modifier = Modifier.size(34.dp),
                            )
                            if (!compactNavigation) {
                                Image(
                                    painter = painterResource(Res.drawable.echo_now_logo_text_only),
                                    contentDescription = "ECHO NOW",
                                    contentScale = ContentScale.Fit,
                                    colorFilter = ColorFilter.colorMatrix(
                                        ColorMatrix().apply { setToSaturation(0f) },
                                    ),
                                    modifier = Modifier.padding(start = 4.dp).width(80.dp).height(34.dp),
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(menuItemHeight)
                                .background(
                                        if (current.route == SearchRoute) selectedNavigationSurface
                                        else navigationItemSurface,
                                        buttonCustomization.shape,
                                    )
                                    .border(
                                        if (current.route == SearchRoute) 1.dp else 0.dp,
                                        if (current.route == SearchRoute) navigationFocusColor.copy(alpha = .7f)
                                        else Color.Transparent,
                                        buttonCustomization.shape,
                                    )
                                    .then(
                                        if (current.route == SearchRoute) Modifier.focusRequester(navigationFocusRequester)
                                        else Modifier
                                    )
                                    .clickable { navController.navigateTop(SearchRoute) }
                                    .focusable()
                                    .onFocusChanged {
                                        navigationFocused = it.isFocused
                                        if (it.isFocused) navigationHadFocus = true
                                    },
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = if (compactNavigation) Arrangement.Center else Arrangement.Start,
                        ) {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = "Search",
                                    tint = if (current.route == SearchRoute) selectedNavigationContentColor
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (!compactNavigation) {
                                    Text(
                                        "Search",
                                        modifier = Modifier.padding(start = 12.dp),
                                        color = if (current.route == SearchRoute) selectedNavigationContentColor
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                        }
                            destinations.filter { it.route != SearchRoute }.forEach { item ->
                                var itemFocused by remember(item) { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(menuItemHeight)
                                        .background(
                                            if (current == item) selectedNavigationSurface
                                            else Color.Transparent,
                                            buttonCustomization.shape,
                                        )
                                        .border(
                                            if (itemFocused) 2.dp else 0.dp,
                                            if (itemFocused) navigationFocusColor else Color.Transparent,
                                            buttonCustomization.shape,
                                        )
                                        .then(
                                            if (item == current) Modifier.focusRequester(navigationFocusRequester)
                                            else Modifier
                                        )
                                        .clickable { navController.navigateTop(item.route) }
                                        .focusable()
                                        .onFocusChanged {
                                            itemFocused = it.isFocused
                                            navigationFocused = it.isFocused
                                            if (it.isFocused) navigationHadFocus = true
                                        }
                                        .padding(
                                            horizontal = if (compactNavigation) 0.dp else 14.dp,
                                            vertical = menuItemVerticalPadding,
                                        ),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    horizontalArrangement = if (compactNavigation) Arrangement.Center else Arrangement.Start,
                                ) {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.label,
                                        tint = if (current == item) selectedNavigationContentColor
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (!compactNavigation) {
                                        Text(
                                            item.label,
                                            modifier = Modifier.padding(start = 14.dp),
                                            color = if (current == item) selectedNavigationContentColor
                                            else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    }
                                }
                            }
                            var profileItemFocused by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier.fillMaxWidth().height(menuItemHeight)
                                    .clickable { showProfiles = true }
                                    .focusable()
                                    .onFocusChanged {
                                        profileItemFocused = it.isFocused
                                        navigationFocused = it.isFocused
                                        if (it.isFocused) navigationHadFocus = true
                                    }
                                    .padding(
                                        horizontal = if (compactNavigation) 0.dp else 14.dp,
                                        vertical = menuItemVerticalPadding,
                                    ),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = if (compactNavigation) Arrangement.Center else Arrangement.Start,
                            ) {
                                if (activeProfile != null) {
                                    ProfileAvatar(
                                        activeProfile.avatarId,
                                        selected = profileItemFocused || showProfiles,
                                    )
                                } else {
                                    androidx.compose.foundation.layout.Box(
                                        Modifier.size(menuItemHeight - 4.dp)
                                            .background(selectedNavigationSurface, CircleShape)
                                            .border(
                                                if (profileItemFocused || showProfiles) 2.dp else 1.dp,
                                                if (profileItemFocused || showProfiles) navigationFocusColor
                                                else Color.White.copy(alpha = .3f),
                                                CircleShape,
                                            ),
                                        contentAlignment = androidx.compose.ui.Alignment.Center,
                                    ) {
                                        Icon(Icons.Rounded.Person, contentDescription = "Profile")
                                    }
                                }
                                if (!compactNavigation) {
                                    Text(
                                        activeProfile?.nickname ?: "Profile",
                                        modifier = Modifier.padding(start = 14.dp),
                                        color = if (showProfiles) selectedNavigationContentColor
                                        else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF0D0C0C), Color(0xFF02070A))))) {
                if (showProfiles) ProfileSwitcherDialog(onDismiss = { showProfiles = false })
                NavHost(navController = navController, startDestination = HomeRoute) {
                    composable<HomeRoute> {
                        HomeScreen(
                            onOpen = { navController.navigate(DetailsRoute(it.id)) },
                            onPlay = { navController.navigate(PlayerRoute(it.id)) },
                            onResume = { progress ->
                                navController.navigate(
                                    PlayerRoute(
                                        progress.mediaId,
                                        progress.season,
                                        progress.episode,
                                        qualityIndex = 0,
                                        resumePositionMillis = progress.positionMillis,
                                    ),
                                )
                            },
                        )
                    }
                    composable<MoviesRoute> {
                        CatalogScreen(MediaType.MOVIE) { navController.navigate(DetailsRoute(it.id)) }
                    }
                    composable<SeriesRoute> {
                        CatalogScreen(MediaType.SERIES) { navController.navigate(DetailsRoute(it.id)) }
                    }
                    composable<SearchRoute> {
                        SearchScreen { navController.navigate(DetailsRoute(it.id)) }
                    }
                    composable<SettingsRoute> { SettingsScreen() }
                    composable<DetailsRoute> { backStack ->
                        val route = backStack.toRoute<DetailsRoute>()
                        DetailsScreen(
                            id = route.id,
                            onBack = { navController.popBackStack() },
                            onPlay = { season, episode, quality, resumePositionMillis ->
                                navController.navigate(
                                    PlayerRoute(
                                        route.id,
                                        season,
                                        episode,
                                        quality,
                                        resumePositionMillis,
                                    ),
                                )
                            },
                        )
                    }
                    composable<PlayerRoute> { backStack ->
                        val route = backStack.toRoute<PlayerRoute>()
                        PlayerScreen(
                            route.id,
                            route.season,
                            route.episode,
                            route.qualityIndex,
                            route.resumePositionMillis,
                            onNextEpisode = { nextSeason, nextEpisode ->
                                navController.popBackStack()
                                navController.navigate(
                                    PlayerRoute(
                                        route.id,
                                        nextSeason,
                                        nextEpisode,
                                        qualityIndex = 0,
                                    ),
                                )
                            },
                        ) {
                            val detailsRoute = DetailsRoute(route.id)
                            if (!navController.popBackStack(detailsRoute, inclusive = false)) {
                                navController.popBackStack()
                                navController.navigate(detailsRoute) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }
                if (showClock) {
                    Text(
                        text = formatConfiguredDateTime(
                            epochMillis = clockEpoch,
                            timeZoneId = preferences.timeZoneId,
                            languageTag = preferences.languageTag,
                            pattern = preferences.clockPattern,
                            manualClockValue = if (preferences.clockMode == "MANUAL") {
                                preferences.manualClockValue
                            } else {
                                ""
                            },
                        ),
                        modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd)
                            .padding(top = 12.dp, end = 16.dp)
                            .background(
                                Color.Black.copy(alpha = 0.48f),
                                RoundedCornerShape(10.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        color = Color(preferences.clockColor),
                        fontSize = preferences.clockSizeSp.coerceIn(10, 28).sp,
                        maxLines = 1,
                        )
                }
            }
            }
        }
    }
}

private fun androidx.navigation.NavHostController.navigateTop(route: Any) {
    when (route) {
        HomeRoute -> navigate(HomeRoute) { popUpTo(graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
        MoviesRoute -> navigate(MoviesRoute) { popUpTo(graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
        SeriesRoute -> navigate(SeriesRoute) { popUpTo(graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
        SearchRoute -> navigate(SearchRoute) { popUpTo(graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
        else -> navigate(SettingsRoute) { popUpTo(graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
    }
}
