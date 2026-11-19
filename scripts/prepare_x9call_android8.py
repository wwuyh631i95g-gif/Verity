from pathlib import Path
import re, sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else "Verity-main")
main = root / "app/src/main/java/dev/veriti/app/MainActivity.kt"
s = main.read_text(encoding="utf-8")

# The HIG 1.3.1 artifacts require Android 10 (API 29). Replace these
# composables with the Material 3 components already used by Verity.
s = re.sub(r"^import zone\\.ien\\.hig\\..*\\n", "", s, flags=re.M)
anchor = "import androidx.compose.material3.AlertDialog\\n"
imports = """import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.NavigationBar as MaterialNavigationBar
import androidx.compose.material3.NavigationBarItem as MaterialNavigationBarItem
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.material3.Scaffold as MaterialScaffold
import androidx.compose.material3.Slider as MaterialSlider
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.material3.TopAppBar as MaterialTopAppBar
"""
if "import androidx.compose.material3.Button as MaterialButton" not in s:
    if anchor not in s:
        raise SystemExit("Material 3 import insertion point not found")
    s = s.replace(anchor, anchor + imports, 1)

s = s.replace("AdaptiveSwitch(", "Switch(")

start = s.find("@OptIn(ExperimentalAdaptiveApi::class)\\n@Composable\\nprivate fun VeritiTheme(")
end = s.find("\\nprivate enum class Screen", start)
if start < 0 or end < 0:
    raise SystemExit("Adaptive theme block not found")
new_theme = """@Composable
private fun VeritiTheme(style: String, accent: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val materialScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && style == "material") {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (style == "sf") {
        if (dark) darkColorScheme(primary = Color(0xFF0A84FF), background = Color(0xFF000000), surface = Color(0xFF1C1C1E), surfaceVariant = Color(0xFF2C2C2E))
        else lightColorScheme(primary = Color(0xFF007AFF), background = Color(0xFFF2F2F7), surface = Color.White, surfaceVariant = Color(0xFFE5E5EA))
    } else if (dark) VeritiDark else VeritiLight
    val accentColors = accentPalette(accent)
    val finalScheme = materialScheme.copy(primary = accentColors.primary, secondary = accentColors.secondary, tertiary = accentColors.tertiary)
    val shapes = if (style == "sf") {
        Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp))
    } else {
        Shapes(extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(16.dp), medium = RoundedCornerShape(24.dp), large = RoundedCornerShape(32.dp), extraLarge = RoundedCornerShape(44.dp))
    }
    MaterialTheme(colorScheme = finalScheme, shapes = shapes, content = content)
}
"""
s = s[:start] + new_theme + s[end:]

start = s.find("@OptIn(ExperimentalAdaptiveApi::class)\\n@Composable\\nprivate fun AppBottomBar(")
end = s.find("\\n@Composable\\nprivate fun StyleTabIcon", start)
if start < 0 or end < 0:
    raise SystemExit("Adaptive bottom bar block not found")
new_nav = """@Composable
private fun AppBottomBar(selected: Screen, sfStyle: Boolean, onSelected: (Screen) -> Unit) {
    if (sfStyle) {
        val items = listOf(
            Triple(Screen.CHAT, R.drawable.sf_bubble, "Чаты"), Triple(Screen.GAME, R.drawable.sf_gamecontroller, "Игра"),
            Triple(Screen.STUDIO, R.drawable.sf_star, "Мастерская"), Triple(Screen.SETTINGS, R.drawable.sf_gear, "Настройки"),
            Triple(Screen.PLUS, R.drawable.sf_star, "Verity+"), Triple(Screen.LIFE, R.drawable.sf_bubble, "Мир")
        )
        Surface(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp),
            color = Color(0xE61C1C1E), shape = RoundedCornerShape(28.dp), shadowElevation = 14.dp) {
            Row(Modifier.fillMaxWidth().padding(5.dp)) {
                items.forEach { (screen, icon, label) ->
                    val selectedColor by animateColorAsState(if (selected == screen) Color(0x553A3A3C) else Color.Transparent, label = "cupertino tab glass")
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(selectedColor)
                        .clickable { onSelected(screen) }.padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Image(painterResource(icon), contentDescription = label, modifier = Modifier.size(22.dp),
                            colorFilter = ColorFilter.tint(if (selected == screen) Color.White else Color(0xFFAEAEB2)))
                        Text(label, color = if (selected == screen) Color.White else Color(0xFFAEAEB2), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        return
    }
    MaterialNavigationBar {
        MaterialNavigationBarItem(selected = selected == Screen.CHAT, onClick = { onSelected(Screen.CHAT) },
            icon = { Icon(Icons.Rounded.ChatBubbleOutline, null) }, label = { Text("Чаты") })
        MaterialNavigationBarItem(selected = selected == Screen.GAME, onClick = { onSelected(Screen.GAME) },
            icon = { Icon(Icons.Rounded.AutoAwesome, null) }, label = { Text("Игра") })
        MaterialNavigationBarItem(selected = selected == Screen.STUDIO, onClick = { onSelected(Screen.STUDIO) },
            icon = { Icon(Icons.Rounded.DashboardCustomize, null) }, label = { Text("Мастерская") })
        MaterialNavigationBarItem(selected = selected == Screen.SETTINGS, onClick = { onSelected(Screen.SETTINGS) },
            icon = { Icon(Icons.Rounded.Settings, null) }, label = { Text("Настройки") })
        MaterialNavigationBarItem(selected = selected == Screen.PLUS, onClick = { onSelected(Screen.PLUS) },
            icon = { Icon(Icons.Rounded.Star, null) }, label = { Text("Verity+") })
        MaterialNavigationBarItem(selected = selected == Screen.LIFE, onClick = { onSelected(Screen.LIFE) },
            icon = { Icon(Icons.Rounded.AutoAwesome, null) }, label = { Text("Мир") })
    }
}
"""
s = s[:start] + new_nav + s[end:]

s = s.replace(") = AdaptiveButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)",
              ") = MaterialButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)")
s = s.replace(") = AdaptiveTonalButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)",
              ") = MaterialOutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)")
s = s.replace(") = AdaptiveTextButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)",
              ") = MaterialTextButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)")
s = s.replace(") = AdaptiveSlider(\\n", ") = MaterialSlider(\\n")
s = s.replace(") = AdaptiveScaffold(\\n", ") = MaterialScaffold(\\n")
s = s.replace(") = AdaptiveTopAppBar(\\n    title = title,\\n    modifier = modifier,\\n    navigationIcon = navigationIcon,\\n    actions = actions,\\n    adaptation = { material { this.colors = colors } }\\n)",
              ") = MaterialTopAppBar(\\n    title = title,\\n    modifier = modifier,\\n    navigationIcon = navigationIcon,\\n    actions = actions,\\n    colors = colors\\n)")
s = s.replace("@OptIn(ExperimentalAdaptiveApi::class)\\n", "")
s = s.replace("@Composable\\nprivate fun TopAppBar(\\n", "@OptIn(ExperimentalMaterial3Api::class)\\n@Composable\\nprivate fun TopAppBar(\\n", 1)

if any(x in s for x in ["zone.ien.hig", "AdaptiveTheme", "AdaptiveSwitch", "AdaptiveScaffold", "AdaptiveButton", "AdaptiveTopAppBar"]):
    raise SystemExit("Unconverted HIG API remains in MainActivity.kt")
main.write_text(s, encoding="utf-8")

gradle = root / "app/build.gradle.kts"
g = gradle.read_text(encoding="utf-8")
g = "".join(line for line in g.splitlines(keepends=True)
            if "zone.ien.hig:hig:" not in line and "zone.ien.hig:hig-adaptive:" not in line)
gradle.write_text(g, encoding="utf-8")

manifest = root / "app/src/main/AndroidManifest.xml"
m = manifest.read_text(encoding="utf-8")
m = re.sub(r"\\s*<uses-sdk\\s+tools:overrideLibrary=\"[^\"]*\"\\s*/>", "", m)
m = m.replace(' xmlns:tools="http://schemas.android.com/tools"', "")
m = re.sub(r' android:foregroundServiceType="specialUse"', "", m)
m = re.sub(r'\\s*<property\\s+android:name="android\\.app\\.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"\\s+android:value="[^"]*"\\s*/>', "", m)
manifest.write_text(m, encoding="utf-8")

print("Prepared Verity for Android 8: replaced Android-10-only HIG components with Compose Material 3.")
