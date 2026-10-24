"""Google Play graphics: 512 px icon and 1024 x 500 feature graphic, from the app's own icon and status colours.

Usage: python scripts/play_graphics.py  (needs `pip install playwright pillow` and `playwright install chromium`)
Writes play/graphics/icon-512.png and play/graphics/feature-graphic.png.
"""

from __future__ import annotations

import io
import re
from pathlib import Path

from PIL import Image
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
OUT = ROOT / "play" / "graphics"

# Launcher icon: colour and parcel outline (adaptive icon foreground, 108 dp canvas, as ha-parcel's icon).
BACKGROUND = re.search(r'name="launcher_background">(#[0-9A-Fa-f]{6})<', (RES / "values" / "colors.xml").read_text())[1]
FOREGROUND = (RES / "drawable" / "ic_launcher_foreground.xml").read_text()
BOX = re.search(r'pathData="([^"]+)"', FOREGROUND)[1]
SCALE = re.search(r'scaleX="([\d.]+)"', FOREGROUND)[1]
SHIFT = re.search(r'translateX="([\d.]+)"', FOREGROUND)[1]
STROKE = re.search(r'strokeWidth="([\d.]+)"', FOREGROUND)[1]

# Statuses on the feature graphic: label, colour (ui/Ui.kt, host dark theme).
STATUSES = [("En transit", "#1a73e8"), ("À retirer", "#b06000"), ("Livré", "#188038")]


def icon_svg(size: int) -> str:
    """Full-bleed square: Google Play applies its own mask and shadow."""
    return (
        f"<svg xmlns='http://www.w3.org/2000/svg' width='{size}' height='{size}' viewBox='0 0 108 108'>"
        f"<rect width='108' height='108' fill='{BACKGROUND}'/>"
        f"<g transform='translate({SHIFT} {SHIFT}) scale({SCALE})'><path d='{BOX}' fill='none' stroke='#fff' "
        f"stroke-width='{STROKE}' stroke-linejoin='round' stroke-linecap='round'/></g></svg>"
    )


def chip(label: str, color: str) -> str:
    return f"<div class='chip'><span class='dot' style='background:{color}'></span>{label}</div>"


FEATURE = """<!doctype html><html><head><meta charset='utf-8'><style>
* {{ margin: 0; padding: 0; box-sizing: border-box; }}
body {{ width: 1024px; height: 500px; overflow: hidden; font-family: Inter, Roboto, sans-serif;
       background: radial-gradient(circle at 78% 40%, #fbbf24 0%, {bg} 45%, #b45309 100%); color: #fff;
       display: flex; align-items: center; padding: 0 64px; gap: 48px; }}
.icon {{ width: 200px; height: 200px; border-radius: 46px; overflow: hidden; flex: none;
        box-shadow: 0 18px 40px rgba(0,0,0,.30); }}
h1 {{ font-size: 76px; font-weight: 800; letter-spacing: -1px; }}
p {{ font-size: 27px; font-weight: 500; margin-top: 10px; opacity: .97; line-height: 1.3; white-space: nowrap; }}
.chips {{ display: flex; gap: 12px; margin-top: 30px; }}
.chip {{ display: flex; align-items: center; gap: 10px; height: 52px; padding: 0 22px 0 16px; border-radius: 26px;
        background: #fff; color: #202124; font-size: 21px; font-weight: 700; box-shadow: 0 6px 16px rgba(0,0,0,.2); }}
.dot {{ width: 16px; height: 16px; border-radius: 8px; }}
</style></head><body>
<div class='icon'>{icon}</div>
<div><h1>Parcel</h1><p>Vos colis sur l'écran de votre voiture,<br>notifiés à chaque mise à jour.</p>
<div class='chips'>{chips}</div></div>
</body></html>"""


def screenshot(page, html: str, width: int, height: int) -> Image.Image:
    page.set_viewport_size({"width": width, "height": height})
    page.set_content(html)
    page.wait_for_load_state("load")
    return Image.open(io.BytesIO(page.screenshot()))


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page()
        icon = screenshot(page, f"<body style='margin:0'>{icon_svg(512)}</body>", 512, 512)
        icon.convert("RGB").save(OUT / "icon-512.png", optimize=True)
        html = FEATURE.format(bg=BACKGROUND, icon=icon_svg(200), chips="".join(chip(*s) for s in STATUSES))
        # The feature graphic must have no alpha channel.
        screenshot(page, html, 1024, 500).convert("RGB").save(OUT / "feature-graphic.png", optimize=True)
        browser.close()
    print("play/graphics/icon-512.png\nplay/graphics/feature-graphic.png")


if __name__ == "__main__":
    main()
