# Font and UI correction verification — 2026-10-20

The catalog filter's first section was receiving product-media styling through the shared `.store-card > div:first-child` selector. Image backgrounds, rounding, aspect ratio and price typography now use dedicated product-card classes. The filter search has an associated label and a shared Inter font.

Inter is bundled locally with its SIL Open Font License. Body text and Tailwind `font-sans` utilities share the same family and Vietnamese glyph coverage. Delivery was verified with Google Fonts blocked.

Additional corrections cover secondary text contrast, button hover colors, stock/review/order labels, warning/error colors, admin table headings and mobile floating contact controls.

## Checks

- `npm run check`: TypeScript, 8 unit tests, production build passed.
- `audit-report.json`: 166 user/admin route, theme, viewport and dialog/hover combinations checked; zero uncaught browser errors, font-load failures, page-width overflow or measured text-contrast issues in the inspected states.
- `../qa-result.json`: storefront/admin navigation, theme persistence, assistant opening, auth guard, login validation, sidebar search/collapse and keyboard dismissal passed on the Docker build.
- `../production-font-result.json`: actual Docker catalog checked in light/dark and mobile; no remote Google Fonts requests, local Inter loaded, filter section free of image styling.
- Frontend, backend, PostgreSQL and Redis remained healthy after updating only the frontend container.

Protected-page audit data uses isolated browser fixtures. The production application's authentication and backend data are not replaced. Contrast checks inspect computed text/background colors; screenshots are retained for visual review.

Before/after focus: `../filter-fixed-production-dark.png` and `../filter-fixed-production-light.png` show the corrected filter search in the deployed local app.
