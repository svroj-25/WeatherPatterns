package uz.ttpu.weatherpatterns.mvvm.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uz.ttpu.weatherpatterns.data.api.FakeWeatherApi
import uz.ttpu.weatherpatterns.data.repository.WeatherRepository

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() =
        WeatherViewModel(WeatherRepository(FakeWeatherApi(latencyMs = 0)))

    @Test
    fun selecting_known_city_emits_success_state() {
        val viewModel = newViewModel()
        viewModel.onCitySelected("Tashkent")

        val state = viewModel.uiState.value
        assertTrue(state is WeatherUiState.Success)
        assertEquals("Tashkent", (state as WeatherUiState.Success).weather.city)
        assertEquals(24, state.weather.temperatureC)
    }

    @Test
    fun selecting_unknown_city_emits_error_state() {
        val viewModel = newViewModel()
        viewModel.onCitySelected("Atlantis")

        val state = viewModel.uiState.value
        assertTrue(state is WeatherUiState.Error)
        assertEquals("No forecast for Atlantis", (state as WeatherUiState.Error).message)
    }
}
