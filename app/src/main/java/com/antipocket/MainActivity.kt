package com.antipocket

import android.Manifest
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PreferencesManager
    private lateinit var mainContainer: LinearLayout
    private lateinit var permissionContainer: LinearLayout
    private lateinit var settingsContainer: LinearLayout

    private lateinit var secureCallsToggle: SettingToggleView
    private lateinit var onlyInPocketToggle: SettingToggleView
    private lateinit var antiPocketToggle: SettingToggleView
    private lateinit var lockInPocketToggle: SettingToggleView
    private lateinit var hideLauncherToggle: SettingToggleView
    private lateinit var statusText: TextView

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        checkAllPermissions()
    }

    private val callRedirectionRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        updateStatus()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferencesManager.getInstance(this)
        createUI()
    }

    override fun onResume() {
        super.onResume()
        val allGranted = checkAllPermissions()
        updateToggleStates()
        updateStatus()

        // Auto-request call redirection role when permissions are granted
        if (allGranted && !isCallRedirectionRoleHeld()) {
            requestCallRedirectionRole()
        }
    }

    private fun createUI() {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(0xFFFFFFFF.toInt())
        }

        mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Title with padding
        val titleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(48), dp(24), dp(16))
        }

        val title = TextView(this).apply {
            text = "Anti Pocket"
            textSize = 28f
            setTextColor(0xFF000000.toInt())
        }
        titleContainer.addView(title)
        mainContainer.addView(titleContainer)

        // Permission container (shown when permissions not granted)
        permissionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(24), 0, dp(24), dp(24))
        }

        val permissionText = TextView(this).apply {
            text = "Permissions required to continue.\nPlease grant all permissions."
            textSize = 16f
            setTextColor(0xFF000000.toInt())
            setPadding(0, 0, 0, dp(16))
        }
        permissionContainer.addView(permissionText)

        val grantButton = TextView(this).apply {
            text = "[ GRANT PERMISSIONS ]"
            textSize = 16f
            setTextColor(0xFF000000.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setOnClickListener { requestAllPermissions() }
        }
        permissionContainer.addView(grantButton)

        val batteryButton = TextView(this).apply {
            text = "[ DISABLE BATTERY OPTIMIZATION ]"
            textSize = 16f
            setTextColor(0xFF000000.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setOnClickListener { requestBatteryOptimizationExemption() }
        }
        permissionContainer.addView(batteryButton)

        mainContainer.addView(permissionContainer)

        // Settings container (shown when permissions granted)
        settingsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        // Status text with padding
        val statusContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), 0, dp(24), dp(8))
        }
        statusText = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFF000000.toInt())
        }
        statusContainer.addView(statusText)
        settingsContainer.addView(statusContainer)

        settingsContainer.addView(createDivider())

        // Secure Calls toggle
        secureCallsToggle = SettingToggleView(
            this,
            "Secure Calls",
            "Require slider confirmation before making calls"
        ) { enabled ->
            prefs.secureCallsEnabled = enabled
            if (enabled && !isCallRedirectionRoleHeld()) {
                requestCallRedirectionRole()
            }
            updateServiceState()
            updateOnlyInPocketVisibility()
        }
        settingsContainer.addView(secureCallsToggle)

        // Only when in pocket toggle (sub-setting of Secure Calls)
        onlyInPocketToggle = SettingToggleView(
            this,
            "  Only When In Pocket",
            "  Only require confirmation when phone is in pocket"
        ) { enabled ->
            prefs.onlyWhenInPocket = enabled
            updateServiceState()
        }
        onlyInPocketToggle.visibility = View.GONE
        settingsContainer.addView(onlyInPocketToggle)

        settingsContainer.addView(createDivider())

        // Anti Pocket toggle
        antiPocketToggle = SettingToggleView(
            this,
            "Anti Pocket",
            "Show unlock slider when phone enters pocket"
        ) { enabled ->
            prefs.antiPocketEnabled = enabled
            updateServiceState()
            updateLockToggleVisibility()
        }
        settingsContainer.addView(antiPocketToggle)

        // Lock when in pocket toggle (sub-setting of Anti Pocket)
        lockInPocketToggle = SettingToggleView(
            this,
            "  Lock Device",
            "  Also lock device when entering pocket (requires Accessibility Service)"
        ) { enabled ->
            if (enabled) {
                if (!CallInterceptorService.isRunning) {
                    openAccessibilitySettings()
                    lockInPocketToggle.isChecked = false
                } else {
                    prefs.lockWhenInPocket = true
                }
            } else {
                prefs.lockWhenInPocket = false
            }
        }
        lockInPocketToggle.visibility = View.GONE
        settingsContainer.addView(lockInPocketToggle)

        settingsContainer.addView(createDivider())

        // Hide launcher icon toggle
        hideLauncherToggle = SettingToggleView(
            this,
            "Hide Launcher Icon",
            "Hide app icon from launcher (dial *#*#2684#*#* to reopen)"
        ) { enabled ->
            prefs.hideLauncherIcon = enabled
            setLauncherIconVisible(!enabled)
            if (enabled) {
                Toast.makeText(this, "Dial *#*#2684#*#* to reopen", Toast.LENGTH_LONG).show()
            }
        }
        settingsContainer.addView(hideLauncherToggle)

        mainContainer.addView(settingsContainer)

        scrollView.addView(mainContainer)
        setContentView(scrollView)
    }

    private fun checkAllPermissions(): Boolean {
        val hasPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val hasCallPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        val hasOverlayPermission = Settings.canDrawOverlays(this)
        val hasBatteryExemption = isIgnoringBatteryOptimizations()

        val allGranted = hasPhonePermission && hasCallPermission && hasNotificationPermission && hasOverlayPermission && hasBatteryExemption

        if (allGranted) {
            permissionContainer.visibility = View.GONE
            settingsContainer.visibility = View.VISIBLE
        } else {
            permissionContainer.visibility = View.VISIBLE
            settingsContainer.visibility = View.GONE
        }

        return allGranted
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CALL_PHONE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ANSWER_PHONE_CALLS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestBatteryOptimizationExemption() {
        if (!isIgnoringBatteryOptimizations()) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    private fun updateStatus() {
        val hasRole = isCallRedirectionRoleHeld()
        statusText.text = if (hasRole) {
            "Status: ACTIVE"
        } else {
            "Status: INACTIVE"
        }
    }

    private fun isCallRedirectionRoleHeld(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            return roleManager.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION)
        }
        return false
    }

    private fun requestCallRedirectionRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_REDIRECTION)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_REDIRECTION)
                    callRedirectionRoleLauncher.launch(intent)
                }
            }
        }
    }

    private fun updateLockToggleVisibility() {
        lockInPocketToggle.visibility = if (prefs.antiPocketEnabled) View.VISIBLE else View.GONE
    }

    private fun updateOnlyInPocketVisibility() {
        onlyInPocketToggle.visibility = if (prefs.secureCallsEnabled) View.VISIBLE else View.GONE
    }

    private fun updateLockToggleState() {
        if (CallInterceptorService.isRunning && prefs.lockWhenInPocket) {
            lockInPocketToggle.isChecked = true
        } else if (!CallInterceptorService.isRunning) {
            // Accessibility service not running, disable lock setting
            prefs.lockWhenInPocket = false
            lockInPocketToggle.isChecked = false
        }
    }

    private fun openAccessibilitySettings() {
        Toast.makeText(this, "Enable 'Anti Pocket Call Interceptor' accessibility service", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun createDivider(): View {
        return View(this).apply {
            setBackgroundColor(0xFF000000.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun updateToggleStates() {
        secureCallsToggle.isChecked = prefs.secureCallsEnabled
        onlyInPocketToggle.isChecked = prefs.onlyWhenInPocket
        antiPocketToggle.isChecked = prefs.antiPocketEnabled
        lockInPocketToggle.isChecked = prefs.lockWhenInPocket
        hideLauncherToggle.isChecked = prefs.hideLauncherIcon
        updateLockToggleVisibility()
        updateOnlyInPocketVisibility()
    }

    private fun updateServiceState() {
        val needService = prefs.onlyWhenInPocket || prefs.antiPocketEnabled
        if (needService) {
            PocketDetectionService.start(this)
        } else {
            PocketDetectionService.stop(this)
        }
    }

    private fun setLauncherIconVisible(visible: Boolean) {
        val componentName = ComponentName(this, "$packageName.MainActivityAlias")
        val state = if (visible) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        packageManager.setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP)
    }

    inner class SettingToggleView(
        context: Context,
        title: String,
        description: String,
        private val onToggle: (Boolean) -> Unit
    ) : LinearLayout(context) {

        var isChecked: Boolean = false
            set(value) {
                field = value
                updateCheckbox()
            }

        private val checkbox: TextView

        init {
            orientation = HORIZONTAL
            setPadding(dp(24), dp(16), dp(24), dp(16))

            val textContainer = LinearLayout(context).apply {
                orientation = VERTICAL
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            }

            val titleView = TextView(context).apply {
                text = title
                textSize = 18f
                setTextColor(0xFF000000.toInt())
            }
            textContainer.addView(titleView)

            val descView = TextView(context).apply {
                text = description
                textSize = 14f
                setTextColor(0xFF000000.toInt())
                setPadding(0, dp(4), 0, 0)
            }
            textContainer.addView(descView)

            addView(textContainer)

            checkbox = TextView(context).apply {
                textSize = 24f
                setTextColor(0xFF000000.toInt())
                setPadding(dp(16), 0, 0, 0)
            }
            addView(checkbox)
            updateCheckbox()

            setOnClickListener {
                isChecked = !isChecked
                onToggle(isChecked)
            }
        }

        private fun updateCheckbox() {
            checkbox.text = if (isChecked) "[X]" else "[ ]"
        }
    }
}
