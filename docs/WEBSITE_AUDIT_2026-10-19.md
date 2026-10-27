> Remediation status: [AUDIT_FIX_STATUS.md](AUDIT_FIX_STATUS.md). This audit is a dated baseline, not the current unresolved-bug list. F05 has been corrected because scheduled tracking already existed.

# Styled Somehow website audit — 19 October 2026

## Verdict and scope

Not ready for public paid orders yet. The storefront and development commerce flows exist, but live payments, shipping operations, abuse protection and production readiness need work.

Reviewed local source corresponding to feature/size-guide through commit 67195ee, including the email fixes from this session. This is an engineering audit, not a penetration-test certification, legal review or visual approval. No production credentials, customer database or private environment files were inspected. No paid orders, emails or shipments were created. Application behavior was not changed by this audit.

Evidence labels: **Confirmed** = source or local HTTP evidence; **Risk** = a reachable design weakness, not an observed production incident; **Gap** = missing feature or operational verification. High = resolve before unrestricted paid launch. Medium = resolve before launch or establish an explicit limited-launch workaround. Low = polish or longer-term reliability.

## Checks performed

| Check | Result |
| --- | --- |
| Frontend ESLint | Passed |
| Next.js production build and TypeScript | Passed |
| Local production HTTP: home, cart, checkout, orders and both policy pages | All returned 200; client/API flows were not exercised |
| Six collection links in homepage navigation | All returned 404; see F11 |
| Literal image/video paths in frontend TSX | No missing referenced files found; DB-uploaded assets not covered |
| Public media file sizes and hero dimensions | Inspected; see F24 |
| Full backend Gradle test attempt | Blocked before compilation: Gradle distribution download failed with Network unreachable |
| MySQL concurrency/migrations, SMTP, Google callback, Razorpay, Shiprocket | Not executed against live/local user services |
| Desktop/mobile rendered visual QA, keyboard and screen-reader testing | Not completed; source review cannot certify appearance or accessibility |
| Dependency vulnerability scan / external penetration testing | Not performed; no clean-security claim |

## High-priority findings

### F01 — Live payment support is not implemented [Confirmed / High]

`RazorpayGateway.ready()` accepts only `rzp_test_` keys. `PaymentController.config()` and `PaymentService.start()` hardcode TEST. Stock movement reasons and frontend payment copy are TEST-specific. Changing environment keys alone will disable payments; changing only a label would incorrectly classify payments.

Evidence: `backend/.../payments/RazorpayGateway.java`, `PaymentService.java`, `PaymentController.java`; `frontend/app/checkout/payment-button.tsx`, `view.tsx`.

Action: introduce validated TEST/LIVE configuration across gateway, stored orders, UI and stock ledger; isolate staging/test inventory from production. Then verify a controlled live payment, duplicate callback/webhook, late payment and refund. Keep real shipment booking blocked for TEST orders.

### F02 — Stock reservations can be exhausted using fresh sessions [Risk / High]

Checkout allows one pending order per session, but there is no cross-session checkout-creation limit. A caller can obtain new sessions and reserve available blanks without paying. CSRF and a session cookie do not prevent automated same-origin clients from doing this. The small shared stock pool makes the impact material.

Evidence: `CheckoutService.create()`, `CheckoutController`, `AdminSecurity`; only admin-login and OTP paths have dedicated throttles.

Action: apply edge and application limits to checkout creation and expensive shipping/PIN endpoints, bound outstanding reservations across sessions, and monitor repeated holds/cancellations. Preserve guest checkout; requiring login is not the only solution. Test that legitimate retries remain idempotent.

### F03 — Slow external requests can block unrelated stock operations [Risk / High]

`PaymentService.lock()` takes pessimistic write locks on every blank-stock row. `start`, `verify`, `refresh`, webhook and reconciliation then make Razorpay network calls within that transaction. Cart quotes and checkout access also acquire all stock locks. An external timeout can therefore stall unrelated quotes, checkout, payment processing and adjustments.

Evidence: `PaymentService`, `CheckoutService.prepare()`, `StockRepository.lockedAll()`.

Action: fetch provider state outside inventory transactions, then revalidate and apply in a short locked transaction with durable idempotency. Narrow locks to required stock rows with a consistent lock order. Test simultaneous last-blank purchases, expiry and callbacks on real MySQL.

### F04 — Checkout shipping acceptance does not use the same parcel as booking [Confirmed / High]

Customer serviceability uses 0.5 kg without dimensions. Admin comparison and booking use editable weight plus length, breadth and height. The ₹110 checkout cutoff can accept an order whose actual packed parcel is more expensive or has different courier availability. This mismatch does not prove the cause of every past Shiprocket discrepancy, but it is present in the request construction.

Evidence: `ShiprocketGateway.rate()/fetch()`, `ShipmentService.book()`, `frontend/app/admin/orders/shipment.tsx`.

Action: use one approved parcel rule for checkout and booking, or explicitly accept merchant-paid differences. Verify using the same account, PINs, declared value, payment mode, weight and dimensions. Do not silently charge the customer again after payment.

### F05 — Correction: scheduled courier polling already exists [Corrected finding]

The original audit incorrectly said tracking was manual-only. `TrackingPoller` already runs every minute and selects up to ten nonterminal shipments whose tracking is at least ten minutes old. This finding is withdrawn. Provider/API verification and tracking failure monitoring remain relevant; there is no reason to add a duplicate poller.

### F06 — Refreshing a new checkout can restore an old paid order [Confirmed by source / High]

The checkout loader uses the last confirmed order whenever navigation is a reload, even when a new cart is nonempty. It explicitly accepts that old confirmation on reload. Reproduction: complete order A, add another product, open checkout, refresh before creating order B. A can replace the new checkout.

Evidence: `frontend/app/checkout/view.tsx`, initial load effect using `reload`, `ORDER_KEY`, `CONFIRMED_KEY`.

Action: bind confirmation recovery to an explicit order reference/confirmation route. A new cart must resume its own draft or start a new checkout; do not infer order identity solely from reload navigation.

### F07 — Deployment, persistence and recovery are not demonstrated [Gap / High]

No deployment implementation or tested restore procedure was found. Uploaded media is stored on the backend filesystem. Secure cookies default false and STORE_URL defaults to localhost. A container or host redeploy without persistent media would break uploaded products. The health endpoint returns a static UP response without testing database availability.

Evidence: `application.properties`, `MediaController`, `HealthController`, `next.config.ts`; launch documentation describes planned operations.

Action: configure production HTTPS, COOKIE_SECURE=true, correct STORE_URL/BACKEND_URL/Google callbacks, private database access, persistent media, off-server DB/media backups and a tested restore. Validate forwarded IP handling at a trusted proxy and edge rate limiting; do not trust arbitrary forwarded headers. Add separate liveness/readiness checks and alerts. Production settings were not inspected, so this is a readiness gap, not a claim they are currently insecure.

### F08 — Normal paid cancellation/return/refund operations are missing [Gap / High]

The implemented refund worker addresses late, duplicate or unfulfillable payments. There is no normal admin/customer paid-order cancellation, return/exchange approval or controlled full/partial refund workflow. `CheckoutService.cancel()` only releases unpaid pending reservations. Returned printed garments must not automatically become unused blank stock.

Evidence: `PaymentService.reconcileRefund()`, `CheckoutService.cancel()`, available admin controllers; published return/exchange policy.

Action: define and implement operator actions with reasons, eligibility, payment confirmation and audit history. A limited launch could use an explicit documented manual gateway/courier process, but the portal must reconcile the resulting payment/fulfilment state and stock correctly.

### F09 — Ambiguous shipment results cannot be recovered in the portal [Gap / High]

Shipment booking correctly blocks duplicate creation after an uncertain response, but REVIEW_REQUIRED/CREATING/ORDER_CREATED bookings have no reconciliation, cancellation/rebooking or safe attach-existing-shipment action. The frontend Refresh shipment status just reads the local row. An operator may fix Shiprocket manually while the website remains stuck.

Evidence: `ShipmentService.book()/status()`, `ShipmentController`, admin `shipment.tsx`.

Action: implement read-only provider reconciliation first, then audited recovery/cancel/rebook actions. Keep duplicate protection. Labels and pickup currently remain manual in Shiprocket; that is an explicit workflow choice, not automatic pickup support.

## Medium-priority findings

| ID | Finding and evidence | Impact and action |
| --- | --- | --- |
| F10 | **Confirmed constraint:** `ShiprocketGateway.rate()/fetch()` and `ShipmentService.payload()` reject more than two T-shirts, while cart controls allow 20 per variant. | Customers can build an unfulfillable checkout. Either support verified larger parcels or enforce and explain a consistent two-item launch limit before checkout. |
| F11 | **Confirmed broken navigation:** `/collections/new-drop`, `/collections/graphic-tees`, `/collections/plain-tees`, `/collections/joggers`, `/collections/best-sellers`, `/collections/couple-tshirts` all returned 404 from the local production server. Links are in `storefront-hero.tsx`. Mobile Search has no handler. | Remove unfinished links or connect them to real collections/filtering; implement search or remove its control. |
| F12 | **Confirmed inventory/admin mismatch:** product admin allows arbitrary colour names, but inventory only adjusts existing stock IDs. Initial stock migration creates White/Black rows; there is no admin create-stock-variant endpoint. | New colours such as Beige cannot be stocked entirely through the portal. Add stock-variant management with canonical colour/size values. Do not assume production quantities from seed migrations. |
| F13 | **Gap:** no server-side pause-orders control was found. | Hiding a Pay button is insufficient. Add a persisted checkout-creation/payment-start gate while allowing payment verification, webhooks, refunds and existing-order access to continue. |
| F14 | **Risk:** email login holds the single `AccessBudget` lock during synchronous SMTP send. | One slow OTP send serializes other OTP requests/verification behind it. Reserve quota/challenge in a short transaction, deliver outside the global lock, and preserve single-use semantics and failed-send allowance accounting. |
| F15 | **Risk:** expiry, payment reconciliation and mail use scheduled jobs, with no dedicated scheduler/pool configuration found. | Slow batch/provider calls can delay other background work under default scheduling. Configure separate/bounded executors and per-order concurrency protection; add duration/backlog monitoring. Do not only increase concurrency without race tests. |
| F16 | **Confirmed operational limit:** admin orders/payments and stock history expose only the latest 100 rows; frontend search filters that loaded slice. | Older orders and adjustments become inaccessible through those lists. Add server-side pagination, reference lookup and date/status filtering. Customer history already has pagination. |
| F17 | **Gap:** mail failures/retries are in logs and DB fields, but no admin failed-mail/backlog view or alerting was found. Payment reconciliation catches failures without an operator alert. | Failures can remain unnoticed. Add safe queue health, last error category, next retry, attempt count and audited retry/reconcile controls. Never show credentials, OTPs or private links in logs. |
| F18 | **Confirmed media limitation:** upload validation checks size and a short file signature, without decoding, dimensions limits, thumbnail generation or cleanup. `/api/media` images are displayed unoptimized. | Admin-only malformed/huge-pixel images can be published; original uploads can be slow and orphan uploads accumulate. Decode/validate image content, limit pixels, generate display variants and clean unreferenced assets safely after a retention window. |
| F19 | **Gap:** no sitemap, robots route, product structured data or canonical/Open Graph setup found. `/robots.txt` and `/sitemap.xml` returned 404. Product metadata mainly sets title. | Search/share presentation is incomplete. Add truthful product/offer data, canonical URLs, share images and sitemap. Cart, checkout, orders and admin already declare noindex. |
| F20 | **Confirmed source limitation:** homepage carousel advances every 2.5 seconds with no pause control or reduced-motion timer check; mobile menu has no Escape/focus-management code. | Auto-advance can interrupt reading and menu keyboard use needs improvement. Add pause, focus/hover suspension, reduced-motion behavior and menu dismissal/focus restoration. Full accessibility validation is still required. |
| F21 | **Confirmed lifecycle gap:** historical order items always link to `/products/{slug}`, while the public product endpoint hides archived products. | Old orders retain their snapshots, but View product can lead to 404 after archival. Provide a discontinued-product view or disable the link with clear wording. |
| F22 | **Confirmed CI coverage gap:** workflow push trigger only includes `feature/shop-for-images`, not active `feature/size-guide` or main. PR trigger exists. Tests use H2 rather than a real MySQL service. | Direct pushes to the active branch do not trigger this workflow. Include active/main branches and add a MySQL migration/concurrency job before launch. |
| F23 | **Gap:** privacy and terms pages/contact-seller details are not implemented as complete site pages. `/policies/privacy-policy` and `/policies/terms-of-service` returned 404; footer exposes shipping/returns and support email only. | Complete approved seller/privacy/terms/cancellation information and the applicable invoice process before launch. Existing policy wording needs owner/professional approval; this audit does not determine legal compliance. |
| F24 | **Measured performance issue:** raw homepage mobile lineup WebP is 1,364,050 bytes, desktop lineup 1,314,312 bytes. Collection banners range up to about 1.78 MB. Hero uses raw picture/img sources. | Generate responsive display-sized assets while retaining originals; compare visual quality. Measure actual mobile LCP/CLS and transfer sizes before setting a performance claim. |
| F25 | **Confirmed error-handling weakness:** orders `refreshStatus()` clears an authorized order for every failure and says access may have expired, including transient 5xx/network errors. | A provider/server hiccup looks like logout. Only clear access for authentication/authorization failures; retain last known order on temporary failures and offer retry. |
| F26 | **Risk in latest ordering fix:** email ordering uses `occurredAt` and random UUID as a tie breaker. | Distinct stages persisted at the same DB timestamp can be ordered by UUID rather than real transition order. Add a monotonic per-order sequence or guaranteed increasing event timestamp and a same-timestamp regression test. The recent retry fix handles distinct timestamps, not this tie case. |

## Lower-priority findings and constraints

- **F27 — UI consistency (source evidence):** the rack explicitly uses Arial, while root typography uses Geist; the cart still has beige rounded summary styles while checkout/orders use their own modules. Consolidate tokens and component rules after functional blockers. Actual spacing, crop, contrast and touch targets need rendered desktop/mobile review, not source-only opinions.
- **F28 — Order exception presentation (source evidence):** the six-step timeline uses index lookup; DELIVERY_ISSUE/RETURNED_TO_ORIGIN have no matching index, so no normal progress step is marked reached. Add a deliberate exception presentation and correct payment/refund totals rather than always saying Total paid on refunded orders.
- **F29 — SMTP exact-once delivery (known design limit):** provider acceptance and database commit are separate operations. A commit failure can lead to a duplicate on retry despite stable Message-IDs. Document and monitor this; do not promise exactly-once inbox delivery. Thread grouping is ultimately controlled by email clients.
- **F30 — Temporary documentation drift:** the implementation checklist and launch plan contain older statements superseded by later milestones (including old recovery/email and implementation status). Consolidate current status so completed work is not mistaken for pending work. Historical secret-exposure notes require an owner rotation check; credentials/history were not inspected in this audit.

## Protections already present in source

- Backend enforces product prices, offered variants and aggregate shared-blank availability; client prices are not authoritative.
- Reservation/payment application uses locking and idempotent records; late/unfulfillable payments enter refund/review handling.
- Razorpay checkout/webhook signatures are verified and provider payment state is fetched before applying captured payments.
- TEST orders cannot create real shipments. Ready-to-ship and handover gates exist. Ambiguous booking retries are blocked to avoid duplicate charges.
- Admin APIs require ADMIN role; BCrypt passwords, CSRF and login throttling exist. This is not a claim of complete production security or verified MFA.
- Email login has browser-bound, expiring, hashed, single-use codes with attempt/resend/email/IP/browser/global request limits. Gmail-only acceptance is an intentional current business rule, not an audit bug.
- Order history is scoped to the verified email; private signed links are read-only and order-specific. Secret rotation invalidates links.
- Customer tracking DTOs omit internal notes and courier costs; tracking URLs are HTTPS/domain checked.
- Frontend build/lint pass and referenced literal image assets exist.

## Recommended implementation order

1. Fix F06 repeat checkout and F11 broken navigation first; small, directly visible regressions.
2. Add F02 abuse controls and shorten F03 stock transactions before opening checkout publicly.
3. Resolve F04 parcel/quote consistency and F10 quantity support; verify existing tracking and finish F09 booking recovery.
4. Build validated LIVE mode (F01) in isolated staging, then perform controlled payment/refund/shipping verification.
5. Finish operator controls: paid cancellation/refunds, pause ordering, older-order lookup and failed-job visibility.
6. Complete production persistence/backups/HTTPS, policies and CI/MySQL checks.
7. Optimize media/SEO and perform full rendered mobile/desktop/accessibility QA, then launch with monitoring.

## Required acceptance run before public launch

Run Java 21 backend suites and Flyway migrations against a clean and upgraded MySQL database. Verify last-stock concurrency, duplicate payment events, expired/cancelled checkouts, refund retry and provider outages. Test failed email followed by later statuses, equal-timestamp status events and old/new private links. Rehearse real courier creation, AWB, labels/pickup, tracking and ambiguous-response recovery. Verify new product/colour addition, archival, upload persistence across redeploy and DB/media restore. Test checkout with old confirmation history and a new cart, login/logout and session expiry in multiple browsers. Visually inspect at 360/390/430px and 1280/1440px, keyboard navigation and reduced motion. Record evidence rather than treating the existence of tests as a passing test run.
