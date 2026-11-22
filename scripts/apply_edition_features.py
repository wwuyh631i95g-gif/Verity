from pathlib import Path
import re
import shutil
import sys

ROOT = Path(sys.argv[1] if len(sys.argv) > 1 else "Verity-main")
REPO = Path(__file__).resolve().parent.parent
JAVA = ROOT / "app/src/main/java/dev/veriti/app"
MAIN = JAVA / "MainActivity.kt"
FEATURE_SOURCE = REPO / "edition/EditionFeaturesScreen.kt"
FEATURE_TARGET = JAVA / "EditionFeaturesScreen.kt"

if not FEATURE_SOURCE.is_file():
    raise SystemExit(f"Edition screen source not found: {FEATURE_SOURCE}")
FEATURE_TARGET.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(FEATURE_SOURCE, FEATURE_TARGET)

s = MAIN.read_text(encoding="utf-8")

def once(old: str, new: str, label: str):
    global s
    n = s.count(old)
    if n == 1:
        s = s.replace(old, new, 1)
    elif n == 0 and new in s:
        return
    else:
        raise SystemExit(f"{label}: expected one match, got {n}")

enum_match = re.search(r"private enum class Screen \\{([^}]+)\\}", s)
if not enum_match:
    raise SystemExit("edition screen enum: enum declaration not found")
enum_values = [v.strip() for v in enum_match.group(1).split(",")]
if "EDITION_FEATURES" not in enum_values:
    enum_values.append("EDITION_FEATURES")
    s = s[:enum_match.start()] + "private enum class Screen {" + ", ".join(enum_values) + "}" + s[enum_match.end():]
once(
    "                onFeatures = { screen = Screen.FEATURES },\n                onSave = vm::updateSettings,",
    "                onFeatures = { screen = Screen.FEATURES },\n                onEditionFeatures = { screen = Screen.EDITION_FEATURES },\n                onSave = vm::updateSettings,",
    "pass edition feature navigation"
)
once(
    "            Screen.FEATURES -> FeatureListScreen(onBack = { screen = Screen.SETTINGS })",
    "            Screen.FEATURES -> FeatureListScreen(onBack = { screen = Screen.SETTINGS })\n            Screen.EDITION_FEATURES -> EditionFeaturesScreen(onBack = { screen = Screen.SETTINGS })",
    "edition screen route"
)
once(
    "    onFeatures: () -> Unit,\n    onSave: (AppSettings) -> Unit,",
    "    onFeatures: () -> Unit,\n    onEditionFeatures: () -> Unit,\n    onSave: (AppSettings) -> Unit,",
    "SettingsScreen edition callback"
)
anchor = '''            item {
                Card(
                    onClick = { showOfflineModels = true },'''
card = '''            item {
                Card(
                    onClick = onEditionFeatures,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("✦", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("10 новых функций этой версии", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Отдельный набор для телефона, часов или разработчика", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                Card(
                    onClick = { showOfflineModels = true },'''
once(anchor, card, "insert edition-specific settings card")

MAIN.write_text(s, encoding="utf-8")
print("Verity Edition Features applied.")
