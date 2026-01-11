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
    private var createdTime = 0L

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
        initFromIntent(intent)
        createUI()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { initFromIntent(it) }
        // Reset the slider if it exists
        if (::sliderView.isInitialized) {
            sliderView.reset()
        }
    }

    private fun initFromIntent(intent: Intent) {
        // Reset state for fresh start
        callPlaced = false
        createdTime = System.currentTimeMillis()

        mode = if (intent.getStringExtra(EXTRA_MODE) == MODE_UNLOCK) {
            VerticalSliderView.SliderMode.UNLOCK
        } else {
            VerticalSliderView.SliderMode.CALL
        }
        isRedialMode = intent.getBooleanExtra(EXTRA_REDIAL, false)
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
            bottomMargin = dp(100)
        }
        container.addView(sliderView, sliderParams)

        // Add cancel button at bottom
        val cancelButton = TextView(this).apply {
            text = "[ CANCEL ]"
            textSize = 18f
            setTextColor(0xFF000000.toInt())
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(16), dp(24), dp(16))
            setOnClickListener {
                handleCancel()
            }
        }

        val cancelParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(32)
        }
        container.addView(cancelButton, cancelParams)

        setContentView(container)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun handleCancel() {
        prefs.pendingCallNumber = null
        finish()
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
        // But only if the activity has been visible for at least 500ms (to prevent race conditions)
        val timeSinceCreated = System.currentTimeMillis() - createdTime
        if (!callPlaced && timeSinceCreated > 500) {
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
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            context.startActivity(intent)
        }

        fun startForUnlock(context: Context) {
            val intent = Intent(context, SliderActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_UNLOCK)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            context.startActivity(intent)
        }
    }
}
