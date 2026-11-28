#!/usr/bin/env python3
"""Build Verity's compact 4x4 skin atlas, including source smileys and NASA Galle Crater."""
from pathlib import Path
from urllib.request import Request, urlopen
from PIL import Image, ImageDraw, ImageFilter
import io
import sys

root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("Verity-main")
atlas_path = root / "app/src/main/assets/character_catalog/verity_new_skins_atlas.png"
atlas_path.parent.mkdir(parents=True, exist_ok=True)
CELL = 96
atlas = Image.new("RGBA", (CELL * 4, CELL * 4), (0, 0, 0, 0))

def put(slot, painter):
    tile = Image.new("RGBA", (CELL, CELL), (0, 0, 0, 0))
    painter(ImageDraw.Draw(tile))
    atlas.alpha_composite(tile, ((slot % 4) * CELL, (slot // 4) * CELL))

def base_face(d):
    d.ellipse((6, 6, 90, 90), fill=(255, 238, 0, 255))
    d.ellipse((29, 28, 44, 49), fill=(7, 7, 9, 255))
    d.ellipse((56, 28, 71, 49), fill=(7, 7, 9, 255))

def smile(d):
    base_face(d); d.arc((22, 38, 76, 80), 12, 168, fill=(5, 5, 7, 255), width=4)

def wavy(d):
    base_face(d)
    d.line([(22, 57), (29, 62), (36, 57), (43, 62), (50, 57),
            (57, 62), (64, 57), (71, 62), (76, 56)], fill=(5, 5, 7, 255), width=3)

def open_smile(d):
    base_face(d)
    d.ellipse((23, 48, 73, 78), fill=(4, 4, 6, 255))
    d.arc((32, 63, 65, 81), 185, 350, fill=(255, 228, 70, 255), width=5)

def sad(d):
    base_face(d); d.arc((22, 56, 76, 96), 195, 345, fill=(5, 5, 7, 255), width=3)

def sad_open(d):
    base_face(d)
    d.pieslice((22, 46, 76, 86), 185, 355, fill=(5, 5, 7, 255))
    d.arc((31, 57, 67, 87), 185, 355, fill=(255, 234, 48, 255), width=5)

def angry_teeth(d):
    base_face(d)
    d.line((25, 28, 46, 40), fill=(5, 5, 7, 255), width=4)
    d.line((71, 28, 51, 40), fill=(5, 5, 7, 255), width=4)
    d.rounded_rectangle((20, 52, 77, 78), radius=9, fill=(255, 255, 255, 255),
                        outline=(4, 4, 6, 255), width=3)
    d.line((33, 54, 33, 76), fill=(8, 8, 10, 255), width=2)
    d.line((47, 53, 47, 77), fill=(8, 8, 10, 255), width=2)
    d.line((62, 53, 62, 77), fill=(8, 8, 10, 255), width=2)
    d.line((23, 65, 74, 65), fill=(8, 8, 10, 255), width=2)

def angry_open(d):
    base_face(d)
    d.line((23, 28, 46, 42), fill=(5, 5, 7, 255), width=4)
    d.line((73, 28, 51, 42), fill=(5, 5, 7, 255), width=4)
    d.ellipse((18, 50, 79, 82), fill=(4, 4, 6, 255))
    d.arc((34, 65, 64, 87), 185, 355, fill=(255, 227, 66, 255), width=5)

# The eight expressions from the user's first image form one animated skin (group 49).
for index, painter in enumerate((smile, wavy, smile, open_smile, sad, sad_open, angry_teeth, angry_open)):
    put(index, painter)

def parcel(d):
    d.rounded_rectangle((12, 18, 84, 82), radius=5, fill=(237, 174, 113, 255), outline=(42, 29, 25, 255), width=4)
    d.rectangle((39, 18, 57, 82), fill=(255, 225, 164, 255), outline=(42, 29, 25, 255), width=3)
    d.rectangle((39, 18, 57, 46), fill=(255, 236, 191, 255), outline=(42, 29, 25, 255), width=2)
    d.rectangle((61, 56, 80, 74), fill=(245, 246, 242, 255), outline=(22, 27, 34, 255), width=2)
    d.line((64, 62, 77, 62), fill=(22, 27, 34, 255), width=2)
    d.line((64, 67, 77, 67), fill=(22, 27, 34, 255), width=2)

def three_eye(d):
    d.ellipse((6, 6, 90, 90), fill=(255, 230, 0, 255), outline=(224, 190, 0, 255), width=2)
    for x, y in ((28, 29), (48, 18), (68, 29)):
        d.ellipse((x - 5, y - 9, x + 5, y + 9), fill=(0, 0, 0, 255))
    d.arc((25, 42, 72, 76), 5, 175, fill=(0, 0, 0, 255), width=4)

def television(d):
    d.rounded_rectangle((8, 25, 88, 79), radius=7, fill=(48, 58, 77, 255), outline=(14, 20, 34, 255), width=4)
    d.rounded_rectangle((16, 33, 67, 70), radius=4, fill=(125, 220, 239, 255), outline=(11, 20, 31, 255), width=2)
    d.line((37, 25, 27, 10), fill=(30, 35, 43, 255), width=3)
    d.line((54, 25, 65, 10), fill=(30, 35, 43, 255), width=3)
    d.ellipse((73, 38, 81, 46), fill=(255, 211, 89, 255))
    d.ellipse((73, 53, 81, 61), fill=(148, 231, 173, 255))

put(8, parcel)
put(9, three_eye)
put(10, television)

def skull(d):
    d.ellipse((9, 3, 87, 82), fill=(235, 241, 239, 255), outline=(24, 34, 47, 255), width=4)
    d.rounded_rectangle((24, 57, 72, 93), radius=10, fill=(232, 239, 237, 255), outline=(24, 34, 47, 255), width=3)
    d.ellipse((23, 34, 39, 51), fill=(15, 22, 34, 255))
    d.ellipse((57, 34, 73, 51), fill=(15, 22, 34, 255))
    d.polygon([(48, 50), (42, 60), (54, 60)], fill=(15, 22, 34, 255))
    d.rounded_rectangle((31, 67, 65, 84), radius=3, fill=(15, 22, 34, 255))
    for x in range(34, 65, 7):
        d.line((x, 68, x, 83), fill=(244, 244, 236, 255), width=2)

def pumpkin(d):
    d.rounded_rectangle((39, 4, 57, 30), radius=5, fill=(77, 144, 75, 255), outline=(40, 72, 41, 255), width=2)
    d.ellipse((10, 20, 86, 91), fill=(244, 126, 31, 255), outline=(93, 48, 29, 255), width=4)
    d.ellipse((23, 23, 48, 88), fill=(255, 165, 52, 255), outline=(177, 77, 24, 255), width=2)
    d.ellipse((47, 23, 73, 88), fill=(255, 165, 52, 255), outline=(177, 77, 24, 255), width=2)
    d.polygon([(23, 48), (42, 57), (29, 65)], fill=(24, 29, 39, 255))
    d.polygon([(54, 57), (75, 48), (66, 66)], fill=(24, 29, 39, 255))
    d.arc((26, 52, 70, 83), 0, 180, fill=(24, 29, 39, 255), width=4)
    d.polygon([(37, 70), (60, 70), (53, 78), (43, 78)], fill=(255, 224, 127, 255))

put(11, skull)
put(12, pumpkin)

def nasa_galle_face():
    urls = (
        "https://assets.science.nasa.gov/dynamicimage/assets/science/psd/photojournal/pia/pia04/pia04520/PIA04520.jpg?crop=faces%2Cfocalpoint&fit=clip&h=2993&w=1995",
        "https://photojournal.jpl.nasa.gov/jpeg/PIA04520.jpg",
    )
    photo = None
    for url in urls:
        try:
            request = Request(url, headers={"User-Agent": "Verity-Android-App/1.0 (NASA image credit in app assets)"})
            with urlopen(request, timeout=40) as response:
                raw = response.read()
            candidate = Image.open(io.BytesIO(raw)).convert("RGB")
            photo = candidate
            break
        except Exception as exc:
            print("NASA image source failed:", type(exc).__name__)
    if photo is None:
        raise SystemExit("Unable to download NASA/JPL PIA04520. No generated substitute will be used.")
    # NASA's vertical page image has multiple treatments; use the lower crater panel.
    if photo.height > photo.width * 1.15:
        cx = int(photo.width * 0.49)
        cy = int(photo.height * 0.72)
        side = int(min(photo.width * 0.58, photo.height * 0.36))
    else:
        cx = photo.width // 2
        cy = photo.height // 2
        side = int(min(photo.width, photo.height) * 0.84)
    half = side // 2
    crop = photo.crop((max(0, cx-half), max(0, cy-half), min(photo.width, cx+half), min(photo.height, cy+half)))
    w, h = crop.size
    side = min(w, h)
    crop = crop.crop(((w-side)//2, (h-side)//2, (w-side)//2+side, (h-side)//2+side))
    crop = crop.resize((CELL, CELL), Image.Resampling.LANCZOS)
    mask = Image.new("L", (CELL * 3, CELL * 3), 0)
    ImageDraw.Draw(mask).ellipse((3, 3, CELL*3-3, CELL*3-3), fill=255)
    mask = mask.resize((CELL, CELL), Image.Resampling.LANCZOS).filter(ImageFilter.GaussianBlur(0.25))
    result = crop.convert("RGBA")
    result.putalpha(mask)
    return result

atlas.alpha_composite(nasa_galle_face(), (3 * CELL, 3 * CELL))
atlas.save(atlas_path, format="PNG", optimize=True)

credit = atlas_path.parent / "skin_sources.txt"
credit.write_text(
    "Group 49: eight separate emoji expression frames based on the user's supplied first smiley image.\\n"
    "Groups 53–54: newly drawn generic skeleton and pumpkin mascots; not copied from the supplied Spooky Month poster.\\n"
    "Group 55: circular crop of NASA/JPL/Malin Space Science Systems image PIA04520, Galle Crater on Mars, informally called Happy Face.\\n"
    "Official NASA source: https://science.nasa.gov/photojournal/happy-face-crater/\\n",
    encoding="utf-8"
)
print("Created 55th skin atlas: eight smiley frames, original skull/pumpkin drawings and NASA Galle Crater.")
