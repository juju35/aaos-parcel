"""README screenshots: mock-ups of the app's three templates as the Android Automotive host draws them.

Usage: python scripts/screenshots.py  (needs `pip install playwright pillow` and `playwright install chromium`)

The pages copy the templates host layout (dark theme, 1408 x 792 like the AAOS emulator) and the app's own
drawing rules: status line and last event per row (ui/DeliveryListScreen.kt), summary and event rows
(ui/DeliveryDetailScreen.kt), settings rows (ui/SettingsScreen.kt). The deliveries are those of the "demo" key
(data/DemoData.kt) on a fixed day; the icons are the app's own drawables. No network needed.
"""

from __future__ import annotations

import html
import re
from datetime import datetime, timedelta
from functools import cache
from pathlib import Path

from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parent.parent
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable"
COLORS = (ROOT / "app" / "src" / "main" / "res" / "values" / "colors.xml").read_text()
OUT = ROOT / "screenshots"
WIDTH, HEIGHT = 1408, 792

# Saturday 24 October 2026, 10:00: the demo dates are relative to it.
NOW = datetime(2026, 10, 24, 10, 0)
TODAY = NOW.replace(hour=0)
DAYS = ["lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim."]
MONTHS = ["janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."]

# Host colours for CarColor in the dark theme.
COLOR = {"green": "#6ee7a0", "blue": "#8ab4f8", "yellow": "#fdd663", "red": "#ff8a80", "default": "#e8eaed"}

# Status: label, drawable, colour (data/Models.kt, ui/Ui.kt).
STATUS = {
    "delivered": ("Livré", "ic_check", "green"),
    "in_transit": ("En transit", "ic_truck", "blue"),
    "awaiting_pickup": ("À retirer", "ic_store", "yellow"),
    "out_for_delivery": ("En cours de livraison", "ic_truck", "blue"),
    "info_received": ("Informations reçues", "ic_parcel", "blue"),
}

H = timedelta(hours=1)
D = timedelta(days=1)

# The demo deliveries (DemoData.kt), carrier names as Parcel's list gives them.
DELIVERIES = [
    {
        "title": "Casque audio",
        "carrier": "Colissimo",
        "tracking": "6A23456789012",
        "status": "out_for_delivery",
        "expected": (TODAY, None),
        "events": [
            ("Votre colis est en cours de livraison", NOW - 2 * H, "Rennes"),
            ("Votre colis est arrivé sur le site de distribution", NOW - 9 * H, "Rennes PPDC"),
            ("Votre colis est pris en charge par La Poste", NOW - D - 5 * H, "Paris"),
        ],
    },
    {
        "title": "Chaussures de randonnée",
        "carrier": "Chronopost",
        "tracking": "XW123456789FR",
        "status": "awaiting_pickup",
        "expected": None,
        "extra": "À retirer avant le 1 novembre, avec une pièce d'identité",
        "events": [
            ("Colis disponible en point relais", NOW - 5 * H, "Relais Pickup, 12 rue de Paris, 35000 Rennes"),
            ("Colis en cours d'acheminement", NOW - D, "Chilly-Mazarin"),
        ],
    },
    {
        "title": "Livres",
        "carrier": "DHL Express",
        "tracking": "JD014600006281234567",
        "status": "in_transit",
        "expected": (TODAY + 2 * D, TODAY + 3 * D),
        "events": [
            ("Scanné au centre de tri", NOW, "Roissy-en-France"),
            ("Arrivé au pays de destination", NOW - D, "Paris"),
            ("Expédié", NOW - 3 * D, "Leipzig, Allemagne"),
        ],
    },
    {
        "title": "Cartouches d'encre",
        "carrier": "UPS",
        "tracking": "1Z999AA10123456784",
        "status": "info_received",
        "expected": None,
        "events": [("Informations d'expédition reçues", NOW - 3 * H, "")],
    },
    {
        "title": "Câble USB-C",
        "carrier": "DPD",
        "tracking": "250012345678",
        "status": "delivered",
        "expected": None,
        "events": [("Colis livré", NOW - D - 4 * H, "Rennes"), ("En cours de livraison", NOW - D - 7 * H, "Rennes")],
    },
]

# ── Helpers (same rules as the app) ──────────────────────────────────────────


def day(d: datetime) -> str:
    """SimpleDateFormat("EEE d MMM", Locale.FRANCE)."""
    return f"{DAYS[d.weekday()]} {d.day} {MONTHS[d.month - 1]}"


def expected(d) -> str | None:
    """Ui.expected."""
    if not d["expected"]:
        return None
    start, end = d["expected"]
    if end is None or day(end) == day(start):
        return f"Prévu le {day(start)}"
    return f"Prévu entre le {day(start)} et le {day(end)}"


def join(*parts) -> str:
    return " · ".join(p for p in parts if p)


@cache
def drawable(name: str) -> tuple[str, str]:
    """(viewBox, inner SVG) of one of the app's vector drawables."""
    xml = (DRAWABLE / f"{name}.xml").read_text()
    viewport = re.search(r'viewportWidth="(\d+)"', xml)[1]
    path = re.search(r'pathData="([^"]+)"', xml)[1]
    if "strokeWidth" in xml:
        width = re.search(r'strokeWidth="([\d.]+)"', xml)[1]
        shape = (
            f'<path d="{path}" fill="none" stroke="currentColor" stroke-width="{width}" '
            'stroke-linejoin="round" stroke-linecap="round"/>'
        )
    else:
        shape = f'<path d="{path}" fill="currentColor"/>'
    return f"0 0 {viewport} {viewport}", shape


def icon(name: str, size: int = 30, color: str = COLOR["default"]) -> str:
    view_box, shape = drawable(name)
    return f'<svg viewBox="{view_box}" style="width:{size}px;height:{size}px;color:{color};flex:none">{shape}</svg>'


BACK = '<svg viewBox="0 0 24 24" style="width:32px;height:32px;fill:#e8eaed"><path d="M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z"/></svg>'

# ── Pages ────────────────────────────────────────────────────────────────────

CSS = """
* { box-sizing: border-box; margin: 0; padding: 0; }
body { width: 1408px; height: 792px; overflow: hidden; background: #0b0c0e; color: #e8eaed;
       font-family: Inter, Roboto, sans-serif; -webkit-font-smoothing: antialiased; position: relative; }
.secondary { color: #a8adb4; }
.header { display: flex; align-items: center; gap: 20px; height: 104px; padding: 0 40px; font-size: 30px; font-weight: 600; }
.round { width: 56px; height: 56px; border-radius: 28px; display: flex; align-items: center; justify-content: center;
         background: #2d3035; flex: none; }
.appicon { width: 52px; height: 52px; border-radius: 14px; display: flex; align-items: center; justify-content: center; flex: none; }
.strip { position: absolute; right: 40px; top: 24px; display: flex; gap: 12px; }
.chip { display: flex; align-items: center; gap: 10px; height: 60px; padding: 0 26px 0 20px; border-radius: 30px;
        background: #2d3035; font-size: 23px; font-weight: 600; }
.chip.icon { width: 60px; padding: 0; justify-content: center; }
.list { padding: 0 40px; }
.row { display: flex; align-items: center; gap: 28px; min-height: 112px; padding: 14px 8px; border-bottom: 1px solid #3c4043; }
.row:last-child { border-bottom: none; }
.txt { flex: 1; min-width: 0; }
.t { font-size: 26px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.l { font-size: 21px; margin-top: 5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ic { width: 48px; display: flex; justify-content: center; flex: none; }
.switch { width: 64px; height: 36px; border-radius: 18px; background: #8ab4f8; position: relative; flex: none; }
.switch div { position: absolute; top: 4px; left: 32px; width: 28px; height: 28px; border-radius: 14px; background: #0b0c0e; }
"""


def page(body: str) -> str:
    return f"<!doctype html><html><head><meta charset='utf-8'><style>{CSS}</style></head><body>{body}</body></html>"


def row(image: str, title: str, lines: list[str], end: str = "") -> str:
    texts = "".join(
        f'<div class="l{"" if i == 0 and "<span" in line else " secondary"}">{line}</div>'
        for i, line in enumerate(lines)
    )
    return f'<div class="row">{image}<div class="txt"><div class="t">{html.escape(title)}</div>{texts}</div>{end}</div>'


def status_line(d) -> str:
    """DeliveryListScreen.row: the status in its colour, then the expected date, or the carrier once delivered."""
    label, _, color = STATUS[d["status"]]
    rest = None if d["status"] == "delivered" else expected(d)
    rest = rest or d["carrier"]
    return f'<span style="color:{COLOR[color]}">{label}</span> · {html.escape(rest)}'


def status_icon(d, size=48) -> str:
    _, name, color = STATUS[d["status"]]
    return icon(name, size, COLOR[color])


def list_page() -> str:
    key = TODAY + 100 * D
    shown = sorted(DELIVERIES, key=lambda d: (d["status"] == "delivered", d["expected"][0] if d["expected"] else key))
    rows = []
    for d in shown:
        text, _, place = d["events"][0]
        rows.append(row(status_icon(d), d["title"], [status_line(d), html.escape(join(text, place))]))
    background = re.search(r'name="launcher_background">(#[0-9A-Fa-f]{6})<', COLORS)[1]
    body = f"""
    <div class="header"><div class="appicon" style="background:{background}">{icon("ic_parcel", 34, "#fff")}</div>Colis</div>
    <div class="strip"><div class="chip icon">{icon("ic_refresh", 30)}</div><div class="chip icon">{icon("ic_settings", 30)}</div></div>
    <div class="list">{"".join(rows)}</div>
    """
    return page(body)


def detail_page() -> str:
    d = DELIVERIES[1]
    label = STATUS[d["status"]][0]
    rows = [row(status_icon(d), label, [html.escape(join(d["carrier"], d["tracking"]))])]
    rows.append(row('<div style="width:48px"></div>', "Informations", [html.escape(d["extra"])]))
    for text, when, place in d["events"]:
        rows.append(
            row('<div style="width:48px"></div>', text, [html.escape(join(when.strftime("%d/%m/%Y %H:%M"), place))])
        )
    body = f"""
    <div class="header"><div class="round">{BACK}</div>{html.escape(d["title"])}</div>
    <div class="strip"><div class="chip">{icon("ic_navigate", 28)}Y aller</div></div>
    <div class="list">{"".join(rows)}</div>
    """
    return page(body)


def settings_page() -> str:
    switch = '<div class="switch"><div></div></div>'
    rows = [
        row(f'<div class="ic">{icon("ic_key", 40)}</div>', "Clé d'API Parcel", ["Configurée"]),
        row(f'<div class="ic">{icon("ic_filter", 40)}</div>', "Colis affichés", ["En cours et livrés récemment"]),
        row(
            f'<div class="ic">{icon("ic_notifications", 40)}</div>',
            "Notifications",
            ["Vérification toutes les heures", "Dernière vérification il y a 12 minutes"],
            switch,
        ),
    ]
    body = f"""
    <div class="header"><div class="round">{BACK}</div>Réglages</div>
    <div class="list">{"".join(rows)}</div>
    """
    return page(body)


def main():
    OUT.mkdir(exist_ok=True)
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": WIDTH, "height": HEIGHT}, device_scale_factor=1)
        page_ = ctx.new_page()
        for name, html_ in (("list", list_page()), ("detail", detail_page()), ("settings", settings_page())):
            page_.set_content(html_)
            page_.wait_for_load_state("load")
            page_.screenshot(path=str(OUT / f"{name}.png"))
            print(f"screenshots/{name}.png")
        browser.close()


if __name__ == "__main__":
    main()
