package sjsu.cmpe277.myandroidmulti.Network

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

private const val WeatherAPPID = "2b492c001d57cd5499947bd3d3f9c47b"

class WeatherViewModel : ViewModel() {
    // TODO: Implement the ViewModel

    // The internal MutableLiveData String that stores the most recent response
    val _response = MutableLiveData<String>()

    val _backendresponse = MutableLiveData<String>()



    /**
     * Call getWeatherProperties() on init so we can display status immediately.
     */
    init {
        getWeatherProperties()
        //getBackendProperties()
    }

    private fun getWeatherProperties() {
        //_response.value = "Set the Weather API Response here!"
        //WeatherApi.retrofitService.getProperties()
        WeatherApi.retrofitService.getProperties("San Jose", WeatherAPPID).enqueue(
            object: Callback<WeatherProperty> {
                override fun onFailure(call: Call<WeatherProperty>, t: Throwable) {
                    //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    _response.value = "Failure: " + t.message
                }

                override fun onResponse(call: Call<WeatherProperty>, response: Response<WeatherProperty>) {
                    //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    //_response.value = response.body()
                    _response.value = "Success: ${response.body()?.name} city retrieved; Temperature: ${response.body()?.mainpart?.temp}; Humidity: ${response.body()?.mainpart?.humidity}"
                }

            }
        )
    }

    private fun getBackendProperties() {
        var token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjU1NXZOMWxiNnNVckxtbU9JZ2VRTiJ9.eyJpc3MiOiJodHRwczovL3Nqc3VjbXBlLmF1dGgwLmNvbS8iLCJzdWIiOiI1aG1jMFdYcUNlMExCUEJxbU9tRERySzJQN3VNOU1kdUBjbGllbnRzIiwiYXVkIjoiaHR0cHM6Ly9hd3NhcGkuc2pzdWNtcGUiLCJpYXQiOjE1ODYyOTY2NzQsImV4cCI6MTU4NjM4MzA3NCwiYXpwIjoiNWhtYzBXWHFDZTBMQlBCcW1PbUREcksyUDd1TTlNZHUiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.JQY5qtfOX9wgpb4MluZ-4tqWFNniU8QzEBEOqTNAPk5n_E9f5G2RzIJ914kJ1EFJ9Hczka2Qh1olyVezpKDFndgY2SJX7ts7T1rkTeQN1fsu4xFUcHjlJBzgruJzlqLFxqysLAGtAb4Lk3NeAjvDZ0T8dtGQ-0nv2oOjNF0r8PQ08GHnPrXTIOFI0MuP7h5uVCs4tkRyW9G3np0OgTypKdmeR8VRgswcpU7Xo8tDve4NJBO8p4gxL9JTOWsdq8YVMXHrT8gEaGH9qANq_964AXZs80QVd2Zfsrr_4BIrLBfQtT2mpmXp_JOVHqZMI45J5uoff_vuNmt_Rdld1LvSAQ"
        BackendApi.retrofitService.getProperties("webnew4", "web", "Bearer "+token) .enqueue(
            object: Callback<BackendDataProperty> {
                override fun onFailure(call: Call<BackendDataProperty>, t: Throwable) {
                    _backendresponse.value = "Failure: " + t.message
                }

                override fun onResponse(call: Call<BackendDataProperty>, response: Response<BackendDataProperty>) {
                    //_backendresponse.value = "Success: current time: ${response.body()?.currenttime}; Temperature: ${response.body()?.message?.sensorData?.temperature}; }"
                    _backendresponse.value = "Success: current time: ${response.body()?.currenttime}; Temperature: ${response.body()?.message?.sensorData?.temperature}; "
                }

            }
        )
    }

    fun sendFirestoreMessage() {
        val db = Firebase.firestore

        val message = hashMapOf(
            "name" to "test",
            "profilePicUrl" to "/images/firebase-logo.png",
            "text" to "testing FCM",
            "timestamp" to Timestamp(Date())
        )
        // Add a new document with a generated ID
        db.collection("messages")
            .add(message)
            .addOnSuccessListener { documentReference ->
                Log.d("TitleViewModel", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("TitleViewModel", "Error adding document", e)
            }
    }
}
