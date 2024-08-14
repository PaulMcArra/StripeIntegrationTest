package com.example.stripeintegrationtest

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.example.stripeintegrationtest.databinding.ActivityMainBinding
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.model.PaymentOption

class MainActivity : AppCompatActivity() {

    companion object {
        const val MERCHANT_NAME = "River island"
        const val PAYMENT_SHEET_BUTTON_CORNER_RADIUS = 4.0f
        const val PAYMENT_SHEET_SHAPES_CORNER_RADIUS = 12.0f
        const val PAYMENT_SHEET_SHAPES_BORDER = 0.5f
        private val BUTTON_TEXT = "Choose payment"
    }

    private val binding by viewBinding(ActivityMainBinding::inflate)
    private var isFlowControllerSetup = false

    private val flowController: PaymentSheet.FlowController by lazy {
        PaymentSheet.FlowController.create(
            this,
            ::onPaymentOption,
            ::onCreateIntent,
            ::onPaymentSheetResult
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        PaymentConfiguration.init(this, "pk_test_51KUY6UDbmNZgq1PdGvxDrZ9iNcEHDdSo3XF5KeDHN2OzvEnViQiI8aKz6sJCI0IW9kbw0VV8gzV49bUTGANcgCjc00FMJVZy0T")
        createUI()
    }

    private fun createUI() {
        binding.apply {
            setupFlowController.setOnClickListener {
                if (
                    amountInput.text?.isNotBlank().orFalse() &&
                    paymentSecretInput.text?.isNotBlank().orFalse() &&
                    stripeCustomerInput.text?.isNotBlank().orFalse() &&
                    ephemeralKeyInput.text?.isNotBlank().orFalse()
                ) {
                    binding.result.text = "setupFlowController clicked"
                    setupFlowController()
                }
            }

            selectPayment.setOnClickListener {
                if (isFlowControllerSetup) {
                    flowController.presentPaymentOptions()
                } else {
                    setResult("Configure flowController first")
                }
            }

            confirm.setOnClickListener {
                if (isFlowControllerSetup) {
                    binding.result.text = "CONFIRM clicked"
                    flowController.confirm()
                } else {
                    setResult("Configure flowController first")
                }
            }
        }
    }

    // region Stripe
    private fun setupFlowController() {
        flowController.configureWithIntentConfiguration(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = binding.amountInput.text.toString().toLong(),
                    currency = "GBP",
                ),
                paymentMethodTypes = listOf("card", "paypal", "klarna")
            ),
            configuration = getPaymentSheetConfig(this),
            callback = { success, error ->
                isFlowControllerSetup = success
                binding.result.text = if (success) "SETUP COMPLETE" else error?.message
                onPaymentOption(flowController.getPaymentOption())
            }
        )
    }

    private fun onCreateIntent(paymentMethod: PaymentMethod, shouldSavePaymentMethod: Boolean): CreateIntentResult {
        return CreateIntentResult.Success(binding.paymentSecretInput.text.toString())
    }

    private fun onPaymentSheetResult(result: PaymentSheetResult) {
        val message = when (result) {
            is PaymentSheetResult.Canceled -> "Cancelled"
            is PaymentSheetResult.Failed -> result.error.message ?: "Error"
            is PaymentSheetResult.Completed -> "Completed"
        }
        setResult(message)
    }

    private fun onPaymentOption(paymentOption: PaymentOption?) {
        binding.selectPayment.text = paymentOption?.label ?: "Select payment"
    }
    //endregion

    private fun getPaymentSheetConfig(context: Context): PaymentSheet.Configuration =
        PaymentSheet.Configuration(
            merchantDisplayName = MERCHANT_NAME,
            customer = PaymentSheet.CustomerConfiguration(
                id = binding.stripeCustomerInput.text.toString(),
                ephemeralKeySecret = binding.ephemeralKeyInput.text.toString()
            ),
            primaryButtonLabel = BUTTON_TEXT,
            googlePay = PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                countryCode = "GB",
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
            ),
            appearance = PaymentSheet.Appearance(
                primaryButton = PaymentSheet.PrimaryButton(
                    colorsLight = PaymentSheet.PrimaryButtonColors(
                        background = context.color(R.color.black),
                        onBackground = context.color(R.color.black),
                        border = context.color(R.color.basket_transactional_stroke_grey),
                        onSuccessBackgroundColor = context.color(R.color.black)
                    ),
                    shape = PaymentSheet.PrimaryButtonShape(
                        cornerRadiusDp = PAYMENT_SHEET_BUTTON_CORNER_RADIUS
                    )
                ),
                shapes = PaymentSheet.Shapes(
                    cornerRadiusDp = PAYMENT_SHEET_SHAPES_CORNER_RADIUS,
                    borderStrokeWidthDp = PAYMENT_SHEET_SHAPES_BORDER
                )
            )
        )

    private fun setResult(message: String) {
        binding.result.text = message
    }

    fun <T : ViewBinding> Activity.viewBinding(bind: (LayoutInflater) -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { bind(layoutInflater) }

    fun Context.color(@ColorRes colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

    fun Boolean?.orFalse(): Boolean = this ?: false
}