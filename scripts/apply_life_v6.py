from pathlib import Path
import base64
import gzip
import re
import subprocess

ROOT = Path("Verity-main")
BUNDLE_PARTS = sorted(Path("scripts").glob("life_v6_bundle_*.txt"))
if not ROOT.is_dir() or len(BUNDLE_PARTS) != 7:
    raise SystemExit("Life V6: project or patch bundle parts are missing")
bundle_text = "".join(p.read_text(encoding="ascii").strip() for p in BUNDLE_PARTS)
patch_bytes = gzip.decompress(base64.b64decode(bundle_text))
patch_file = Path("/tmp/verity-life-v6.patch")
patch_file.write_bytes(patch_bytes)
result = subprocess.run(["patch", "--batch", "-p1", "-i", str(patch_file)], text=True, capture_output=True)
if result.returncode != 0:
    raise SystemExit("Life V6 source patch failed:\n" + result.stdout + result.stderr)
print(result.stdout.strip())

# MainActivity is also patched by the existing character-customization pipeline.
# Apply these additions after that pipeline so its screens remain intact.
main = ROOT / "app/src/main/java/dev/veriti/app/MainActivity.kt"
source = main.read_text(encoding="utf-8")

def once(old: str, new: str, label: str):
    global source
    count = source.count(old)
    if count == 0 and new in source:
        return
    if count != 1:
        raise SystemExit(f"Life V6 MainActivity {label}: expected one anchor, found {count}")
    source = source.replace(old, new, 1)

# Supports both the base enum and the enum extended by the pre-existing customizations patch.
match = re.search(r"private enum class Screen \{[^\n}]+\}", source)
if not match:
    raise SystemExit("Life V6 MainActivity: Screen enum not found")
if "LIFE" not in match.group(0):
    old_line = match.group(0)
    new_line = old_line[:-1].rstrip() + ", LIFE }"
    source = source.replace(old_line, new_line, 1)

once(
    '            Screen.FEATURES -> FeatureListScreen(onBack = { screen = Screen.SETTINGS })',
    '            Screen.FEATURES -> FeatureListScreen(onBack = { screen = Screen.SETTINGS })\n            Screen.LIFE -> VerityLifeScreen(settings = state.settings, onSave = vm::updateSettings, onBack = { screen = Screen.CHAT })',
    "screen route",
)
# Keep the other four tabs and add Life as a fifth entry.
tabs_match = re.search(r"val tabs = listOf\(([^)]*)\)", source)
if not tabs_match:
    raise SystemExit("Life V6 MainActivity: bottom navigation list not found")
if "Screen.LIFE" not in tabs_match.group(1):
    updated = "val tabs = listOf(" + tabs_match.group(1).rstrip() + ", Screen.LIFE)"
    source = source[:tabs_match.start()] + updated + source[tabs_match.end():]

# Cupertino-style bottom bar item. Developer builds already have a fifth tab,
# so place Life after it there; standard builds place Life after Verity+.
is_developer = "DEVELOPER" in match.group(0)
if is_developer:
    line_pattern = r'(?m)^(?P<indent>\s*)Triple\(Screen\.DEVELOPER, R\.drawable\.sf_gear, "Разраб\."\)(?P<comma>,?)$'
    line_match = re.search(line_pattern, source)
    if not line_match and "Triple(Screen.LIFE" not in source:
        raise SystemExit("Life V6 MainActivity: Developer Cupertino item not found")
    if line_match and "Triple(Screen.LIFE" not in source:
        indent, comma = line_match.group("indent"), line_match.group("comma")
        plus_dev_line = line_match.group(0)
        life_line = indent + 'Triple(Screen.LIFE, R.drawable.sf_bubble, "Life")' + comma
        separator = "" if comma else ","
        source = source.replace(plus_dev_line, plus_dev_line + separator + "\n" + life_line, 1)
else:
    line_pattern = r'(?m)^(?P<indent>\s*)Triple\(Screen\.PLUS, R\.drawable\.sf_star, "Verity\+"\)(?P<comma>,?)$'
    line_match = re.search(line_pattern, source)
    if not line_match and "Triple(Screen.LIFE" not in source:
        raise SystemExit("Life V6 MainActivity: Verity+ Cupertino item not found")
    if line_match and "Triple(Screen.LIFE" not in source:
        indent, comma = line_match.group("indent"), line_match.group("comma")
        plus_line = line_match.group(0)
        life_line = indent + 'Triple(Screen.LIFE, R.drawable.sf_bubble, "Life")' + comma
        separator = "" if comma else ","
        source = source.replace(plus_line, plus_line + separator + "\n" + life_line, 1)

# Material adaptive bottom bar item.
if is_developer:
    nav_old = '''        AdaptiveNavigationBarItem(
            index = 4,
            onClick = { onSelected(Screen.DEVELOPER) },
            icon = { StyleTabIcon(sfStyle, R.drawable.sf_gear) { Icon(Icons.Rounded.Settings, null) } },
            label = { Text("Разраб.") }
        )'''
    nav_new = nav_old + '''
        AdaptiveNavigationBarItem(
            index = 5,
            onClick = { onSelected(Screen.LIFE) },
            icon = { StyleTabIcon(sfStyle, R.drawable.sf_bubble) { Icon(Icons.Rounded.AutoAwesome, null) } },
            label = { Text("Life") }
        )'''
else:
    nav_old = '''        AdaptiveNavigationBarItem(
            index = 3,
            onClick = { onSelected(Screen.PLUS) },
            icon = { StyleTabIcon(sfStyle, R.drawable.sf_star) { Icon(Icons.Rounded.Star, null) } },
            label = { Text("Verity+") }
        )'''
    nav_new = nav_old + '''
        AdaptiveNavigationBarItem(
            index = 4,
            onClick = { onSelected(Screen.LIFE) },
            icon = { StyleTabIcon(sfStyle, R.drawable.sf_bubble) { Icon(Icons.Rounded.AutoAwesome, null) } },
            label = { Text("Life") }
        )'''
once(nav_old, nav_new, "adaptive Life tab")
main.write_text(source, encoding="utf-8")
print("Verity Life V6 features applied successfully.")
