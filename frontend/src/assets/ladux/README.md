# Ladux — Warm Orange visual identity

Original assets created for the storefront and administration redesign, October 2026.

## Files

| Asset | Purpose | Format |
| --- | --- | --- |
| `hero.webp` | Storefront hero and customer login/register artwork | 1536 × 1024, ~89 KB |
| `operations.webp` | Admin login and dashboard welcome illustration | 1254 × 1254, ~56 KB |
| `assistant.svg` | Original orange assistant mascot | Editable SVG |
| `source/hero-orange.png` | Original generated hero | PNG |
| `source/operations-orange.png` | Original generated operations illustration | PNG |

The brand symbol is drawn in `components/common/BrandMark.tsx`; a standalone favicon is in `public/ladux-mark.svg`. The category treatments and SmartFit illustration are rendered with SVG/CSS in the storefront. Existing product photographs are preserved.

Raster images were created with the built-in ImageGen tool, then encoded as WebP for delivery without changing their composition. Original PNGs are retained here. The image generation prompts are recorded verbatim below.

## Hero prompt

Use case: product-mockup. Asset type: premium Vietnamese laptop shop Ladux website hero artwork, landscape 3:2. Create an exceptionally polished photorealistic CGI still life of a thin brushed silver laptop opened 110 degrees and angled three quarters, hovering just above a sculptural ivory circular plinth, one beautiful broad translucent amber-orange glass ribbon curling elegantly behind the laptop. The laptop screen shows an abstract bright orange sunset sphere against a dark warm charcoal display. A small off-white wireless mouse and one orange anodized aluminum geometric block balance the foreground. Background is a seamless warm ivory studio, soft peach light on the right, refined subtle shadows. Restrained industrial design, editorial product photography, premium physically based materials, precise beautiful keyboard and trackpad. Entire laptop completely in frame, subject centered, fill frame beautifully, no people, no text, no letters, no brand logos, no watermark. No clouds, no cartoon, no pastel blue. This is the actual artwork used beside live HTML heading, do not draw a webpage or UI.

## Operations prompt

Use case: stylized-concept. Asset type: original editorial 3D illustration for Ladux technology store administration workspace. Square 1:1 image. An elegant sculptural still life: three matte ivory modular shipping boxes stacked into an abstract ascending staircase, an orange anodized aluminum torus leaning on the tallest box, a slim brushed silver laptop partially open beside the boxes, and a single small frosted amber glass sphere. Meticulous product-design CGI, soft sculptural shadows, beautiful edge highlights, warm peach and burnt-orange accent light, seamless warm off-white studio floor and backdrop. Premium tactile minimalism, not cartoon. Center composition with generous negative space around, isometric three quarter view, entire arrangement in frame, no text, no logos, no watermarks, no numbers, no charts, no screenshot. Must feel like a refined coherent asset for a white and orange commerce software brand. Crisp immaculate physically based materials.

## Theme

`styles/design-system.css` defines semantic surface, text, neutral and orange tokens shared across user/admin pages and portalled dialogs. Light mode uses warm white; dark mode uses charcoal and warm orange. The `ThemeProvider` stores the choice under `ladux-theme`; the document head restores it before the app renders.

## Visual verification

Local screenshots and ad hoc visual QA scripts are kept outside the committed assets in `output/ladux-redesign/` at repository root. Verification covered 166 route/theme/viewport combinations, plus the production Docker build with external font requests blocked. Storefront checks used public catalog responses; admin checks used isolated browser fixtures for authenticated layouts. No development authentication bypass or sample metrics are included in the production app.
