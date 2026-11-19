from pathlib import Path
import re
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else "Verity-main")
main = root / "app/src/main/java/dev/veriti/app/MainActivity.kt"
s = main.read_text(encoding="utf-8")
if "import androidx.compose.ui.unit.sp" not in s:
    dp_import = "import androidx.compose.ui.unit.dp\n"
    if dp_import not in s:
        raise SystemExit("Compose dp import insertion point not found")
    s = s.replace(dp_import, dp_import + "import androidx.compose.ui.unit.sp\n", 1)

def once(old: str, new: str, label: str):
    global s
    count = s.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, got {count}")
    s = s.replace(old, new, 1)

once(
    "private enum class Screen {CHAT, GAME, SETTINGS, PLUS, CUSTOMIZATIONS, ABOUT, SKIN_EDITOR, CHARACTERS, APP_BUILDER, HOME_ASSISTANT, FRIENDS, FEATURES, LIFE, STUDIO, VIRTUAL_OS}\n",
    "private enum class Screen {CHAT, GAME, SETTINGS, PLUS, CUSTOMIZATIONS, ABOUT, SKIN_EDITOR, CHARACTERS, APP_BUILDER, HOME_ASSISTANT, FRIENDS, FEATURES, LIFE, STUDIO, VIRTUAL_OS}\n\nprivate fun isX9CallSizedDisplay(configuration: android.content.res.Configuration): Boolean =\n    configuration.screenWidthDp <= 420 && configuration.screenHeightDp <= 720\n",
    "display helper"
)
once(
    "        AdMobController.requestConsentAndInitialize(this)\n        setContent { VerityRoot() }",
    "        if (!isX9CallSizedDisplay(resources.configuration)) AdMobController.requestConsentAndInitialize(this)\n        setContent { VerityRoot() }",
    "skip startup ads on compact display"
)
once(
    "private fun VerityRoot() {\n    val context = LocalContext.current",
    "private fun VerityRoot() {\n    val context = LocalContext.current\n    val compactWatchLayout = isX9CallSizedDisplay(LocalConfiguration.current)",
    "root display detection"
)
once(
    "        if (onboardingComplete) {\n            VeritiApp(",
    "        if (onboardingComplete || compactWatchLayout) {\n            VeritiApp(",
    "bypass phone-first onboarding on watch"
)
once(
    "    val context = LocalContext.current\n    val state = vm.state\n    AppSpeechEffect(state.settings)",
    "    val context = LocalContext.current\n    val compactWatchLayout = isX9CallSizedDisplay(LocalConfiguration.current)\n    val state = vm.state\n    AppSpeechEffect(state.settings)",
    "app display detection"
)
once(
    "                AppBottomBar(selected = screen, sfStyle = state.settings.uiStyle == \"sf\", onSelected = { screen = it })",
    "                if (compactWatchLayout) X9CallBottomBar(selected = screen, onSelected = { screen = it })\n                else AppBottomBar(selected = screen, sfStyle = state.settings.uiStyle == \"sf\", onSelected = { screen = it })",
    "compact bottom bar"
)
once(
    "                showApiSuggestion = state.settings.apiKey.isBlank(),\n                onMenu = { scope.launch { drawerState.open() } },",
    "                showApiSuggestion = state.settings.apiKey.isBlank(),\n                compactWatchLayout = compactWatchLayout,\n                onMenu = { scope.launch { drawerState.open() } },",
    "pass compact layout to chat"
)
once(
    "    ModalDrawerSheet(modifier = Modifier.width(320.dp)) {",
    "    ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.88f).widthIn(max = 320.dp)) {",
    "responsive drawer width"
)
once(
    "    showApiSuggestion: Boolean,\n    onMenu: () -> Unit,",
    "    showApiSuggestion: Boolean,\n    compactWatchLayout: Boolean,\n    onMenu: () -> Unit,",
    "chat parameter"
)
once(
    "                    actions = {\n                        IconButton(onClick = { mainAvatarLauncher.launch(\"image/*\") }) { Icon(Icons.Rounded.Add, \"Добавить фото персонажа\") }\n                        IconButton(onClick = onProvider) { StyleTabIcon(sfStyle, R.drawable.sf_gear) { Icon(Icons.Rounded.Settings, \"Модель и API\") } }\n                    },",
    "                    actions = {\n                        if (!compactWatchLayout) IconButton(onClick = { mainAvatarLauncher.launch(\"image/*\") }) { Icon(Icons.Rounded.Add, \"Добавить фото персонажа\") }\n                        IconButton(onClick = onProvider) { StyleTabIcon(sfStyle, R.drawable.sf_gear) { Icon(Icons.Rounded.Settings, \"Модель и API\") } }\n                    },",
    "compact chat header"
)
once(
    "                if (!settings.verityPlus) AdPlacement(onOpenPlus, Modifier.padding(horizontal = 16.dp))",
    "                if (!settings.verityPlus && !compactWatchLayout) AdPlacement(onOpenPlus, Modifier.padding(horizontal = 16.dp))",
    "hide banner ad on small screen"
)
once(
    "                sfStyle = sfStyle,\n                onSend = {",
    "                sfStyle = sfStyle,\n                compactWatchLayout = compactWatchLayout,\n                onSend = {",
    "composer compact flag"
)
once(
    "                if (showApiSuggestion) {\n                    GeminiSuggestionCard(onClick = onGeminiSuggestion, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))\n                }\n                EmptyPet(settings, modifier = Modifier.fillMaxWidth().weight(1f))",
    "                if (showApiSuggestion && !compactWatchLayout) GeminiSuggestionCard(onClick = onGeminiSuggestion, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))\n                EmptyPet(settings, modifier = Modifier.fillMaxWidth().weight(1f), compact = compactWatchLayout)",
    "compact empty state"
)
once(
    "                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),\n                verticalArrangement = Arrangement.spacedBy(14.dp)\n            ) {\n                if (showApiSuggestion) item { GeminiSuggestionCard(onClick = onGeminiSuggestion) }",
    "                contentPadding = PaddingValues(horizontal = if (compactWatchLayout) 6.dp else 16.dp, vertical = if (compactWatchLayout) 8.dp else 18.dp),\n                verticalArrangement = Arrangement.spacedBy(if (compactWatchLayout) 6.dp else 14.dp)\n            ) {\n                if (showApiSuggestion && !compactWatchLayout) item { GeminiSuggestionCard(onClick = onGeminiSuggestion) }",
    "compact message list"
)
once(
    "private fun EmptyPet(settings: AppSettings, modifier: Modifier) {",
    "private fun EmptyPet(settings: AppSettings, modifier: Modifier, compact: Boolean = false) {",
    "pet composable signature"
)
once(
    "        VerityFace(settings, \"happy\", Modifier.offset(y = hop.dp).size(148.dp))",
    "        VerityFace(settings, \"happy\", Modifier.offset(y = hop.dp).size(if (compact) 88.dp else 148.dp))",
    "smaller pet"
)
once(
    "    sfStyle: Boolean,\n    onSend: () -> Unit\n) {\n    val focusRequester = remember { FocusRequester() }",
    "    sfStyle: Boolean,\n    compactWatchLayout: Boolean = false,\n    onSend: () -> Unit\n) {\n    val focusRequester = remember { FocusRequester() }",
    "composer signature"
)
once(
    "        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 12.dp, vertical = 8.dp)) {\n            if (attachments.isNotEmpty()) {",
    """        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = if (compactWatchLayout) 5.dp else 12.dp, vertical = if (compactWatchLayout) 4.dp else 8.dp)) {
            if (compactWatchLayout) {
                val canSend = enabled && value.isNotBlank()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Surface(Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Box(Modifier.fillMaxSize().padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                            if (value.isBlank()) Text("Сообщение…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            BasicTextField(
                                value = value, onValueChange = onValueChange,
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                enabled = enabled, maxLines = 2,
                                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() })
                            )
                        }
                    }
                    FilledIconButton(onClick = if (canSend) onSend else onVoiceInput, enabled = enabled, modifier = Modifier.size(40.dp)) {
                        if (canSend) Icon(Icons.AutoMirrored.Rounded.Send, "Отправить", Modifier.size(19.dp))
                        else Icon(Icons.Rounded.Mic, "Голосовой ввод", Modifier.size(19.dp))
                    }
                }
            } else {
                if (attachments.isNotEmpty()) {""",
    "watch composer"
)
insert_at = "@OptIn(ExperimentalAdaptiveApi::class)\n@Composable\nprivate fun AppBottomBar("
if s.count(insert_at) != 1:
    raise SystemExit("bottom-bar insertion point not found")
nav = '''@Composable
private fun X9CallBottomBar(selected: Screen, onSelected: (Screen) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 4.dp, vertical = 2.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 3.dp
    ) {
        Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 2.dp, vertical = 1.dp)) {
            X9CallNavItem("●", "Чат", selected == Screen.CHAT, Modifier.weight(1f)) { onSelected(Screen.CHAT) }
            X9CallNavItem("◈", "Игра", selected == Screen.GAME, Modifier.weight(1f)) { onSelected(Screen.GAME) }
            X9CallNavItem("⚙", "Настр.", selected == Screen.SETTINGS, Modifier.weight(1f)) { onSelected(Screen.SETTINGS) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                X9CallNavItem("⋯", "Ещё", expanded, Modifier.fillMaxSize()) { expanded = true }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Мастерская") }, onClick = { expanded = false; onSelected(Screen.STUDIO) })
                    DropdownMenuItem(text = { Text("Verity+") }, onClick = { expanded = false; onSelected(Screen.PLUS) })
                    DropdownMenuItem(text = { Text("Мир Verity") }, onClick = { expanded = false; onSelected(Screen.LIFE) })
                }
            }
        }
    }
}

@Composable
private fun X9CallNavItem(glyph: String, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(glyph, fontSize = 16.sp, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, fontSize = 8.sp, maxLines = 1, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

'''
s = s.replace(insert_at, nav + insert_at, 1)
imp = "import androidx.compose.material3.DrawerValue\n"
if s.count(imp) != 1:
    raise SystemExit("Compose import insertion point not found")
s = s.replace(imp, imp + "import androidx.compose.material3.DropdownMenu\nimport androidx.compose.material3.DropdownMenuItem\n", 1)

# Close the classic attachment and composer branches inside the non-watch case.
once(
    "                }\n            }\n        }\n    }\n}\n\n@Composable\nprivate fun FormattedAssistantText",
    "                }\n            }\n            }\n        }\n    }\n}\n\n@Composable\nprivate fun FormattedAssistantText",
    "composer close"
)
main.write_text(s, encoding="utf-8")

gradle = root / "app/build.gradle.kts"
g = gradle.read_text(encoding="utf-8")
for pattern, replacement, label in [
    (r'(?m)^\s*applicationId = "[^"]+"', '        applicationId = "dev.veriti.app.x9call"', "app id"),
    (r'(?m)^\s*minSdk = \d+', '        minSdk = 26', "min sdk"),
    (r'(?m)^\s*targetSdk = \d+', '        targetSdk = 27', "target sdk"),
    (r'(?m)^\s*versionCode = \d+', '        versionCode = 55001', "version code"),
    (r'(?m)^\s*versionName = "[^"]+"', '        versionName = "5.5.0-X9CALL"', "version name"),
]:
    g, n = re.subn(pattern, replacement, g, count=1)
    if n != 1: raise SystemExit(f"Could not patch {label}")
g = g.replace('"Verity-5.5.0-$adMode-${variant.name}.apk"', '"Verity-5.5.0-X9CALL-${variant.name}.apk"')
gradle.write_text(g, encoding="utf-8")

manifest = root / "app/src/main/AndroidManifest.xml"
m = manifest.read_text(encoding="utf-8")
m = m.replace(' android:foregroundServiceType="specialUse"', '')
m = re.sub(r'\s*<property\s+android:name="android\.app\.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"\s+android:value="[^"]*"\s*/>', '', m)
feature = '    <uses-feature android:name="android.hardware.type.watch" android:required="false" />\n'
if 'android.hardware.type.watch' not in m:
    needle = '    <uses-permission android:name="android.permission.INTERNET" />'
    if needle not in m: raise SystemExit("Manifest permission insertion point not found")
    m = m.replace(needle, feature + needle, 1)
if 'xmlns:tools=' not in m:
    m = m.replace('xmlns:android="http://schemas.android.com/apk/res/android"', 'xmlns:android="http://schemas.android.com/apk/res/android" xmlns:tools="http://schemas.android.com/tools"', 1)
if 'tools:overrideLibrary="zone.ien.highigadaptive,zone.ien.highignative"' not in m:
    manifest_start = m.find("<manifest")
    manifest_end = m.find(">", manifest_start)
    if manifest_start < 0 or manifest_end < 0:
        raise SystemExit("Manifest root insertion point not found")
    m = m[:manifest_end + 1] + '\n    <uses-sdk tools:overrideLibrary="zone.ien.highigadaptive,zone.ien.highignative" />' + m[manifest_end + 1:]
manifest.write_text(m, encoding="utf-8")

print("Prepared Verity 5.5.0 X9CALL edition for Android 8.0+.")
