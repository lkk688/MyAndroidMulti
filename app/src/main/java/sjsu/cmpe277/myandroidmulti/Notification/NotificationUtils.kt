package sjsu.cmpe277.myandroidmulti.Notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import sjsu.cmpe277.myandroidmulti.MainActivity
import sjsu.cmpe277.myandroidmulti.R

// Notification ID.
private val NOTIFICATION_ID = 0
private val REQUEST_CODE = 0
private val FLAGS = 0

/**
 * Builds and delivers the notification.
 *
 * @param context, activity context.
 */

//Most simple version of sendNotification
//fun NotificationManager.sendNotification(messageBody: String, applicationContext: Context) {
//
//    // get an instance of NotificationCompat.Builder
//    // Build the notification
//    val builder = NotificationCompat.Builder(
//        applicationContext,
//        applicationContext.getString(R.string.cmpe277_notification_channel_id)
//    )
//        .setSmallIcon(R.drawable.cmpe277_notification_icon) // set title, text and icon to builder
//        .setContentTitle(applicationContext
//            .getString(R.string.notification_title))
//        .setContentText(messageBody)
//
//    notify(NOTIFICATION_ID, builder.build())
//}

//add intent to the notification
fun NotificationManager.sendNotification(messageBody: String, applicationContext: Context) {

    // Create the content intent for the notification, which launches
    // this activity
    // use this intent to tell the system to open MainActivity when the user taps the notification
    val contentIntent = Intent(applicationContext, MainActivity::class.java)
    // To make an intent work outside your app, you need to create a new PendingIntent.
    //PendingIntent grants rights to another application or the system to perform an operation on behalf of your application.
    val contentPendingIntent = PendingIntent.getActivity(
        applicationContext,
        NOTIFICATION_ID,
        contentIntent,
        PendingIntent.FLAG_UPDATE_CURRENT //do not want to create a new notification if there is an existing one
    )

    // Add style to the notification
    val notifyImage = BitmapFactory.decodeResource(
        applicationContext.resources,
        R.drawable.sos//sun_notification_icon//cmpe277_notification_icon
    )
    val bigPicStyle = NotificationCompat.BigPictureStyle()//shows large-format notifications that include a large image attachment.
        .bigPicture(notifyImage)
        .bigLargeIcon(null) //the large icon goes away when the notification is expanded.

    // get an instance of NotificationCompat.Builder
    // Build the notification
    val builder = NotificationCompat.Builder(
        applicationContext,
        applicationContext.getString(R.string.cmpe277_notification_channel_id)
    )
        .setSmallIcon(R.drawable.sun_notification_icon)//cmpe277_notification_icon) // set title, text and icon to builder
        .setContentTitle(applicationContext
            .getString(R.string.notification_title))
        .setContentText(messageBody)
        // set content intent via PendingIntent
        .setContentIntent(contentPendingIntent)
        .setAutoCancel(true)//when the user taps on the notification, the notification dismisses itself as it takes them to the app
        // Add style to builder
        .setStyle(bigPicStyle)
        .setLargeIcon(notifyImage)

    notify(NOTIFICATION_ID, builder.build())
}

/**
 * Cancels all notifications.
 *
 */
fun NotificationManager.cancelNotifications() {
    cancelAll()
}