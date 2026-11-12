from pathlib import Path
import re
import base64
import gzip
import sys

REPO = Path(__file__).resolve().parent.parent
ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("Verity-main")
JAVA = ROOT / "app/src/main/java/dev/veriti/app"

def replace_once(path: Path, old: str, new: str, label: str):
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count == 0 and new in text:
        return
    if count != 1:
        raise SystemExit(f"{label}: expected one match, got {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")

# Boot animation, persistent virtual files/notes, calculator, shell and arcade.
screen_source = REPO / "scripts/verity53/VerityOsScreen.kt.gz.b64"
if not screen_source.is_file():
    raise SystemExit(f"Missing virtual OS source: {screen_source}")
try:
    screen_bytes = gzip.decompress(base64.b64decode(screen_source.read_text(encoding="ascii")))
except Exception as exc:
    raise SystemExit(f"Unable to decode virtual OS source: {exc}")
(JAVA / "VerityOsScreen.kt").write_bytes(screen_bytes)

main = JAVA / "MainActivity.kt"
main_text = main.read_text(encoding="utf-8")
match_enum = re.search(r'private enum class Screen \{([^}]+)\}', main_text)
if not match_enum:
    raise SystemExit("Screen enum not found")
if "VIRTUAL_OS" not in match_enum.group(1):
    main_text = main_text[:match_enum.start()] + match_enum.group(0)[:-1] + ", VIRTUAL_OS}" + main_text[match_enum.end():]
if "screen != Screen.VIRTUAL_OS" not in main_text:
    main_text, n = re.subn(r'(if \(screen != Screen\.ABOUT.*?Screen\.CUSTOMIZATIONS)(\) \{)', r'\1 && screen != Screen.VIRTUAL_OS\2', main_text, count=1, flags=re.S)
    if n != 1:
        raise SystemExit("Unable to hide root navigation while virtual OS is open")
call_old = "                onOpenCustomizations = { screen = Screen.CUSTOMIZATIONS },\n                onAccentChange = onAccentChange\n            )"
call_new = "                onOpenCustomizations = { screen = Screen.CUSTOMIZATIONS },\n                onOpenVirtualOs = { screen = Screen.VIRTUAL_OS },\n                onAccentChange = onAccentChange\n            )"
if call_old in main_text:
    main_text = main_text.replace(call_old, call_new, 1)
elif call_new not in main_text:
    raise SystemExit("Studio route call insertion point missing")
route_old = "            Screen.SKIN_EDITOR -> SkinEditorScreen("
route_new = "            Screen.VIRTUAL_OS -> VerityOsScreen(\n                onBack = { screen = Screen.STUDIO },\n                onOpenVerity = { screen = Screen.CHAT },\n                onOpenGame = { screen = Screen.GAME },\n                onOpenWorld = { screen = Screen.LIFE }\n            )\n            Screen.SKIN_EDITOR -> SkinEditorScreen("
if route_old in main_text:
    main_text = main_text.replace(route_old, route_new, 1)
elif "Screen.VIRTUAL_OS -> VerityOsScreen(" not in main_text:
    raise SystemExit("Virtual OS route insertion point missing")
main.write_text(main_text, encoding="utf-8")

studio = JAVA / "VerityStudioScreen.kt"
text = studio.read_text(encoding="utf-8")
if "import androidx.compose.animation.animateContentSize" not in text:
    text = text.replace("import androidx.compose.foundation.background\n", "import androidx.compose.foundation.background\nimport androidx.compose.animation.animateContentSize\n", 1)
old_sig = '''    onOpenCustomizations: () -> Unit,
    onAccentChange: (String) -> Unit'''
new_sig = '''    onOpenCustomizations: () -> Unit,
    onOpenVirtualOs: () -> Unit,
    onAccentChange: (String) -> Unit'''
if old_sig in text:
    text = text.replace(old_sig, new_sig, 1)
elif new_sig not in text:
    raise SystemExit("Studio signature insertion point missing")
old_insert = '''            item { StudioSection("desk", "Рабочий стол Verity", "Настрой панели и смотри их вместе", isExpanded("desk"), ::toggle) }'''
new_insert = '''            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenVirtualOs).animateContentSize(),
                    shape = RoundedCornerShape(25.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF29234B))
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFFFD36A), modifier = Modifier.size(58.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("V", color = Color(0xFF29234B), fontSize = 35.sp, fontWeight = FontWeight.Black) }
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("VERITY OS", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                            Text("Собственная система с загрузчиком, рабочим столом и приложениями", color = Color(0xFFE0DCF7), style = MaterialTheme.typography.bodySmall)
                            Text("ОТКРЫТЬ ВИРТУАЛЬНОЕ УСТРОЙСТВО  →", color = Color(0xFFFFD36A), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFFFFD36A))
                    }
                }
            }

            item { StudioSection("desk", "Рабочий стол Verity", "Настрой панели и смотри их вместе", isExpanded("desk"), ::toggle) }'''
if old_insert in text:
    text = text.replace(old_insert, new_insert, 1)
elif 'Text("VERITY OS"' not in text:
    raise SystemExit("Studio OS card insertion point missing")
text = text.replace(
    'Card(modifier = Modifier.fillMaxWidth().clickable { toggle(id) }, shape = RoundedCornerShape(18.dp))',
    'Card(modifier = Modifier.fillMaxWidth().clickable { toggle(id) }.animateContentSize(), shape = RoundedCornerShape(18.dp))', 1)
text = text.replace(
    'Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer))',
    'Card(modifier.animateContentSize(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer))', 1)
studio.write_text(text, encoding="utf-8")

# Version metadata is changed after Developer's prepare_dev_build.py has run.
gradle = ROOT / "app/build.gradle.kts"
g = gradle.read_text(encoding="utf-8")
match = re.search(r'versionCode = \d+\s+versionName = "[^"]+"', g)
if not match:
    raise SystemExit("Gradle version metadata not found")
application_id = re.search(r'applicationId = "([^"]+)"', g)
is_dev = bool(application_id and application_id.group(1).endswith(".developer"))
new_meta = 'versionCode = 53000\n        versionName = "5.3.0-Developer"' if is_dev else 'versionCode = 53000\n        versionName = "5.3.0"'
g = g[:match.start()] + new_meta + g[match.end():]
g = g.replace('Verity-5.2.0-$adMode-', 'Verity-5.3.0-$adMode-')
gradle.write_text(g, encoding="utf-8")
print("Verity 5.3.0 virtual OS and Studio animation integration applied.")
