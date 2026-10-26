package me.jstephan.parcel.work

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import me.jstephan.parcel.R
import me.jstephan.parcel.data.Delivery

/** One notification per delivery with news, replaced by the next one for the same delivery. */
object Notifier {
    /** Delivery key a notification opens, read by the session (ParcelService.kt). */
    const val EXTRA_DELIVERY = "me.jstephan.parcel.DELIVERY"

    private const val CHANNEL = "deliveries"
    private const val AUTH_TAG = "auth"
    private const val NOTIFICATION_ID = 1

    fun createChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.channel_name))
            .setDescription(context.getString(R.string.channel_description))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun deliveriesChanged(context: Context, changed: List<Delivery>) {
        changed.forEach { delivery ->
            val event = delivery.lastEvent
            // "En transit · Arrivé sur le site", then where and when.
            val text = listOf(context.getString(delivery.status.label), event?.text).filterNot { it.isNullOrBlank() }.joinToString(" · ")
            val details = listOf(event?.location, event?.date).filterNot { it.isNullOrBlank() }.joinToString(" · ")
            post(context, delivery.key, delivery.title, text, details, delivery.key)
        }
    }

    fun authFailed(context: Context) = post(
        context,
        AUTH_TAG,
        context.getString(R.string.notification_auth_title),
        context.getString(R.string.notification_auth_text),
        details = "",
        deliveryKey = null
    )

    // areNotificationsEnabled() is false while POST_NOTIFICATIONS isn't granted (Android 13+).
    @SuppressLint("MissingPermission")
    private fun post(context: Context, tag: String, title: String, text: String, details: String, deliveryKey: String?) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_parcel)
            .setColor(ContextCompat.getColor(context, R.color.brand))
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        if (details.isNotEmpty()) builder.setStyle(NotificationCompat.BigTextStyle().bigText("$text\n$details"))
        openApp(context, tag, deliveryKey)?.let(builder::setContentIntent)
        manager.notify(tag, NOTIFICATION_ID, builder.build())
    }

    private fun openApp(context: Context, tag: String, deliveryKey: String?): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        deliveryKey?.let { intent.putExtra(EXTRA_DELIVERY, it) }
        return PendingIntent.getActivity(
            context,
            tag.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
