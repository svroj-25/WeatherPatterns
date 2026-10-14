package uz.ttpu.weatherpatterns.mvi.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.ttpu.weatherpatterns.di.WeatherModule
import uz.ttpu.weatherpatterns.mvi.model.WeatherIntent
import uz.ttpu.weatherpatterns.mvi.model.WeatherState
import uz.ttpu.weatherpatterns.mvi.viewmodel.WeatherMviViewModel
import uz.ttpu.weatherpatterns.ui.WeatherScreen

class MviActivity : ComponentActivity() {

    private val viewModel: WeatherMviViewModel by viewModels {
        WeatherModule.weatherMviViewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                WeatherScreen(
                    status = state.toStatusText(),
                    onCitySelected = { city ->
                        viewModel.onIntent(WeatherIntent.SelectCity(city))
                    },
                )
            }
        }
    }
}

// When isLoading is true and an older weather is still in state,
// the isLoading check must come first so the user sees "Loading…"
// while the new city data is being fetched.
private fun WeatherState.toStatusText(): String = when {
    isLoading -> "Loading…"
    error != null -> "Error: $error"
    weather != null -> "${weather.city}: ${weather.temperatureC}°C, ${weather.condition}"
    else -> "Pick a city"
}
