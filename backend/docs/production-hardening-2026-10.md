# Production P0/P1 hardening (2026-10-06)

> **Historical design note.** Refund guards described below were later changed from order-level to payment-level in V58. Use the [current engineering notes](../../docs/ENGINEERING.md), [project status](../../docs/PROJECT_STATUS.md) and [production runbook](../../docs/PRODUCTION_RUNBOOK.md) for the portfolio snapshot.

## Security
- Order cancellation obtains the authenticated principal's userId, then locks **only that user's** order using `findOwnedWithItemsForUpdate` before changing status/stock.
- Rate limiting uses servlet `getRemoteAddr()`; Caddy, as the *sole* trusted public edge, overwrites untrusted forwarding headers before Spring's `ForwardedHeaderFilter` processes requests. If a load balancer/CDN is added upstream, update Caddy trusted-proxy policy **before** deploying it, or the edge IP will be used for rate limits.

## Refunds
- Only full refunds matching **both** original successful payment amount and order final amount are supported. Partial refunds are deliberately rejected.
- Refund attempts are durably saved as PROCESSING before contacting VNPay, with one unique request per order.
- VNPay WebAPI refund replies require HMAC validation and consistency checks; unconfirmed/invalid responses stay REVIEW_REQUIRED. Do **not** retry automatically; reconcile against VNPay first.
- A crashed process can leave PROCESSING; an operator must reconcile against gateway before deciding any recovery. No automatic reset to PENDING or re-send.
- External HTTP call runs outside database transaction. Order/payment are marked REFUNDED in a separate transaction only on verified gateway success. Non-VNPay refund is an explicit operator acknowledgement of an **already performed** manual transfer; avoid using this to trigger actual bank transfer.
- Admin Order Status UI calls the dedicated refund endpoint. Status-machine PATCH endpoint explicitly rejects REFUNDED.

## Flyway warning — edited V23 and V38
- The pre-existing V23 inserted dev-only supplier rows referencing dev-only products. It prevented initialization of a production database without test data.
- The corrected V23 is conditional and safely skips demo product associations on an empty DB, keeping dev/test seeded databases operational.
- **V23 and V38 content changed**. Databases that previously applied the historical V23 will encounter a checksum mismatch. Back up the DB, compare installed migration checksums to source, validate changes, then perform an explicitly reviewed `flyway repair` **only on those databases**. Never set automatic repair in production.
- V38 now runs the legacy demo variant/description inserts only when all 12 known sample products exist with their expected (id, slug) pairs. A fresh production DB contains none, so demo inserts are skipped instead of violating FK constraints.
- Verify the full migration chain on both a truly empty DB using the prod profile and a copy of an existing environment.

## Release gate
Run `mvn -B -f backend/pom.xml clean verify`, `cd frontend && npm ci && npm run check`, initialize a clean DB using prod Flyway locations, and exercise login/order/cancel/refund with actual sandbox gateway. No claim of production-readiness until these gates pass.
