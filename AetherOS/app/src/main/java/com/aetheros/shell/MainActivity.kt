package com.aetheros.shell

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("aether_preferences", MODE_PRIVATE) }
    private var activeWebView: WebView? = null
    private val recentApps = ArrayList<String>()
    private lateinit var shell: LinearLayout
    private lateinit var pageBody: LinearLayout
    private val exportRequest = 731

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(14, 22, 44)
        window.navigationBarColor = Color.rgb(14, 22, 44)
        showHome()
    }

    override fun onBackPressed() {
        val web = activeWebView
        if (web != null && web.canGoBack()) web.goBack()
        else {
            activeWebView = null
            showHome()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == exportRequest && resultCode == RESULT_OK && data?.data != null) {
            try {
                val profile = (prefs.getString("profile", "Guest") ?: "Guest").replace("\\", "\\\\").replace("\"", "\\\"")
                val notes = (prefs.getString("notes", "") ?: "").replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                val json = "{\"system\":\"Aether OS\",\"version\":\"1.0.0\",\"profile\":\"" + profile +
                    "\",\"darkMode\":" + prefs.getBoolean("dark", true) + ",\"notes\":\"" + notes + "\"}"
                contentResolver.openOutputStream(data.data!!)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                toast("Backup exported")
            } catch (_: Exception) {
                toast("Could not export backup")
            }
        }
    }

    private fun background(): GradientDrawable {
        val dark = prefs.getBoolean("dark", true)
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            if (dark) intArrayOf(Color.rgb(11, 18, 38), Color.rgb(28, 38, 72), Color.rgb(27, 49, 72))
            else intArrayOf(Color.rgb(231, 241, 255), Color.rgb(211, 228, 255), Color.rgb(232, 241, 255))
        )
    }

    private fun primaryText(): Int = if (prefs.getBoolean("dark", true)) Color.WHITE else Color.rgb(20, 30, 54)
    private fun secondaryText(): Int = if (prefs.getBoolean("dark", true)) Color.rgb(190, 205, 231) else Color.rgb(73, 89, 119)

    private fun beginPage(title: String, subtitle: String, homeButton: Boolean = true): LinearLayout {
        activeWebView = null
        shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(12))
            background = background()
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        if (homeButton) {
            val back = label("‹", 30, primaryText(), true)
            back.gravity = Gravity.CENTER
            back.setOnClickListener { showHome() }
            header.addView(back, LinearLayout.LayoutParams(dp(42), dp(46)))
        }
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(label(title, 25, primaryText(), true))
        titles.addView(label(subtitle, 12, secondaryText(), false))
        header.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        shell.addView(header)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        pageBody = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(22))
        }
        scroll.addView(pageBody)
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(shell)
        return pageBody
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(dp(2), dp(3), dp(2), dp(3))
        }
    }

    private fun addText(parent: LinearLayout, text: String, size: Int = 14, bold: Boolean = false) {
        parent.addView(label(text, size, if (bold) primaryText() else secondaryText(), bold),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7) })
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun addButton(parent: LinearLayout, text: String, action: () -> Unit) {
        val view = TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(13), dp(15), dp(13))
            background = rounded(if (prefs.getBoolean("dark", true)) Color.rgb(47, 67, 107) else Color.rgb(56, 92, 157), 17)
            setOnClickListener { action() }
        }
        parent.addView(view, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(5); bottomMargin = dp(5)
        })
    }

    private fun addCard(parent: LinearLayout, icon: String, title: String, detail: String, action: () -> Unit) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(if (prefs.getBoolean("dark", true)) Color.argb(185, 37, 53, 88) else Color.argb(220, 255, 255, 255), 19)
            setOnClickListener { action() }
        }
        card.addView(label(icon, 25, primaryText(), false))
        card.addView(label(title, 15, primaryText(), true))
        card.addView(label(detail, 11, secondaryText(), false))
        parent.addView(card, LinearLayout.LayoutParams(0, dp(112), 1f).apply {
            setMargins(dp(4), dp(4), dp(4), dp(4))
        })
    }

    private fun addGridRow(parent: LinearLayout, left: Triple<String, String, String>, right: Triple<String, String, String>,
                           leftAction: () -> Unit, rightAction: () -> Unit) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        addCard(row, left.first, left.second, left.third, leftAction)
        addCard(row, right.first, right.second, right.third, rightAction)
        parent.addView(row, LinearLayout.LayoutParams(-1, -2))
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun rememberApp(name: String) {
        recentApps.remove(name)
        recentApps.add(0, name)
        while (recentApps.size > 8) recentApps.removeAt(recentApps.lastIndex)
    }

    private fun showHome() {
        activeWebView = null
        val body = beginPage("Aether OS", "A personal mobile environment • build 1.0.0", false)
        val profile = prefs.getString("profile", "Guest") ?: "Guest"
        val date = SimpleDateFormat("EEE, d MMM  •  HH:mm", Locale.getDefault()).format(Date())
        addText(body, "Good day, " + profile + "  •  " + date, 13, true)
        addButton(body, "⌕   Search everything", { searchDialog() })
        addGridRow(body, Triple("🌐", "Browser", "Internet & Google"), Triple("▦", "Store", "Web apps & shortcuts"),
            { openApp("Browser") }, { openApp("Store") })
        addGridRow(body, Triple("🗂", "Files", "Private system files"), Triple("✎", "Notes", "Local notes"),
            { openApp("Files") }, { openApp("Notes") })
        addGridRow(body, Triple("⚙", "Settings", "Personalize Aether"), Triple("◈", "Control", "Quick control panel"),
            { openApp("Settings") }, { openApp("Control") })
        addGridRow(body, Triple("▣", "Gallery", "Choose photos"), Triple("▤", "Camera", "Open camera app"),
            { openApp("Gallery") }, { openApp("Camera") })
        addGridRow(body, Triple("＋", "Studio", "Create a mini-app"), Triple("⌗", "Calculator", "Quick calculations"),
            { openApp("Studio") }, { openApp("Calculator") })
        addGridRow(body, Triple("◷", "Clock", "Date & time"), Triple("▤", "Notifications", "Aether activity"),
            { openApp("Clock") }, { openApp("Notifications") })
        addGridRow(body, Triple("▣", "Multitasking", "Recent Aether apps"), Triple("⌑", "Lock screen", "Aether welcome screen"),
            { openApp("Multitasking") }, { openApp("Lock") })
        val shortcuts = prefs.getStringSet("shortcuts", emptySet()) ?: emptySet()
        if (shortcuts.isNotEmpty()) {
            addText(body, "YOUR WEB APPS", 12, true)
            shortcuts.sorted().forEach { entry ->
                val parts = entry.split("|||", limit = 2)
                if (parts.size == 2) addButton(body, "↗   " + parts[0], { openBrowser(parts[1]) })
            }
        }
        addText(body, "Aether runs as an app over Android. Your phone's original system stays unchanged.", 11, false)
    }

    private fun openApp(name: String) {
        rememberApp(name)
        when (name) {
            "Browser" -> openBrowser()
            "Store" -> showStore()
            "Files" -> showFiles()
            "Notes" -> showNotes()
            "Settings" -> showSettings()
            "Control" -> showControl()
            "Gallery" -> showGallery()
            "Camera" -> showCamera()
            "Studio" -> showStudio()
            "Calculator" -> showCalculator()
            "Clock" -> showClock()
            "Notifications" -> showNotifications()
            "Multitasking" -> showMultitasking()
            "Lock" -> showLock()
            else -> showHome()
        }
    }

    private fun searchDialog() {
        val input = EditText(this).apply { hint = "App, setting, or web search" }
        AlertDialog.Builder(this).setTitle("Aether Search").setView(input)
            .setPositiveButton("Search") { _, _ ->
                val q = input.text.toString().trim()
                if (q.isNotEmpty()) {
                    val key = q.lowercase(Locale.ROOT)
                    when {
                        key.contains("note") -> openApp("Notes")
                        key.contains("file") -> openApp("Files")
                        key.contains("setting") -> openApp("Settings")
                        key.contains("store") -> openApp("Store")
                        key.contains("browser") -> openApp("Browser")
                        else -> openBrowser("https://www.google.com/search?q=" + Uri.encode(q))
                    }
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun openBrowser(start: String = "https://www.google.com") {
        rememberApp("Browser")
        activeWebView = null
        shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(4))
            setBackgroundColor(Color.rgb(13, 20, 39))
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = TextView(this).apply {
            text = "‹"; textSize = 29f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setOnClickListener { showHome() }
        }
        bar.addView(back, LinearLayout.LayoutParams(dp(38), dp(48)))
        val address = EditText(this).apply {
            setSingleLine(true); textSize = 13f; setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY)
            hint = "Search or enter website"
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = rounded(Color.rgb(40, 51, 76), 18)
            setText(start)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        bar.addView(address, LinearLayout.LayoutParams(0, dp(46), 1f))
        val web = WebView(this)
        val go = TextView(this).apply {
            text = "GO"; textSize = 13f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(8), 0)
            setOnClickListener { web.loadUrl(normalizeAddress(address.text.toString())) }
        }
        bar.addView(go, LinearLayout.LayoutParams(dp(48), dp(46)))
        shell.addView(bar)
        activeWebView = web
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.cacheMode = WebSettings.LOAD_DEFAULT
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (url != null) address.setText(url)
            }
        }
        web.webChromeClient = WebChromeClient()
        shell.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(shell)
        web.loadUrl(normalizeAddress(start))
    }

    private fun normalizeAddress(value: String): String {
        val text = value.trim()
        if (text.isEmpty()) return "https://www.google.com"
        if (text.contains(" ") || (!text.contains(".") && !text.startsWith("http"))) {
            return "https://www.google.com/search?q=" + Uri.encode(text)
        }
        return if (text.startsWith("http://") || text.startsWith("https://")) text else "https://" + text
    }

    private fun showStore() {
        val body = beginPage("Aether Store", "A lightweight catalogue of web apps")
        addText(body, "These entries open websites in Aether Browser. They are not native APK installations.", 12)
        addButton(body, "＋   Create your own web app shortcut", { openApp("Studio") })
        val items = listOf(
            Triple("Google", "Search, web services", "https://www.google.com"),
            Triple("YouTube", "Video platform", "https://m.youtube.com"),
            Triple("Gmail", "Email in the browser", "https://mail.google.com"),
            Triple("Google Drive", "Cloud files", "https://drive.google.com"),
            Triple("Google Maps", "Maps in the browser", "https://maps.google.com"),
            Triple("Wikipedia", "Knowledge library", "https://www.wikipedia.org"),
            Triple("DuckDuckGo", "Private search website", "https://duckduckgo.com"),
            Triple("OpenStreetMap", "Community maps", "https://www.openstreetmap.org")
        )
        items.forEach { item -> addButton(body, "↗   " + item.first + "  •  " + item.second, { openBrowser(item.third) }) }
    }

    private fun showStudio() {
        val body = beginPage("Aether Studio", "Create a launchable web-app shortcut")
        addText(body, "This first version creates lightweight shortcuts. It does not compile or install native Android APKs.")
        val title = EditText(this).apply { hint = "App name"; setSingleLine(true) }
        val url = EditText(this).apply { hint = "Website, e.g. https://example.com"; setSingleLine(true) }
        body.addView(title); body.addView(url)
        addButton(body, "Save shortcut to home screen", {
            val name = title.text.toString().trim()
            val link = normalizeAddress(url.text.toString())
            if (name.isEmpty()) toast("Enter an app name")
            else {
                val set = (prefs.getStringSet("shortcuts", emptySet()) ?: emptySet()).toMutableSet()
                set.add(name + "|||" + link)
                prefs.edit().putStringSet("shortcuts", set).apply()
                toast("Shortcut saved")
                showHome()
            }
        })
        addButton(body, "Open browser preview", { openBrowser(normalizeAddress(url.text.toString())) })
    }

    private fun showNotes() {
        val body = beginPage("Notes", "Saved privately inside Aether OS")
        val editor = EditText(this).apply {
            minLines = 9; gravity = Gravity.TOP; textSize = 15f
            setTextColor(primaryText()); setHintTextColor(secondaryText())
            hint = "Write something..."
            setText(prefs.getString("notes", ""))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(if (prefs.getBoolean("dark", true)) Color.rgb(30, 43, 70) else Color.WHITE, 16)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        body.addView(editor, LinearLayout.LayoutParams(-1, dp(230)))
        addButton(body, "Save note", { prefs.edit().putString("notes", editor.text.toString()).apply(); toast("Note saved") })
        addButton(body, "Share note", {
            val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, editor.text.toString())
            startActivity(Intent.createChooser(intent, "Share note"))
        })
    }

    private fun filesFolder(): File {
        val dir = File(getExternalFilesDir(null) ?: filesDir, "AetherFiles")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun showFiles() {
        val body = beginPage("Aether Files", "Private app storage • no broad storage permission")
        addText(body, "Files saved here are in this app's private folder. Share them out when needed.")
        addButton(body, "＋   Create text file", { createTextFile() })
        val files = filesFolder().listFiles()?.sortedBy { it.name } ?: emptyList()
        if (files.isEmpty()) addText(body, "No files yet. Create your first text file.")
        files.forEach { file ->
            addButton(body, "▤   " + file.name + "  (" + file.length() + " bytes)", {
                AlertDialog.Builder(this).setTitle(file.name).setMessage(file.readText().take(4000))
                    .setPositiveButton("Share") { _, _ ->
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, file.readText())
                        startActivity(Intent.createChooser(send, "Share file"))
                    }.setNegativeButton("Close", null).show()
            })
        }
    }

    private fun createTextFile() {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(8), dp(12), 0) }
        val name = EditText(this).apply { hint = "File name"; setSingleLine(true) }
        val contents = EditText(this).apply { hint = "Text"; minLines = 4; gravity = Gravity.TOP }
        form.addView(name); form.addView(contents)
        AlertDialog.Builder(this).setTitle("New Aether file").setView(form)
            .setPositiveButton("Save") { _, _ ->
                try {
                    var safeName = name.text.toString().trim().ifEmpty { "note" }.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    if (!safeName.endsWith(".txt")) safeName += ".txt"
                    File(filesFolder(), safeName).writeText(contents.text.toString())
                    showFiles()
                } catch (_: Exception) { toast("File could not be saved") }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showSettings() {
        val body = beginPage("Settings", "Personalize your Aether environment")
        val dark = prefs.getBoolean("dark", true)
        addButton(body, "Appearance: " + (if (dark) "Dark" else "Light") + "  •  tap to switch", {
            prefs.edit().putBoolean("dark", !dark).apply(); showSettings()
        })
        val motion = prefs.getBoolean("motion", true)
        addButton(body, "Animations: " + (if (motion) "On" else "Reduced") + "  •  tap to switch", {
            prefs.edit().putBoolean("motion", !motion).apply(); showSettings()
        })
        addButton(body, "Profile name", { editProfile() })
        addButton(body, "Export Aether backup (JSON)", { exportBackup() })
        addButton(body, "Open Android app settings", {
            try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName))) }
            catch (_: Exception) { toast("Settings unavailable") }
        })
        addButton(body, "Open Android display settings", { launchSystem(Settings.ACTION_DISPLAY_SETTINGS) })
        addText(body, "Privacy: Aether stores notes and shortcuts locally. It asks only for internet access in this build. Other apps such as the camera and photo picker are opened through Android.", 12)
        addText(body, "Aether cannot replace Android's system settings, security model, or Google Play Services.", 12)
    }

    private fun editProfile() {
        val input = EditText(this).apply { setSingleLine(true); hint = "Your display name"; setText(prefs.getString("profile", "Guest")) }
        AlertDialog.Builder(this).setTitle("Aether profile").setView(input)
            .setPositiveButton("Save") { _, _ -> prefs.edit().putString("profile", input.text.toString().ifBlank { "Guest" }).apply(); showHome() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showControl() {
        val body = beginPage("Control Center", "Quick links and Aether-only switches")
        addText(body, "These tiles open Android's own settings where applicable; they do not secretly toggle protected phone settings.")
        addButton(body, "Wi-Fi & network settings", { launchSystem(Settings.ACTION_WIFI_SETTINGS) })
        addButton(body, "Bluetooth settings", { launchSystem(Settings.ACTION_BLUETOOTH_SETTINGS) })
        addButton(body, "Display settings", { launchSystem(Settings.ACTION_DISPLAY_SETTINGS) })
        addButton(body, "Battery settings", { launchSystem(Settings.ACTION_BATTERY_SAVER_SETTINGS) })
        addButton(body, "Aether dark appearance", {
            prefs.edit().putBoolean("dark", !prefs.getBoolean("dark", true)).apply(); showControl()
        })
        addButton(body, "Aether reduce motion", {
            prefs.edit().putBoolean("motion", !prefs.getBoolean("motion", true)).apply(); showControl()
        })
    }

    private fun launchSystem(action: String) {
        try { startActivity(Intent(action)) } catch (_: Exception) { toast("This setting is unavailable on this Android version") }
    }

    private fun showGallery() {
        val body = beginPage("Gallery", "Android photo picker")
        addText(body, "The photo picker opens only after you choose it. Aether does not request broad photo access.")
        addButton(body, "Choose a photo", {
            try {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }
                startActivityForResult(intent, 741)
            } catch (_: Exception) { toast("Photo picker unavailable") }
        })
    }

    private fun showCamera() {
        val body = beginPage("Camera", "Open your phone's camera app")
        addText(body, "Aether hands the action to your phone's camera app. Camera permission is handled by Android's camera app.")
        addButton(body, "Launch camera", {
            try { startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE)) }
            catch (_: Exception) { toast("No compatible camera app found") }
        })
    }

    private fun showCalculator() {
        val body = beginPage("Calculator", "Simple local calculator")
        val first = EditText(this).apply { hint = "First number"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED }
        val second = EditText(this).apply { hint = "Second number"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED }
        val result = label("Result appears here", 18, primaryText(), true)
        body.addView(first); body.addView(second); body.addView(result)
        fun calculate(op: String) {
            val a = first.text.toString().toDoubleOrNull()
            val b = second.text.toString().toDoubleOrNull()
            if (a == null || b == null) { result.text = "Enter two numbers"; return }
            val value = when (op) { "+" -> a + b; "−" -> a - b; "×" -> a * b; else -> if (b == 0.0) Double.NaN else a / b }
            result.text = if (value.isNaN()) "Cannot divide by zero" else "Result: " + value
        }
        listOf("+", "−", "×", "÷").forEach { op -> addButton(body, op, { calculate(op) }) }
    }

    private fun showClock() {
        val body = beginPage("Clock", "Aether local time")
        addText(body, SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()), 42, true)
        addText(body, SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()), 17, true)
        addButton(body, "Refresh time", { showClock() })
    }

    private fun showNotifications() {
        val body = beginPage("Notifications", "Aether activity centre")
        addText(body, "This is Aether's own activity area, not a mirror of Android notifications.")
        addButton(body, "System status: Aether is running", { toast("Everything is okay") })
        addButton(body, "Storage: private Aether folder is available", { showFiles() })
        addButton(body, "Browser: Google Search is available", { openBrowser() })
        addButton(body, "Clear Aether activity", { toast("Aether activity cleared") })
    }

    private fun showMultitasking() {
        val body = beginPage("Multitasking", "Recently opened Aether modules")
        if (recentApps.isEmpty()) addText(body, "No recent modules yet.")
        recentApps.toList().forEach { name -> addButton(body, "▣   " + name + "   •   Reopen", { openApp(name) }) }
        addButton(body, "Return to home", { showHome() })
    }

    private fun showLock() {
        val body = beginPage("Aether Lock Screen", "A separate welcome screen inside this app", false)
        addText(body, SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), 52, true)
        addText(body, SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()), 17, true)
        addButton(body, "Swipe / tap to unlock Aether", { showHome() })
    }

    private fun exportBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "aether-os-backup.json")
        }
        try { startActivityForResult(intent, exportRequest) } catch (_: Exception) { toast("Document picker unavailable") }
    }
}
