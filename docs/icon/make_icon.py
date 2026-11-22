#!/usr/bin/env python3
"""
Generates the launcher icon of Majon: a ball of yarn with a knitting needle.

  python3 docs/icon/make_icon.py v2            # writes the Android vector drawables + legacy bitmaps
  python3 docs/icon/make_icon.py v1 --svg-only # only the SVG master (docs/icon/icon-v1.svg)

v1 = neat crossing strands (looks like a globe); v2 = chaotic wound strands (looks like yarn).
Needs rsvg-convert and ImageMagick for the legacy bitmaps (API 24-25 only).
"""
import math, os, random, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "..", "app", "src", "main", "res")
version = sys.argv[1] if len(sys.argv) > 1 else "v2"
svg_only = "--svg-only" in sys.argv

CX, CY, R = 52.0, 48.0, 27.0
BASE, DARK, LIGHT, MID = "#D8664E", "#B4472F", "#F4B7A4", "#E48A72"
NEEDLE, NEEDLE_D = "#6B9B7F", "#4F7A62"
BG = "#FBF1E4"
# A thin thread that leaves the windings and lies on the table in a soft S (same thickness as the strands)
TAIL = "M62,69 C66,76 61,79 64,83 S73,88 69,90"
NEEDLE_PATH = "M30,72 L74,28"
f = lambda v: f"{v:.2f}".rstrip("0").rstrip(".")


def outline_path(rng):
    """The ball's silhouette: a circle with a slightly lumpy edge (v2) or a perfect one (v1)."""
    if version == "v1":
        return f"M{f(CX-R)},{f(CY)} a{f(R)},{f(R)} 0 1,0 {f(2*R)},0 a{f(R)},{f(R)} 0 1,0 {f(-2*R)},0"
    n = 28
    pts = []
    for i in range(n):
        a = 2 * math.pi * i / n
        rr = R * (1 + rng.uniform(-0.04, 0.035))
        pts.append((CX + rr * math.cos(a), CY + rr * math.sin(a)))
    mids = [((pts[i][0] + pts[(i + 1) % n][0]) / 2, (pts[i][1] + pts[(i + 1) % n][1]) / 2) for i in range(n)]
    d = f"M{f(mids[-1][0])},{f(mids[-1][1])}"
    for i in range(n):
        d += f" Q{f(pts[i][0])},{f(pts[i][1])} {f(mids[i][0])},{f(mids[i][1])}"
    return d + "Z"


def strands(rng):
    """(path, colour, width, opacity) for every strand, in drawing order."""
    out = []
    if version == "v1":
        for i in range(-4, 5):
            s = i * R * 0.30
            out.append((f"M{f(CX-R*1.1)},{f(CY+s+R*0.55)} Q{f(CX)},{f(CY+s-R*0.55)} {f(CX+R*1.1)},{f(CY+s+R*0.55)}", DARK, 3, 0.75))
        for i in range(-3, 4):
            s = i * R * 0.36
            out.append((f"M{f(CX+s+R*0.5)},{f(CY-R*1.1)} Q{f(CX+s-R*0.55)},{f(CY)} {f(CX+s+R*0.5)},{f(CY+R*1.1)}", LIGHT, 2.3, 0.6))
        return out
    # v2: wound in layers, each layer at its own angle, thin and dense, every strand a little off
    layers = [(-38, 15), (14, 16), (62, 13), (-8, 8)]
    for base_deg, count in layers:
        for _ in range(count):
            th = math.radians(base_deg + rng.uniform(-9, 9))
            u = (math.cos(th), math.sin(th))
            p = (-u[1], u[0])
            d = rng.uniform(-0.98, 0.98) * R
            q = (CX + p[0] * d, CY + p[1] * d)
            h = math.sqrt(max(R * R - d * d, 4)) * 1.12
            bend = rng.choice([-1, 1]) * rng.uniform(0.10, 0.42) * R
            a = (q[0] - u[0] * h, q[1] - u[1] * h)
            b = (q[0] + u[0] * h, q[1] + u[1] * h)
            c = (q[0] + p[0] * bend, q[1] + p[1] * bend)
            colour = rng.choice([DARK, MID, MID, LIGHT, BASE, MID])
            out.append((f"M{f(a[0])},{f(a[1])} Q{f(c[0])},{f(c[1])} {f(b[0])},{f(b[1])}", colour, round(rng.uniform(1.3, 2.3), 1), round(rng.uniform(0.35, 0.7), 2)))
    return out


def loose_ends(rng):
    """A few strands that escape over the edge of the ball."""
    out = []
    for deg in (212,):
        a = math.radians(deg + rng.uniform(-8, 8))
        sx, sy = CX + R * 0.93 * math.cos(a), CY + R * 0.93 * math.sin(a)
        ex, ey = CX + (R + 3.2) * math.cos(a + 0.28), CY + (R + 3.2) * math.sin(a + 0.28)
        cx_, cy_ = CX + (R + 4) * math.cos(a + 0.08), CY + (R + 4) * math.sin(a + 0.08)
        out.append(f"M{f(sx)},{f(sy)} Q{f(cx_)},{f(cy_)} {f(ex)},{f(ey)}")
    return out


rng = random.Random(7)
OUTLINE = outline_path(rng)
STRANDS = strands(rng)
LOOSE = loose_ends(rng) if version != "v1" else []
HIGHLIGHT = (CX - R * 0.38, CY - R * 0.42, R * 0.45)
SHADE = (CX + R * 0.55, CY + R * 0.6, R * 0.9)

# ------------------------------------------------------------------ SVG master
svg = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">\n<rect width="108" height="108" fill="{BG}"/>\n'
svg += f'<ellipse cx="{f(CX)}" cy="82" rx="21" ry="3.6" fill="#000" opacity=".11"/>\n'
svg += f'<path d="{TAIL}" fill="none" stroke="{DARK}" stroke-width="3.1" stroke-linecap="round"/>\n<path d="{TAIL}" fill="none" stroke="{BASE}" stroke-width="1.7" stroke-linecap="round"/>\n'
for d in LOOSE:
    svg += f'<path d="{d}" fill="none" stroke="{DARK}" stroke-width="2.6" stroke-linecap="round"/><path d="{d}" fill="none" stroke="{MID}" stroke-width="1.3" stroke-linecap="round"/>\n'
svg += f'<path d="{OUTLINE}" fill="{BASE}"/>\n<clipPath id="b"><path d="{OUTLINE}"/></clipPath>\n<g clip-path="url(#b)">\n'
for d, c, w, o in STRANDS:
    svg += f'<path d="{d}" fill="none" stroke="{c}" stroke-opacity="{o}" stroke-width="{w}" stroke-linecap="round"/>\n'
svg += f'<circle cx="{f(SHADE[0])}" cy="{f(SHADE[1])}" r="{f(SHADE[2])}" fill="#000" opacity=".13"/>\n'
svg += f'<circle cx="{f(HIGHLIGHT[0])}" cy="{f(HIGHLIGHT[1])}" r="{f(HIGHLIGHT[2])}" fill="#fff" opacity=".2"/>\n</g>\n'
svg += f'<path d="{NEEDLE_PATH}" fill="none" stroke="{NEEDLE_D}" stroke-width="4.2" stroke-linecap="round"/>\n<path d="{NEEDLE_PATH}" fill="none" stroke="{NEEDLE}" stroke-width="2.6" stroke-linecap="round"/>\n'
svg += f'<circle cx="74" cy="28" r="4" fill="{NEEDLE_D}"/><circle cx="73.4" cy="27.4" r="2.6" fill="{NEEDLE}"/>\n</svg>\n'
svg_path = os.path.join(HERE, f"icon-{version}.svg")
open(svg_path, "w").write(svg)
print("wrote", svg_path)
if svg_only:
    sys.exit(0)

# ------------------------------------------------------------------ Android vector drawables
def argb(hexcolor, alpha):
    return f"#{round(alpha * 255):02X}{hexcolor[1:]}"

def stroke(d, color, w, alpha=1.0, indent="    "):
    c = color if alpha == 1.0 else argb(color, alpha)
    return f'{indent}<path android:pathData="{d}" android:strokeColor="{c}" android:strokeWidth="{w}" android:strokeLineCap="round" />\n'

def disc(cx, cy, r, color):
    return f'<path android:fillColor="{color}" android:pathData="M{f(cx-r)},{f(cy)} a{f(r)},{f(r)} 0 1,0 {f(2*r)},0 a{f(r)},{f(r)} 0 1,0 {f(-2*r)},0" />'

head = '<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="108dp"\n    android:height="108dp"\n    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
fg = head + f'\n    <!-- Shadow on the table -->\n    {disc(CX, 82, 21, "#1C000000").replace("a21,21", "a21,3.6").replace("a21,21", "a21,3.6")}\n'
fg = fg.replace(disc(CX, 82, 21, "#1C000000").replace("a21,21", "a21,3.6"), f'<path android:fillColor="#1C000000" android:pathData="M31,82 a21,3.6 0 1,0 42,0 a21,3.6 0 1,0 -42,0" />')
fg += "\n    <!-- Loose end of the yarn -->\n" + stroke(TAIL, DARK, 3.1) + stroke(TAIL, BASE, 1.7)
for d in LOOSE:
    fg += stroke(d, DARK, 2.6) + stroke(d, MID, 1.3)
fg += f'\n    <!-- The ball: wound strands, shaded and lit -->\n    <path android:fillColor="{BASE}" android:pathData="{OUTLINE}" />\n    <group>\n        <clip-path android:pathData="{OUTLINE}" />\n'
for d, c, w, o in STRANDS:
    fg += stroke(d, c, w, o, "        ")
fg += f'        {disc(SHADE[0], SHADE[1], SHADE[2], "#21000000")}\n        {disc(HIGHLIGHT[0], HIGHLIGHT[1], HIGHLIGHT[2], "#33FFFFFF")}\n    </group>\n'
fg += "\n    <!-- Knitting needle stuck in the ball -->\n" + stroke(NEEDLE_PATH, NEEDLE_D, 4.2) + stroke(NEEDLE_PATH, NEEDLE, 2.6)
fg += f'    {disc(74, 28, 4, NEEDLE_D)}\n    {disc(73.4, 27.4, 2.6, NEEDLE)}\n</vector>\n'
open(os.path.join(RES, "drawable", "ic_launcher_foreground.xml"), "w").write(fg)

open(os.path.join(RES, "drawable", "ic_launcher_background.xml"), "w").write(
    head + f'    <path\n        android:fillColor="{BG}"\n        android:pathData="M0,0h108v108h-108z" />\n</vector>\n')

mono = head + f'    <path android:pathData="{OUTLINE}" android:strokeColor="#000000" android:strokeWidth="3.4" />\n' + stroke(TAIL, "#000000", 2.6)
mono += f'    <group>\n        <clip-path android:pathData="{OUTLINE}" />\n'
for d, c, w, o in STRANDS[::3]:
    mono += stroke(d, "#000000", 2.2, 1.0, "        ")
mono += "    </group>\n" + "".join(stroke(d, "#000000", 2.6) for d in LOOSE) + stroke(NEEDLE_PATH, "#000000", 3.6) + f'    {disc(74, 28, 4, "#000000")}\n</vector>\n'
open(os.path.join(RES, "drawable", "ic_launcher_monochrome.xml"), "w").write(mono)

# ------------------------------------------------------------------ legacy bitmaps (Android 7.x)
for density, px in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
    png = os.path.join("/tmp", f"majon_icon_{density}.png")
    subprocess.run(["rsvg-convert", "-w", str(px), "-h", str(px), svg_path, "-o", png], check=True)
    d = os.path.join(RES, f"mipmap-{density}")
    subprocess.run(["magick", png, "-define", "webp:lossless=true", os.path.join(d, "ic_launcher.webp")], check=True)
    subprocess.run(["magick", png, "(", "-size", f"{px}x{px}", "xc:none", "-fill", "white", "-draw", f"circle {px//2},{px//2} {px//2},0", ")",
                    "-compose", "DstIn", "-composite", "-define", "webp:lossless=true", os.path.join(d, "ic_launcher_round.webp")], check=True)
    os.remove(png)
print("wrote the Android resources for", version)
