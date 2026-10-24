package me.jstephan.parcel.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.InputCallback
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Template
import androidx.car.app.model.signin.InputSignInMethod
import androidx.car.app.model.signin.SignInTemplate
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.jstephan.parcel.Prefs
import me.jstephan.parcel.R
import me.jstephan.parcel.data.DeliveryRepository
import me.jstephan.parcel.data.ParcelApi
import me.jstephan.parcel.data.ParcelException
import me.jstephan.parcel.work.CheckWorker
import java.io.IOException

/**
 * Typing the Parcel API key (the car's keyboard, parked only). The key is checked with one deliveries call
 * before it is kept; that answer becomes the first check, so the deliveries already there aren't notified.
 */
class ApiKeyScreen(carContext: CarContext) : Screen(carContext) {
    private val prefs = Prefs(carContext)
    private val repo = DeliveryRepository.get(carContext)
    private var errorText: String? = null
    private var checking = false

    override fun onGetTemplate(): Template {
        val input = InputSignInMethod.Builder(object : InputCallback {
            override fun onInputSubmitted(text: String) = submit(text.trim())
        })
            .setHint(carContext.getString(R.string.api_key_hint))
            .setInputType(InputSignInMethod.INPUT_TYPE_DEFAULT)
            .setShowKeyboardByDefault(true)
        errorText?.let(input::setErrorMessage)
        val builder = SignInTemplate.Builder(input.build())
            .setTitle(carContext.getString(R.string.api_key_title))
            .setHeaderAction(Action.BACK)
            .setInstructions(carContext.getString(R.string.api_key_instructions))
            .setLoading(checking)
        if (prefs.hasKey) {
            // The host only takes parked-only actions on a sign-in template.
            val delete = Action.Builder()
                .setTitle(carContext.getString(R.string.api_key_delete))
                .setOnClickListener(ParkedOnlyOnClickListener.create { deleteKey() })
                .build()
            builder.addAction(delete)
        }
        return builder.build()
    }

    private fun submit(key: String) {
        if (checking) return
        if (key.isEmpty()) return showError(R.string.api_key_empty)
        checking = true
        errorText = null
        invalidate()
        lifecycleScope.launch {
            val body = try {
                withContext(Dispatchers.IO) { ParcelApi.fetchDeliveries(key) }
            } catch (e: ParcelException.RateLimit) {
                // Parcel knows the key, it only throttles it: keep it, the next check fills the list.
                null
            } catch (e: IOException) {
                checking = false
                showError(Ui.errorMessage(e))
                return@launch
            }
            checking = false
            save(key, body)
        }
    }

    private suspend fun save(key: String, body: String?) {
        prefs.apiKey = key
        prefs.authFailureNotified = false
        repo.reset(body)
        CheckWorker.schedule(carContext)
        Ui.toast(carContext, R.string.api_key_saved)
        // The first key is also when notifications start: ask for the permission now, parked.
        setResult(true)
        Ui.requestNotifications(carContext) { screenManager.pop() }
    }

    private fun deleteKey() {
        prefs.apiKey = null
        CheckWorker.cancel(carContext)
        lifecycleScope.launch {
            repo.reset(null)
            Ui.toast(carContext, R.string.api_key_deleted)
            screenManager.pop()
        }
    }

    private fun showError(message: Int) {
        errorText = carContext.getString(message)
        invalidate()
    }
}
