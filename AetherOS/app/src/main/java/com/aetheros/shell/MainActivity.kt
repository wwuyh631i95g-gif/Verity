package com.aetheros.shell

import android.app.Activity
import android.app.AlertDialog
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Chronometer
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("aether_preferences", MODE_PRIVATE) }
    private var activeWebView: WebView? = null
    private var runningTimer: CountDownTimer? = null
    private var stopwatch: Chronometer? = null
    private val recentApps = ArrayList<String>()
    private lateinit var shell: LinearLayout
    private lateinit var pageBody: LinearLayout
    private val exportRequest = 731
    private val importRequest = 732
    private val galleryRequest = 733
    private val vaultAlias = "aether_os_vault_v1"

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun dark(): Boolean = prefs.getString("appearance", "dark") != "light"
    private fun primaryText(): Int = if (dark()) Color.WHITE else Color.rgb(24, 35, 58)
    private fun secondaryText(): Int = if (dark()) Color.rgb(190, 207, 232) else Color.rgb(75, 91, 119)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(13, 20, 42)
        window.navigationBarColor = Color.rgb(13, 20, 42)
        showHome()
    }

    override fun onDestroy() {
        runningTimer?.cancel()
        activeWebView?.destroy()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val web = activeWebView
        if (web != null && web.canGoBack()) web.goBack()
        else {
            activeWebView = null
            showHome()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data!!
        try {
            when (requestCode) {
                exportRequest -> {
                    contentResolver.openOutputStream(uri)?.use { it.write(makeBackup().toString(2).toByteArray(Charsets.UTF_8)) }
                    toast("Aether backup exported")
                    logEvent("Backup exported")
                }
                importRequest -> {
                    val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("Empty file")
                    restoreBackup(JSONObject(json))
                    toast("Backup restored")
                    logEvent("Backup restored")
                    showHome()
                }
                galleryRequest -> {
                    val preview = beginPage("Photo viewer", "Opened through the Android system picker")
                    addText(preview, "Selected image URI:")
                    addText(preview, uri.toString(), 11)
                    addButton(preview, "Share selected photo", {
                        val send = Intent(Intent.ACTION_SEND).setType("image/*").putExtra(Intent.EXTRA_STREAM, uri)
                        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivity(Intent.createChooser(send, "Share image"))
                    })
                    addButton(preview, "Back to home", { showHome() })
                }
            }
        } catch (_: Exception) {
            toast("This file could not be read or written")
        }
    }

    private fun pageBackground(): GradientDrawable {
        val wallpaper = prefs.getString("wallpaper", "aurora") ?: "aurora"
        val colors = when (wallpaper) {
            "ocean" -> if (dark()) intArrayOf(Color.rgb(5, 28, 53), Color.rgb(0, 89, 120), Color.rgb(18, 43, 86)) else intArrayOf(Color.rgb(194, 233, 255), Color.rgb(150, 210, 238), Color.rgb(225, 244, 255))
            "sunrise" -> if (dark()) intArrayOf(Color.rgb(50, 23, 55), Color.rgb(130, 55, 75), Color.rgb(39, 40, 83)) else intArrayOf(Color.rgb(255, 218, 196), Color.rgb(255, 194, 177), Color.rgb(255, 235, 211))
            "graphite" -> if (dark()) intArrayOf(Color.rgb(12, 16, 25), Color.rgb(39, 46, 61), Color.rgb(25, 32, 45)) else intArrayOf(Color.rgb(220, 225, 235), Color.rgb(201, 211, 226), Color.rgb(241, 244, 250))
            else -> if (dark()) intArrayOf(Color.rgb(10, 18, 43), Color.rgb(43, 49, 100), Color.rgb(8, 78, 97)) else intArrayOf(Color.rgb(224, 239, 255), Color.rgb(219, 223, 255), Color.rgb(208, 245, 246))
        }
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply { cornerRadius = dp(0).toFloat() }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun glassCard(): GradientDrawable {
        val color = if (dark()) Color.argb(178, 44, 58, 91) else Color.argb(210, 255, 255, 255)
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(20).toFloat()
            setStroke(dp(1), if (dark()) Color.argb(90, 210, 230, 255) else Color.argb(150, 255, 255, 255))
        }
    }

    private fun beginPage(title: String, subtitle: String, homeButton: Boolean = true): LinearLayout {
        activeWebView = null
        shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = pageBackground()
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        if (homeButton) {
            val back = label("⌂", 23, primaryText(), true)
            back.gravity = Gravity.CENTER
            back.setOnClickListener { showHome() }
            header.addView(back, LinearLayout.LayoutParams(dp(42), dp(46)))
        }
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(label(title, 24, primaryText(), true))
        titles.addView(label(subtitle, 11, secondaryText(), false))
        header.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        shell.addView(header)
        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS }
        pageBody = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(22))
        }
        scroll.addView(pageBody)
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(shell)
        return pageBody
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size.toFloat() * prefs.getFloat("font_scale", 1.0f)
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(dp(2), dp(3), dp(2), dp(3))
        }

    private fun addText(parent: LinearLayout, text: String, size: Int = 14, bold: Boolean = false) {
        parent.addView(label(text, size, if (bold) primaryText() else secondaryText(), bold),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
    }

    private fun addButton(parent: LinearLayout, text: String, action: () -> Unit) {
        val view = TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = glassCard()
            setOnClickListener { action() }
        }
        parent.addView(view, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(4); bottomMargin = dp(4)
        })
    }

    private fun addCard(parent: LinearLayout, icon: String, title: String, detail: String, action: () -> Unit) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(11), dp(10), dp(9), dp(10))
            background = glassCard()
            setOnClickListener { action() }
        }
        card.addView(label(icon, 24, primaryText(), false))
        card.addView(label(title, 14, primaryText(), true))
        card.addView(label(detail, 10, secondaryText(), false))
        parent.addView(card, LinearLayout.LayoutParams(0, dp(106), 1f).apply {
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

    private fun logEvent(message: String) {
        val old = try { JSONArray(prefs.getString("activity_log", "[]")) } catch (_: Exception) { JSONArray() }
        val next = JSONArray()
        next.put(JSONObject().put("time", System.currentTimeMillis()).put("text", message))
        for (i in 0 until minOf(old.length(), 29)) next.put(old.getJSONObject(i))
        prefs.edit().putString("activity_log", next.toString()).apply()
    }

    private fun rememberApp(name: String) {
        recentApps.remove(name)
        recentApps.add(0, name)
        while (recentApps.size > 12) recentApps.removeAt(recentApps.lastIndex)
        val set = (prefs.getStringSet("recent", emptySet()) ?: emptySet()).toMutableSet()
        set.remove(name)
        set.add(name)
        prefs.edit().putStringSet("recent", set).apply()
        logEvent("Opened " + name)
    }

    private fun batteryText(): String {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val value = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (value in 0..100) "Battery " + value + "%" else "Battery status unavailable"
    }

    private fun showHome() {
        val body = beginPage("Aether OS", "Fusion 3.0 • Android + iPhone-inspired mobile environment", false)
        val profile = prefs.getString("profile", "Guest") ?: "Guest"
        val date = SimpleDateFormat("EEE, d MMM  •  HH:mm", Locale.getDefault()).format(Date())
        addText(body, "Welcome back, " + profile + "  •  " + date + "  •  " + batteryText(), 12, true)
        addGridRow(body, Triple("▦", "All apps", "Full app drawer"), Triple("⌘", "System hub", "Aether OS modules"),
            { showAppDrawer() }, { showSystemHub() })
        addGridRow(body, Triple("◉", "Quick panel", "System shortcuts"), Triple("▣", "Device center", "Battery, memory, storage"),
            { showQuickPanel() }, { showDeviceCenter() })
        addButton(body, "✧   Aether Now — assistant, search and actions", { showAssistant() })
        addGridRow(body, Triple("🌐", "Browser", "Internet & Google"), Triple("▦", "Store", "Apps & web shortcuts"),
            { openApp("Browser") }, { openApp("Store") })
        addGridRow(body, Triple("▤", "App Library", "Installed phone apps"), Triple("✎", "Studio", "Build mini web apps"),
            { showInstalledApps() }, { openApp("Studio") })
        addGridRow(body, Triple("▣", "Workspace", "Bubbles & split view"), Triple("◫", "Widgets", "Personal dashboard"),
            { showWorkspace() }, { showWidgets() })
        addGridRow(body, Triple("🗂", "Files", "Private storage"), Triple("✧", "Vault", "Encrypted private notes"),
            { openApp("Files") }, { showVault() })
        addGridRow(body, Triple("✎", "Notes", "Local notebook"), Triple("▦", "Planner", "Tasks & events"),
            { openApp("Notes") }, { showPlanner() })
        addGridRow(body, Triple("◈", "Control", "Quick settings"), Triple("⚙", "Settings", "Personalize Aether"),
            { showControl() }, { showSettings() })
        addGridRow(body, Triple("▧", "Photos", "System photo picker"), Triple("◉", "Camera", "Open camera app"),
            { showGallery() }, { showCamera() })
        addGridRow(body, Triple("◷", "Clock", "Timer & stopwatch"), Triple("↗", "Routines", "One-tap automations"),
            { showClock() }, { showRoutines() })
        addGridRow(body, Triple("◌", "Activity", "Aether notifications"), Triple("⌑", "Lock Screen", "Aether welcome screen"),
            { showNotifications() }, { showLock() })
        addGridRow(body, Triple("⌕", "Calculator", "Built-in calculator"), Triple("▦", "Calendar & calls", "Host phone apps"),
            { showCalculator() }, { showContactsAndComms() })
        addGridRow(body, Triple("◈", "Focus & wellbeing", "Focus sessions"), Triple("◉", "Personalization", "Icons, type, themes"),
            { showFocusCenter() }, { showPersonalization() })
        addGridRow(body, Triple("⚙", "System settings", "Full settings hub"), Triple("▣", "Security", "Privacy & app access"),
            { showSystemSettings() }, { showSecurityCenter() })
        addText(body, "Pinned web apps", 12, true)
        val apps = webApps()
        if (apps.length() == 0) addText(body, "Create your first mini-app in Studio.")
        for (i in 0 until apps.length()) {
            val item = apps.optJSONObject(i) ?: continue
            addButton(body, (item.optString("icon", "◈") + "   " + item.optString("name", "App")), { launchWebApp(item) })
        }
        addText(body, "Aether is an app-based system shell. Android remains the host operating system.", 10, false)
    }

    private fun openApp(name: String) {
        rememberApp(name)
        when (name) {
            "Browser" -> openBrowser()
            "Store" -> showStore()
            "Files" -> showFiles()
            "Notes" -> showNotes()
            "Studio" -> showStudio()
            "Control" -> showControl()
            "Settings" -> showSettings()
            "Clock" -> showClock()
            "App Drawer" -> showAppDrawer()
            "System Hub" -> showSystemHub()
            "Quick Panel" -> showQuickPanel()
            "Device Center" -> showDeviceCenter()
            "Calculator" -> showCalculator()
            "Calendar" -> showCalendarHub()
            "Contacts" -> showContactsAndComms()
            "Focus" -> showFocusCenter()
            "Personalization" -> showPersonalization()
            "Security" -> showSecurityCenter()
            "Activity" -> showNotifications()
            else -> showHome()
        }
    }

    private fun searchDialog() {
        val input = EditText(this).apply { hint = "App, setting, note, or web search" }
        AlertDialog.Builder(this).setTitle("Aether Search").setView(input)
            .setPositiveButton("Search") { _, _ -> runSearch(input.text.toString()) }
            .setNegativeButton("Cancel", null).show()
    }

    private fun runSearch(raw: String) {
        val q = raw.trim()
        if (q.isEmpty()) return
        val s = q.lowercase(Locale.ROOT)
        when {
            s.contains("note") -> showNotes()
            s.contains("file") -> showFiles()
            s.contains("setting") -> showSettings()
            s.contains("store") || s.contains("app") -> showStore()
            s.contains("photo") || s.contains("gallery") -> showGallery()
            s.contains("assistant") || s.contains("siri") -> showAssistant()
            s.contains("calculator") || s.contains("calculate") -> showCalculator()
            s.contains("app drawer") || s == "apps" -> showAppDrawer()
            s.contains("quick panel") || s.contains("control center") -> showQuickPanel()
            s.contains("device") || s.contains("battery") || s.contains("storage") -> showDeviceCenter()
            s.contains("calendar") || s.contains("event") -> showCalendarHub()
            s.contains("contact") || s.contains("phone") || s.contains("call") -> showContactsAndComms()
            s.contains("focus") || s.contains("wellbeing") -> showFocusCenter()
            s.contains("theme") || s.contains("wallpaper") || s.contains("font") -> showPersonalization()
            s.contains("security") || s.contains("privacy") -> showSecurityCenter()
            s.contains("system settings") -> showSystemSettings()
            s.contains("vault") || s.contains("password") -> showVault()
            s.contains("calendar") || s.contains("task") || s.contains("planner") -> showPlanner()
            else -> openBrowser("https://www.google.com/search?q=" + Uri.encode(q))
        }
    }

    private fun openBrowser(start: String = "https://www.google.com") {
        rememberApp("Browser")
        shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(4))
            background = pageBackground()
        }
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val back = TextView(this).apply {
            text = "⌂"; textSize = 24f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setOnClickListener { showHome() }
        }
        bar.addView(back, LinearLayout.LayoutParams(dp(40), dp(48)))
        val address = EditText(this).apply {
            setSingleLine(true); textSize = 13f; setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY)
            hint = "Search or enter website"; setPadding(dp(12), dp(6), dp(12), dp(6))
            background = rounded(Color.rgb(45, 57, 82), 19)
            setText(start)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        bar.addView(address, LinearLayout.LayoutParams(0, dp(45), 1f))
        val go = TextView(this).apply {
            text = "GO"; textSize = 13f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(dp(10), 0, dp(8), 0)
        }
        val web = WebView(this)
        go.setOnClickListener { web.loadUrl(normalizeAddress(address.text.toString())) }
        bar.addView(go, LinearLayout.LayoutParams(dp(46), dp(45)))
        shell.addView(bar)
        activeWebView = web
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.cacheMode = WebSettings.LOAD_DEFAULT
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = false
        if (Build.VERSION.SDK_INT >= 26) web.settings.safeBrowsingEnabled = true
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) { if (url != null) address.setText(url) }
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

    private fun webApps(): JSONArray = try { JSONArray(prefs.getString("web_apps", "[]")) } catch (_: Exception) { JSONArray() }

    private fun showStore() {
        val body = beginPage("Aether Store", "A curated launcher for websites and your mini-apps")
        addText(body, "The store launches web apps inside Aether. It does not install native APKs or emulate Google Play.", 12)
        val catalog = listOf(
            Triple("Google", "Search & services", "https://www.google.com"),
            Triple("YouTube", "Video platform", "https://m.youtube.com"),
            Triple("Gmail", "Email", "https://mail.google.com"),
            Triple("Google Drive", "Cloud documents", "https://drive.google.com"),
            Triple("Google Maps", "Maps", "https://maps.google.com"),
            Triple("Google Translate", "Translation", "https://translate.google.com"),
            Triple("Wikipedia", "Knowledge", "https://www.wikipedia.org"),
            Triple("OpenStreetMap", "Community maps", "https://www.openstreetmap.org"),
            Triple("Khan Academy", "Learning", "https://www.khanacademy.org"),
            Triple("Scratch", "Creative projects", "https://scratch.mit.edu")
        )
        catalog.forEach { item -> addButton(body, "↗   " + item.first + "  •  " + item.second, { openBrowser(item.third) }) }
        addText(body, "Your creations", 13, true)
        val apps = webApps()
        if (apps.length() == 0) addText(body, "No mini-apps yet. Create one in Aether Studio.")
        for (i in 0 until apps.length()) {
            val item = apps.optJSONObject(i) ?: continue
            addButton(body, item.optString("icon", "◈") + "   " + item.optString("name", "App"), { launchWebApp(item) })
        }
        addButton(body, "＋   Create a mini-app", { showStudio() })
    }

    private fun showStudio() {
        val body = beginPage("Aether Studio", "Create small offline HTML apps or website shortcuts")
        addText(body, "Apps made here are stored inside this APK's private app data. HTML mini-apps work offline after saving.")
        val name = EditText(this).apply { hint = "App name"; setSingleLine(true) }
        val icon = EditText(this).apply { hint = "Icon emoji (optional)"; setSingleLine(true) }
        val url = EditText(this).apply { hint = "Website URL (leave empty for HTML app)"; setSingleLine(true) }
        val html = EditText(this).apply {
            hint = "Optional HTML code for an offline mini-app"; minLines = 7; gravity = Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        body.addView(name); body.addView(icon); body.addView(url)
        body.addView(html, LinearLayout.LayoutParams(-1, dp(160)))
        addButton(body, "Save mini-app", {
            val title = name.text.toString().trim()
            val link = url.text.toString().trim()
            val code = html.text.toString().trim()
            if (title.isEmpty() || (link.isEmpty() && code.isEmpty())) {
                toast("Enter a name and either a website or HTML code")
            } else {
                val item = JSONObject()
                    .put("id", System.currentTimeMillis().toString())
                    .put("name", title)
                    .put("icon", icon.text.toString().ifBlank { "◈" })
                    .put("url", if (link.isNotEmpty()) normalizeAddress(link) else "")
                    .put("html", code)
                val apps = webApps()
                apps.put(item)
                prefs.edit().putString("web_apps", apps.toString()).apply()
                logEvent("Created mini-app: " + title)
                toast("Saved inside Aether")
                showHome()
            }
        })
        addText(body, "Security note: mini-app HTML runs in a WebView without a native-code bridge. Do not enter passwords into pages you do not trust.")
    }

    private fun launchWebApp(item: JSONObject) {
        val name = item.optString("name", "Mini-app")
        rememberApp(name)
        val html = item.optString("html", "")
        if (html.isNotBlank()) {
            val web = WebView(this)
            activeWebView = web
            web.settings.javaScriptEnabled = true
            web.settings.domStorageEnabled = true
            web.settings.allowFileAccess = false
            web.settings.allowContentAccess = false
            val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
            addButton(page, "⌂   Back to Aether", { showHome() })
            page.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
            setContentView(page)
            web.loadDataWithBaseURL("https://aether.local/", html, "text/html", "UTF-8", null)
        } else {
            openBrowser(item.optString("url", "https://www.google.com"))
        }
    }

    private fun showInstalledApps() {
        val body = beginPage("App Library", "Open apps already installed on your Honor phone")
        addText(body, "Aether does not copy other apps into a second Android. It can launch apps installed in the host Android.")
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val resolved = packageManager.queryIntentActivities(launcher, 0)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.ROOT) }
        if (resolved.isEmpty()) addText(body, "No launchable apps were returned by Android.")
        resolved.forEach { info ->
            val title = info.loadLabel(packageManager).toString()
            addButton(body, "▣   " + title, { launchHostApp(info.activityInfo.packageName) })
        }
    }

    private fun launchHostApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
                startActivity(intent)
            } else toast("This app has no launch screen")
        } catch (_: Exception) { toast("Could not open this app") }
    }

    private fun showWorkspace() {
        val body = beginPage("Workspace", "Android 17-inspired in-app bubbles and split workspace")
        addText(body, "Bubbles stay inside Aether so no overlay permission is needed. External apps can be asked to open side-by-side, but Android decides whether split-screen is available.")
        val names = listOf("Browser", "Notes", "Files", "Store", "Assistant", "Planner", "Settings", "Vault")
        names.forEach { name ->
            addButton(body, "◉   " + name + "   •   Open bubble", {
                val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10)); background = glassCard() }
                addText(panel, name + " bubble", 16, true)
                addText(panel, "This is an in-app floating task card. It does not draw over other apps.")
                addButton(panel, "Open " + name, { openWorkspaceModule(name) })
                addButton(panel, "Return to workspace", { showWorkspace() })
                setContentView(panel)
            })
        }
        addButton(body, "▥   Open split browser workspace", { showSplitView() })
        addButton(body, "▤   Open Android app library", { showInstalledApps() })
    }

    private fun openWorkspaceModule(name: String) {
        when (name) {
            "Browser" -> openBrowser()
            "Notes" -> showNotes()
            "Files" -> showFiles()
            "Store" -> showStore()
            "Assistant" -> showAssistant()
            "Planner" -> showPlanner()
            "Settings" -> showSettings()
            "Vault" -> showVault()
            else -> showHome()
        }
    }

    private fun showSplitView() {
        activeWebView = null
        shell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(8), dp(8), dp(8)); background = pageBackground() }
        addButton(shell, "⌂   Return to Aether home", { showHome() })
        val firstUrl = EditText(this).apply { hint = "Top pane URL"; setSingleLine(true); setText("https://www.google.com") }
        val secondUrl = EditText(this).apply { hint = "Bottom pane URL"; setSingleLine(true); setText("https://www.wikipedia.org") }
        shell.addView(firstUrl); shell.addView(secondUrl)
        val panes = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        shell.addView(panes, LinearLayout.LayoutParams(-1, 0, 1f))
        addButton(shell, "Load both pages", {
            panes.removeAllViews()
            listOf(firstUrl.text.toString(), secondUrl.text.toString()).forEach { address ->
                val web = WebView(this)
                web.settings.javaScriptEnabled = true
                web.settings.domStorageEnabled = true
                web.settings.allowFileAccess = false
                web.settings.allowContentAccess = false
                web.webViewClient = WebViewClient()
                web.webChromeClient = WebChromeClient()
                web.loadUrl(normalizeAddress(address))
                panes.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
            }
        })
        setContentView(shell)
    }

    private fun showWidgets() {
        val body = beginPage("Aether Widgets", "A customizable dashboard inside your system shell")
        addText(body, "Live information cards are calculated locally. They do not require widgets or overlay permissions.")
        val date = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
        addButton(body, "◷   " + SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()) + "   •   " + date, { showClock() })
        addButton(body, "▣   " + batteryText(), { showControl() })
        addButton(body, "✎   Notes preview: " + (prefs.getString("notes", "") ?: "").take(85).ifBlank { "No saved note" }, { showNotes() })
        addButton(body, "▦   " + webApps().length() + " mini-apps saved", { showStore() })
        addButton(body, "◌   Profile: " + (prefs.getString("profile", "Guest") ?: "Guest"), { showSettings() })
        addButton(body, "＋   Pin web app to dashboard", { showStudio() })
    }

    private fun showAssistant() {
        val body = beginPage("Aether Now", "Offline command assistant • private on this device")
        addText(body, "Try: open browser, open notes, search cats, switch to light mode, show battery, open planner, or help.")
        val conversation = label(prefs.getString("assistant_log", "Aether: Hello! I can open modules and run simple local actions.") ?: "", 13, primaryText(), false)
        conversation.setPadding(dp(12), dp(12), dp(12), dp(12))
        conversation.background = glassCard()
        body.addView(conversation, LinearLayout.LayoutParams(-1, -2))
        val input = EditText(this).apply { hint = "Tell Aether what to do"; setSingleLine(true) }
        body.addView(input)
        addButton(body, "Send command", {
            val command = input.text.toString().trim()
            if (command.isNotEmpty()) {
                val answer = runAssistantCommand(command)
                val updated = (prefs.getString("assistant_log", "") ?: "") + "\n\nYou: " + command + "\nAether: " + answer
                prefs.edit().putString("assistant_log", updated.takeLast(3500)).apply()
                conversation.text = updated.takeLast(3500)
                input.setText("")
            }
        })
        addButton(body, "Clear assistant history", { prefs.edit().remove("assistant_log").apply(); showAssistant() })
        addText(body, "This is a local command assistant, not a cloud AI model. It does not read your other apps, messages, photos, or personal accounts.")
    }

    private fun runAssistantCommand(command: String): String {
        val q = command.lowercase(Locale.ROOT)
        return when {
            q.contains("battery") -> batteryText()
            q.contains("light") -> { prefs.edit().putString("appearance", "light").apply(); "Switched Aether to light appearance." }
            q.contains("dark") -> { prefs.edit().putString("appearance", "dark").apply(); "Switched Aether to dark appearance." }
            q.contains("browser") -> { openBrowser(); "Opening the browser." }
            q.contains("note") -> { showNotes(); "Opening your private notes." }
            q.contains("file") -> { showFiles(); "Opening Aether Files." }
            q.contains("planner") || q.contains("task") || q.contains("calendar") -> { showPlanner(); "Opening your planner." }
            q.contains("store") -> { showStore(); "Opening Aether Store." }
            q.contains("vault") || q.contains("password") -> { showVault(); "Opening your encrypted vault." }
            q.contains("settings") -> { showSettings(); "Opening settings." }
            q.contains("help") -> "I can open Browser, Notes, Files, Store, Settings, Planner and Vault; search the web; show battery status; or switch Aether's appearance."
            q.startsWith("search ") -> { openBrowser("https://www.google.com/search?q=" + Uri.encode(command.substringAfter("search "))); "Searching the web." }
            q.startsWith("open ") -> {
                val target = q.substringAfter("open ").trim()
                when {
                    target.contains("photo") || target.contains("gallery") -> { showGallery(); "Opening the photo picker." }
                    target.contains("camera") -> { showCamera(); "Opening the camera app." }
                    target.contains("clock") || target.contains("timer") -> { showClock(); "Opening Clock." }
                    target.contains("assistant") -> { showAssistant(); "Here I am." }
                    target.contains("app") -> { showInstalledApps(); "Opening the app library." }
                    else -> { runSearch(target); "Opening the closest matching Aether module." }
                }
            }
            else -> "I understood this as a note for later, but this offline assistant only performs local commands. Try 'help' for examples."
        }
    }

    private fun showNotes() {
        val body = beginPage("Notes", "Local notebook • stored privately on this phone")
        val editor = EditText(this).apply {
            minLines = 10; gravity = Gravity.TOP; textSize = 15f
            setTextColor(primaryText()); setHintTextColor(secondaryText())
            hint = "Write something..."
            setText(prefs.getString("notes", ""))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = glassCard()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        body.addView(editor, LinearLayout.LayoutParams(-1, dp(250)))
        addButton(body, "Save note", { prefs.edit().putString("notes", editor.text.toString()).apply(); logEvent("Note saved"); toast("Note saved") })
        addButton(body, "Copy note", {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Aether note", editor.text.toString()))
            toast("Copied")
        })
        addButton(body, "Share note", {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, editor.text.toString())
            startActivity(Intent.createChooser(send, "Share note"))
        })
    }

    private fun filesFolder(): File {
        val dir = File(filesDir, "AetherFiles")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun showFiles() {
        val body = beginPage("Aether Files", "Private app storage • no broad storage permission")
        addText(body, "These files are in Aether's sandbox. Export or share a file when you choose.")
        addButton(body, "＋   Create text file", { createTextFile() })
        val files = filesFolder().listFiles()?.sortedBy { it.name } ?: emptyList()
        if (files.isEmpty()) addText(body, "No files yet.")
        files.forEach { file ->
            addButton(body, "▤   " + file.name + "  •  " + file.length() + " bytes", {
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
                    logEvent("Created file " + safeName)
                    showFiles()
                } catch (_: Exception) { toast("File could not be saved") }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showPlanner() {
        val body = beginPage("Planner", "Private tasks and reminders inside Aether")
        val events = try { JSONArray(prefs.getString("planner_items", "[]")) } catch (_: Exception) { JSONArray() }
        addButton(body, "＋   Add a task or event", { addPlannerItem() })
        if (events.length() == 0) addText(body, "Your planner is empty.")
        for (i in events.length() - 1 downTo 0) {
            val item = events.optJSONObject(i) ?: continue
            val done = item.optBoolean("done", false)
            addButton(body, (if (done) "✓  " else "○  ") + item.optString("title", "Event") + "  •  " + item.optString("date", "No date"), {
                item.put("done", !done)
                events.put(i, item)
                prefs.edit().putString("planner_items", events.toString()).apply()
                showPlanner()
            })
        }
        addText(body, "Planner entries are local to Aether and do not sync with Google Calendar or send background alerts.")
    }

    private fun addPlannerItem() {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), dp(6), dp(10), 0) }
        val title = EditText(this).apply { hint = "Task or event"; setSingleLine(true) }
        val date = EditText(this).apply { hint = "Date / time (optional)"; setSingleLine(true) }
        form.addView(title); form.addView(date)
        AlertDialog.Builder(this).setTitle("New planner item").setView(form)
            .setPositiveButton("Save") { _, _ ->
                if (title.text.isNotBlank()) {
                    val events = try { JSONArray(prefs.getString("planner_items", "[]")) } catch (_: Exception) { JSONArray() }
                    events.put(JSONObject().put("title", title.text.toString()).put("date", date.text.toString()).put("done", false))
                    prefs.edit().putString("planner_items", events.toString()).apply()
                    logEvent("Added planner item")
                    showPlanner()
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showRoutines() {
        val body = beginPage("Routines", "One-tap local automations")
        addText(body, "Routines change Aether's own appearance or open a module. They do not silently change protected Android settings.")
        val routines = listOf(
            Triple("Focus", "Reduce visual distractions", "focus"),
            Triple("Night", "Dark appearance and notes", "night"),
            Triple("Creative", "Open Studio", "studio"),
            Triple("Research", "Open browser and planner", "research")
        )
        routines.forEach { r -> addButton(body, "↗   " + r.first + "  •  " + r.second, { runRoutine(r.third) }) }
        addButton(body, "＋   Create a custom routine", { createRoutine() })
        val custom = prefs.getStringSet("custom_routines", emptySet()) ?: emptySet()
        custom.forEach { entry ->
            val parts = entry.split("|||", limit = 2)
            if (parts.size == 2) addButton(body, "◇   " + parts[0] + "   •   Run", { runRoutine(parts[1]) })
        }
    }

    private fun createRoutine() {
        val title = EditText(this).apply { hint = "Routine name"; setSingleLine(true) }
        val actions = arrayOf("Open Browser", "Open Notes", "Open Planner", "Dark appearance", "Light appearance")
        AlertDialog.Builder(this).setTitle("Choose routine action").setItems(actions) { _, which ->
            val action = when (which) { 0 -> "browser"; 1 -> "notes"; 2 -> "planner"; 3 -> "dark"; else -> "light" }
            AlertDialog.Builder(this).setTitle("Name this routine").setView(title)
                .setPositiveButton("Save") { _, _ ->
                    val set = (prefs.getStringSet("custom_routines", emptySet()) ?: emptySet()).toMutableSet()
                    set.add(title.text.toString().ifBlank { "My routine" } + "|||" + action)
                    prefs.edit().putStringSet("custom_routines", set).apply()
                    showRoutines()
                }.setNegativeButton("Cancel", null).show()
        }.setNegativeButton("Cancel", null).show()
    }

    private fun runRoutine(action: String) {
        when (action) {
            "focus" -> { prefs.edit().putString("appearance", "graphite").putBoolean("reduce_motion", true).apply(); showHome() }
            "night", "dark" -> { prefs.edit().putString("appearance", "dark").apply(); showNotes() }
            "studio" -> showStudio()
            "research", "browser" -> openBrowser()
            "notes" -> showNotes()
            "planner" -> showPlanner()
            "light" -> { prefs.edit().putString("appearance", "light").apply(); showHome() }
            else -> showHome()
        }
        logEvent("Routine executed: " + action)
    }

    private fun showVault() {
        val body = beginPage("Aether Vault", "Encrypted private entries • Android Keystore")
        addText(body, "Vault contents are encrypted on this device using a key stored in Android Keystore. Clearing Aether data can make the vault unrecoverable. Vault data is excluded from portable backups.")
        addButton(body, "＋   Save a private entry", { createVaultEntry() })
        try {
            val raw = prefs.getString("vault_blob", "") ?: ""
            if (raw.isBlank()) addText(body, "Your vault is empty.")
            else {
                val entries = JSONArray(decryptVault(raw))
                for (i in 0 until entries.length()) {
                    val item = entries.optJSONObject(i) ?: continue
                    addButton(body, "🔒   " + item.optString("title", "Private entry"), {
                        AlertDialog.Builder(this).setTitle(item.optString("title", "Private entry"))
                            .setMessage(item.optString("secret", ""))
                            .setPositiveButton("Copy") { _, _ ->
                                val clip = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                clip.setPrimaryClip(ClipData.newPlainText("Aether Vault", item.optString("secret", "")))
                                toast("Copied to clipboard")
                            }.setNegativeButton("Close", null).show()
                    })
                }
            }
        } catch (_: Exception) {
            addText(body, "Vault data could not be decrypted. It may have been cleared or restored to another phone.")
        }
    }

    private fun createVaultEntry() {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(8), dp(12), 0) }
        val title = EditText(this).apply { hint = "Entry title"; setSingleLine(true) }
        val secret = EditText(this).apply { hint = "Private text"; minLines = 3; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE }
        form.addView(title); form.addView(secret)
        AlertDialog.Builder(this).setTitle("New encrypted entry").setView(form)
            .setPositiveButton("Encrypt and save") { _, _ ->
                try {
                    val entries = if ((prefs.getString("vault_blob", "") ?: "").isNotBlank()) JSONArray(decryptVault(prefs.getString("vault_blob", "")!!)) else JSONArray()
                    entries.put(JSONObject().put("title", title.text.toString().ifBlank { "Private entry" }).put("secret", secret.text.toString()))
                    prefs.edit().putString("vault_blob", encryptVault(entries.toString())).apply()
                    logEvent("Saved encrypted vault entry")
                    showVault()
                } catch (_: Exception) { toast("Could not encrypt this entry") }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun vaultKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (!keyStore.containsAlias(vaultAlias)) {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                vaultAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build()
            generator.init(spec)
            generator.generateKey()
        }
        return keyStore.getKey(vaultAlias, null) as SecretKey
    }

    private fun encryptVault(text: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, vaultKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    private fun decryptVault(blob: String): String {
        val data = Base64.decode(blob, Base64.NO_WRAP)
        val iv = data.copyOfRange(0, 12)
        val encrypted = data.copyOfRange(12, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, vaultKey(), javax.crypto.spec.GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    private fun showSettings() {
        val body = beginPage("Settings", "Unified Android flexibility and iPhone-inspired personalization")
        addButton(body, "Appearance & Liquid Glass styles", { showThemeStudio() })
        addButton(body, "Profile name", { editProfile() })
        addButton(body, "Export full Aether backup (JSON)", { exportBackup() })
        addButton(body, "Import Aether backup (JSON)", { importBackup() })
        addButton(body, "App permissions and battery settings", {
            try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName))) }
            catch (_: Exception) { toast("Settings unavailable") }
        })
        addButton(body, "Open Android display settings", { launchSystem(Settings.ACTION_DISPLAY_SETTINGS) })
        addButton(body, "Open Android privacy settings", { launchSystem(Settings.ACTION_PRIVACY_SETTINGS) })
        addButton(body, "Reset Aether appearance", {
            prefs.edit().putString("appearance", "dark").putString("wallpaper", "aurora").apply()
            showSettings()
        })
        addText(body, "Aether needs INTERNET for browsing. Photos, camera, contacts, microphone, location and notifications are not requested by this build. The host Android controls protected settings.", 12)
    }

    private fun editProfile() {
        val input = EditText(this).apply { setSingleLine(true); hint = "Your display name"; setText(prefs.getString("profile", "Guest")) }
        AlertDialog.Builder(this).setTitle("Aether profile").setView(input)
            .setPositiveButton("Save") { _, _ -> prefs.edit().putString("profile", input.text.toString().ifBlank { "Guest" }).apply(); logEvent("Profile updated"); showHome() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showThemeStudio() {
        val body = beginPage("Theme Studio", "Clear glass, tinted glass, dark and light")
        addText(body, "Choose the overall tone and wallpaper for the Aether interface.")
        addButton(body, "◐   Dark glass", { prefs.edit().putString("appearance", "dark").apply(); showThemeStudio() })
        addButton(body, "◯   Light glass", { prefs.edit().putString("appearance", "light").apply(); showThemeStudio() })
        addButton(body, "◈   Graphite focus", { prefs.edit().putString("appearance", "graphite").apply(); showThemeStudio() })
        addText(body, "Wallpapers", 13, true)
        addButton(body, "✦   Aurora", { prefs.edit().putString("wallpaper", "aurora").apply(); showThemeStudio() })
        addButton(body, "🌊   Ocean", { prefs.edit().putString("wallpaper", "ocean").apply(); showThemeStudio() })
        addButton(body, "🌅   Sunrise", { prefs.edit().putString("wallpaper", "sunrise").apply(); showThemeStudio() })
        addButton(body, "▧   Graphite", { prefs.edit().putString("wallpaper", "graphite").apply(); showThemeStudio() })
        addButton(body, "Reduce motion", { prefs.edit().putBoolean("reduce_motion", !prefs.getBoolean("reduce_motion", false)).apply(); toast("Motion preference saved") })
    }

    private fun showControl() {
        val body = beginPage("Control Center", "Quick actions and safe links to Android settings")
        addText(body, "Aether controls its own theme and shortcuts. Wi-Fi, Bluetooth, cellular, brightness and system DND are controlled by Android.")
        addButton(body, "Wi-Fi & network settings", { launchSystem(Settings.ACTION_WIFI_SETTINGS) })
        addButton(body, "Bluetooth settings", { launchSystem(Settings.ACTION_BLUETOOTH_SETTINGS) })
        addButton(body, "Display & brightness settings", { launchSystem(Settings.ACTION_DISPLAY_SETTINGS) })
        addButton(body, "Battery settings", { launchSystem(Settings.ACTION_BATTERY_SAVER_SETTINGS) })
        addButton(body, "Privacy dashboard", { launchSystem(Settings.ACTION_PRIVACY_SETTINGS) })
        addButton(body, "Switch Aether appearance", {
            prefs.edit().putString("appearance", if (dark()) "light" else "dark").apply()
            showControl()
        })
        addButton(body, "Open Android app information", {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName)))
        })
    }

    private fun launchSystem(action: String) {
        try { startActivity(Intent(action)) } catch (_: Exception) { toast("This setting is unavailable on this Android version") }
    }

    private fun showGallery() {
        val body = beginPage("Photos", "System photo picker • no broad media permission")
        addText(body, "Select a photo using Android's system picker. Aether only receives access to the file you select.")
        addButton(body, "Choose a photo", {
            try {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }
                startActivityForResult(intent, galleryRequest)
            } catch (_: Exception) { toast("Photo picker unavailable") }
        })
    }

    private fun showCamera() {
        val body = beginPage("Camera", "Handed off to your phone's camera application")
        addText(body, "Aether does not request camera access. Android's camera app handles its own permission.")
        addButton(body, "Launch camera", {
            try { startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE)) }
            catch (_: Exception) { toast("No compatible camera app found") }
        })
    }

    private fun showClock() {
        val body = beginPage("Clock", "Time, stopwatch and countdown timer")
        addText(body, SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()), 40, true)
        addText(body, SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()), 16, true)
        addButton(body, "↻   Refresh clock", { showClock() })
        addText(body, "Stopwatch", 14, true)
        val chrono = Chronometer(this).apply { textSize = 24f; setTextColor(primaryText()); base = android.os.SystemClock.elapsedRealtime() }
        stopwatch = chrono
        body.addView(chrono)
        addButton(body, "Start stopwatch", { chrono.base = android.os.SystemClock.elapsedRealtime(); chrono.start() })
        addButton(body, "Stop stopwatch", { chrono.stop() })
        addButton(body, "Reset stopwatch", { chrono.stop(); chrono.base = android.os.SystemClock.elapsedRealtime() })
        addText(body, "Countdown", 14, true)
        val seconds = EditText(this).apply { hint = "Seconds (for example 60)"; inputType = InputType.TYPE_CLASS_NUMBER; setSingleLine(true) }
        val status = label("Timer is ready", 16, primaryText(), true)
        body.addView(seconds); body.addView(status)
        addButton(body, "Start countdown", {
            val amount = seconds.text.toString().toLongOrNull()
            if (amount == null || amount !in 1..86400) toast("Enter 1 to 86400 seconds")
            else {
                runningTimer?.cancel()
                runningTimer = object : CountDownTimer(amount * 1000, 1000) {
                    override fun onTick(millisUntilFinished: Long) { status.text = "Time left: " + (millisUntilFinished / 1000) + " sec" }
                    override fun onFinish() { status.text = "Timer finished"; logEvent("Countdown finished"); toast("Aether timer finished") }
                }.start()
            }
        })
        addButton(body, "Cancel countdown", { runningTimer?.cancel(); status.text = "Timer cancelled" })
    }

    private fun showNotifications() {
        val body = beginPage("Activity Center", "Aether's own activity timeline")
        addText(body, "For privacy, this is not a mirror of Android notifications. It records actions performed inside Aether.")
        addButton(body, "Clear Aether activity", { prefs.edit().putString("activity_log", "[]").apply(); showNotifications() })
        val events = try { JSONArray(prefs.getString("activity_log", "[]")) } catch (_: Exception) { JSONArray() }
        if (events.length() == 0) addText(body, "No recent activity.")
        for (i in 0 until events.length()) {
            val item = events.optJSONObject(i) ?: continue
            val time = SimpleDateFormat("HH:mm  •  d MMM", Locale.getDefault()).format(Date(item.optLong("time")))
            addText(body, time + "   " + item.optString("text"), 12)
        }
    }

    private fun showLock() {
        val body = beginPage("Aether Lock Screen", "A welcome screen inside the Aether app", false)
        addText(body, SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), 56, true)
        addText(body, SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()), 17, true)
        addText(body, "Adaptive time layout inspired by modern mobile lock screens.")
        addButton(body, "Swipe / tap to unlock Aether", { showHome() })
    }

    private fun showPrivacy() {
        val body = beginPage("Privacy & Security", "Review what Aether can access")
        addText(body, "Permissions declared by this build:", 13, true)
        addText(body, "• INTERNET — required for browser pages and online web apps.")
        addText(body, "Not requested: camera, microphone, location, contacts, SMS, notifications, call logs, broad photo access, overlay, accessibility, or package-wide app visibility.")
        addButton(body, "Open Android permission manager for Aether", {
            try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName))) }
            catch (_: Exception) { toast("Settings unavailable") }
        })
        addButton(body, "Open Android privacy dashboard", { launchSystem(Settings.ACTION_PRIVACY_SETTINGS) })
        addButton(body, "Encrypted private vault", { showVault() })
        addText(body, "Vault entries use AES-GCM encryption with an Android Keystore key. The key is device-local and is not exported.")
    }

    private fun exportBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "aether-os-2-backup.json")
        }
        try { startActivityForResult(intent, exportRequest) } catch (_: Exception) { toast("Document picker unavailable") }
    }

    private fun importBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"
        }
        try { startActivityForResult(intent, importRequest) } catch (_: Exception) { toast("Document picker unavailable") }
    }

    private fun makeBackup(): JSONObject {
        return JSONObject()
            .put("system", "Aether OS")
            .put("version", "2.0.0")
            .put("profile", prefs.getString("profile", "Guest"))
            .put("appearance", prefs.getString("appearance", "dark"))
            .put("wallpaper", prefs.getString("wallpaper", "aurora"))
            .put("notes", prefs.getString("notes", ""))
            .put("web_apps", prefs.getString("web_apps", "[]"))
            .put("planner_items", prefs.getString("planner_items", "[]"))
            .put("custom_routines", JSONArray((prefs.getStringSet("custom_routines", emptySet()) ?: emptySet()).toList()))
    }

    private fun restoreBackup(json: JSONObject) {
        val editor = prefs.edit()
        editor.putString("profile", json.optString("profile", "Guest"))
        editor.putString("appearance", json.optString("appearance", "dark"))
        editor.putString("wallpaper", json.optString("wallpaper", "aurora"))
        editor.putString("notes", json.optString("notes", ""))
        editor.putString("web_apps", json.optString("web_apps", "[]"))
        editor.putString("planner_items", json.optString("planner_items", "[]"))
        val routines = json.optJSONArray("custom_routines") ?: JSONArray()
        val set = mutableSetOf<String>()
        for (i in 0 until routines.length()) set.add(routines.optString(i))
        editor.putStringSet("custom_routines", set)
        editor.apply()
    }

    // ---------------------------------------------------------------------
    // Fusion 3.0 system layer: launcher, device center, system hand-offs,
    // built-in tools, personalization and communication shortcuts.
    // These modules run inside Android and never pretend to control protected
    // host settings that Android keeps under system authority.
    // ---------------------------------------------------------------------

    private fun showSystemHub() {
        val body = beginPage("Aether System Hub", "The central control room of the Aether environment")
        addText(body, "Aether brings its own home, app drawer, apps, workspace and personal data together. Android remains the host underneath.")
        addGridRow(body, Triple("▦", "App drawer", "All installed apps"), Triple("◉", "Quick panel", "Settings shortcuts"),
            { showAppDrawer() }, { showQuickPanel() })
        addGridRow(body, Triple("▣", "Device center", "Hardware status"), Triple("◈", "Personalization", "Appearance and layout"),
            { showDeviceCenter() }, { showPersonalization() })
        addGridRow(body, Triple("⌕", "Search", "Apps, modules, web"), Triple("⚙", "Settings", "Android + Aether"),
            { showSearchHub() }, { showSystemSettings() })
        addGridRow(body, Triple("▤", "App manager", "Open, inspect, remove"), Triple("⌑", "Privacy", "Permission controls"),
            { showAppManagement() }, { showSecurityCenter() })
        addGridRow(body, Triple("◷", "Focus", "Focus sessions"), Triple("▦", "Digital wellbeing", "Aether activity"),
            { showFocusCenter() }, { showDigitalWellbeing() })
        addGridRow(body, Triple("✎", "Studio", "Create mini-apps"), Triple("▣", "Backup", "Export and restore"),
            { showStudio() }, { showBackupCenter() })
        addText(body, "Included design families: Android home/app drawer, widgets, quick settings, share sheet, default-app links, privacy controls, app details and multi-window hand-offs; iPhone-style glass, focus, spotlight-like search, app library, lock-screen styling and compact control panels.")
    }

    private fun showAppDrawer(filterText: String = "") {
        val body = beginPage("All apps", "Aether App Library • searchable host apps + built-in modules")
        val search = EditText(this).apply {
            hint = "Search installed apps"; setSingleLine(true); setText(filterText)
        }
        body.addView(search, LinearLayout.LayoutParams(-1, -2))
        addButton(body, "Search apps", { showAppDrawer(search.text.toString().trim()) })
        addText(body, "Aether built-in apps", 13, true)
        val builtins = listOf(
            "Browser" to "🌐", "Store" to "▦", "Studio" to "✎", "Notes" to "▤",
            "Files" to "🗂", "Vault" to "🔒", "Planner" to "▦", "Clock" to "◷",
            "Calculator" to "⌕", "Calendar" to "▣", "Contacts" to "◉", "Focus" to "◈",
            "Settings" to "⚙", "Photos" to "▧", "Camera" to "◉", "Activity" to "◌"
        )
        val q = filterText.lowercase(Locale.ROOT)
        builtins.filter { it.first.lowercase(Locale.ROOT).contains(q) }.forEach { item ->
            addButton(body, item.second + "   " + item.first, {
                when (item.first) {
                    "Browser", "Store", "Studio", "Notes", "Files", "Vault", "Planner", "Clock", "Settings", "Photos", "Camera", "Activity" -> openApp(item.first)
                    "Calculator" -> showCalculator()
                    "Calendar" -> showCalendarHub()
                    "Contacts" -> showContactsAndComms()
                    "Focus" -> showFocusCenter()
                }
            })
        }
        addText(body, "Installed Android apps", 13, true)
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val apps = packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.ROOT) }
        val favorites = (prefs.getStringSet("favorite_packages", emptySet()) ?: emptySet()).toMutableSet()
        val shown = apps.filter {
            it.loadLabel(packageManager).toString().lowercase(Locale.ROOT).contains(q) ||
                it.activityInfo.packageName.lowercase(Locale.ROOT).contains(q)
        }
        if (shown.isEmpty()) addText(body, "No installed app matches this search.")
        shown.forEach { info ->
            val pkg = info.activityInfo.packageName
            val title = info.loadLabel(packageManager).toString()
            addButton(body, (if (favorites.contains(pkg)) "★  " else "☆  ") + title + "   ·   Open", {
                launchHostApp(pkg)
            })
            addButton(body, "    " + (if (favorites.contains(pkg)) "Remove from favorites" else "Add to favorites") + "  ·  " + title, {
                val next = (prefs.getStringSet("favorite_packages", emptySet()) ?: emptySet()).toMutableSet()
                if (!next.add(pkg)) next.remove(pkg)
                prefs.edit().putStringSet("favorite_packages", next).apply()
                showAppDrawer(filterText)
            })
        }
        addText(body, "Favorites are stored locally in Aether. Host apps open in Android, not inside a fake guest operating system.")
        addButton(body, "Manage Android home-app defaults", { launchSystem("android.settings.HOME_SETTINGS") })
    }

    private fun showQuickPanel() {
        val body = beginPage("Quick panel", "Aether Control Center • direct shortcuts to host controls")
        addText(body, batteryText() + "   ·   " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), 16, true)
        addGridRow(body, Triple("◉", "Wi-Fi", "Open network controls"), Triple("ᛒ", "Bluetooth", "Connected devices"),
            { launchSystem(Settings.ACTION_WIFI_SETTINGS) }, { launchSystem(Settings.ACTION_BLUETOOTH_SETTINGS) })
        addGridRow(body, Triple("☼", "Brightness", "Display controls"), Triple("♫", "Sound", "Volume and audio"),
            { launchSystem(Settings.ACTION_DISPLAY_SETTINGS) }, { launchSystem("android.settings.SOUND_SETTINGS") })
        addGridRow(body, Triple("◈", "Battery", "Power saving"), Triple("⌑", "Privacy", "Permission dashboard"),
            { launchSystem(Settings.ACTION_BATTERY_SAVER_SETTINGS) }, { launchSystem(Settings.ACTION_PRIVACY_SETTINGS) })
        addGridRow(body, Triple("⌁", "Mobile network", "SIM and data"), Triple("◌", "Airplane mode", "Wireless settings"),
            { launchSystem("android.settings.NETWORK_OPERATOR_SETTINGS") }, { launchSystem(Settings.ACTION_WIRELESS_SETTINGS) })
        addGridRow(body, Triple("▣", "NFC", "Contactless settings"), Triple("◷", "Do not disturb", "Notification policy"),
            { launchSystem("android.settings.NFC_SETTINGS") }, { launchSystem("android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS") })
        addButton(body, "Open full Android Settings", { launchSystem(Settings.ACTION_SETTINGS) })
        addButton(body, "Switch Aether theme", {
            prefs.edit().putString("appearance", if (dark()) "light" else "dark").apply()
            showQuickPanel()
        })
        addText(body, "Protected radios and system modes are changed only in Android's own settings. This avoids pretending that an ordinary launcher has privileged system access.")
    }

    private fun showDeviceCenter() {
        val body = beginPage("Device center", "Live information from the Honor phone")
        val memory = ActivityManager.MemoryInfo()
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
        val stat = StatFs(filesDir.absolutePath)
        val totalStorage = stat.totalBytes / (1024L * 1024L * 1024L)
        val freeStorage = stat.availableBytes / (1024L * 1024L * 1024L)
        val metrics = resources.displayMetrics
        addText(body, Build.MANUFACTURER + " " + Build.MODEL, 22, true)
        addText(body, "Android " + Build.VERSION.RELEASE + "  ·  API " + Build.VERSION.SDK_INT)
        addText(body, "Build: " + Build.DISPLAY)
        addText(body, batteryText(), 18, true)
        addText(body, "RAM: " + (memory.totalMem / (1024L * 1024L)) + " MB total · " +
            (memory.availMem / (1024L * 1024L)) + " MB available")
        addText(body, "Aether private storage volume: " + totalStorage + " GB total · " + freeStorage + " GB free")
        addText(body, "Display: " + metrics.widthPixels + " × " + metrics.heightPixels +
            " px · density " + String.format(Locale.ROOT, "%.2f", metrics.density))
        addButton(body, "Battery usage and health", { launchSystem(Settings.ACTION_BATTERY_SAVER_SETTINGS) })
        addButton(body, "Storage manager", { launchSystem("android.settings.INTERNAL_STORAGE_SETTINGS") })
        addButton(body, "Installed apps", { showAppManagement() })
        addButton(body, "System information", { launchSystem(Settings.ACTION_DEVICE_INFO_SETTINGS) })
        addButton(body, "Open Android Settings", { launchSystem(Settings.ACTION_SETTINGS) })
    }

    private fun showPersonalization() {
        val body = beginPage("Personalization", "Your Aether home, typography and visual style")
        addText(body, "Interface scale: " + (prefs.getFloat("font_scale", 1f) * 100).toInt() + "%", 14, true)
        addButton(body, "Smaller text · 90%", { prefs.edit().putFloat("font_scale", 0.9f).apply(); showPersonalization() })
        addButton(body, "Default text · 100%", { prefs.edit().putFloat("font_scale", 1f).apply(); showPersonalization() })
        addButton(body, "Larger text · 110%", { prefs.edit().putFloat("font_scale", 1.1f).apply(); showPersonalization() })
        addButton(body, "Extra large text · 120%", { prefs.edit().putFloat("font_scale", 1.2f).apply(); showPersonalization() })
        addText(body, "Themes and wallpapers", 13, true)
        addButton(body, "Open Theme Studio", { showThemeStudio() })
        addButton(body, "Aurora wallpaper", { prefs.edit().putString("wallpaper", "aurora").apply(); showPersonalization() })
        addButton(body, "Ocean wallpaper", { prefs.edit().putString("wallpaper", "ocean").apply(); showPersonalization() })
        addButton(body, "Sunrise wallpaper", { prefs.edit().putString("wallpaper", "sunrise").apply(); showPersonalization() })
        addButton(body, "Graphite wallpaper", { prefs.edit().putString("wallpaper", "graphite").apply(); showPersonalization() })
        addButton(body, "Toggle reduced motion", {
            prefs.edit().putBoolean("reduce_motion", !prefs.getBoolean("reduce_motion", false)).apply()
            showPersonalization()
        })
        addButton(body, "Choose profile name", { editProfile() })
        addButton(body, "Customize home and choose default launcher", { launchSystem("android.settings.HOME_SETTINGS") })
        addButton(body, "Change Android wallpaper", { launchSystem("android.intent.action.SET_WALLPAPER") })
        addButton(body, "Display size and system font", { launchSystem(Settings.ACTION_DISPLAY_SETTINGS) })
    }

    private fun showSystemSettings() {
        val body = beginPage("System settings", "Aether settings + Android's complete settings app")
        addText(body, "Aether manages its own data and appearance. Phone-wide permissions, radios, default apps and accessibility remain under Android.")
        addGridRow(body, Triple("◉", "Network", "Wi-Fi, SIM, VPN"), Triple("ᛒ", "Bluetooth", "Pair devices"),
            { launchSystem(Settings.ACTION_WIFI_SETTINGS) }, { launchSystem(Settings.ACTION_BLUETOOTH_SETTINGS) })
        addGridRow(body, Triple("☼", "Display", "Brightness and size"), Triple("♫", "Sound", "Volume and vibration"),
            { launchSystem(Settings.ACTION_DISPLAY_SETTINGS) }, { launchSystem("android.settings.SOUND_SETTINGS") })
        addGridRow(body, Triple("⌑", "Privacy", "Permissions"), Triple("▣", "Security", "Lock and credentials"),
            { launchSystem(Settings.ACTION_PRIVACY_SETTINGS) }, { launchSystem(Settings.ACTION_SECURITY_SETTINGS) })
        addGridRow(body, Triple("▦", "Apps", "Installed and defaults"), Triple("◷", "Notifications", "App alert controls"),
            { showAppManagement() }, { launchSystem("android.settings.APP_NOTIFICATION_SETTINGS") })
        addGridRow(body, Triple("♿", "Accessibility", "System services"), Triple("⌨", "Language & input", "Keyboard and locale"),
            { launchSystem(Settings.ACTION_ACCESSIBILITY_SETTINGS) }, { launchSystem(Settings.ACTION_INPUT_METHOD_SETTINGS) })
        addGridRow(body, Triple("◷", "Date & time", "Clock and timezone"), Triple("▤", "Storage", "Space and cleanup"),
            { launchSystem(Settings.ACTION_DATE_SETTINGS) }, { launchSystem("android.settings.INTERNAL_STORAGE_SETTINGS") })
        addButton(body, "Battery optimization", { launchSystem("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS") })
        addButton(body, "Default apps", { launchSystem("android.settings.MANAGE_DEFAULT_APPS_SETTINGS") })
        addButton(body, "Developer options", { launchSystem(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) })
        addButton(body, "Print services", { launchSystem("android.settings.ACTION_PRINT_SETTINGS") })
        addButton(body, "All Android Settings", { launchSystem(Settings.ACTION_SETTINGS) })
        addButton(body, "Aether-only settings", { showSettings() })
    }

    private fun showCalculator() {
        val body = beginPage("Calculator", "Aether Calculator • offline arithmetic")
        val expression = EditText(this).apply {
            hint = "Example: (12 + 8) × 3"; textSize = 22f; setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        val result = label("Result appears here", 20, primaryText(), true)
        body.addView(expression, LinearLayout.LayoutParams(-1, dp(58)))
        body.addView(result)
        addButton(body, "Calculate", {
            try {
                val value = ExpressionParser(expression.text.toString()).parse()
                if (!value.isFinite()) throw IllegalArgumentException("Invalid result")
                result.text = "= " + String.format(Locale.getDefault(), "%.10f", value).trimEnd('0').trimEnd('.')
                logEvent("Used calculator")
            } catch (_: Exception) { result.text = "Check the expression and try again." }
        })
        addButton(body, "Copy result", {
            val clip = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clip.setPrimaryClip(ClipData.newPlainText("Aether calculation", result.text.toString().removePrefix("= ")))
            toast("Copied")
        })
        addText(body, "Supports +, −, ×, ÷, parentheses, decimals and powers (^). Calculations stay on the phone.")
    }

    private inner class ExpressionParser(private val source: String) {
        private var pos = 0
        fun parse(): Double {
            val value = expression()
            skip()
            if (pos != source.length) throw IllegalArgumentException("Unexpected character")
            return value
        }
        private fun skip() { while (pos < source.length && source[pos].isWhitespace()) pos++ }
        private fun take(ch: Char): Boolean {
            skip()
            if (pos < source.length && source[pos] == ch) { pos++; return true }
            return false
        }
        private fun expression(): Double {
            var value = term()
            while (true) {
                if (take('+')) value += term()
                else if (take('-')) value -= term()
                else return value
            }
        }
        private fun term(): Double {
            var value = power()
            while (true) {
                if (take('*') || take('×')) value *= power()
                else if (take('/') || take('÷')) {
                    val divisor = power()
                    if (divisor == 0.0) throw ArithmeticException("Division by zero")
                    value /= divisor
                } else return value
            }
        }
        private fun power(): Double {
            var value = atom()
            if (take('^')) value = Math.pow(value, power())
            return value
        }
        private fun atom(): Double {
            if (take('+')) return atom()
            if (take('-')) return -atom()
            if (take('(')) {
                val value = expression()
                if (!take(')')) throw IllegalArgumentException("Missing )")
                return value
            }
            skip()
            val start = pos
            var dots = 0
            while (pos < source.length && (source[pos].isDigit() || source[pos] == '.')) {
                if (source[pos] == '.') dots++
                if (dots > 1) throw IllegalArgumentException("Invalid number")
                pos++
            }
            if (start == pos) throw IllegalArgumentException("Number expected")
            return source.substring(start, pos).toDouble()
        }
    }

    private fun showCalendarHub() {
        val body = beginPage("Calendar & alarms", "Aether planner plus Android calendar hand-offs")
        addText(body, SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()), 22, true)
        addButton(body, "Open Aether Planner", { showPlanner() })
        addButton(body, "Create event in phone calendar", {
            try {
                val intent = Intent(Intent.ACTION_INSERT).setDataAndType(Uri.parse("content://com.android.calendar/events"), "vnd.android.cursor.item/event")
                startActivity(intent)
            } catch (_: Exception) { toast("No calendar app is available") }
        })
        addButton(body, "Open calendar app", {
            try { startActivity(Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_CALENDAR")) }
            catch (_: Exception) { toast("No calendar app found") }
        })
        addButton(body, "Set an alarm", {
            try { startActivity(Intent("android.intent.action.SET_ALARM")) }
            catch (_: Exception) { toast("No clock app found") }
        })
        addButton(body, "Open clock and stopwatch", { showClock() })
        addText(body, "Phone calendar events and alarms are created by the system apps you confirm. Aether does not read your calendar or set hidden background alarms.")
    }

    private fun showContactsAndComms() {
        val body = beginPage("Phone & communication", "Use the installed apps you already trust")
        addButton(body, "Open phone dialer", {
            try { startActivity(Intent(Intent.ACTION_DIAL)) } catch (_: Exception) { toast("Dialer unavailable") }
        })
        addButton(body, "Open contacts", {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.contacts/contacts"))) }
            catch (_: Exception) { toast("Contacts unavailable") }
        })
        addButton(body, "Create a contact", {
            try { startActivity(Intent(Intent.ACTION_INSERT, Uri.parse("content://com.android.contacts/contacts"))) }
            catch (_: Exception) { toast("Contacts app unavailable") }
        })
        addButton(body, "Write an SMS", {
            try { startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))) }
            catch (_: Exception) { toast("Messaging app unavailable") }
        })
        addButton(body, "Write an email", {
            try { startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))) }
            catch (_: Exception) { toast("Email app unavailable") }
        })
        addButton(body, "Share text or a link", {
            val input = EditText(this).apply { hint = "Text or link to share"; minLines = 2 }
            AlertDialog.Builder(this).setTitle("Share").setView(input)
                .setPositiveButton("Continue") { _, _ ->
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, input.text.toString())
                    startActivity(Intent.createChooser(send, "Share with"))
                }.setNegativeButton("Cancel", null).show()
        })
        addText(body, "Calls, contacts, SMS and email remain in the phone's own apps. Aether does not request access to your private communications.")
    }

    private fun showFocusCenter() {
        val body = beginPage("Focus mode", "Focus sessions and quiet-mode shortcuts")
        addText(body, "Create a local focus timer. It stays active while Aether remains alive; Android may pause the app in the background.")
        val minutes = EditText(this).apply {
            hint = "Duration in minutes"; setSingleLine(true); inputType = InputType.TYPE_CLASS_NUMBER
        }
        val status = label("No focus session running", 16, primaryText(), true)
        body.addView(minutes)
        body.addView(status)
        addButton(body, "Start focus session", {
            val value = minutes.text.toString().toLongOrNull()
            if (value == null || value !in 1..240) toast("Enter 1 to 240 minutes")
            else {
                runningTimer?.cancel()
                runningTimer = object : CountDownTimer(value * 60000L, 1000L) {
                    override fun onTick(left: Long) {
                        status.text = "Focus time left: " + (left / 60000) + "m " + ((left / 1000) % 60) + "s"
                    }
                    override fun onFinish() {
                        status.text = "Focus session complete"
                        logEvent("Focus session completed")
                        toast("Focus session complete")
                    }
                }.start()
                prefs.edit().putBoolean("reduce_motion", true).apply()
                logEvent("Focus session started")
            }
        })
        addButton(body, "Cancel focus session", { runningTimer?.cancel(); status.text = "Focus session cancelled" })
        addButton(body, "Open Android Do Not Disturb settings", {
            launchSystem("android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS")
        })
        addButton(body, "Open Android notification controls", { launchSystem("android.settings.APP_NOTIFICATION_SETTINGS") })
        addText(body, "Aether cannot silently enable system Do Not Disturb. Use Android's own screen to configure it.")
    }

    private fun showDigitalWellbeing() {
        val body = beginPage("Digital wellbeing", "Aether activity and focus, not a system-wide usage tracker")
        val events = try { JSONArray(prefs.getString("activity_log", "[]")) } catch (_: Exception) { JSONArray() }
        val focusCount = (0 until events.length()).count { events.optJSONObject(it)?.optString("text", "")?.contains("Focus", true) == true }
        addText(body, "Recorded Aether actions: " + events.length(), 22, true)
        addText(body, "Focus-related events: " + focusCount)
        addButton(body, "Start a focus session", { showFocusCenter() })
        addButton(body, "Review Aether activity", { showNotifications() })
        addButton(body, "Open Android Digital Wellbeing", { launchSystem("android.settings.DIGITAL_WELLBEING_SETTINGS") })
        addButton(body, "Open battery usage", { launchSystem(Settings.ACTION_BATTERY_SAVER_SETTINGS) })
        addText(body, "Android controls system app-usage data. Aether intentionally does not request Usage Access.")
    }

    private fun showAppManagement() {
        val body = beginPage("App manager", "Inspect or open apps installed in Android")
        addText(body, "Choose an app to open it, view its Android details, or ask Android to uninstall it.")
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val apps = packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.ROOT) }
        apps.forEach { info ->
            val pkg = info.activityInfo.packageName
            val title = info.loadLabel(packageManager).toString()
            addButton(body, "▣   " + title + "  ·  Open", { launchHostApp(pkg) })
            addButton(body, "    App details", {
                try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg))) }
                catch (_: Exception) { toast("App details unavailable") }
            })
            addButton(body, "    Uninstall request", {
                try { startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg))) }
                catch (_: Exception) { toast("Uninstall screen unavailable") }
            })
        }
        addButton(body, "Android default apps", { launchSystem("android.settings.MANAGE_DEFAULT_APPS_SETTINGS") })
    }

    private fun showSecurityCenter() {
        val body = beginPage("Security & privacy", "Aether's privacy center with Android system controls")
        addText(body, "Aether requests internet access for its browser. Other protected phone data is opened through Android's system pickers or external apps.")
        addButton(body, "Encrypted Aether Vault", { showVault() })
        addButton(body, "Review Aether permissions", {
            try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName))) }
            catch (_: Exception) { toast("App settings unavailable") }
        })
        addButton(body, "Android privacy dashboard", { launchSystem(Settings.ACTION_PRIVACY_SETTINGS) })
        addButton(body, "Android permission manager", { launchSystem("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION") })
        addButton(body, "Lock screen and device security", { launchSystem(Settings.ACTION_SECURITY_SETTINGS) })
        addButton(body, "Backup and restore Aether data", { showBackupCenter() })
        addText(body, "Aether does not intercept notifications, read messages, watch other apps, collect location, or draw over the phone.")
    }

    private fun showBackupCenter() {
        val body = beginPage("Backup center", "Portable Aether settings and personal data")
        addText(body, "Export a JSON backup to a location you choose. The encrypted Vault is excluded because its key is held by this device's Android Keystore.")
        addButton(body, "Export backup", { exportBackup() })
        addButton(body, "Import backup", { importBackup() })
        addButton(body, "Open private files", { showFiles() })
        addButton(body, "Open notes", { showNotes() })
        addButton(body, "Open mini-app manager", { showMiniAppManager() })
    }

    private fun showMiniAppManager() {
        val body = beginPage("Mini-app manager", "Your personal Aether Store library")
        val apps = webApps()
        addButton(body, "Create a mini-app", { showStudio() })
        if (apps.length() == 0) addText(body, "No saved mini-apps yet.")
        for (i in 0 until apps.length()) {
            val item = apps.optJSONObject(i) ?: continue
            val name = item.optString("name", "Mini-app")
            addButton(body, item.optString("icon", "◈") + "   " + name + "  ·  Open", { launchWebApp(item) })
            addButton(body, "    Delete " + name, {
                val next = JSONArray()
                for (j in 0 until apps.length()) if (j != i) next.put(apps.optJSONObject(j))
                prefs.edit().putString("web_apps", next.toString()).apply()
                logEvent("Deleted mini-app: " + name)
                showMiniAppManager()
            })
        }
        addText(body, "Saved HTML mini-apps can work offline. Website shortcuts need internet access.")
    }

    private fun showSearchHub() {
        val body = beginPage("Aether Search", "Spotlight-style search across Aether and the web")
        val query = EditText(this).apply { hint = "Search apps, settings, notes or the web"; setSingleLine(true) }
        body.addView(query)
        addButton(body, "Search Aether", { runSearch(query.text.toString()) })
        addButton(body, "Find installed apps", { showAppDrawer(query.text.toString()) })
        addButton(body, "Search the web", { openBrowser("https://www.google.com/search?q=" + Uri.encode(query.text.toString())) })
        addText(body, "Search understands common commands such as calculator, apps, notes, files, photos, settings, calendar, contacts, focus, battery and web search.")
    }

    private fun showLocaleInputSettings() {
        val body = beginPage("Language & input", "System keyboard, language and voice-input options")
        addButton(body, "System language and region", { launchSystem(Settings.ACTION_LOCALE_SETTINGS) })
        addButton(body, "On-screen keyboard settings", { launchSystem(Settings.ACTION_INPUT_METHOD_SETTINGS) })
        addButton(body, "Text-to-speech output", { launchSystem("android.settings.TTS_SETTINGS") })
        addButton(body, "Accessibility settings", { launchSystem(Settings.ACTION_ACCESSIBILITY_SETTINGS) })
        addText(body, "Language and keyboard changes affect Android and all host apps, not just Aether.")
    }

}
