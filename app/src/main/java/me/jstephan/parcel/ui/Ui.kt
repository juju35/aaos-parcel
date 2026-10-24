package me.jstephan.parcel.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ForegroundCarColorSpan
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import me.jstephan.parcel.R
import me.jstephan.parcel.data.Delivery
import me.jstephan.parcel.data.ParcelException
import me.jstephan.parcel.data.Status
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small helpers shared by the screens. */
object Ui {
    private const val DEFAULT_LIMIT = 6

    fun icon(context: Context, @DrawableRes res: Int, tint: CarColor = CarColor.DEFAULT): CarIcon =
        CarIcon.Builder(IconCompat.createWithResource(context, res)).setTint(tint).build()

    fun statusIcon(context: Context, status: Status): CarIcon = icon(context, status.icon, statusColor(status))

    fun statusColor(status: Status): CarColor = when (status) {
        Status.DELIVERED -> CarColor.GREEN
        Status.IN_TRANSIT, Status.OUT_FOR_DELIVERY, Status.INFO_RECEIVED -> CarColor.BLUE
        Status.AWAITING_PICKUP -> CarColor.YELLOW
        Status.FAILED_ATTEMPT, Status.EXCEPTION -> CarColor.RED
        Status.FROZEN, Status.NOT_FOUND, Status.UNKNOWN -> CarColor.DEFAULT
    }

    fun colored(text: CharSequence, color: CarColor): CharSequence = SpannableString(text).apply {
        setSpan(ForegroundCarColorSpan.create(color), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    /** Non-blank parts joined with " · ". */
    fun join(vararg parts: String?): String = parts.filterNot { it.isNullOrBlank() }.joinToString(" · ")

    /** "Prévu le sam. 25 oct.", "Prévu entre le … et le …"; null when Parcel has no date. */
    fun expected(context: Context, delivery: Delivery): String? {
        val from = delivery.expectedFrom?.let(::day) ?: delivery.dateExpected ?: return null
        val to = delivery.expectedTo?.let(::day) ?: delivery.dateExpectedEnd
        return if (to == null || to == from) {
            context.getString(R.string.expected_on, from)
        } else {
            context.getString(R.string.expected_between, from, to)
        }
    }

    private fun day(epochSeconds: Long): String = SimpleDateFormat("EEE d MMM", Locale.FRANCE).format(Date(epochSeconds * 1000))

    @StringRes
    fun errorMessage(e: IOException): Int = when (e) {
        is ParcelException.Auth -> R.string.error_auth
        is ParcelException.RateLimit -> R.string.error_rate_limit
        else -> R.string.error_network
    }

    /** How many rows the host shows in a list. */
    fun listLimit(carContext: CarContext): Int = runCatching {
        carContext.getCarService(ConstraintManager::class.java).getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
    }.getOrDefault(DEFAULT_LIMIT)

    fun action(context: Context, @StringRes title: Int, primary: Boolean = false, onClick: () -> Unit): Action {
        val builder = Action.Builder().setTitle(context.getString(title)).setOnClickListener { onClick() }
        if (primary) builder.setBackgroundColor(CarColor.PRIMARY)
        return builder.build()
    }

    fun iconAction(context: Context, @DrawableRes icon: Int, onClick: () -> Unit): Action =
        Action.Builder().setIcon(icon(context, icon)).setOnClickListener { onClick() }.build()

    fun message(context: Context, @StringRes text: Int, @DrawableRes icon: Int, header: Action, vararg actions: Action): Template {
        val builder = MessageTemplate.Builder(context.getString(text))
            .setTitle(context.getString(R.string.app_name))
            .setHeaderAction(header)
            .setIcon(icon(context, icon))
        actions.forEach { builder.addAction(it) }
        return builder.build()
    }

    fun toast(context: CarContext, @StringRes text: Int) = CarToast.makeText(context, text, CarToast.LENGTH_LONG).show()

    /** Asks for the notification permission where it exists (Android 13+), then runs [then]. */
    fun requestNotifications(carContext: CarContext, then: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return then()
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(carContext, permission) == PackageManager.PERMISSION_GRANTED) return then()
        carContext.requestPermissions(listOf(permission)) { granted, _ ->
            if (granted.isEmpty()) toast(carContext, R.string.permission_denied)
            then()
        }
    }
}
