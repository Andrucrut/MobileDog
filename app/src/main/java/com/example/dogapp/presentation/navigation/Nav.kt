package com.example.dogapp.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dogapp.di.AppContainer
import com.example.dogapp.presentation.screens.*
import com.example.dogapp.presentation.viewmodel.MainViewModel
import com.example.dogapp.ui.theme.DogAppTheme

private data class BottomTab(val route: String, val title: String)

@Composable
fun DogAppRoot() {
    val context = LocalContext.current
    val vm: MainViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(AppContainer.repository(context)) as T
        }
    })

    val state by vm.state.collectAsState()
    val navController = rememberNavController()
    val isWalker = state.user?.role?.key.equals("walker", ignoreCase = true)
    val tabs = remember(isWalker) {
        if (isWalker) {
            listOf(
                BottomTab("map", "Карта"),
                BottomTab("bookings", "Заявки"),
                BottomTab("profile", "Профиль"),
            )
        } else {
            listOf(
                BottomTab("map", "Карта"),
                BottomTab("bookings", "Заявки"),
                BottomTab("dogs", "Питомцы"),
                BottomTab("profile", "Профиль"),
            )
        }
    }

    DogAppTheme(darkTheme = state.darkThemeEnabled, dynamicColor = false) {
        if (!state.startupChecked || !state.serverReady) {
            StartupScreen(
                loading = state.loading,
                error = state.error,
                onRetry = vm::retryStartup,
            )
            return@DogAppTheme
        }
        if (!state.authorized) {
            AuthScreen(
                loading = state.loading,
                error = state.error,
                onLogin = vm::login,
                onRegister = vm::register,
            )
            return@DogAppTheme
        }

        val isOwnerRole = state.user?.role?.key.equals("owner", ignoreCase = true)
        val pendingOwnerPayment = if (isOwnerRole) {
            state.ownerBookings.filter { it.status.equals("AWAITING_OWNER_PAYMENT", ignoreCase = true) }
        } else {
            emptyList()
        }

        Box {
        Scaffold(
            topBar = {
                if (pendingOwnerPayment.isNotEmpty()) {
                    val payBookingId = pendingOwnerPayment.first().id
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        tonalElevation = 3.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Подтвердите прогулку и оплатите заказ",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = {
                                    navController.navigate("booking/$payBookingId") {
                                        launchSingleTop = true
                                    }
                                },
                            ) { Text("К заказу") }
                        }
                    }
                }
            },
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                NavigationBar {
                    tabs.forEach { tab ->
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
                            label = { Text(tab.title) },
                            icon = {}
                        )
                    }
                }
            }
        ) { innerPadding ->
            val navBackStackEntryForMap by navController.currentBackStackEntryAsState()
            val routeForRefresh = navBackStackEntryForMap?.destination?.route
            LaunchedEffect(routeForRefresh) {
                if (routeForRefresh == "map") {
                    vm.loadAll()
                }
            }
            NavHost(navController = navController, startDestination = "map", modifier = Modifier.padding(innerPadding)) {
                composable("map") {
                    MapScreen(
                        state = state,
                        onOpenBookingDetail = { id -> navController.navigate("booking/$id") },
                    )
                }
                composable("booking/{id}") { entry ->
                    val bookingId = entry.arguments?.getString("id") ?: return@composable
                    val booking = (state.ownerBookings + state.walkerBookings).firstOrNull { it.id == bookingId }
                    val dogName = booking?.let { b -> state.dogs.firstOrNull { it.id == b.dog_id }?.name }
                    val isWalker = state.user?.role?.key.equals("walker", ignoreCase = true)
                    val isOwner = state.user?.role?.key.equals("owner", ignoreCase = true)
                    val walkReady = booking?.status?.uppercase() in setOf("CONFIRMED", "IN_PROGRESS")
                    val conversation = state.conversations.firstOrNull { it.booking_id == bookingId }
                    LaunchedEffect(bookingId, isOwner, isWalker) {
                        if (isOwner || isWalker) vm.loadApplications(bookingId)
                    }
                    LaunchedEffect(bookingId, booking?.status) {
                        val shouldLoadRoute = booking?.status?.uppercase() in setOf(
                            "CONFIRMED",
                            "IN_PROGRESS",
                            "AWAITING_OWNER_PAYMENT",
                            "COMPLETED",
                        )
                        if (shouldLoadRoute) {
                            val quietRoute = booking?.status?.uppercase() == "AWAITING_OWNER_PAYMENT"
                            vm.loadRouteByBooking(bookingId, withGlobalLoading = !quietRoute)
                        }
                    }
                    BookingDetailScreen(
                        booking = booking,
                        dogName = dogName,
                        route = state.routeByBooking[bookingId],
                        applications = state.applicationsByBooking[bookingId].orEmpty(),
                        currentUserId = state.user?.id,
                        isWalker = isWalker,
                        isOwner = isOwner,
                        hasConversation = conversation != null,
                        canOpenWalk = isWalker && walkReady,
                        feedbackText = state.error ?: state.notice,
                        feedbackIsError = state.error != null,
                        onBack = { navController.popBackStack() },
                        onRefreshRoute = {
                            vm.loadRouteByBooking(
                                bookingId,
                                withGlobalLoading = booking?.status?.uppercase() != "AWAITING_OWNER_PAYMENT",
                            )
                        },
                        onOpenWalk = { navController.navigate("walk/$bookingId") },
                        onWalkerConfirmBooking = { vm.walkerConfirmAssignedBooking(bookingId) },
                        onRefreshApplications = { vm.loadApplications(bookingId) },
                        onSubmitApplication = { msg -> vm.submitApplication(bookingId, msg) },
                        onWithdrawApplication = { appId -> vm.withdrawApplication(bookingId, appId) },
                        onOpenApplication = { appId ->
                            navController.navigate("booking/$bookingId/application/$appId")
                        },
                        onOpenChat = {
                            val c = state.conversations.firstOrNull { it.booking_id == bookingId }
                            if (c != null) {
                                navController.navigate("chat/${c.id}")
                            } else {
                                vm.refreshConversations()
                            }
                        },
                        wallet = state.wallet,
                        paymentLoading = state.loading,
                        ownerPaymentError = state.error,
                        onOwnerTopUp = vm::topUpWallet,
                        onOwnerSettle = { vm.ownerSettleBooking(bookingId) },
                        onOwnerRefreshForPayment = vm::loadAll,
                        onWalkerPushToOwnerPayment = { vm.walkerPushPaymentToOwner(bookingId) },
                    )
                }
                composable("bookings") {
                    BookingsHubScreen(
                        state = state,
                        onRefresh = vm::loadAll,
                        onReview = vm::reviewBooking,
                        onAcceptAsWalker = vm::applyAsWalker,
                        onOpenBooking = { id -> navController.navigate("booking/$id") },
                        onOpenWalk = { id -> navController.navigate("walk/$id") },
                        onPrefetchHistoryRoutes = vm::prefetchCompletedWalkRoutes,
                    )
                }
                composable("walk/{bookingId}") { entry ->
                    val bookingId = entry.arguments?.getString("bookingId") ?: return@composable
                    val booking = state.walkerBookings.firstOrNull { it.id == bookingId }
                        ?: state.ownerBookings.firstOrNull { it.id == bookingId }
                    val allowWalk = booking?.status?.uppercase() == "CONFIRMED" || booking?.status?.uppercase() == "IN_PROGRESS"
                    if (!allowWalk) {
                        val isWalkerWalk = state.user?.role?.key.equals("walker", true)
                        val isOwnerWalk = state.user?.role?.key.equals("owner", true)
                        LaunchedEffect(bookingId, isOwnerWalk, isWalkerWalk) {
                            if (isOwnerWalk || isWalkerWalk) vm.loadApplications(bookingId)
                        }
                        LaunchedEffect(bookingId, booking?.status) {
                            val shouldLoadRoute = booking?.status?.uppercase() in setOf(
                                "CONFIRMED",
                                "IN_PROGRESS",
                                "AWAITING_OWNER_PAYMENT",
                                "COMPLETED",
                            )
                            if (shouldLoadRoute) {
                                val quietRoute = booking?.status?.uppercase() == "AWAITING_OWNER_PAYMENT"
                                vm.loadRouteByBooking(bookingId, withGlobalLoading = !quietRoute)
                            }
                        }
                        BookingDetailScreen(
                            booking = booking,
                            dogName = booking?.let { b -> state.dogs.firstOrNull { it.id == b.dog_id }?.name },
                            route = state.routeByBooking[bookingId],
                            applications = state.applicationsByBooking[bookingId].orEmpty(),
                            currentUserId = state.user?.id,
                            isWalker = isWalkerWalk,
                            isOwner = isOwnerWalk,
                            hasConversation = state.conversations.any { it.booking_id == bookingId },
                            canOpenWalk = false,
                            feedbackText = state.error ?: state.notice,
                            feedbackIsError = state.error != null,
                            onBack = { navController.popBackStack() },
                            onRefreshRoute = {
                                vm.loadRouteByBooking(
                                    bookingId,
                                    withGlobalLoading = booking?.status?.uppercase() != "AWAITING_OWNER_PAYMENT",
                                )
                            },
                            onOpenWalk = {},
                            onWalkerConfirmBooking = { vm.walkerConfirmAssignedBooking(bookingId) },
                            onRefreshApplications = { vm.loadApplications(bookingId) },
                            onSubmitApplication = { msg -> vm.submitApplication(bookingId, msg) },
                            onWithdrawApplication = { appId -> vm.withdrawApplication(bookingId, appId) },
                            onOpenApplication = { appId ->
                                navController.navigate("booking/$bookingId/application/$appId")
                            },
                            onOpenChat = {
                                val c = state.conversations.firstOrNull { it.booking_id == bookingId }
                                if (c != null) navController.navigate("chat/${c.id}")
                            },
                            wallet = state.wallet,
                            paymentLoading = state.loading,
                            ownerPaymentError = state.error,
                            onOwnerTopUp = vm::topUpWallet,
                            onOwnerSettle = { vm.ownerSettleBooking(bookingId) },
                            onOwnerRefreshForPayment = vm::loadAll,
                            onWalkerPushToOwnerPayment = { vm.walkerPushPaymentToOwner(bookingId) },
                        )
                        return@composable
                    }
                    WalkSessionScreen(
                        booking = booking,
                        route = state.routeByBooking[bookingId],
                        trackPoints = state.trackPoints,
                        activeForBooking = state.activeSession != null && state.activeWalkBookingId == bookingId,
                        loading = state.loading,
                        feedbackText = state.error ?: state.notice,
                        feedbackIsError = state.error != null,
                        onClearFeedback = vm::clearFeedback,
                        onBack = { navController.popBackStack() },
                        onStartOrResume = { vm.startTracking(bookingId) },
                        onAddPoint = { lat, lng -> vm.addTrackPoint(lat, lng) },
                        onAddFakePoint = vm::addFakePoint,
                        onSimulatedWalkTick = { n, e, s -> vm.simulatedWalkStep(bookingId, n, e, s) },
                        onFinish = vm::finishTracking,
                        onRefreshRoute = { vm.loadRouteByBooking(bookingId) },
                    )
                }
                composable("chat/{conversationId}") { entry ->
                    val conversationId = entry.arguments?.getString("conversationId") ?: return@composable
                    LaunchedEffect(conversationId) {
                        vm.loadChatMessages(conversationId, reset = true)
                        vm.markChatRead(conversationId)
                    }
                    val convo = state.conversations.firstOrNull { it.id == conversationId }
                    val peerTitle = if (state.user?.id != null && convo != null) {
                        when (state.user?.id) {
                            convo.owner_id -> {
                                val app = state.applicationsByBooking[convo.booking_id].orEmpty()
                                    .firstOrNull { it.walker_user_id == convo.walker_user_id }
                                listOfNotNull(app?.walker_first_name, app?.walker_last_name).joinToString(" ").ifBlank { "Собеседник" }
                            }
                            convo.walker_user_id -> "Владелец"
                            else -> "Собеседник"
                        }
                    } else "Собеседник"
                    ChatScreen(
                        conversationId = conversationId,
                        currentUserId = state.user?.id,
                        peerTitle = peerTitle,
                        messages = state.chatMessagesByConversation[conversationId].orEmpty(),
                        hasMore = state.chatHasMoreByConversation[conversationId] ?: false,
                        loading = state.loading,
                        onBack = { navController.popBackStack() },
                        onRefresh = {
                            vm.loadChatMessages(conversationId, reset = true)
                            vm.markChatRead(conversationId)
                        },
                        onLoadMore = { vm.loadChatMessages(conversationId, reset = false) },
                        onSend = { text -> vm.sendChatMessage(conversationId, text) },
                    )
                }
                composable("booking/{bookingId}/application/{applicationId}") { entry ->
                    val bookingId = entry.arguments?.getString("bookingId") ?: return@composable
                    val applicationId = entry.arguments?.getString("applicationId") ?: return@composable
                    val application = state.applicationsByBooking[bookingId].orEmpty().firstOrNull { it.id == applicationId }
                    val walkerId = application?.walker_id
                    val walker = state.walkerProfileById[walkerId]
                    LaunchedEffect(walkerId) {
                        if (!walkerId.isNullOrBlank()) vm.loadWalkerProfile(walkerId)
                    }
                    WalkerApplicationScreen(
                        walker = walker,
                        application = application,
                        reviews = walkerId?.let { state.walkerReviewsById[it].orEmpty() }.orEmpty(),
                        onBack = { navController.popBackStack() },
                        onReject = {
                            vm.rejectApplication(bookingId, applicationId)
                            navController.popBackStack()
                        },
                        onAccept = {
                            vm.chooseApplication(bookingId, applicationId) { conversationId ->
                                val cid = conversationId
                                if (!cid.isNullOrBlank()) {
                                    navController.navigate("chat/$cid")
                                } else {
                                    navController.navigate("chat-by-booking/$bookingId")
                                }
                            }
                        },
                    )
                }
                composable("chat-by-booking/{bookingId}") { entry ->
                    val bookingId = entry.arguments?.getString("bookingId") ?: return@composable
                    val isWalkerCb = state.user?.role?.key.equals("walker", true)
                    val isOwnerCb = state.user?.role?.key.equals("owner", true)
                    LaunchedEffect(bookingId) { vm.refreshConversations() }
                    LaunchedEffect(bookingId, isOwnerCb, isWalkerCb) {
                        if (isOwnerCb || isWalkerCb) vm.loadApplications(bookingId)
                    }
                    val conversation = state.conversations.firstOrNull { it.booking_id == bookingId }
                    val bookingCb = (state.ownerBookings + state.walkerBookings).firstOrNull { it.id == bookingId }
                    if (conversation == null) {
                        BookingDetailScreen(
                            booking = bookingCb,
                            dogName = bookingCb?.let { b ->
                                state.dogs.firstOrNull { it.id == b.dog_id }?.name
                            },
                            route = state.routeByBooking[bookingId],
                            applications = state.applicationsByBooking[bookingId].orEmpty(),
                            currentUserId = state.user?.id,
                            isWalker = isWalkerCb,
                            isOwner = isOwnerCb,
                            hasConversation = false,
                            canOpenWalk = false,
                            feedbackText = "Чат создается, попробуйте через пару секунд",
                            feedbackIsError = false,
                            onBack = { navController.popBackStack() },
                            onRefreshRoute = { vm.loadRouteByBooking(bookingId) },
                            onOpenWalk = {},
                            onWalkerConfirmBooking = { vm.walkerConfirmAssignedBooking(bookingId) },
                            onRefreshApplications = { vm.loadApplications(bookingId) },
                            onSubmitApplication = { msg -> vm.submitApplication(bookingId, msg) },
                            onWithdrawApplication = { appId -> vm.withdrawApplication(bookingId, appId) },
                            onOpenApplication = { appId ->
                                navController.navigate("booking/$bookingId/application/$appId")
                            },
                            onOpenChat = {},
                            onWalkerPushToOwnerPayment = { vm.walkerPushPaymentToOwner(bookingId) },
                        )
                    } else {
                        ChatScreen(
                            conversationId = conversation.id,
                            currentUserId = state.user?.id,
                            peerTitle = run {
                                val app = state.applicationsByBooking[bookingId].orEmpty()
                                    .firstOrNull { it.walker_user_id == conversation.walker_user_id }
                                listOfNotNull(app?.walker_first_name, app?.walker_last_name).joinToString(" ").ifBlank { "Собеседник" }
                            },
                            messages = state.chatMessagesByConversation[conversation.id].orEmpty(),
                            hasMore = state.chatHasMoreByConversation[conversation.id] ?: false,
                            loading = state.loading,
                            onBack = { navController.popBackStack() },
                            onRefresh = {
                                vm.loadChatMessages(conversation.id, reset = true)
                                vm.markChatRead(conversation.id)
                            },
                            onLoadMore = { vm.loadChatMessages(conversation.id, reset = false) },
                            onSend = { text -> vm.sendChatMessage(conversation.id, text) },
                        )
                    }
                }
                composable("dogs") {
                    if (isWalker) {
                        BookingsHubScreen(
                            state = state,
                            onRefresh = vm::loadAll,
                            onReview = vm::reviewBooking,
                            onAcceptAsWalker = vm::applyAsWalker,
                            onOpenBooking = { id -> navController.navigate("booking/$id") },
                            onOpenWalk = { id -> navController.navigate("walk/$id") },
                            onPrefetchHistoryRoutes = vm::prefetchCompletedWalkRoutes,
                        )
                    } else {
                        DogsScreen(state, { id -> navController.navigate("dog/$id") }, { navController.navigate("dog/add") })
                    }
                }
                composable("dog/add") {
                    DogAddScreen(
                        onBack = { navController.popBackStack() },
                        onAdd = { name, breed, birthDate, weightKg, gender, vaccinated, sterilized, aggressive, behavior, medical ->
                            vm.addDog(name, breed, birthDate, weightKg, gender, vaccinated, sterilized, aggressive, behavior, medical)
                            navController.popBackStack()
                        },
                    )
                }
                composable("dog/{id}") { entry ->
                    val dogId = entry.arguments?.getString("id")
                    DogDetailScreen(
                        dog = state.dogs.firstOrNull { it.id == dogId },
                        localPhotoUri = dogId?.let { state.dogLocalPhotos[it] },
                        ownerCity = state.user?.city,
                        ownerCountry = state.user?.country,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate("dog/$dogId/edit") },
                        onCreateBooking = { navController.navigate("dog/$dogId/booking") },
                    )
                }
                composable("dog/{id}/booking") { entry ->
                    val dogId = entry.arguments?.getString("id")
                    BookingCreateScreen(
                        dog = state.dogs.firstOrNull { it.id == dogId },
                        onSuggestStreet = { country, city, streetQuery -> vm.suggestStreet(country, city, streetQuery) },
                        onCreate = { did, duration, country, city, street, house, apartment, meetingLat, meetingLng, price, extra ->
                            vm.createBooking(
                                dogId = did,
                                durationMinutes = duration,
                                addressCountry = country,
                                addressCity = city,
                                addressStreet = street,
                                addressHouse = house,
                                addressApartment = apartment,
                                meetingLat = meetingLat,
                                meetingLng = meetingLng,
                                desiredPrice = price,
                                extraParams = extra,
                            )
                            navController.navigate("map") { launchSingleTop = true }
                        }
                    )
                }
                composable("dog/{id}/edit") { entry ->
                    val dogId = entry.arguments?.getString("id") ?: return@composable
                    DogEditScreen(
                        dog = state.dogs.firstOrNull { it.id == dogId },
                        localPhotoUri = state.dogLocalPhotos[dogId],
                        onBack = { navController.popBackStack() },
                        onSave = { name, breed, birthDate, weightKg, gender, vaccinated, sterilized, aggressive, behavior, medical ->
                            vm.updateDog(
                                dogId = dogId,
                                name = name,
                                breed = breed,
                                birthDate = birthDate,
                                weightKg = weightKg,
                                gender = gender,
                                isVaccinated = vaccinated,
                                isSterilized = sterilized,
                                isAggressive = aggressive,
                                behaviorNotes = behavior,
                                medicalNotes = medical,
                            )
                            navController.popBackStack()
                        },
                        onPickPhoto = { uri -> vm.saveDogLocalPhoto(dogId, uri) },
                        onClearPhoto = { vm.clearDogLocalPhoto(dogId) },
                    )
                }
                composable("profile") {
                    ProfileScreen(
                        state = state,
                        onUpdateProfile = vm::updateProfile,
                        onOpenSettings = { navController.navigate("settings") },
                        onLogout = vm::logout,
                        onBack = { navController.popBackStack() },
                        onTopUpWallet = vm::topUpWallet,
                        onOpenWithdrawals = { navController.navigate("withdrawals") },
                    )
                }
                composable("withdrawals") {
                    LaunchedEffect(Unit) { vm.refreshWithdrawals() }
                    WalkerWithdrawalsScreen(
                        wallet = state.wallet,
                        withdrawals = state.withdrawals,
                        loading = state.loading,
                        feedback = state.error,
                        onBack = { navController.popBackStack() },
                        onRefresh = vm::refreshWithdrawals,
                        onSubmitWithdrawal = vm::requestWithdrawal,
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        state = state,
                        onSetDarkTheme = vm::setDarkTheme,
                        onSetCompactUi = vm::setCompactUi,
                        onSetHideNotificationsPreview = vm::setHideNotificationsPreview,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
            state.reviewPromptBookingId?.let { bid ->
                ReviewPromptDialog(
                    bookingId = bid,
                    onDismiss = vm::clearReviewPrompt,
                    onSubmit = { id, rating, comment ->
                        vm.reviewBooking(id, rating, comment)
                        Unit
                    },
                )
            }
        }
    }
}
