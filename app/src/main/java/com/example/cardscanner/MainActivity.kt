package com.example.cardscanner

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.cardscanner.databinding.ActivityMainBinding
import com.google.android.gms.wallet.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var client: PaymentsClient
    private lateinit var cardRecognitionPendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        client = createPaymentsClient(this)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.fab)
                .setAction("Action", null).show()
        }
        binding.contentMain.googleScan.setOnClickListener {
            startPaymentCardOcr()
        }
    }

    override fun onResume() {
        super.onResume()
        possiblyShowPaymentCardOcrButton()
    }

    private fun createPaymentsClient(activity: Activity): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
            .build()

        return Wallet.getPaymentsClient(activity, walletOptions)
    }

    private fun possiblyShowPaymentCardOcrButton() {
        // The request can be used to configure the type of the payment card recognition. Currently
        // the only supported type is card OCR, so it is sufficient to call the getDefaultInstance()
        // method.
        val request = PaymentCardRecognitionIntentRequest.getDefaultInstance()
        client
            .getPaymentCardRecognitionIntent(request)
            .addOnSuccessListener { intentResponse ->
                cardRecognitionPendingIntent = intentResponse.paymentCardRecognitionPendingIntent
                binding.contentMain.googleScan.isEnabled = true
            }
            .addOnFailureListener { e ->
                // The API is not available either because the feature is not enabled on the device
                // or because your app is not registered.
                binding.contentMain.googleScan.isEnabled = false
                Log.e("TAG", "Payment card ocr not available.", e)
            }
    }

    private fun startPaymentCardOcr() {
        try {
            ActivityCompat.startIntentSenderForResult(
                this@MainActivity,
                cardRecognitionPendingIntent.intentSender,
                PAYMENT_CARD_RECOGNITION_REQUEST_CODE,
                null, 0, 0, 0, null
            )
        } catch (e: IntentSender.SendIntentException) {
            throw RuntimeException("Failed to start payment card recognition.", e)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PAYMENT_CARD_RECOGNITION_REQUEST_CODE -> {
                possiblyShowPaymentCardOcrButton()
                when (resultCode) {
                    RESULT_OK -> data?.let { intent ->
                        PaymentCardRecognitionResult.getFromIntent(intent)
                            ?.let(::handlePaymentCardRecognitionSuccess)
                    }
                    RESULT_CANCELED -> {
                        // The user cancelled the scan card attempt
                    }
                }
            }
        }
    }

    private fun handlePaymentCardRecognitionSuccess(
        cardRecognitionResult: PaymentCardRecognitionResult
    ) {
        val creditCardExpirationDate = cardRecognitionResult.creditCardExpirationDate
        val expirationDate = creditCardExpirationDate?.let { "%02d/%d".format(it.month, it.year) }
        val cardResultText = "PAN: ${cardRecognitionResult.pan}\nExpiration date: $expirationDate"
        Toast.makeText(this, cardResultText, Toast.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object{
        const val PAYMENT_CARD_RECOGNITION_REQUEST_CODE: Int = 161
    }
}