from pathlib import Path
import base64
import gzip
import re
import sys

REPO = Path(__file__).resolve().parent
ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("Verity-main")
JAVA = ROOT / "app/src/main/java/dev/veriti/app"

payload = REPO / "scripts/verity53/VerityOsUniverse.kt.gz.b64"
try:
    source = gzip.decompress(base64.b64decode(payload.read_text(encoding="ascii")))
except Exception as exc:
    raise SystemExit(f"Unable to decode Verity Universe module: {exc}")
(JAVA / "VerityOsUniverse.kt").write_bytes(source)

os_file = JAVA / "VerityOsScreen.kt"
os = os_file.read_text(encoding="utf-8")
old_app = '            OsApp("world", "Мир Verity", "Комнаты и жизнь", Icons.Rounded.Public, Color(0xFFB8E28C))'
new_app = old_app + ',\n            OsApp("universe", "Universe", "18 мини-миров", Icons.Rounded.Explore, Color(0xFFBEA7FF))'
if old_app in os:
    os = os.replace(old_app, new_app, 1)
elif 'OsApp("universe", "Universe"' not in os:
    raise SystemExit("Virtual OS app launcher insertion point not found")
os = os.replace('"10 установлено"', '"11 установлено"', 1)
old_route = '                    "world" -> OsSimpleAction("Мир Verity", "Комнаты, персонажи, предметы и повседневная жизнь.", Icons.Rounded.Public, "Открыть мир Verity", onOpenWorld)'
new_route = old_route + '\n                    "universe" -> VerityOsUniverse(prefs)'
if old_route in os:
    os = os.replace(old_route, new_route, 1)
elif '"universe" -> VerityOsUniverse(prefs)' not in os:
    raise SystemExit("Virtual OS app route insertion point not found")
os_file.write_text(os, encoding="utf-8")

gradle = ROOT / "app/build.gradle.kts"
g = gradle.read_text(encoding="utf-8")
match = re.search(r'versionCode = \d+\s+versionName = "[^"]+"', g)
if not match:
    raise SystemExit("Gradle version metadata not found")
app_id = re.search(r'applicationId = "([^"]+)"', g)
is_dev = bool(app_id and app_id.group(1).endswith(".developer"))
metadata = 'versionCode = 54000\n        versionName = "5.4.0-Developer"' if is_dev else 'versionCode = 54000\n        versionName = "5.4.0"'
g = g[:match.start()] + metadata + g[match.end():]
g = g.replace('Verity-5.3.0-$adMode-', 'Verity-5.4.0-$adMode-')
gradle.write_text(g, encoding="utf-8")
print("Verity 5.4.0 Universe modules integrated successfully.")
