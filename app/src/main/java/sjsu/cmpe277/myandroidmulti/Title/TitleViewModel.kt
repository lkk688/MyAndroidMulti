package sjsu.cmpe277.myandroidmulti.Title

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.TypedArrayUtils.getString
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import sjsu.cmpe277.myandroidmulti.Notification.MyFirebaseMessagingService
import sjsu.cmpe277.myandroidmulti.Notification.cancelNotifications
import sjsu.cmpe277.myandroidmulti.Notification.sendNotification
import sjsu.cmpe277.myandroidmulti.R
import java.util.*

//AndroidViewModel is a subclass of ViewModel that is aware of the Application context.
class TitleViewModel(private val app: Application): AndroidViewModel(app)//ViewModel()
{
    private val TOPIC = "MyFCMtoApp"

    var yourname = MutableLiveData<String>()
    // The current risk score
    val riskscore = MutableLiveData<Int>()

    init {
        Log.i("TitleViewModel", "TitleViewModel created!")
        yourname.value = ""
        riskscore.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        Log.i("TitleViewModel","TitleViewModel destroyed!")
    }

    fun updateRiskscore() {
        riskscore.value = (riskscore.value)?.plus(1)
        Log.i("TitleViewModel",riskscore.value.toString())
    }

    /**
     * Creates a new alarm, notification and timer
     */
    fun sendNotification() {
        val notificationManager = ContextCompat.getSystemService(
            app,
            NotificationManager::class.java
        ) as NotificationManager

        notificationManager.cancelNotifications()//cancel previous notification

        notificationManager.sendNotification(app.getString(R.string.timer_running), app)
    }

    fun createChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                // change importance
                NotificationManager.IMPORTANCE_HIGH
            )// disable badges for this channel
                .apply {
                    setShowBadge(true)
                }

            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = app.getString(R.string.cmpe277_notification_channel_description)

            val notificationManager = app.getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(notificationChannel)

        }
    }

    fun subscribeTopic() {
        // [START subscribe_topic]
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)
            .addOnCompleteListener { task ->
                var message = app.getString(R.string.message_subscribed)
                if (!task.isSuccessful) {
                    message = app.getString(R.string.message_subscribe_failed)
                }
                Log.i("TitleViewModel", message)
                //Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        // [END subscribe_topics]
    }

    fun fetchTokens() {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("TitleViewModel", "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token
                sendRegistrationToServer(token)

                // Log and toast
                //val msg = getString(R.string.msg_token_fmt, token)
                Log.i("TitleViewModel", "token: "+ token)
            })
    }

    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send token to your app server.

        // Access a Cloud Firestore instance from your Activity
        val db = Firebase.firestore

        val currentuser = FirebaseAuth.getInstance().currentUser
        if (currentuser != null){
            val name = currentuser?.displayName
            val email = currentuser?.email
            val photoUrl = currentuser?.photoUrl
            val uid = currentuser?.uid
            Log.i(
                "TitleViewModel",
                "TitleViewModel User info, name: ${name}; email: ${email}; photoUrl: ${photoUrl}; ."
            )

            // Create a new user with a first and last name
            val user = hashMapOf(
                "UserID" to uid,
                "Displayname" to name,
                "Email" to email,
                "Singintime" to Timestamp(Date()),
                "FCMtoken" to token
            )
            // Add a new document with a generated ID
            db.collection("users")
                .add(user)
                .addOnSuccessListener { documentReference ->
                    Log.d("TitleViewModel", "DocumentSnapshot added with ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.w("TitleViewModel", "Error adding document", e)
                }

        }

    }
}