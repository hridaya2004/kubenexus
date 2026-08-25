# M3 Design Tokens — Reference Specsheet

> Original summary of factual specifications from
> https://m3.material.io/foundations/design-tokens/overview
> (verified live, Aug 2026). Facts only; prose paraphrased. Source licensed Apache 2.0 / CC BY 4.0.

## What tokens are

A design token pairs a code-like **name** with a **value** (color, typeface, measurement, or another token).

Example from the live page:
- `md.ref.palette.secondary90` → `#E8DEF8`
- `md.comp.fab.primary.container.color` — names exactly where/how the value applies

Tokens replace hard-coded values so style decisions stay consistent across design files and implementations and can be updated globally.

## Naming grammar

Names read general → specific, dot-separated:

```
md . <class> . <domain>… . <property>
```

- `md` — system prefix (Material Design)
- class: `ref` | `sys` | `comp`
- trailing words describe purpose (e.g., `on-secondary`)

## Three token classes

| Class | Prefix | Role |
|---|---|---|
| Reference | `ref` | The full menu of available style values; static; never context-dependent |
| System | `sys` | Design-language decisions for a theme/context (color roles, type scale, elevation, shape); this is where theming happens; should point at `ref` tokens |
| Component | `comp` | Per-component element styling (container color, label text, icon, states); should point at `sys`/`ref`, not raw values; marked in development on-site |

Chain example described on-page: component token → system token(s) → reference token → hex value; changing the hex propagates everywhere without touching syntax.

## Contexts

Tokens can resolve to different **contextual values** by conditions such as dark theme, form factor, density, or RTL. A context acts like a tag that overrides the default value when active (e.g., background sys token pointing to different refs in light vs dark).

## Where tokens appear on the site

- Interactive token modules inside style pages (typography, shape, spacing, elevation) — expandable folders per style/axis.
- Component pages' **Specs** tabs, grouped first by state (enabled, disabled, hover…) then by element (container, label text, icon).
- Module columns: Name · Token ID · Description · Context/value.

## Baseline token resources

- Material baseline theme + tokens downloadable as **DSP** (Design System Package) — linked from the tokens page resources table.
- Material Theme Builder (+ Figma plugin) generates schemes/tokens from a source color.

## Canonical system-token families referenced across this specsheet

| Family | Example |
|---|---|
| Color roles | `md.sys.color.primary`, `md.sys.color.on-primary-container`, `md.sys.color.surface-container-high` |
| Typography | `md.sys.typescale.headline-large` (+ `.emphasized.*`) |
| Elevation | `md.sys.elevation.level0…level5` |
| Shape | corner scale incl. expressive `large increased` 20dp / `xl increased` 32dp / `xxl` 48dp / `full` |
| Spacing | `space100` = 8dp baseline + multiples |
| Motion | `md.sys.motion.spring.fast.spatial`, legacy easing/duration tokens |

## Section map (pages crawled)

- `/foundations/design-tokens/overview` — taxonomy, naming, contexts
