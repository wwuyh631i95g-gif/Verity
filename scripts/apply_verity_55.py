from pathlib import Path
import re
import shutil
import sys

REPO = Path(__file__).resolve().parent.parent
ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("Verity-main")
JAVA = ROOT / "app/src/main/java/dev/veriti/app"

source = REPO / "scripts/verity55/VerityPhoneExperience.kt"
if not source.is_file():
    raise SystemExit(f"Missing Phone OS source: {source}")
JAVA.mkdir(parents=True, exist_ok=True)
shutil.copyfile(source, JAVA / "VerityPhoneExperience.kt")

os_file = JAVA / "VerityOsScreen.kt"
if not os_file.is_file():
    raise SystemExit(f"Virtual OS screen not found: {os_file}")
os = os_file.read_text(encoding="utf-8")

old_app = '            OsApp("universe", "Universe", "18 мини-миров", Icons.Rounded.Explore, Color(0xFFBEA7FF))'
new_app = old_app + ',\n            OsApp("phoneos", "Phone OS 18", "Функции iPhone в локальном симуляторе", Icons.Rounded.Explore, Color(0xFF9DC7FF))'
if old_app in os:
    os = os.replace(old_app, new_app, 1)
elif 'OsApp("phoneos", "Phone OS 18"' not in os:
    raise SystemExit("Phone OS launcher insertion point not found")

old_route = '                    "universe" -> VerityOsUniverse(prefs)'
new_route = old_route + '\n                    "phoneos" -> VerityPhoneExperience()'
if old_route in os:
    os = os.replace(old_route, new_route, 1)
elif '"phoneos" -> VerityPhoneExperience()' not in os:
    raise SystemExit("Phone OS app route insertion point not found")

os = os.replace('"11 установлено"', '"12 установлено"', 1)
os_file.write_text(os, encoding="utf-8")

gradle = ROOT / "app/build.gradle.kts"
g = gradle.read_text(encoding="utf-8")
match = re.search(r'versionCode = \d+\s+versionName = "[^"]+"', g)
if not match:
    raise SystemExit("Gradle version metadata not found")
application_id = re.search(r'applicationId = "([^"]+)"', g)
is_dev = bool(application_id and application_id.group(1).endswith(".developer"))
metadata = 'versionCode = 55000\n        versionName = "5.5.0-Developer"' if is_dev else 'versionCode = 55000\n        versionName = "5.5.0"'
g = g[:match.start()] + metadata + g[match.end():]
g = g.replace("Verity-5.4.0-$adMode-", "Verity-5.5.0-$adMode-")
gradle.write_text(g, encoding="utf-8")
print("Verity 5.5.0 Phone OS 18 integration applied.")
