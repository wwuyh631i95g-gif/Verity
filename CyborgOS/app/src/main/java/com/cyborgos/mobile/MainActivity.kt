package com.cyborgos.mobile

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.DragEvent
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.SeekBar
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : Activity() {
    private data class AppTile(
        val id: String, val title: String, val icon: String, val side: String,
        val packageName: String = "", val url: String = ""
    )
    private data class StoreItem(val id: String, val title: String, val icon: String, val info: String)

    private val prefs by lazy { getSharedPreferences("cyborg_os", MODE_PRIVATE) }
    private val tiles = ArrayList<AppTile>()
    private lateinit var root: FrameLayout
    private lateinit var mainColumn: LinearLayout
    private lateinit var pointerLayer: PointerLayer
    private var iosPage: String? = null
    private var androidPage: String? = null
    private var pointerEnabled = true
    private var pointerVisible = true
    private var pointerX = 0f
    private var pointerY = 0f
    private var pointerAt = 0L
    private var trackpadVisible = false
    private var selectedTileId: String? = null
    private var activeSide = "ios"
    private var ctrlLatched = false
    private var altLatched = false
    private var shiftLatched = false
    private var fullScreen = false
    private val reqPickApk = 7101
    private val reqPickIpa = 7102
    private val reqInstallSettings = 7103
    private val reqPickCyborgPackage = 7104
    private val reqPickLabFile = 7105

    private val catalogue = listOf(
        StoreItem("mykit", "MyKit Search", "⌕", "Search apps, settings and the web"),
        StoreItem("shared_notes", "Shared Notes", "✎", "One notebook for both halves"),
        StoreItem("weather", "Weather", "☀", "Open live weather in the browser"),
        StoreItem("calendar", "Calendar", "▦", "Open the phone calendar"),
        StoreItem("files", "Files", "▣", "Open the system document picker"),
        StoreItem("camera", "Camera", "◉", "Open the camera app"),
        StoreItem("pcdesk", "PC Desktop", "▤", "Desktop workspace and control strip"),
        StoreItem("web", "Mini Browser", "🌐", "Open a safe in-app browser")
    )

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int, stroke: Int = 0) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != 0) setStroke(dp(1), stroke)
        }

    private fun text(value: String, size: Float = 12f, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(2), dp(3), dp(2), dp(3))
        }

    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 16, 29)
        window.navigationBarColor = Color.rgb(8, 13, 24)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        applyImmersiveMode()
        loadTiles()
        root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(7, 12, 23)) }
        mainColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(mainColumn, FrameLayout.LayoutParams(-1, -1))
        pointerLayer = PointerLayer(this)
        root.addView(pointerLayer, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        render()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && fullScreen) applyImmersiveMode()
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= 26 && packageManager.canRequestPackageInstalls()) {
            launchPendingApkInstaller()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        keepReadAccess(uri)
        when (requestCode) {
            reqPickApk -> {
                val name = displayName(uri)
                if (!name.endsWith(".apk", true)) {
                    toast("Choose one .apk file")
                    return
                }
                prefs.edit().putString("pending_apk_uri", uri.toString()).apply()
                if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
                    AlertDialog.Builder(this)
                        .setTitle("Allow APK installation")
                        .setMessage("Android requires a one-time permission for CYBORG VM to request APK installation. Android will still show its own confirmation before installing.")
                        .setPositiveButton("OPEN SETTINGS") { _, _ ->
                            try {
                                startActivityForResult(
                                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:$packageName")),
                                    reqInstallSettings
                                )
                            } catch (_: Exception) {
                                toast("Open Android Settings and allow installs from this app")
                            }
                        }
                        .setNegativeButton("CANCEL", null)
                        .show()
                } else launchPendingApkInstaller()
            }
            reqPickIpa -> {
                val name = displayName(uri)
                if (!name.endsWith(".ipa", true)) {
                    toast("Choose an .ipa file")
                    return
                }
                val arr = JSONArray(prefs.getString("ipa_archives", "[]"))
                arr.put(JSONObject()
                    .put("name", name)
                    .put("uri", uri.toString())
                    .put("added", System.currentTimeMillis()))
                prefs.edit().putString("ipa_archives", arr.toString()).apply()
                setPage("ios", "ipa")
                toast("IPA archived; native iOS games cannot run on Android")
            }
            reqPickCyborgPackage -> installCyborgPackage(uri)
            reqPickLabFile -> quarantineFile(uri)
        }
    }

    private fun keepReadAccess(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) { }
    }

    private fun displayName(uri: Uri): String {
        var name = uri.lastPathSegment ?: "package"
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) name = c.getString(0) ?: name
            }
        } catch (_: Exception) { }
        return name
    }

    private fun openApkPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES,
                arrayOf("application/vnd.android.package-archive", "application/octet-stream"))
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try { startActivityForResult(intent, reqPickApk) }
        catch (_: Exception) { toast("Android file picker is unavailable") }
    }

    private fun openIpaPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES,
                arrayOf("application/octet-stream", "application/zip", "application/x-itunes-ipa"))
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try { startActivityForResult(intent, reqPickIpa) }
        catch (_: Exception) { toast("Android file picker is unavailable") }
    }

    // .capp is a CYBORG web-app definition, not a native iOS/Android binary.
    // JSON format: {"name":"Game","icon":"🎮","side":"ios","url":"https://example.com"}
    private fun openCyborgPackagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try { startActivityForResult(intent, reqPickCyborgPackage) }
        catch (_: Exception) { toast("File picker is unavailable") }
    }

    private fun installCyborgPackage(uri: Uri) {
        try {
            val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalArgumentException("Could not read package")
            val obj = JSONObject(raw)
            val title = obj.optString("name").trim()
            val icon = obj.optString("icon", "🎮").ifBlank { "🎮" }
            val url = obj.optString("url").trim()
            val side = if (obj.optString("side", activeSide) == "ios") "ios" else "android"
            if (title.isBlank() || url.isBlank() ||
                !(url.startsWith("https://") || url.startsWith("http://"))) {
                throw IllegalArgumentException("Package must contain name and http/https URL")
            }
            val id = "capp:" + title.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_") +
                ":" + System.currentTimeMillis()
            tiles.add(AppTile(id, title, icon, side, "", url))
            saveTiles()
            setPage(side, null)
            toast(title + " installed in CYBORG")
        } catch (e: Exception) {
            AlertDialog.Builder(this).setTitle("Cannot install CYBORG package")
                .setMessage(e.message ?: "Invalid .capp package")
                .setPositiveButton("OK", null).show()
        }
    }

    private fun launchPendingApkInstaller() {
        val raw = prefs.getString("pending_apk_uri", null) ?: return
        if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) return
        val uri = Uri.parse(raw)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            prefs.edit().remove("pending_apk_uri").apply()
            startActivity(intent)
        } catch (_: Exception) {
            toast("Android could not open this APK. Check that the file is valid.")
        }
    }

    private fun applyImmersiveMode() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            when {
                event.keyCode in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 -> {
                    handleFunctionKey(event.keyCode)
                    return true
                }
                event.keyCode == KeyEvent.KEYCODE_ESCAPE -> {
                    onBackPressed()
                    return true
                }
                event.keyCode == KeyEvent.KEYCODE_TAB -> {
                    cycleActiveSide()
                    return true
                }
                event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                event.keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> {
                    moveFocus(event.keyCode)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (::root.isInitialized && pointerEnabled &&
            (event.actionMasked == MotionEvent.ACTION_DOWN ||
             event.actionMasked == MotionEvent.ACTION_MOVE ||
             event.actionMasked == MotionEvent.ACTION_UP)) {
            val loc = IntArray(2)
            root.getLocationOnScreen(loc)
            pointerX = event.rawX - loc[0]
            pointerY = event.rawY - loc[1]
            pointerAt = System.currentTimeMillis()
            pointerVisible = true
            if (::pointerLayer.isInitialized) pointerLayer.invalidate()
        }
        return super.dispatchTouchEvent(event)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (iosPage != null) iosPage = null
        else if (androidPage != null) androidPage = null
        else { super.onBackPressed(); return }
        render()
    }

    private fun defaults() = listOf(
        AppTile("phone", "Phone", "☎", "ios"),
        AppTile("messages", "Messages", "▰", "ios"),
        AppTile("photos", "Photos", "🌈", "ios"),
        AppTile("safari", "Safari", "🧭", "ios"),
        AppTile("appstore", "App Store", "A", "ios"),
        AppTile("iosmaps", "Maps", "🗺", "ios"),
        AppTile("music", "Music", "♫", "ios"),
        AppTile("notes", "Notes", "✎", "ios"),
        AppTile("mykit", "MyKit Search", "⌕", "ios"),
        AppTile("iossettings", "Settings", "⚙", "ios"),
        AppTile("ipaarchive", "IPA Archive", "▣", "ios"),
        AppTile("gamehub", "Game Center", "🎮", "ios"),
        AppTile("playstore", "Play Store", "▶", "android"),
        AppTile("chrome", "Chrome", "🌐", "android"),
        AppTile("camera", "Camera", "📷", "android"),
        AppTile("files", "Files", "📁", "android"),
        AppTile("youtube", "YouTube", "▶", "android"),
        AppTile("maps", "Maps", "📍", "android"),
        AppTile("calculator", "Calculator", "🧮", "android"),
        AppTile("clock", "Clock", "◷", "android"),
        AppTile("androidsearch", "MyKit Search", "⌕", "android"),
        AppTile("androidsettings", "Settings", "⚙", "android"),
        AppTile("apkmanager", "APK Installer", "↓", "android"),
        AppTile("securitylab", "Virus Lab", "🛡", "android"),
        AppTile("androidgames", "Game Library", "🎮", "android")
    )

    private fun loadTiles() {
        val raw = prefs.getString("tiles", null)
        if (raw.isNullOrBlank()) {
            tiles.clear()
            tiles.addAll(defaults())
            saveTiles()
            return
        }
        try {
            val arr = JSONArray(raw)
            tiles.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                tiles.add(AppTile(
                    obj.optString("id"), obj.optString("title"), obj.optString("icon", "◈"),
                    obj.optString("side", "android"), obj.optString("package"), obj.optString("url")
                ))
            }
            if (tiles.isEmpty()) tiles.addAll(defaults())
            else {
                defaults().filter { fresh -> tiles.none { it.id == fresh.id } }.forEach { tiles.add(it) }
            }
            saveTiles()
        } catch (_: Exception) {
            tiles.clear()
            tiles.addAll(defaults())
        }
    }

    private fun saveTiles() {
        val arr = JSONArray()
        tiles.forEach { t ->
            arr.put(JSONObject().put("id", t.id).put("title", t.title).put("icon", t.icon)
                .put("side", t.side).put("package", t.packageName).put("url", t.url))
        }
        prefs.edit().putString("tiles", arr.toString()).apply()
    }

    private fun setPage(side: String, page: String?) {
        if (side == "ios") iosPage = page else androidPage = page
        render()
    }

    private fun render() {
        mainColumn.removeAllViews()
        val halves = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(2), dp(4), dp(3))
        }
        halves.addView(buildPanel("ios"), LinearLayout.LayoutParams(0, -1, 1f).apply {
            setMargins(dp(2), dp(2), dp(3), dp(2))
        })
        halves.addView(buildPanel("android"), LinearLayout.LayoutParams(0, -1, 1f).apply {
            setMargins(dp(3), dp(2), dp(2), dp(2))
        })
        mainColumn.addView(halves, LinearLayout.LayoutParams(-1, 0, 1f))
        mainColumn.addView(buildPcDock(), LinearLayout.LayoutParams(-1, -2))
    }

    private fun buildTopBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), dp(6), dp(13), dp(5))
        }
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleBox.addView(text("CYBORG VM", 19f, Color.WHITE, true))
        titleBox.addView(text("VIRTUAL DEVICE  •  TOUCH + PC CONTROLS", 8.5f, Color.rgb(154, 176, 208), true))
        bar.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val battery = try {
            (getSystemService(BATTERY_SERVICE) as BatteryManager)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (_: Exception) { -1 }
        bar.addView(text(time + if (battery in 0..100) "  " + battery + "%" else "",
            12f, Color.rgb(211, 225, 246), true))
        return bar
    }

    private fun buildPanel(side: String): View {
        val ios = side == "ios"
        val accent = if (ios) Color.rgb(151, 181, 255)
            else prefs.getInt("android_accent", Color.rgb(174, 164, 255))
        val colors = if (ios) intArrayOf(Color.rgb(70, 103, 190), Color.rgb(118, 112, 194), Color.rgb(207, 143, 173))
                    else intArrayOf(Color.rgb(23, 53, 69), Color.rgb(39, 87, 100), Color.rgb(69, 105, 133))
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(5), dp(6), dp(6))
            background = DualWallpaperDrawable(ios)
            setOnDragListener { _, e ->
                when (e.action) {
                    DragEvent.ACTION_DRAG_STARTED -> e.localState is AppTile
                    DragEvent.ACTION_DRAG_ENTERED -> { alpha = 0.84f; true }
                    DragEvent.ACTION_DRAG_EXITED -> { alpha = 1f; true }
                    DragEvent.ACTION_DROP -> {
                        alpha = 1f
                        val moving = e.localState as? AppTile
                        if (moving != null) {
                            val ix = tiles.indexOfFirst { it.id == moving.id }
                            if (ix >= 0) {
                                tiles[ix] = tiles[ix].copy(side = side)
                                saveTiles()
                                toast(moving.title + " moved to " + if (ios) "left workspace" else "right workspace")
                                render()
                            }
                        }
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> { alpha = 1f; true }
                    else -> true
                }
            }
        }
        panel.addView(buildStatusStrip(ios), LinearLayout.LayoutParams(-1, dp(22)))
        if (ios) {
            val island = FrameLayout(this)
            val pill = View(this).apply { background = rounded(Color.BLACK, 20) }
            island.addView(pill, FrameLayout.LayoutParams(dp(50), dp(13), Gravity.CENTER))
            panel.addView(island, LinearLayout.LayoutParams(-1, dp(17)))
            panel.addView(text(
                SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
                8.5f, Color.rgb(220, 231, 255), true
            ))
            panel.addView(text("9:41", 22f, Color.WHITE, true))
        } else {
            val search = TextView(this).apply {
                text = "G     Search     ☷"
                textSize = 10f
                setTextColor(Color.rgb(40, 40, 58))
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), 0, dp(8), 0)
                background = rounded(Color.rgb(227, 225, 246), 24)
                setOnClickListener { setPage("android", "search") }
            }
            panel.addView(search, LinearLayout.LayoutParams(-1, dp(32)).apply {
                topMargin = dp(7); bottomMargin = dp(5)
            })
            panel.addView(text(
                SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date()),
                9f, Color.rgb(220, 216, 255), true
            ))
        }
        val scroll = ScrollView(this).apply { isFillViewport = false }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(1), dp(5), dp(1), dp(5))
        }
        val page = if (ios) iosPage else androidPage
        if (page == null) renderHome(body, side, accent) else renderPage(body, side, page, accent)
        scroll.addView(body)
        panel.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        panel.addView(buildOsDock(side), LinearLayout.LayoutParams(-1, dp(58)))
        return panel
    }

    private fun buildStatusStrip(ios: Boolean): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5), 0, dp(5), 0)
        }
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        row.addView(text(time, 8.5f, Color.WHITE, true), LinearLayout.LayoutParams(0, -1, 1f))
        row.addView(text(
            if (ios) "●●●  Wi-Fi  ▰" else "▴  ●●●  Wi-Fi  ▰",
            7.5f, Color.WHITE, true
        ))
        return row
    }

    private fun buildOsDock(side: String): View {
        val ios = side == "ios"
        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(3), dp(3), dp(3), dp(3))
            background = rounded(
                if (ios) Color.argb(125, 225, 235, 255) else Color.argb(135, 220, 218, 250),
                17
            )
        }
        val preferred = if (ios) listOf("phone", "messages", "safari", "music")
                       else listOf("phone", "chrome", "camera", "photos")
        val dockTiles = preferred.mapNotNull { id -> tiles.firstOrNull { it.id == id && it.side == side } }
        dockTiles.take(4).forEach { tile ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isFocusable = true
                setOnClickListener { selectedTileId = tile.id; activeSide = side; openTile(tile) }
            }
            item.addView(tileIconView(tile, 34), LinearLayout.LayoutParams(dp(34), dp(34)))
            dock.addView(item, LinearLayout.LayoutParams(0, -1, 1f))
        }
        return dock
    }

    private fun renderHome(body: LinearLayout, side: String, accent: Int) {
        val ios = side == "ios"
        val widget = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(7), dp(10), dp(7))
            background = rounded(
                if (ios) Color.argb(105, 245, 246, 255) else Color.argb(120, 226, 243, 239),
                14, Color.argb(75, 255, 255, 255)
            )
        }
        if (ios) {
            widget.addView(text(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
                8.5f, Color.WHITE, true))
            widget.addView(text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                27f, Color.WHITE, true))
            widget.addView(text("Cupertino-style home workspace", 8f, Color.rgb(238, 239, 255)))
        } else {
            widget.addView(text("At a glance", 8.5f, Color.WHITE, true))
            widget.addView(text(SimpleDateFormat("EEE, MMM d  •  HH:mm", Locale.getDefault()).format(Date()),
                17f, Color.WHITE, true))
            widget.addView(text("Search, apps and device shortcuts", 8f, Color.rgb(228, 245, 243)))
        }
        body.addView(widget, LinearLayout.LayoutParams(-1, dp(82)).apply { bottomMargin = dp(7) })
        body.addView(text(if (ios) "Suggestions" else "Your apps", 9.5f, Color.WHITE, true))
        val dockIds = if (ios) listOf("phone", "messages", "safari", "music")
                      else listOf("phone", "chrome", "camera", "photos")
        val list = tiles.filter { it.side == side && it.id !in dockIds }
        var i = 0
        while (i < list.size) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (col in 0..2) {
                val ix = i + col
                if (ix < list.size) {
                    row.addView(appTileView(list[ix]),
                        LinearLayout.LayoutParams(0, dp(68), 1f).apply {
                            setMargins(dp(1), dp(1), dp(1), dp(1))
                        })
                } else row.addView(View(this), LinearLayout.LayoutParams(0, dp(68), 1f))
            }
            body.addView(row)
            i += 3
        }
        body.addView(text(if (ios) "APP LIBRARY" else "ALL APPS", 8f, accent, true))
        if (ios) {
            addButton(body, "◉  Control Center • Focus • Live Island") { setPage(side, "control") }
            addButton(body, "🎮  Game Center • iOS compatibility") { setPage(side, "games") }
            addButton(body, "▣  IPA Library") { setPage(side, "ipa") }
        } else {
            addButton(body, "⚡  Quick Settings • Material You") { setPage(side, "quick") }
            addButton(body, "🛡  Virus Lab • Safe Quarantine") { setPage(side, "securitylab") }
            addButton(body, "🎮  Game Library") { setPage(side, "games") }
        }
        addButton(body, "＋  Add app") { makerDialog(side) }
        addButton(body, "▦  All installed apps") { setPage(side, "apps") }
        addButton(body, "⌕  Search") { setPage(side, "search") }
        addButton(body, if (ios) "App Store" else "Play Store") { setPage(side, "store") }
        addButton(body, "▤  PC Desktop") { showPcDesktop() }
    }

    private fun iconBackground(tile: AppTile, side: String): GradientDrawable {
        val palette = when (tile.id) {
            "phone" -> intArrayOf(Color.rgb(65, 205, 129), Color.rgb(20, 142, 92))
            "messages" -> intArrayOf(Color.rgb(90, 224, 139), Color.rgb(28, 160, 93))
            "photos" -> intArrayOf(Color.rgb(255, 202, 99), Color.rgb(239, 112, 111))
            "safari", "chrome", "web" -> intArrayOf(Color.rgb(92, 194, 255), Color.rgb(57, 106, 225))
            "appstore", "playstore" -> intArrayOf(Color.rgb(103, 181, 255), Color.rgb(65, 102, 222))
            "camera" -> intArrayOf(Color.rgb(115, 125, 143), Color.rgb(47, 56, 75))
            "maps", "iosmaps" -> intArrayOf(Color.rgb(103, 207, 157), Color.rgb(65, 138, 117))
            "music" -> intArrayOf(Color.rgb(255, 111, 145), Color.rgb(185, 61, 127))
            "notes", "shared_notes" -> intArrayOf(Color.rgb(255, 225, 125), Color.rgb(210, 156, 65))
            "iossettings", "androidsettings" -> intArrayOf(Color.rgb(164, 174, 190), Color.rgb(86, 99, 122))
            else -> {
                val seed = tile.id.hashCode()
                intArrayOf(
                    Color.rgb(75 + (seed ushr 16 and 0x7F), 75 + (seed ushr 8 and 0x7F), 95 + (seed and 0x7F)),
                    Color.rgb(35 + (seed ushr 10 and 0x7F), 40 + (seed ushr 3 and 0x7F), 65 + (seed ushr 5 and 0x7F))
                )
            }
        }
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, palette).apply {
            // Clearly distinguish the two virtual interfaces: circular iPhone badges vs square Android icons.
            shape = if (side == "ios") GradientDrawable.OVAL else GradientDrawable.RECTANGLE
            cornerRadius = dp(if (side == "ios") 21 else 7).toFloat()
        }
    }

    private fun hostPackageFor(tile: AppTile): String? {
        if (tile.packageName.isNotBlank()) return tile.packageName
        if (tile.side != "android") return null
        val candidates = when (tile.id) {
            "chrome", "safari", "web" -> listOf("com.android.chrome")
            "youtube" -> listOf("com.google.android.youtube")
            "photos" -> listOf("com.google.android.apps.photos", "com.huawei.photos")
            "phone" -> listOf("com.google.android.dialer", "com.android.dialer", "com.android.phone")
            "messages" -> listOf("com.google.android.apps.messaging", "com.android.mms")
            "maps", "iosmaps" -> listOf("com.google.android.apps.maps")
            "camera" -> listOf("com.huawei.camera", "com.android.camera2", "com.android.camera")
            "files" -> listOf("com.google.android.documentsui", "com.huawei.filemanager")
            "calculator" -> listOf("com.google.android.calculator", "com.android.calculator2")
            "clock" -> listOf("com.google.android.deskclock", "com.android.deskclock")
            "playstore" -> listOf("com.android.vending")
            else -> emptyList()
        }
        return candidates.firstOrNull { packageManager.getLaunchIntentForPackage(it) != null }
    }

    private fun tileIconView(tile: AppTile, size: Int): View {
        val pkg = hostPackageFor(tile)
        if (pkg != null) {
            try {
                return ImageView(this).apply {
                    setImageDrawable(packageManager.getApplicationIcon(pkg))
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    background = iconBackground(tile, tile.side)
                    setPadding(dp(2), dp(2), dp(2), dp(2))
                    contentDescription = tile.title
                }
            } catch (_: Exception) { }
        }
        return TileGlyphView(this, tile).apply {
            background = iconBackground(tile, tile.side)
            contentDescription = tile.title
        }
    }

    private inner class TileGlyphView(context: Context, private val tile: AppTile) : View(context) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val size = minOf(w, h) * 0.54f
            val cx = w / 2f
            val cy = h / 2f
            p.color = Color.WHITE
            p.strokeWidth = size * 0.095f
            p.strokeCap = Paint.Cap.ROUND
            p.strokeJoin = Paint.Join.ROUND
            p.style = Paint.Style.STROKE
            when (tile.id) {
                "phone" -> {
                    val path = Path().apply {
                        moveTo(cx - size * .30f, cy - size * .34f)
                        cubicTo(cx - size * .42f, cy - size * .12f, cx + size * .12f, cy + size * .42f, cx + size * .34f, cy + size * .25f)
                        lineTo(cx + size * .18f, cy + size * .04f)
                        lineTo(cx + size * .02f, cy + size * .13f)
                        cubicTo(cx - size * .09f, cy + size * .08f, cx - size * .18f, cy - size * .02f, cx - size * .20f, cy - size * .15f)
                        lineTo(cx - size * .10f, cy - size * .28f)
                        close()
                    }
                    p.style = Paint.Style.FILL
                    canvas.drawPath(path, p)
                }
                "messages" -> {
                    val box = RectF(cx - size * .38f, cy - size * .28f, cx + size * .38f, cy + size * .25f)
                    canvas.drawRoundRect(box, size * .16f, size * .16f, p)
                    val tail = Path().apply {
                        moveTo(cx - size * .18f, cy + size * .22f)
                        lineTo(cx - size * .30f, cy + size * .42f)
                        lineTo(cx + size * .02f, cy + size * .24f)
                    }
                    canvas.drawPath(tail, p)
                }
                "photos" -> {
                    val colors = intArrayOf(
                        Color.rgb(255, 83, 98), Color.rgb(255, 174, 55),
                        Color.rgb(255, 220, 72), Color.rgb(73, 204, 126),
                        Color.rgb(60, 160, 255), Color.rgb(164, 100, 255)
                    )
                    p.style = Paint.Style.FILL
                    for (i in 0..5) {
                        val a = Math.PI * 2.0 * i / 6.0
                        p.color = colors[i]
                        canvas.drawCircle(cx + kotlin.math.cos(a).toFloat() * size * .20f,
                            cy + kotlin.math.sin(a).toFloat() * size * .20f, size * .15f, p)
                    }
                    p.color = Color.WHITE
                    canvas.drawCircle(cx, cy, size * .09f, p)
                }
                "safari" -> {
                    canvas.drawCircle(cx, cy, size * .40f, p)
                    val needle = Path().apply {
                        moveTo(cx, cy - size * .31f)
                        lineTo(cx + size * .11f, cy + size * .10f)
                        lineTo(cx - size * .06f, cy + size * .02f)
                        close()
                    }
                    p.style = Paint.Style.FILL
                    p.color = Color.rgb(255, 92, 100)
                    canvas.drawPath(needle, p)
                    p.color = Color.WHITE
                    canvas.drawLine(cx, cy, cx + size * .10f, cy + size * .32f, p)
                }
                "appstore" -> {
                    val a = Path().apply {
                        moveTo(cx - size * .27f, cy + size * .28f)
                        lineTo(cx + size * .12f, cy - size * .34f)
                        moveTo(cx - size * .04f, cy - size * .34f)
                        lineTo(cx + size * .34f, cy + size * .28f)
                        moveTo(cx - size * .20f, cy + size * .10f)
                        lineTo(cx + size * .22f, cy + size * .10f)
                    }
                    canvas.drawPath(a, p)
                }
                "iosmaps", "maps" -> {
                    val pin = Path().apply {
                        moveTo(cx, cy + size * .40f)
                        cubicTo(cx - size * .12f, cy + size * .16f, cx - size * .36f, cy - size * .02f, cx - size * .30f, cy - size * .22f)
                        cubicTo(cx - size * .24f, cy - size * .48f, cx + size * .24f, cy - size * .48f, cx + size * .30f, cy - size * .22f)
                        cubicTo(cx + size * .36f, cy - size * .02f, cx + size * .12f, cy + size * .16f, cx, cy + size * .40f)
                    }
                    canvas.drawPath(pin, p)
                    p.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy - size * .20f, size * .09f, p)
                }
                "music" -> {
                    p.strokeWidth = size * .11f
                    canvas.drawLine(cx + size * .18f, cy - size * .30f, cx + size * .18f, cy + size * .18f, p)
                    canvas.drawLine(cx + size * .18f, cy - size * .30f, cx + size * .39f, cy - size * .23f, p)
                    p.style = Paint.Style.FILL
                    canvas.drawCircle(cx - size * .02f, cy + size * .22f, size * .15f, p)
                    canvas.drawCircle(cx + size * .30f, cy + size * .15f, size * .15f, p)
                }
                "notes" -> {
                    val paper = RectF(cx - size * .30f, cy - size * .40f, cx + size * .30f, cy + size * .40f)
                    canvas.drawRoundRect(paper, size * .06f, size * .06f, p)
                    for (i in -1..1) canvas.drawLine(cx - size * .18f, cy + i * size * .15f,
                        cx + size * .18f, cy + i * size * .15f, p)
                }
                "camera" -> {
                    val body = RectF(cx - size * .38f, cy - size * .25f, cx + size * .38f, cy + size * .28f)
                    canvas.drawRoundRect(body, size * .10f, size * .10f, p)
                    canvas.drawCircle(cx, cy, size * .16f, p)
                    canvas.drawCircle(cx + size * .25f, cy - size * .15f, size * .035f, p)
                }
                "files" -> {
                    val folder = Path().apply {
                        moveTo(cx - size * .38f, cy - size * .24f)
                        lineTo(cx - size * .08f, cy - size * .24f)
                        lineTo(cx + size * .02f, cy - size * .12f)
                        lineTo(cx + size * .38f, cy - size * .12f)
                        lineTo(cx + size * .31f, cy + size * .30f)
                        lineTo(cx - size * .33f, cy + size * .30f)
                        close()
                    }
                    canvas.drawPath(folder, p)
                }
                else -> {
                    p.style = Paint.Style.FILL
                    p.textAlign = Paint.Align.CENTER
                    p.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    p.textSize = size * .78f
                    canvas.drawText(tile.icon.take(2), cx, cy + size * .28f, p)
                }
            }
        }
    }

    private fun appTileView(tile: AppTile): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(1), dp(2), dp(1), dp(2))
            isFocusable = true
            contentDescription = tile.title
        }
        card.addView(tileIconView(tile, 42), LinearLayout.LayoutParams(dp(42), dp(42)))
        card.addView(text(tile.title, 8.5f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(-1, dp(21)))
        card.setOnClickListener { selectedTileId = tile.id; activeSide = tile.side; openTile(tile) }
        card.setOnLongClickListener {
            it.startDragAndDrop(ClipData.newPlainText("cyborg_app", tile.id),
                View.DragShadowBuilder(it), tile, 0)
            true
        }
        return card
    }

    private fun renderPage(body: LinearLayout, side: String, page: String, accent: Int) {
        addButton(body, "←  Back to " + if (side == "ios") "iPhone" else "Android") { setPage(side, null) }
        when (page) {
            "store" -> renderStore(body, side, accent)
            "ipa" -> renderIpaArchives(body)
            "apps" -> renderInstalledApps(body, side)
            "control" -> renderIosControlCenter(body)
            "quick" -> renderAndroidQuickSettings(body)
            "games" -> renderGameHub(body, side)
            "securitylab" -> renderSecurityLab(body)
            "search" -> renderSearch(body, accent)
            "notes" -> renderNotes(body, accent)
            else -> renderSettings(body, accent)
        }
    }

    private fun renderStore(body: LinearLayout, side: String, accent: Int) {
        val ios = side == "ios"
        body.addView(text(if (ios) "App Store" else "Google Play", 19f, Color.WHITE, true))
        body.addView(text(if (ios) "CURATED FOR THE iPHONE WORKSPACE" else "ANDROID APP CENTRE",
            8.5f, accent, true))
        body.addView(text(if (ios)
            "CYBORG mini-apps and web apps live here. Imported .ipa files can be archived, but iOS apps cannot run on Android."
            else "Use Google Play for store downloads, or choose an APK file to install through Android.",
            8.5f, Color.LTGRAY))

        if (ios) {
            addButton(body, "＋  Import .IPA package (archive only)") { openIpaPicker() }
            addButton(body, "▣  My imported IPA files") { setPage("ios", "ipa") }
            addButton(body, "⬇  Install CYBORG app package (.capp)") { openCyborgPackagePicker() }
        } else {
            addButton(body, "↓  Install APK from files") { openApkPicker() }
            addButton(body, "⬇  Install CYBORG app package (.capp)") { openCyborgPackagePicker() }
            addButton(body, "▦  Browse apps installed on this phone") { setPage("android", "apps") }
            addButton(body, "Open Google Play") {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=apps")))
                } catch (_: Exception) {
                    runIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps")),
                        "Google Play is unavailable")
                }
            }
        }

        body.addView(text(if (ios) "CYBORG APP SHELF" else "CYBORG MODULES",
            9.5f, Color.WHITE, true))
        catalogue.forEach { item ->
            addActionRow(body, item.icon, item.title, item.info, "ADD") {
                if (tiles.any { it.id == item.id }) toast(item.title + " is already on a workspace")
                else {
                    tiles.add(AppTile(item.id, item.title, item.icon, side))
                    saveTiles()
                    toast(item.title + " added to " + if (ios) "iPhone" else "Android")
                    render()
                }
            }
        }
        addButton(body, "＋  Create a CYBORG mini-app") { makerDialog(side) }
        body.addView(text(
            if (ios)
                "Important: .ipa files are iOS binaries. Android cannot install or execute iPhone games. CYBORG can archive IPA files, or install .capp web-app shortcuts that open compatible online games. This is not Apple's App Store."
            else
                ".capp packages install inside CYBORG as web-app shortcuts. APK files are handed to Android's installer and need system confirmation.",
            8f, Color.LTGRAY
        ))
    }

    private fun renderIpaArchives(body: LinearLayout) {
        body.addView(text("Imported IPA packages", 17f, Color.WHITE, true))
        body.addView(text("Stored as files only. CYBORG can catalogue them, but cannot execute iOS binaries on Android.",
            8.5f, Color.LTGRAY))
        val arr = JSONArray(prefs.getString("ipa_archives", "[]"))
        if (arr.length() == 0) {
            body.addView(text("No IPA files imported yet.", 10f, Color.LTGRAY))
        }
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val title = item.optString("name", "iOS package")
            addActionRow(body, "▣", title, "IPA archive • not installable on Android", "INFO") {
                AlertDialog.Builder(this).setTitle(title)
                    .setMessage("This is an iOS app package archive. Android phones cannot install or run iOS .ipa apps. The file remains listed here as an archive.")
                    .setPositiveButton("OK", null).show()
            }
        }
        addButton(body, "＋  Import another IPA") { openIpaPicker() }
    }

    @Suppress("DEPRECATION")
    private fun installedLaunchers() =
        packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.ROOT) }

    private fun renderInstalledApps(body: LinearLayout, side: String) {
        body.addView(text("Apps installed on this phone", 15f, Color.WHITE, true))
        body.addView(text("Pin one to either half. Its icon can then be dragged to the other side.", 9f, Color.LTGRAY))
        val pinned = tiles.map { it.packageName }.toSet()
        val apps = installedLaunchers()
        if (apps.isEmpty()) body.addView(text("No launchable apps found.", 11f, Color.LTGRAY))
        apps.take(60).forEach { info ->
            val pkg = info.activityInfo.packageName
            val title = info.loadLabel(packageManager).toString()
            addActionRow(body, "◈", title, pkg, if (pinned.contains(pkg)) "OPEN" else "PIN") {
                if (pinned.contains(pkg)) {
                    val launch = packageManager.getLaunchIntentForPackage(pkg)
                    if (launch != null) runIntent(launch, "Could not open " + title)
                } else {
                    tiles.add(AppTile("pkg:" + pkg, title, "◈", side, pkg))
                    saveTiles()
                    toast(title + " pinned")
                    render()
                }
            }
        }
    }

    private fun renderSearch(body: LinearLayout, accent: Int) {
        body.addView(text("MyKit Search", 18f, Color.WHITE, true))
        body.addView(text("Search your app shelf or the web.", 9f, accent))
        val input = EditText(this).apply {
            hint = "App, setting or search phrase"
            textSize = 13f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            background = rounded(Color.argb(100, 255, 255, 255), 12)
            setPadding(dp(10), dp(7), dp(10), dp(7))
        }
        body.addView(input, LinearLayout.LayoutParams(-1, dp(44)).apply { topMargin = dp(8) })
        addButton(body, "⌕  Search Cyborg apps") {
            val q = input.text.toString().trim()
            val found = tiles.filter { it.title.contains(q, true) || it.id.contains(q, true) }
            if (q.isBlank()) toast("Type something first")
            else if (found.isEmpty()) toast("No app found. Try Web Search.")
            else AlertDialog.Builder(this).setTitle("Search results")
                .setMessage(found.joinToString("\n") { it.title + "  •  " + it.side })
                .setPositiveButton("OK", null).show()
        }
        addButton(body, "🌐  Search the web") {
            val q = input.text.toString().trim()
            if (q.isNotEmpty()) runIntent(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q))),
                "No browser is available"
            )
        }
    }

    private fun renderNotes(body: LinearLayout, accent: Int) {
        body.addView(text("Shared Notes", 18f, Color.WHITE, true))
        body.addView(text("One notebook for both system halves.", 9f, accent))
        val input = EditText(this).apply {
            hint = "Write something..."
            minLines = 5
            gravity = Gravity.TOP
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setText(prefs.getString("notes", ""))
            background = rounded(Color.argb(95, 255, 255, 255), 12)
            setPadding(dp(10), dp(9), dp(10), dp(9))
        }
        body.addView(input, LinearLayout.LayoutParams(-1, dp(145)).apply { topMargin = dp(8) })
        addButton(body, "SAVE NOTE") {
            prefs.edit().putString("notes", input.text.toString()).apply()
            toast("Saved to shared Cyborg storage")
        }
    }

    private fun renderSettings(body: LinearLayout, accent: Int) {
        body.addView(text("Cyborg settings", 18f, Color.WHITE, true))
        body.addView(text("Independent interfaces, one shared app shelf.", 9f, accent))
        addButton(body, "↔  How to transfer an app") {
            toast("Long-press an app tile, drag across the divider, then release")
        }
        addButton(body, "⌖  Toggle PC pointer") {
            pointerEnabled = !pointerEnabled
            pointerVisible = pointerEnabled
            pointerLayer.invalidate()
            toast(if (pointerEnabled) "Pointer on" else "Pointer off")
        }
        addButton(body, "⌂  Return to both home screens") {
            iosPage = null; androidPage = null; render()
        }
        addButton(body, "About CYBORG OS 1.0") {
            AlertDialog.Builder(this).setTitle("CYBORG OS")
                .setMessage("A self-contained virtual-device workspace running inside Android. The host phone still provides the underlying operating system.")
                .setPositiveButton("OK", null).show()
        }
    }

    private fun addButton(parent: LinearLayout, title: String, action: () -> Unit) {
        val button = TextView(this).apply {
            text = title
            textSize = 10.5f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(9), dp(9), dp(7), dp(9))
            background = rounded(Color.argb(95, 255, 255, 255), 11, Color.argb(55, 255, 255, 255))
            setOnClickListener { action() }
        }
        parent.addView(button, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(3); bottomMargin = dp(3)
        })
    }

    private fun addActionRow(
        parent: LinearLayout, icon: String, title: String, detail: String,
        actionTitle: String, action: () -> Unit
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.argb(75, 255, 255, 255), 11)
            setPadding(dp(7), dp(5), dp(6), dp(5))
        }
        row.addView(text(icon, 19f, Color.WHITE, true), LinearLayout.LayoutParams(dp(29), dp(42)))
        val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        details.addView(text(title, 10.5f, Color.WHITE, true))
        details.addView(text(detail, 8f, Color.rgb(193, 207, 227)))
        row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(this).apply {
            text = actionTitle; textSize = 8.5f; setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = rounded(Color.argb(150, 92, 119, 191), 8)
            setPadding(dp(8), dp(7), dp(8), dp(7))
            setOnClickListener { action() }
        })
        parent.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(3); bottomMargin = dp(3)
        })
    }

    private fun makerDialog(side: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(7), dp(16), dp(3))
        }
        val name = EditText(this).apply { hint = "App name"; setSingleLine(true) }
        val icon = EditText(this).apply { hint = "Icon / emoji"; setSingleLine(true); setText("✦") }
        val url = EditText(this).apply { hint = "Optional web address"; setSingleLine(true) }
        box.addView(name); box.addView(icon); box.addView(url)
        AlertDialog.Builder(this).setTitle("Create a Cyborg app")
            .setMessage("Save an app name, icon and optional web address.")
            .setView(box)
            .setPositiveButton("CREATE") { _, _ ->
                val title = name.text.toString().trim()
                if (title.isBlank()) toast("Enter an app name")
                else {
                    tiles.add(AppTile("custom:" + System.currentTimeMillis(), title,
                        icon.text.toString().ifBlank { "✦" }, side, "", url.text.toString().trim()))
                    saveTiles()
                    render()
                    toast(title + " created")
                }
            }
            .setNegativeButton("CANCEL", null).show()
    }

    private fun openTile(tile: AppTile) {
        if (tile.packageName.isNotBlank()) {
            val intent = packageManager.getLaunchIntentForPackage(tile.packageName)
            if (intent != null) runIntent(intent, "App is not available")
            else toast("App is not available on this phone")
            return
        }
        if (tile.url.isNotBlank()) {
            var address = tile.url
            if (!address.startsWith("http://") && !address.startsWith("https://")) address = "https://" + address
            showCyborgWebApp(tile.title, address)
            return
        }
        when (tile.id) {
            "appstore", "playstore" -> setPage(tile.side, "store")
            "gamehub", "androidgames" -> setPage(tile.side, "games")
            "securitylab" -> setPage("android", "securitylab")
            "mykit", "androidsearch" -> setPage(tile.side, "search")
            "notes", "shared_notes" -> setPage(tile.side, "notes")
            "iossettings", "androidsettings" -> runIntent(Intent(Settings.ACTION_SETTINGS), "Settings unavailable")
            "phone" -> runIntent(Intent(Intent.ACTION_DIAL), "Phone app unavailable")
            "messages" -> runIntent(Intent(Intent.ACTION_VIEW, Uri.parse("sms:")), "Messages app unavailable")
            "photos" -> runIntent(Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), "Photo picker unavailable")
            "camera" -> runIntent(Intent(MediaStore.ACTION_IMAGE_CAPTURE), "Camera unavailable")
            "files" -> runIntent(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*")
                .addCategory(Intent.CATEGORY_OPENABLE), "File picker unavailable")
            "safari", "chrome", "web" -> runIntent(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")), "Browser unavailable")
            "iosmaps", "maps" -> runIntent(
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=Berlin")), "Maps unavailable")
            "youtube" -> runIntent(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")), "Browser unavailable")
            "clock" -> runIntent(Intent("android.intent.action.SHOW_ALARMS"), "Clock unavailable")
            "calculator" -> openCalculator()
            "weather" -> runIntent(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://weather.com")), "Browser unavailable")
            "calendar" -> runIntent(
                Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time/")), "Calendar unavailable")
            "pcdesk" -> showPcDesktop()
            "apkmanager" -> setPage("android", "store")
            "ipaarchive" -> setPage("ios", "ipa")
            "iossettings" -> setPage("ios", "control")
            "androidsettings" -> setPage("android", "quick")
            else -> AlertDialog.Builder(this).setTitle(tile.title)
                .setMessage("This Cyborg module is ready for an app or web shortcut.")
                .setPositiveButton("OK", null).show()
        }
    }

    private fun openCalculator() {
        val names = listOf("com.google.android.calculator", "com.android.calculator2")
        val pkg = names.firstOrNull { packageManager.getLaunchIntentForPackage(it) != null }
        val intent = if (pkg == null) null else packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) runIntent(intent, "Calculator unavailable")
        else toast("No calculator app was found")
    }

    private fun runIntent(intent: Intent, error: String) {
        try { startActivity(intent) } catch (_: Exception) { toast(error) }
    }

    private fun buildPcDock(): View {
        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(5), dp(8), dp(7))
            background = rounded(Color.rgb(16, 23, 39), 15, Color.rgb(58, 75, 111))
        }
        if (trackpadVisible) {
            dock.addView(TextView(this).apply {
                text = "TRACKPAD  •  move finger to move the pointer"
                textSize = 8.5f; gravity = Gravity.CENTER
                setTextColor(Color.rgb(214, 226, 245))
                background = rounded(Color.rgb(33, 43, 64), 10, Color.rgb(88, 112, 158))
                setOnTouchListener(object : View.OnTouchListener {
                    var px = 0f
                    var py = 0f
                    override fun onTouch(v: View, e: MotionEvent): Boolean {
                        if (e.actionMasked == MotionEvent.ACTION_DOWN) { px = e.x; py = e.y }
                        if (e.actionMasked == MotionEvent.ACTION_MOVE) {
                            pointerX += (e.x - px) * 1.5f
                            pointerY += (e.y - py) * 1.5f
                            px = e.x; py = e.y
                            pointerAt = System.currentTimeMillis()
                            pointerVisible = true
                            pointerLayer.invalidate()
                        }
                        return true
                    }
                })
            }, LinearLayout.LayoutParams(-1, dp(36)).apply { bottomMargin = dp(5) })
        }

        val mainScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val mainRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        pcButton(mainRow, "🖱 Mouse") {
            pointerEnabled = !pointerEnabled
            pointerVisible = pointerEnabled
            pointerLayer.invalidate()
            toast(if (pointerEnabled) "PC pointer on" else "PC pointer off")
        }
        pcButton(mainRow, "⌨ Keyboard") { keyboardDialog() }
        pcButton(mainRow, if (ctrlLatched) "CTRL ✓" else "CTRL") {
            ctrlLatched = !ctrlLatched; toast(if (ctrlLatched) "CTRL armed" else "CTRL off")
        }
        pcButton(mainRow, if (altLatched) "ALT ✓" else "ALT") {
            altLatched = !altLatched; toast(if (altLatched) "ALT armed" else "ALT off")
        }
        pcButton(mainRow, if (shiftLatched) "SHIFT ✓" else "SHIFT") {
            shiftLatched = !shiftLatched; toast(if (shiftLatched) "SHIFT armed" else "SHIFT off")
        }
        pcButton(mainRow, "TAB") { sendVirtualKey(KeyEvent.KEYCODE_TAB) }
        pcButton(mainRow, "ESC") { sendVirtualKey(KeyEvent.KEYCODE_ESCAPE) }
        pcButton(mainRow, "ENTER") { sendVirtualKey(KeyEvent.KEYCODE_ENTER) }
        pcButton(mainRow, "⌫") { sendVirtualKey(KeyEvent.KEYCODE_DEL) }
        pcButton(mainRow, "DEL") { sendVirtualKey(KeyEvent.KEYCODE_FORWARD_DEL) }
        pcButton(mainRow, "⌖ Trackpad") { trackpadVisible = !trackpadVisible; render() }
        pcButton(mainRow, "▤ Desktop") { showPcDesktop() }
        pcButton(mainRow, "⌂ Home") { iosPage = null; androidPage = null; render() }
        mainScroll.addView(mainRow)
        dock.addView(mainScroll, LinearLayout.LayoutParams(-1, dp(39)))

        val functionScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val functionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (n in 1..12) {
            pcButton(functionRow, "F$n") { sendVirtualKey(KeyEvent.KEYCODE_F1 + n - 1) }
        }
        pcButton(functionRow, "←") { sendVirtualKey(KeyEvent.KEYCODE_DPAD_LEFT) }
        pcButton(functionRow, "↑") { sendVirtualKey(KeyEvent.KEYCODE_DPAD_UP) }
        pcButton(functionRow, "↓") { sendVirtualKey(KeyEvent.KEYCODE_DPAD_DOWN) }
        pcButton(functionRow, "→") { sendVirtualKey(KeyEvent.KEYCODE_DPAD_RIGHT) }
        pcButton(functionRow, "INS") { sendVirtualKey(KeyEvent.KEYCODE_INSERT) }
        pcButton(functionRow, "HOME") { iosPage = null; androidPage = null; render() }
        pcButton(functionRow, "END") { sendVirtualKey(KeyEvent.KEYCODE_MOVE_END) }
        pcButton(functionRow, "PG↑") { sendVirtualKey(KeyEvent.KEYCODE_PAGE_UP) }
        pcButton(functionRow, "PG↓") { sendVirtualKey(KeyEvent.KEYCODE_PAGE_DOWN) }
        functionScroll.addView(functionRow)
        dock.addView(functionScroll, LinearLayout.LayoutParams(-1, dp(39)).apply {
            topMargin = dp(3)
        })
        return dock
    }

    private fun pcButton(parent: LinearLayout, title: String, action: () -> Unit) {
        parent.addView(TextView(this).apply {
            text = title; textSize = 9.5f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            background = rounded(Color.rgb(40, 54, 80), 10, Color.rgb(77, 96, 133))
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(-2, dp(37)).apply { setMargins(dp(3), 0, dp(3), 0) })
    }

    private fun sendVirtualKey(keyCode: Int) {
        if (keyCode in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12) {
            ctrlLatched = false; altLatched = false; shiftLatched = false
            handleFunctionKey(keyCode)
            return
        }
        if (keyCode == KeyEvent.KEYCODE_TAB) {
            ctrlLatched = false; altLatched = false; shiftLatched = false
            cycleActiveSide()
            return
        }
        if (keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_BACK) {
            ctrlLatched = false; altLatched = false; shiftLatched = false
            onBackPressed()
            return
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
            keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            ctrlLatched = false; altLatched = false; shiftLatched = false
            moveFocus(keyCode)
            return
        }

        val meta = (if (ctrlLatched) KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON else 0) or
            (if (altLatched) KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON else 0) or
            (if (shiftLatched) KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON else 0)
        val now = System.currentTimeMillis()
        val target = currentFocus ?: root
        target.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
        target.dispatchKeyEvent(KeyEvent(now, now + 1, KeyEvent.ACTION_UP, keyCode, 0, meta))
        ctrlLatched = false; altLatched = false; shiftLatched = false
    }

    private fun handleFunctionKey(keyCode: Int) {
        when (keyCode) {
            KeyEvent.KEYCODE_F1 -> showKeyboardHelp()
            KeyEvent.KEYCODE_F2 -> renameSelectedTile()
            KeyEvent.KEYCODE_F3 -> setPage(activeSide, "search")
            KeyEvent.KEYCODE_F4 -> showPcDesktop()
            KeyEvent.KEYCODE_F5 -> { render(); toast("CYBORG VM refreshed") }
            KeyEvent.KEYCODE_F6 -> cycleActiveSide()
            KeyEvent.KEYCODE_F7 -> keyboardDialog()
            KeyEvent.KEYCODE_F8 -> { trackpadVisible = !trackpadVisible; render() }
            KeyEvent.KEYCODE_F9 -> setPage(activeSide, "apps")
            KeyEvent.KEYCODE_F10 -> runIntent(Intent(Settings.ACTION_SETTINGS), "Settings unavailable")
            KeyEvent.KEYCODE_F11 -> toggleFullScreen()
            KeyEvent.KEYCODE_F12 -> showCyborgAbout()
        }
    }

    private fun cycleActiveSide() {
        activeSide = if (activeSide == "ios") "android" else "ios"
        render()
        toast("Active workspace: " + if (activeSide == "ios") "iPhone side" else "Android side")
    }

    private fun moveFocus(keyCode: Int) {
        val direction = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> View.FOCUS_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> View.FOCUS_RIGHT
            KeyEvent.KEYCODE_DPAD_UP -> View.FOCUS_UP
            else -> View.FOCUS_DOWN
        }
        val current = currentFocus ?: root
        val next = current.focusSearch(direction)
        if (next != null) next.requestFocus()
        else {
            val step = dp(18).toFloat()
            when (direction) {
                View.FOCUS_LEFT -> pointerX -= step
                View.FOCUS_RIGHT -> pointerX += step
                View.FOCUS_UP -> pointerY -= step
                View.FOCUS_DOWN -> pointerY += step
            }
            pointerVisible = true
            pointerAt = System.currentTimeMillis()
            pointerLayer.invalidate()
        }
    }

    private fun renameSelectedTile() {
        val index = tiles.indexOfFirst { it.id == selectedTileId }
        if (index < 0) {
            toast("Tap an app tile first, then press F2")
            return
        }
        val input = EditText(this).apply {
            setSingleLine(true)
            setText(tiles[index].title)
            setSelection(text.length)
        }
        AlertDialog.Builder(this).setTitle("Rename app tile")
            .setView(input)
            .setPositiveButton("SAVE") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotBlank()) {
                    tiles[index] = tiles[index].copy(title = newName)
                    saveTiles()
                    render()
                }
            }
            .setNegativeButton("CANCEL", null).show()
    }

    private fun showKeyboardHelp() {
        AlertDialog.Builder(this).setTitle("CYBORG PC keys")
            .setMessage(
                "F1 Help • F2 Rename selected app • F3 Search • F4 Desktop\n" +
                "F5 Refresh • F6 Switch workspace • F7 Keyboard • F8 Trackpad\n" +
                "F9 Installed apps • F10 Android settings • F11 Full screen • F12 About\n\n" +
                "Arrow keys move focus. TAB switches between the two workspaces. " +
                "CTRL, ALT and SHIFT are one-shot modifiers for the next key."
            )
            .setPositiveButton("OK", null).show()
    }

    @Suppress("DEPRECATION")
    private fun toggleFullScreen() {
        fullScreen = !fullScreen
        window.decorView.systemUiVisibility = if (fullScreen) {
            (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        } else {
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        toast(if (fullScreen) "Full screen on • swipe from an edge to show system bars"
            else "Full screen off")
    }

    private fun showCyborgAbout() {
        AlertDialog.Builder(this).setTitle("CYBORG VM 1.5")
            .setMessage(
                "A virtual-device-style dual workspace with PC controls. The two screens recreate familiar mobile interface patterns; they are not separate bootable Apple or Google operating systems. Android remains the host."
            )
            .setPositiveButton("OK", null).show()
    }

    private fun keyboardDialog() {
        val input = EditText(this).apply { hint = "Type here with your phone keyboard"; minLines = 2 }
        val dialog = AlertDialog.Builder(this).setTitle("PC Keyboard")
            .setMessage("Keyboard bridge for the Cyborg desktop.")
            .setView(input).setPositiveButton("DONE", null).setNegativeButton("CLOSE", null).create()
        dialog.setOnShowListener {
            input.requestFocus()
            input.post {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        dialog.show()
    }

    private fun showPcDesktop() {
        val dialog = Dialog(this)
        val desktop = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(18))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(20, 35, 65), Color.rgb(8, 19, 35)))
        }
        desktop.addView(text("CYBORG DESKTOP", 21f, Color.WHITE, true))
        desktop.addView(text("PC MODE  •  WINDOW WORKSPACE", 10f, Color.rgb(161, 196, 235), true))
        desktop.addView(text("Mouse, keyboard and app windows in one workspace.", 11f, Color.rgb(205, 219, 239)))
        desktopCard(desktop, "▣  File Explorer", "Open the document picker") { runIntent(
            Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),
            "File picker unavailable"); dialog.dismiss() }
        desktopCard(desktop, "🌐  Web Browser", "Open a browser") {
            runIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")), "Browser unavailable")
            dialog.dismiss()
        }
        desktopCard(desktop, "▦  App Manager", "Pin an installed app") {
            androidPage = "apps"; dialog.dismiss(); render()
        }
        desktopCard(desktop, "⚙  Device Settings", "Open Android settings") {
            runIntent(Intent(Settings.ACTION_SETTINGS), "Settings unavailable"); dialog.dismiss()
        }
        desktopCard(desktop, "⌨  Keyboard", "Open keyboard bridge") { dialog.dismiss(); keyboardDialog() }
        desktop.addView(TextView(this).apply {
            text = "CLOSE DESKTOP"; gravity = Gravity.CENTER; textSize = 12f; setTextColor(Color.WHITE)
            background = rounded(Color.rgb(71, 100, 160), 12)
            setPadding(dp(12), dp(13), dp(12), dp(13))
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        dialog.setContentView(desktop)
        dialog.show()
        dialog.window?.setLayout(-1, -1)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun desktopCard(parent: LinearLayout, title: String, info: String, action: () -> Unit) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(9), dp(12), dp(9))
            background = rounded(Color.argb(155, 255, 255, 255), 13, Color.argb(75, 255, 255, 255))
            setOnClickListener { action() }
        }
        card.addView(text(title, 14f, Color.WHITE, true))
        card.addView(text(info, 10f, Color.rgb(201, 217, 239)))
        parent.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(7); bottomMargin = dp(3)
        })
    }

    private inner class PointerLayer(context: Context) : View(context) {
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; style = Paint.Style.FILL
        }
        private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(10, 15, 25); style = Paint.Style.STROKE; strokeWidth = dp(2).toFloat()
        }
        private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(150, 105, 205, 255); style = Paint.Style.STROKE; strokeWidth = dp(1).toFloat()
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (!pointerEnabled || !pointerVisible || System.currentTimeMillis() - pointerAt > 1150L) return
            val x = pointerX + dp(8)
            val y = pointerY + dp(10)
            canvas.drawCircle(x + dp(7), y + dp(8), dp(13).toFloat(), halo)
            val path = Path().apply {
                moveTo(x, y); lineTo(x + dp(17), y + dp(13))
                lineTo(x + dp(10), y + dp(14)); lineTo(x + dp(15), y + dp(24))
                lineTo(x + dp(11), y + dp(26)); lineTo(x + dp(6), y + dp(16))
                lineTo(x + dp(1), y + dp(21)); close()
            }
            canvas.drawPath(path, fill)
            canvas.drawPath(path, outline)
            postInvalidateDelayed(75L)
        }
    }
}
