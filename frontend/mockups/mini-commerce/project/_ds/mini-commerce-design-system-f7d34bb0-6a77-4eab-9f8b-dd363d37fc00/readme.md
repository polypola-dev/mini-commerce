# Mini-Commerce Design System

A generous, photography-led consumer-marketplace design system — Airbnb-style
(오픈마켓 / 숙소·체험 마켓플레이스). Pure-white canvas, near-black ink, and a single
voltage of **Rausch** coral carrying every primary action. The system trusts
photography for visual heft, so typography stays modest and elevation caps at a
single shadow tier.

> **Brand note:** "Mini-Commerce" is an original marketplace brand built to the
> Airbnb-style pattern described in the brief. The logo, wordmark, and product
> icons here are original assets — not Airbnb's marks.

## Sources

- **Brief:** the pasted "Mini-Commerce, 오픈마켓, Airbnb 스타일" specification (colors,
  type, layout, elevation, components, responsive behavior). No codebase, Figma
  file, or slide deck was attached — the system is authored entirely from that
  written spec.
- No font binaries were provided (see Font Substitution below).

---

## Content fundamentals

**Voice.** Warm, plain, second-person. Copy speaks to *you* ("Inspiration for
future getaways", "What this place offers", "Meet your host"). Never corporate,
never salesy — the marketplace is a host of real people, so copy reads like a
helpful friend, not a brand.

**Casing.** Sentence case everywhere — headlines, buttons, nav. The only
uppercase in the system is the tiny 8px "NEW" tab badge and the date-cell
column micro-labels ("CHECK-IN"). Titles like "What this place offers" stay
sentence case even at display size.

**Tone & rhythm.** Short. Concrete. Place-and-number led: "6 guests · 3 bedrooms
· 4 beds · 2 baths", "$182 night", "Nov 3 – 8". Middots (·) separate meta
fragments rather than commas. Trust signals are stated flatly ("Superhost",
"Guest favorite", "98% response rate") — no exclamation, no hype.

**Emoji.** None. The brand expresses warmth through photography and the coral
accent, never emoji. Unicode is used only as functional glyphs (★ rating, ←
back, · separators).

**Examples.**
- H1: *"Inspiration for future getaways"*
- Section head: *"What this place offers"*
- CTA: *"Reserve"*, *"Become a host"*, *"Show all 32 amenities"*
- Meta: *"Sea view villa · Hosted by Aylin"*, *"4.94"*, *"148 reviews"*
- Reassurance: *"You won't be charged yet"*

---

## Visual foundations

**Color.** 90% white + ink, with one or two Rausch moments per page. `--color-primary`
(#ff385c, "Rausch") is the *only* brand color in mainline surfaces — it carries
primary CTAs, the search orb, the saved-heart fill, and the wordmark. Luxe
(#460479) and Plus (#92174d) are sub-brand accents scoped to their own product
surfaces and never appear in marketing. Text is never pure black — ink is
#222222; stars render in **ink, not gold** (a deliberate "yellow stars feel
cheap in travel" choice). Error red (#c13515) is distinct from Rausch.

**Type.** One family carries the whole scale — **Pretendard Variable**, a single
variable face covering Latin, numerals, and 한글. Display weights are modest: the
homepage h1
is just 28/700, the listing h1 a quiet 22/500. The single loud typographic
moment in the entire system is the **64px / 700 rating display** on listing
pages — rating is the peak trust signal, so it alone gets typographic muscle.

**Spacing.** 4px base unit (2px micro-step). Deliberate contrast: editorial
bands breathe at 64px (`--space-section`) while card grids compress to 16px
gutters — "open hero, dense marketplace below". Content caps at 1280px
(editorial) / 1080px (listing detail) / 1440px (search).

**Backgrounds.** Always pure white — no dark mode on the public web, no
gradients, no patterns, no textures. Depth and interest come entirely from
full-bleed **photography** (warm, natural, lived-in interiors and destinations)
and white-on-white surface separation. There is no decorative imagery beyond
real listing photos.

**Corners & cards.** Soft everywhere — the only hard corner is the body grid.
Buttons/inputs 8px, property/host/reservation cards 14px, category strip 32px,
search bar + hearts + orbs fully round. A property card is a 1:1 photo with 14px
corner clipping over a borderless meta block; a reservation/host card is a white
surface with a 1px hairline border + the system shadow.

**Elevation.** Exactly **one shadow tier** plus the flat baseline (95% of
surfaces are flat). `--shadow-card` = `rgba(0,0,0,.02) 0 0 0 1px,
rgba(0,0,0,.04) 0 2px 6px, rgba(0,0,0,.1) 0 4px 8px` — used on hover-floated
cards, the search bar at rest, and dropdowns. No progressive tiers.

**Borders.** 1px hairlines (#dddddd) divide search segments, table rows, footer
columns, and card edges. A lighter #ebebeb separates long editorial scroll. On
input focus the border thickens to 2px ink — **no glow, no ring**.

**Animation & states.** Restrained. Transitions are short (≈120ms) background /
border / opacity fades — no bounces, no springs, no decorative motion. Hover on
cards is a subtle elevation lift (the shadow tier). Press state on the primary
button flips to `--color-primary-active` (#e00b41) — no transform, no scale, no
shadow change.

**Transparency & blur.** Used sparingly — the saved-heart outline sits on a 50%
black scrim over photos; the modal backdrop is `--color-scrim` (#000) at 50%.
No frosted-glass / backdrop-blur surfaces.

---

## Iconography

- **Product tab icons** (`assets/icons/product-*.svg`) — three original
  hand-illustrated 32px line icons (Homes / Experiences / Services), drawn in a
  consistent 2px-stroke style with a soft `--color-primary-disabled` fill accent.
  These ship as project SVGs and are referenced by the `TopNav` component.
- **UI glyphs** (search, globe, hamburger, heart, star, chevrons) are inline
  SVGs authored directly inside the components at a matching ~1.6–2.4px stroke
  weight — there is no icon font and no sprite sheet.
- **No emoji** anywhere. Unicode appears only as functional glyphs (★ ← ·).
- **Substitution flag:** no brand icon set was provided. If you need a broader
  general-purpose icon set, the closest match in stroke feel is **Lucide**
  (2px round-join line icons, CDN-available) — flag any Lucide use to the brand
  owner as a substitution.

---

## Font

The system standardises on a **single shipped face — Pretendard Variable**
(jsDelivr CDN, dynamic subset). One variable file covers Latin, numerals, and
한글, so the whole system loads exactly one font family — a deliberate
performance and consistency choice over a Latin+Korean pairing.

The brand's originally-specified face was *Mini Cereal VF* (licensed custom
variable) with *Circular* as fallback; the team chose Pretendard instead. To
switch to a bespoke face later, add local `@font-face` rules in
`tokens/fonts.css` and prepend the family to `--font-sans`.

**→ Please send the Mini Cereal VF / Circular `.woff2` files to replace Inter.**

---

## Index / manifest

**Root**
- `styles.css` — global entry point (consumers link this one file; `@import`s only).
- `readme.md` — this guide.
- `SKILL.md` — Agent-Skills front-matter for downloadable use.

**`tokens/`** — `fonts.css`, `colors.css`, `typography.css`, `spacing.css`, `radius.css`
(radius + the single elevation shadow).

**`assets/`** — `logo-mark.svg`, `logo-wordmark.svg`, `logo-wordmark-white.svg`,
and `icons/product-{homes,experiences,services}.svg`.

**`components/core/`** — `Button`, `IconButton`, `Input`, `Badge`, `Avatar`, `StarRating`.

**`components/marketplace/`** — `SearchBar`, `PropertyCard`, `TopNav`,
`ReservationCard`, `RatingDisplay`.

**`ui_kits/marketplace/`** — interactive web recreation: `Home.jsx`,
`SearchResults.jsx`, `ListingDetail.jsx` wired through `index.html`
(home → search → listing detail click-through).

**`guidelines/`** — foundation specimen cards (Colors, Type, Spacing,
Foundations, Brand) shown in the Design System tab.

All components are reachable from `window.MiniCommerceDesignSystem_f7d34b`
after loading the compiled `_ds_bundle.js`.
