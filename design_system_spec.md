# DIAMONDS DESIGN SYSTEM
## "Sleek Interface" Specifications for Luxury Mobile Apps

This document defines the UI foundations and layout tokens for the **Diamonds "Sleek Interface" design theme**, designed to deliver an exquisite, ultra-premium experience for high-end operations.

---

### 1. COLOR PALETTE (Material 3 Adaptive Mapping)

The "Sleek Interface" merges soft, warm-neutral background tones with rich royal purple accents and high-contrast charcoal text to achieve structural depth, spaciousness, and premium contrast.

| Token Name | Hex Value | Color Preview | Semantic Role & Mapping |
| :--- | :--- | :--- | :--- |
| **Canvas Background** | `#FDF8F6` | Peach Ivory | Root background, provides light warmth and extreme eye comfort. |
| **Surface Card** | `#FFFFFF` | Pure White | Elevated bento cards, lists, dialog sheets. High negative space. |
| **Structural Border** | `#E7E0EC` | Lavender Grey | Borders, card dividers, outlines, inactive text field frames. |
| **Primary Accent** | `#6750A4` | Royal Purple | Primary brand triggers, main actions, active navigation pills, highlighted states. |
| **Secondary Container**| `#E8DEF8` | Light Lavender | Light accents, highlight cards, active selection backgrounds. |
| **Tertiary Accent** | `#D1E1FF` | Ice Blue | Subtitle accent badges, data trends, alternate visual cards. |
| **Text Primary** | `#1C1B1F` | Deep Charcoal | High-contrast copy, title headers, main labels. |
| **Text Secondary** | `#49454F` | Slate Grey | Subtitles, informational labels, auxiliary labels. |
| **Text Muted** | `#625B71` | Warm Slate | Timestamps, secondary tracking tokens, caption metadata. |

---

### 2. TYPOGRAPHY SCALE

Designed for maximum scan-readability during dynamic operations, utilizing a pairing of condensed display numerals with geometric sans body.

*   **Display Large (Hero Values):** `40sp` | Bold | Tracking `-1.5%` | Line Height `48sp` (e.g., Today's Revenue)
*   **Headline Medium (Screen Headers):** `24sp` | SemiBold | Tracking `0%` | Line Height `32sp` (e.g., "Diamonds Dashboard")
*   **Title Large (Card Titles):** `18sp` | Bold | Tracking `0.5%` | Line Height `24sp` (e.g., "SECURE ACCESS CONTROLS")
*   **Body Large (Main List Items):** `15sp` | Regular/Medium | Tracking `0.25%` | Line Height `20sp`
*   **Body Medium (Meta-descriptions):** `13sp` | Regular | Tracking `0%` | Line Height `18sp`
*   **Label Small (Metadata / Tags):** `10sp` | Bold | Uppercase | Tracking `1.5%` | Line Height `12sp`

---

### 3. SPACING & LAYOUT TOKENS

To ensure a balanced layout with generous negative space, we use a strict **8dp layout grid**.

*   **Page Margin:** `16dp` or `24dp` (tablet)
*   **Card Padding (Bento Grid):** `20dp` or `24dp`
*   **Element Gaps (Vertical list spacing):** `12dp` or `16dp`
*   **Interactive Targets:** Min `48dp x 48dp` tactile container boundaries.
*   **Card Border Radii:** `28px` (`28.dp` in Android Compose) to match classic M3 fluid structural containers.

---

### 4. SHADOW CONFIGURATION

To reflect realistic physical depth on ivory backdrops, we utilize soft, diffused drop shadows.

```css
/* Figma-equivalent CSS drop shadows for the Bento Grid Cards */
.box-shadow-sleek {
  box-shadow: 0px 4px 20px rgba(103, 80, 164, 0.04), 
              0px 1px 3px rgba(0, 0, 0, 0.05);
}
```
*   **Ambient Shadow:** Diffused soft violet/primary shadow.
*   **Directional Shadow:** Core black shadow with high blur and low opacity to give standard 3D depth.
