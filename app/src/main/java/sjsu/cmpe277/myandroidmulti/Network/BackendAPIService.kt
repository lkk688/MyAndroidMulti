package sjsu.cmpe277.myandroidmulti.Network

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

private const val BASE_URL = "xxx"


/**
 * Build the Moshi object that Retrofit will be using, making sure to add the Kotlin adapter for
 * full Kotlin compatibility.
 */
private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

/**
 * Use the Retrofit builder to build a retrofit object using a Scalar converter
 * want Retrofit to fetch a JSON response from the web service, and return it as a String
 */
private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()


/**
 * A public interface that exposes the [getProperties] method
 */
interface BackendAPIService {
    @GET("/default/JSHTTPBackend") ///test1/helloworld realestate Retrofit appends the endpoint to the base URL
    fun getProperties(@Query("deviceID") deviceID: String, @Query("thingType") thingType: String, @Header("Authorization") auth: String):
            Call<BackendDataProperty>
}

/**
 * A public Api object that exposes the lazy-initialized Retrofit service
 * each time your app calls WeatherApi.retrofitService, it will get a singleton Retrofit object that implements ApiService.
 */
object BackendApi {
    val retrofitService : BackendAPIService by lazy {
        retrofit.create(BackendAPIService::class.java) }
    //The Retrofit create() method creates the Retrofit service itself with the ApiService interface.
}


//{"message":{"headerdata":"Sunday","time":"2020-04-06T20:04:15.638Z","deviceID":"webnew4","thingType":"web","sensorData":{"temperature":"30","batteryVoltage":"3300mV"}},"currenttime":"2020-04-07T05:38:19.620Z"}
data class BackendDataProperty(
    @Json(name = "message") val message: MessagePart,
    //val message: String,
    val currenttime: String
)

data class MessagePart(
    val headerdata: String,
    val time: String,
    val deviceID: String,
    val thingType: String,
    val sensorData: SensorData
)

data class SensorData(
    val temperature: String,
    val batteryVoltage: String
)
