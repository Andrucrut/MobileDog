package com.example.dogapp.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val tabs = remember {
        listOf(
            BottomTab("map", "Карта"),
            BottomTab("bookings", "Бронь"),
            BottomTab("dogs", "Питомцы"),
            BottomTab("profile", "Профиль"),
        )
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

        Scaffold(
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
                        onStartTracking = vm::startTracking,
                        onAddFakePoint = vm::addFakePoint,
                        onFinishTracking = vm::finishTracking,
                        onOpenBookingDetail = { id -> navController.navigate("booking/$id") },
                    )
                }
                composable("booking/{id}") { entry ->
                    val bookingId = entry.arguments?.getString("id") ?: return@composable
                    val booking = state.ownerBookings.firstOrNull { it.id == bookingId }
                    val dogName = booking?.let { b -> state.dogs.firstOrNull { it.id == b.dog_id }?.name }
                    BookingDetailScreen(
                        booking = booking,
                        dogName = dogName,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("bookings") { BookingScreen(state, vm::loadAll, vm::confirmAndPay, vm::reviewBooking) }
                composable("dogs") { DogsScreen(state, { id -> navController.navigate("dog/$id") }, { navController.navigate("dog/add") }) }
                composable("dog/add") {
                    DogAddScreen(
                        onAdd = { name, breed, birthDate, weightKg, gender, vaccinated, sterilized, aggressive, behavior, medical ->
                            vm.addDog(name, breed, birthDate, weightKg, gender, vaccinated, sterilized, aggressive, behavior, medical)
                            navController.popBackStack()
                        }
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
    }
}
