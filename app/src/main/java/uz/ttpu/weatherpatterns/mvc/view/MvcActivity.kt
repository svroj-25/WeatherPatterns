package uz.ttpu.weatherpatterns.mvc.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import uz.ttpu.weatherpatterns.di.WeatherModule
import uz.ttpu.weatherpatterns.mvc.controller.WeatherController
import uz.ttpu.weatherpatterns.mvc.model.WeatherModel
import uz.ttpu.weatherpatterns.ui.WeatherScreen

class MvcActivity : ComponentActivity(), WeatherView {

    private val model = WeatherModel(WeatherModule.repository)
    private val controller = WeatherController(model, view = this)
    private var status by mutableStateOf("Pick a city")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WeatherScreen(status = status, onCitySelected = controller::onCitySelected)
            }
        }
    }

    // Why render() takes no parameters:
    // In classic MVC, render() takes no parameters because the View is allowed to observe/read
    // the Model directly; the Controller only notifies the View that state has changed.
    // If the Controller passed the Weather into the View instead, the View would no longer
    // depend on the Model, turning the Controller into a Presenter (MVP pattern).
    override fun render() {
        status = when {
            model.isLoading -> "Loading…"
            model.error != null -> "Error: ${model.error}"
            model.weather != null -> "${model.weather?.city}: ${model.weather?.temperatureC}°C, ${model.weather?.condition}"
            else -> "Pick a city"
        }
    }
}
