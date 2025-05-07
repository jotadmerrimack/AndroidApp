package com.example.weatherforecast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WeatherForecastApp()
            }
        }
    }
}
@Composable
fun WeatherForecastApp() {
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var forecast by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = latitude,
            onValueChange = { latitude = it },
            label = { Text("Enter Latitude") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = longitude,
            onValueChange = { longitude = it },
            label = { Text("Enter Longitude") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            if (latitude.isNotBlank() && longitude.isNotBlank()) {
                fetchForecast(latitude, longitude) {
                    forecast = it
                }
            } else {
                forecast = "Please enter both latitude and longitude."
            }
        }) {
            Text("Get Forecast")
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(text = forecast)
    }
}

fun fetchForecast(lat: String, lon: String, onResult: (String) -> Unit) {
    val client = OkHttpClient()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Get Grid Info
            val pointUrl = "https://api.weather.gov/points/$lat,$lon"
            val pointRequest = Request.Builder().url(pointUrl).build()
            val pointResponse = client.newCall(pointRequest).execute()
            val pointJson = JSONObject(pointResponse.body!!.string())
            val props = pointJson.getJSONObject("properties")
            val office = props.getString("gridId")
            val gridX = props.getInt("gridX")
            val gridY = props.getInt("gridY")

            // Get Forecast
            val forecastUrl = "https://api.weather.gov/gridpoints/$office/$gridX,$gridY/forecast"
            val forecastRequest = Request.Builder().url(forecastUrl).build()
            val forecastResponse = client.newCall(forecastRequest).execute()
            val forecastJson = JSONObject(forecastResponse.body!!.string())
            val periods = forecastJson.getJSONObject("properties").getJSONArray("periods")
            val first = periods.getJSONObject(0)

            val name = first.getString("name")
            val temp = first.getInt("temperature")
            val trend = first.optString("temperatureTrend", "")
            val shortForecast = first.getString("shortForecast")

            val result = buildString {
                append("Forecast for $name: $temp°F")
                if (trend.isNotEmpty()) append(", $trend")
                append(" - $shortForecast")
            }

            withContext(Dispatchers.Main) {
                onResult(result)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult("Error: ${e.localizedMessage}")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun WeatherForecastAppPreview() {
    MaterialTheme {
        WeatherForecastApp()
    }
}
