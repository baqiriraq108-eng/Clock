# Unified Master Specification: Dual-Target Live Clock Widget & Companion Adaptive Launcher Icon

This plan implements the unified master specification defined by the provided SVG (`viewBox="0 0 1024 1024"`) and high-resolution reference PNG (`4:22`), delivering a **100% Live Real-Time Home Screen Widget** (with a clean static base plate, live-rotating vector analog hands, and live-updating wavy vector digital numerals) alongside a **Companion Static 108dp Adaptive Launcher Icon** engineered for glitch-free launcher animations.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> **Strict Layer Separation & Dual-Target Fidelity**
> - **Widget Base Plate (Strictly Empty Dial & Empty Emerald Bubble)**: The widget's static background layer renders *only* the deep-sapphire squircle container (`#1E40AF → #112873 → #071133`), the sculpted concentric blue wave reliefs, the unified curved orange dual-arrow (with zero split seams), the lavender-indigo analog bezel and recessed midnight dial face with its 4 cardinal tick markers (`#93A8F8`), and the empty sea-glass emerald bubble (`#A7F3D0 → #34D399 → #059669 → #064E3B`). Neither clock hands nor digital numerals are baked into the widget base plate.
> - **Live Analog Hands (`AnalogClock`)**: The hour, minute, and electric-azure second hands use the exact SVG rounded-capsule geometry (`rx="13"`, `rx="10"`, `rx="3.5"`), gradients (`whiteHandGrad`, `blueSecondGrad`, `pivotCapGrad`), and drop shadow (`handShadow`), centered precisely at `(654, 472)` in the `1024×1024` coordinate system and driven in real time by the system `AnalogClock`.
> - **Live Digital Numerals (`HH:mm` / `h:mm`)**: Rendered dynamically every minute inside the emerald bubble (`[148..642, 486..822]`) using the playful, undulating cubic-Bezier vector numeral style from the SVG (`numeralShadow`: `dx=0, dy=5, stdDeviation=5, #022C22` at 55% opacity). When the time is `4:22`, the rendered Bezier outlines match the SVG specification 1:1.
> - **Downscaling Smoothness (`1×1` Tile Support)**: All bitmap drawables and runtime bitmaps enable `android:filter="true"`, `android:dither="true"`, and `android:mipMap="true"` so the widget remains silky-smooth at any launcher grid size.
> - **Static Companion Adaptive Icon (`108dp` Canvas / `66dp` Safe Zone)**: Locks the reference `"4:22"` display inside the `66dp` safe zone over a continuous deep navy (`#071133`) bleed background (`0dp → 108dp`) for zero-glitch app drawer transitions.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **Live Real-Time Home Screen Widget & In-App Clock**: Displays the sculpted 3D Material tactile clock where the analog hands sweep continuously around the right-hand dial (`cx=654, cy=472`) and the wavy white vector numerals inside the lower-left sea-glass emerald bubble update live every minute (`h:mm` or `HH:mm`), while the upper-left unified curved orange dual-arrow remains stationary.
  2. **Companion Static Launcher Icon**: Displays the locked `"4:22"` master composition from the unified SVG/PNG specification inside a standard Android Adaptive Icon (`108dp` full canvas, `66dp` safe zone) so app launch and close spring animations never flicker or clip.
- **Target Audience / Persona**: Users who expect collector-grade vector precision, smooth multi-stop gradients, and true live functionality on their home screen without any baked static artifacts.
- **Key Value**: Combines the exact chromatic and geometric art direction of the SVG + PNG master specification with a real-time multi-layer Android rendering pipeline.

---

## 2. User Experience & Visual Design

- **Visual Identity & Exact Coordinate Geometry (`1024×1024` Space)**:
  - **Main Squircle Container**: `x=64, y=64, width=896, height=896, rx=220` filled with `bgSquircleGrad` (`#1E40AF → #112873 → #071133`), framed by the `4px` `squircleRimGrad` specular highlight and elevated by `squircleShadow` (`dy=20, stdDeviation=24, #020617`).
  - **Sculpted Concentric Blue Waves**: Clipped cleanly inside `squircleClip` using sub-pixel alpha masking (`AlphaComposite.DstIn`), featuring the outer wave (`outerWaveGrad`: `#2563EB → #173B8F → #0B1D52`) and inner wave crest (`innerWaveGrad`: `#3B82F6 → #1D4ED8 → #102A6B`) with `waveShadow` (`dx=-8, dy=10, stdDeviation=16`).
  - **Unified Curved Orange Dual-Arrow (Stationary)**: Single continuous organic Bezier silhouette (`M 284,202 ... Z`) with zero split seams, filled with `unifiedArrowGrad` (`#FFE566 → #FFAE19 → #FF7300 → #E04800`), `2.5px` `arrowInnerGlow` stroke, internal 3D crest highlight (`M 294,216 ... Z`), and `arrowShadow` (`dy=14, stdDeviation=14`).
  - **Right Analog Clock Dial (`cx=654, cy=472`)**:
    - Outer 3D lavender-indigo bezel (`r=222`, `bezelRingGrad`: `#C7D2FE → #818CF8 → #4F46E5`) with `dialShadow` (`dx=-6, dy=18, stdDeviation=20`) and outer specular bevel (`r=220.5, #E0E7FF`).
    - Inner recessed midnight navy dial face (`r=176`, `dialFaceGrad`: `#0F1E4A → #08112E → #04091A`) with `4px` `bezelInnerBevel` stroke.
    - 4 minimalist cardinal pill markers (`#93A8F8`, `rx=10`) at 12, 3, 6, and 9 o'clock.
  - **Sea-Glass Emerald Digital Bubble**: Asymmetrical kidney/pebble silhouette (`M 278,486 ... Z`) filled with `emeraldBubbleGrad` (`#A7F3D0 → #34D399 → #059669 → #064E3B`), `3px` `bubbleRimGrad` border, upper-left glass specular reflection band (`M 274,494 ... Z`), and bottom-right inner depth cushion (`M 218,742 ... Z`, `#022C22` at 38% opacity).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Strict Separation of Static Base Plate vs. Live Dynamic Overlays**
  - *Chosen Approach*: Generate the 4x supersampled (`2048×2048`) widget base plate directly from Sections 1–4 and Section 6 of the SVG specification—completely omitting Sections 5 (hands) and 7 (numerals). Bind Section 5's vector hand geometry to `AnalogClock` and render Section 7's wavy cubic-Bezier numerals dynamically in `LiveBubbleTimeRenderer`.
  - *Why*: Guarantees the widget is 100% live and never shows ghost/static hands or baked numbers behind the real-time clock.
- **Decision 2: Bitmap Wrapper Drawables with `filter`, `dither`, and `mipMap` Enabled**
  - *Chosen Approach*: Provide XML `<bitmap>` drawables with `android:filter="true"`, `android:dither="true"`, and `android:mipMap="true"` referencing the high-resolution raster assets, alongside hardware-accelerated trilinear/bilinear filtering on the dynamic numeral canvas.
  - *Why*: Eliminates aliasing or shimmer when users resize the widget down to a compact `1×1` home screen cell.

---

## 4. Technical Architecture & Data Strategy

```
┌───────────────────────────────────────────────────────────────────────────┐
│                 Unified Master Specification (1024×1024 SVG)              │
└───────────────────┬───────────────────────────────────┬───────────────────┘
                    │                                   │
                    ▼                                   ▼
┌───────────────────────────────────────┐ ┌─────────────────────────────────┐
│   1. LIVE HOME SCREEN WIDGET TARGET   │ │  2. STATIC ADAPTIVE ICON TARGET │
│                                       │ │                                 │
│  ┌─────────────────────────────────┐  │ │  ┌───────────────────────────┐  │
│  │ Layer 1: Static Base Plate      │  │ │  │ ic_launcher_background    │  │
│  │ • Squircle + Concentric Waves   │  │ │  │ • 108dp Deep Navy Bleed   │  │
│  │ • Unified Curved Orange Arrow   │  │ │  │   (#071133 -> #04091E)    │  │
│  │ • Empty Dial + 4 Tick Markers   │  │ │  └─────────────┬─────────────┘  │
│  │ • Empty Emerald Glass Bubble    │  │ │                │                │
│  │ • filter=true, dither=true,     │  │ │  ┌─────────────▼─────────────┐  │
│  │   mipMap=true                   │  │ │  │ ic_launcher_foreground    │  │
│  └────────────────┬────────────────┘  │ │  │ • Centered 66dp Safe Zone │  │
│                   │                   │ │  │ • Static "4:22" Master    │  │
│  ┌────────────────▼────────────────┐  │ │  │   PNG + filter/dither/    │  │
│  │ Layer 2: Live AnalogClock       │  │ │  │   mipMap enabled          │  │
│  │ • Centered at (654, 472)        │  │ │  └───────────────────────────┘  │
│  │ • Vector Hour Hand (26×118)     │  │ └─────────────────────────────────┘
│  │ • Vector Minute Hand (20×152)   │  │
│  │ • Vector Second Hand (7×184)    │  │
│  │   + Domed Azure Pivot Cap (r=19)│  │
│  └────────────────┬────────────────┘  │
│                   │                   │
│  ┌────────────────▼────────────────┐  │
│  │ Layer 3: Live Wavy Numerals     │  │
│  │ • LiveBubbleTimeRenderer        │  │
│  │ • Wavy Bezier Glyphs (0-9 & :)  │  │
│  │ • Updates Every Minute (HH:mm)  │  │
│  └─────────────────────────────────┘  │
└───────────────────────────────────────┘
```

- **Coordinate Alignment**:
  - **Analog Clock Center**: Positioned at `(654, 472)` in `1024×1024` space (`63.87%` horizontal, `46.09%` vertical) with a `352×352` inner dial viewport (`weights: left=466, width=344, right=190; top=289, height=344, bottom=367` in `1000`-weight space).
  - **Digital Bubble Viewport**: Positioned over the emerald bubble (`x=[148..642], y=[486..822]` in `1024×1024` space) so the undulating cubic-Bezier numerals (`0–9` and `:`) sit at the exact baseline cascade of `"4:22"` (`digit 1 center ~(272, 667)`, `colon ~(355, 684)`, `digit 2 ~(426, 690)`, `digit 3 ~(540, 702)`).
