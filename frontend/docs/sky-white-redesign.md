# LADUX storefront: Sky & White

[Editable Figma design](https://www.figma.com/design/PGAV0gSfykTjaVSH7pZYcI?node-id=3-30)

Customer-facing UI only. Admin pages, services, stores and backend are unchanged.

- White cards on a very light sky background, slate text, sky actions.
- Rounded corners, a soft blue shadow, quiet hover states, visible keyboard focus.
- A simpler homepage hero, six need categories, brand links and laptop cards.
- Matching light styles for catalog, product detail, cart, checkout, orders,
  wishlist, account, addresses, authentication, contact and payment result.
- Collapsible product filters on mobile; existing filtering and purchase handlers retained.

The Figma file contains desktop and mobile homepages, a desktop catalog and a
desktop sign-in screen. Text, SVG icons and component instances are editable.
Laptop photos are discrete source assets. Color variables, text styles, a card
shadow and shared button/card components are included.

Theme: `src/styles/storefront.css`, scoped to `.storefront-ui`. The existing
global theme and admin styles retain their original behavior.

Validation: TypeScript, eight existing tests, production build, desktop/mobile
visual checks, mobile filter interaction and Figma layer/font/image inspection.
