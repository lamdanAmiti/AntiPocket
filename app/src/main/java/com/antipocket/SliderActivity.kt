package com.antipocket

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SliderActivity : AppCompatActivity() {

    private lateinit var sliderView: VerticalSliderView
    private lateinit var prefs: PreferencesManager

    private var mode: VerticalSliderView.SliderMode = VerticalSliderView.SliderMode.CALL
    private var isRedialMode = false
    private var callPlaced = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make sure activity shows over lock screen
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        prefs = PreferencesManager.getInstance(this)
        mode = if (intent.getStringExtra(EXTRA_MODE) == MODE_UNLOCK) {
            VerticalSliderView.SliderMode.UNLOCK
        } else {
            VerticalSliderView.SliderMode.CALL
        }
        isRedialMode = intent.getBooleanExtra(EXTRA_REDIAL, false)

        createUI()
    }

    private fun createUI() {
        val container = FrameLayout(this).apply {
            setBackgroundColor(0xFFFFFFFF.toInt())
        }

        // Add instruction text at top
        val instructionText = TextView(this).apply {
            text = if (isRedialMode) {
                "Call blocked.\nSlide to confirm,\nthen redial."
            } else if (mode == VerticalSliderView.SliderMode.UNLOCK) {
                "Slide to unlock"
            } else {
                "Slide to call"
            }
            textSize = 20f
            setTextColor(0xFF000000.toInt())
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(60), dp(24), dp(24))
        }

        val textParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP
        }
        container.addView(instructionText, textParams)

        // Add slider
        sliderView = VerticalSliderView(this).apply {
            this.mode = this@SliderActivity.mode
            onSlideComplete = {
                handleSlideComplete()
            }
        }

        val sliderParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply {
            topMargin = dp(120)
            bottomMargin = dp(40)
        }
        container.addView(sliderView, sliderParams)

        setContentView(container)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun handleSlideComplete() {
        when (mode) {
            VerticalSliderView.SliderMode.CALL -> {
                if (isRedialMode) {
                    // Call was already ended, user needs to redial
                    finish()
                } else {
                    // Make the pending call
                    val number = prefs.pendingCallNumber
                    prefs.pendingCallNumber = null
                    if (!number.isNullOrBlank()) {
                        // Set bypass flag so the call goes through without being intercepted again
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            AntiPocketCallRedirectionService.shouldBypass = true
                        }
                        callPlaced = true

                        val callIntent = Intent(Intent.ACTION_CALL).apply {
                            data = Uri.parse("tel:$number")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            startActivity(callIntent)
                        } catch (e: SecurityException) {
                            // Permission denied
                            callPlaced = false
                        }
                    }
                    finish()
                }
            }
            VerticalSliderView.SliderMode.UNLOCK -> {
                // Just dismiss the unlock screen
                finish()
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Prevent back press from dismissing - must use slider
        if (mode == VerticalSliderView.SliderMode.UNLOCK) {
            // For unlock mode, allow back to cancel
            super.onBackPressed()
        }
        // For call mode, do nothing - user must slide or call is cancelled
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // User pressed home or navigated away - cancel and close
        if (!callPlaced) {
            prefs.pendingCallNumber = null
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // If call mode and call wasn't placed, clear the pending call
        if (mode == VerticalSliderView.SliderMode.CALL && !callPlaced) {
            prefs.pendingCallNumber = null
        }
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_REDIAL = "redial"
        const val MODE_CALL = "call"
        const val MODE_UNLOCK = "unlock"

        fun startForCall(context: Context) {
            val intent = Intent(context, SliderActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_CALL)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }

        fun startForUnlock(context: Context) {
            val intent = Intent(context, SliderActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_UNLOCK)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }
    }
}
