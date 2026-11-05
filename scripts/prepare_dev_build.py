from pathlib import Path
import shutil

ROOT = Path("Verity-main")
APP = ROOT / "app"
JAVA = APP / "src/main/java/dev/veriti/app"

def replace_once(path: Path, old: str, new: str, label: str):
    text = path.read_text(encoding="utf-8")
    if text.count(old) != 1:
        raise SystemExit(f"{label}: expected one match, got {text.count(old)}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")

# Embed a pristine source archive for the in-app developer workspace reset.
archive = Path("Verity-5.0.1-Minecraft-2D-Build-Fix.zip")
if not archive.is_file():
    raise SystemExit(f"Source archive missing: {archive}")
assets = APP / "src/main/assets"
assets.mkdir(parents=True, exist_ok=True)
shutil.copy2(archive, assets / "verity-dev-source.zip")

# A separate application ID allows installation beside the public Verity app.
gradle = APP / "build.gradle.kts"
replace_once(gradle, 'applicationId = "dev.veriti.app"',
             'applicationId = "dev.veriti.app.developer"', "application ID")
replace_once(gradle,
             '        versionCode = 50100\n        versionName = "5.0.1"',
             '        versionCode = 50101\n        versionName = "5.0.1-dev1"\n        buildConfigField("boolean", "DEVELOPER_TEST_BUILD", "false")',
             "developer version metadata")
replace_once(gradle,
             '        getByName("debug") {\n            manifestPlaceholders',
             '        getByName("debug") {\n            buildConfigField("boolean", "DEVELOPER_TEST_BUILD", "true")\n            manifestPlaceholders',
             "debug-only test entitlement")
replace_once(gradle,
             '    val adMode = if (variant.buildType == "debug") "AdMob-Test" else "RealAds"',
             '    val adMode = if (variant.buildType == "debug") "Developer-Test-Premium" else "Developer-Release"',
             "developer APK filename")

manifest = APP / "src/main/AndroidManifest.xml"
replace_once(manifest, 'android:label="Verity"',
             'android:label="Verity Developer"', "launcher label")

# Premium is forced only when this developer debug build flag is true.
store = JAVA / "data/ChatStore.kt"
store_text = store.read_text(encoding="utf-8")
if "import dev.veriti.app.BuildConfig" not in store_text:
    store_text = store_text.replace(
        "package dev.veriti.app.data\n",
        "package dev.veriti.app.data\n\nimport dev.veriti.app.BuildConfig\n", 1
    )
old_premium = '        verityPlus = prefs.getBoolean("verityPlus", false),'
if store_text.count(old_premium) != 1:
    raise SystemExit("Premium preference line is not unique")
store.write_text(
    store_text.replace(old_premium,
        '        verityPlus = prefs.getBoolean("verityPlus", false) || BuildConfig.DEVELOPER_TEST_BUILD,', 1),
    encoding="utf-8"
)

main = JAVA / "MainActivity.kt"
text = main.read_text(encoding="utf-8")
patches = [
    ('private enum class Screen { CHAT, GAME, SETTINGS, PLUS, ABOUT, SKIN_EDITOR, CHARACTERS, APP_BUILDER, HOME_ASSISTANT, FRIENDS, FEATURES }',
     'private enum class Screen { CHAT, GAME, SETTINGS, PLUS, ABOUT, SKIN_EDITOR, CHARACTERS, APP_BUILDER, HOME_ASSISTANT, FRIENDS, FEATURES, DEVELOPER }'),
    ('if (preferences.getBoolean("verityPlus", false)) preferences.getString("uiStyle", "material") ?: "material"',
     'if (BuildConfig.DEVELOPER_TEST_BUILD || preferences.getBoolean("verityPlus", false)) preferences.getString("uiStyle", "material") ?: "material"'),
    ('            Screen.FEATURES -> FeatureListScreen(onBack = { screen = Screen.SETTINGS })',
     '            Screen.FEATURES -> FeatureListScreen(onBack = { screen = Screen.SETTINGS })\n            Screen.DEVELOPER -> DeveloperWorkbenchScreen(\n                onResetApp = { vm.updateSettings(AppSettings(verityPlus = BuildConfig.DEVELOPER_TEST_BUILD)) },\n                onOpenSettings = { screen = Screen.SETTINGS }\n            )'),
    ('    val tabs = listOf(Screen.CHAT, Screen.GAME, Screen.SETTINGS, Screen.PLUS)',
     '    val tabs = listOf(Screen.CHAT, Screen.GAME, Screen.SETTINGS, Screen.PLUS, Screen.DEVELOPER)'),
    ('            Triple(Screen.PLUS, R.drawable.sf_star, "Verity+")\n        )',
     '            Triple(Screen.PLUS, R.drawable.sf_star, "Verity+"),\n            Triple(Screen.DEVELOPER, R.drawable.sf_gear, "Разраб.")\n        )'),
    ('        AdaptiveNavigationBarItem(\n            index = 3,\n            onClick = { onSelected(Screen.PLUS) },\n            icon = { StyleTabIcon(sfStyle, R.drawable.sf_star) { Icon(Icons.Rounded.Star, null) } },\n            label = { Text("Verity+") }\n        )',
     '        AdaptiveNavigationBarItem(\n            index = 3,\n            onClick = { onSelected(Screen.PLUS) },\n            icon = { StyleTabIcon(sfStyle, R.drawable.sf_star) { Icon(Icons.Rounded.Star, null) } },\n            label = { Text("Verity+") }\n        )\n        AdaptiveNavigationBarItem(\n            index = 4,\n            onClick = { onSelected(Screen.DEVELOPER) },\n            icon = { StyleTabIcon(sfStyle, R.drawable.sf_gear) { Icon(Icons.Rounded.Code, null) } },\n            label = { Text("Разраб.") }\n        )')
]
replace_once(main, "Icon(Icons.Rounded.Code, null)", "Icon(Icons.Rounded.Settings, null)", "developer tab icon")

for old, new in patches:
    if text.count(old) != 1:
        raise SystemExit(f"MainActivity patch point count mismatch ({text.count(old)}): {old[:70]}")
    text = text.replace(old, new, 1)
main.write_text(text, encoding="utf-8")

# Apply the two compatibility fixes used by the working 5.0.1 build pipeline.
text = main.read_text(encoding="utf-8")
start = text.index("private fun VeritiApp(")
marker = "    val state = vm.state"
idx = text.index(marker, start)
text = text[:idx] + "    val context = LocalContext.current\n" + text[idx:]
main.write_text(text, encoding="utf-8")

service = JAVA / "service/FloatingAssistantService.kt"
service_text = service.read_text(encoding="utf-8")
if service_text.count("antiAlias = true") != 1:
    raise SystemExit("FloatingAssistantService antiAlias patch point is not unique")
service.write_text(service_text.replace("antiAlias = true", "setAntiAlias(true)", 1), encoding="utf-8")

shutil.copy2(Path("developer/DeveloperWorkbenchScreen.kt"), JAVA / "DeveloperWorkbenchScreen.kt")
print("Verity Developer patches applied successfully.")
