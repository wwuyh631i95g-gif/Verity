#!/usr/bin/env python3
"""Replace seasonal mascot atlas cells with original art and add NASA's real Galle crater photo."""
from pathlib import Path
from urllib.request import Request, urlopen
from PIL import Image, ImageDraw, ImageFilter
import io, sys, base64

root=Path(sys.argv[1]) if len(sys.argv)>1 else Path("Verity-main")
atlas_path=root/"app/src/main/assets/character_catalog/verity_new_skins_atlas.png"
atlas_source_b64=Path(__file__).with_name("verity_new_skins_atlas.png.b64")
if not atlas_source_b64.is_file():
    raise SystemExit("Base64 source Verity atlas missing: "+str(atlas_source_b64))
# The original PNG is stored with Git LFS; this text-encoded copy remains available
# in standard GitHub Actions checkouts even when LFS smudging is disabled.
raw_atlas=base64.b64decode(atlas_source_b64.read_text(encoding="ascii").strip(), validate=True)
atlas=Image.open(io.BytesIO(raw_atlas)).convert("RGBA")
atlas_path.parent.mkdir(parents=True, exist_ok=True)
cell=min(atlas.width//4,atlas.height//4)
if cell<32: raise SystemExit("Invalid 4x4 skin atlas")
S=512

def skull():
 im=Image.new("RGBA",(S,S),(0,0,0,0));d=ImageDraw.Draw(im)
 d.ellipse((70,58,442,440),fill=(31,225,199,28))
 d.ellipse((90,58,422,390),fill=(238,243,242,255),outline=(22,36,55,255),width=17)
 d.rounded_rectangle((154,312,358,452),radius=48,fill=(230,237,236,255),outline=(22,36,55,255),width=15)
 d.ellipse((158,190,228,266),fill=(17,25,43,255));d.ellipse((284,190,354,266),fill=(17,25,43,255))
 d.polygon([(255,252),(226,300),(278,300)],fill=(17,25,43,255))
 d.rounded_rectangle((168,340,344,416),radius=12,fill=(22,28,44,255),outline=(22,36,55,255),width=8)
 for x in range(178,344,28): d.line((x,346,x,410),fill=(239,242,238,255),width=7)
 d.line((176,380,336,380),fill=(239,242,238,255),width=7)
 return im

def pumpkin():
 im=Image.new("RGBA",(S,S),(0,0,0,0));d=ImageDraw.Draw(im)
 d.ellipse((63,72,449,454),fill=(255,143,54,30))
 d.ellipse((86,118,426,438),fill=(245,125,31,255),outline=(70,37,44,255),width=18)
 d.ellipse((137,124,254,430),fill=(255,164,58,255),outline=(177,75,27,255),width=7)
 d.ellipse((248,124,367,430),fill=(255,164,58,255),outline=(177,75,27,255),width=7)
 d.rounded_rectangle((219,49,291,151),radius=22,fill=(75,135,67,255),outline=(45,66,48,255),width=12)
 d.polygon([(255,56),(318,88),(286,109)],fill=(105,171,79,255))
 d.polygon([(146,232),(224,260),(184,292)],fill=(32,34,41,255))
 d.polygon([(286,260),(367,230),(335,296)],fill=(32,34,41,255))
 d.arc((150,268,365,397),8,172,fill=(33,31,38,255),width=17)
 d.polygon([(184,342),(326,342),(301,383),(209,383)],fill=(38,35,41,255))
 d.polygon([(204,340),(238,340),(224,366)],fill=(255,221,127,255))
 d.polygon([(272,340),(306,340),(288,366)],fill=(255,221,127,255))
 d.line((255,118,255,424),fill=(175,83,27,150),width=5)
 return im

def nasa_happy_face():
 urls=[
  "https://assets.science.nasa.gov/dynamicimage/assets/science/psd/photojournal/pia/pia04/pia04520/PIA04520.jpg?crop=faces%2Cfocalpoint&fit=clip&h=2993&w=1995",
  "https://photojournal.jpl.nasa.gov/jpeg/PIA04520.jpg"
 ]
 raw=None
 for url in urls:
  try:
   req=Request(url,headers={"User-Agent":"Verity-Android-App/1.0; NASA PIA04520 attribution included"})
   with urlopen(req,timeout=40) as response: raw=response.read()
   if raw[:2]==b"\xff\xd8" or raw[:8]==b"\x89PNG\r\n\x1a\n": break
   raw=None
  except Exception as exc:
   print("NASA source attempt failed:",type(exc).__name__)
 if raw is None: raise SystemExit("NASA official Galle/Happy Face Crater photo download failed; no generated substitute will be used.")
 photo=Image.open(io.BytesIO(raw)).convert("RGB")
 if photo.height>photo.width*1.15:
  cx=int(photo.width*.49);cy=int(photo.height*.72);side=int(min(photo.width*.58,photo.height*.36))
 else:
  cx=photo.width//2;cy=photo.height//2;side=int(min(photo.width,photo.height)*.84)
 half=side//2
 crop=photo.crop((max(0,cx-half),max(0,cy-half),min(photo.width,cx+half),min(photo.height,cy+half)))
 w,h=crop.size;z=min(w,h);crop=crop.crop(((w-z)//2,(h-z)//2,(w-z)//2+z,(h-z)//2+z)).resize((S,S),Image.Resampling.LANCZOS)
 out=Image.new("RGBA",(S,S),(0,0,0,0));mask=Image.new("L",(S*3,S*3),0)
 ImageDraw.Draw(mask).ellipse((5,5,S*3-5,S*3-5),fill=255)
 mask=mask.resize((S,S),Image.Resampling.LANCZOS).filter(ImageFilter.GaussianBlur(.35))
 out.paste(crop,(0,0));out.putalpha(mask);return out

for slot,sprite in {11:skull(),12:pumpkin(),13:nasa_happy_face()}.items():
 sprite=sprite.resize((cell,cell),Image.Resampling.LANCZOS)
 x=(slot%4)*cell;y=(slot//4)*cell
 atlas.paste(sprite,(x,y),sprite)
atlas.save(atlas_path,format="PNG",optimize=True)
credit=root/"app/src/main/assets/character_catalog/skin_sources.txt"
credit.write_text(
 "Group 49: eight emoji expressions from the user's supplied generic smiley sheet (first Verity prototype concept).\n"
 "Groups 53–54: newly drawn generic skeleton and pumpkin mascots; not copied from the uploaded cartoon poster.\n"
 "Group 55: circular crop of NASA/JPL/Malin Space Science Systems image PIA04520, Galle Crater on Mars, informally called Happy Face.\n"
 "Official NASA source: https://science.nasa.gov/photojournal/happy-face-crater/\n",
 encoding="utf-8")
print("Verity atlas updated with original pumpkin/skull mascots and NASA PIA04520.")
