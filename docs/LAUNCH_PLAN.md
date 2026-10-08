# styled_somehow — custom website launch plan

Updated: 8 October 2026. Pricing checked on this date.
Status: implementation plan; no paid services purchased and no production deployment performed.
This document defines the proposed small-store launch scope. It narrows the broader feature wishlist in PROJECT_REQUIREMENTS.md and ARCHITECTURE.md for the first release; it does not claim those features already exist.

## 1. Confirmed choices and outstanding decisions

Confirmed by Khushi on 8 October:
- Continue planning the custom website using the existing stack.
- Print T-shirts after orders rather than assume every design has finished stock.
- Launch with prepaid payments first.
- Use automatic courier booking and tracking updates (provider/account still to be chosen).

Implementation defaults:
- One Next.js app for storefront and /admin; one Spring Boot app; one MySQL database.
- One rented server for all three. DigitalOcean is the verified costing baseline, not a purchased or irrevocable provider choice.
- Store product media locally on a persistent server folder; no paid image service initially.
- Guest checkout first; customer accounts are deferred.
- Razorpay is the proposed payment gateway, subject to account approval and confirmed commercial terms.
- One owner admin initially, with backend-enforced authentication, password hashing, secure session cookies, CSRF protection for cookie-authenticated mutations, login rate limits and TOTP MFA.
- Automatic shipping integration: booking, labels/AWB and tracking sync, with admin retry and exception handling.
- Local development first; pay for a server when a deployment rehearsal is useful.

Before checkout can go live, confirm:
- Domain/registrar account and exact purchased name; examples are not proof of ownership.
- Blank stock quantities, garment types and suppliers, and which designs consume which blanks.
- Printing capacity, dispatch lead time and maximum orders accepted per day.
- Courier, serviceable PIN codes/areas, shipping charge, free-shipping rule if any and expected delivery wording.
- Return/refund/cancellation policy, particularly cancellation after printing begins; no exchange-policy copy should be published until approved.
- Seller identity/contact/address, applicable tax treatment and invoice requirements.
- Real garment measurements: current Bonkers reference size chart is temporary.
- Whether garment GSM/composition and compare-at prices accurately describe the products sold.
- Payment gateway approval, bank account and settlement/refund terms.
- Who receives operational alerts and handles fulfilment/support.

## 2. Repository state verified

Already present:
- Next.js 16.3.4, React 19.2.8, TypeScript, Tailwind tooling and custom CSS.
- Homepage hero, Women/Men cards, New In, collection pages and six detail pages.
- Mobile galleries and desktop image/detail layout; visual navigation/cropping QA remains pending.
- S–XL selectors and Sabr Black/White selection.
- Local images/video under frontend/public.
- Java 21, Spring Boot 4.1.1, Gradle, JPA/MySQL dependencies and /api/health.

Not yet implemented:
- Database product/variant model or product/inventory/order APIs.
- Admin login, product editor, stock controls or order management.
- Complete cart UI, real checkout, gateway integration, refunds or order tracking.
- Automated emails, backups, monitoring, deployment and production domain configuration.

Current products/prices to preserve during migration:
| Product | Compare-at | Selling price | Current collection |
|---|---:|---:|---|
| Cherry Zest | ₹999 | ₹599 | Women, New In |
| Untamed Torque | ₹1,200 | ₹999 | Men, New In |
| She’s Winning | ₹900 | ₹599 | Women, New In |
| Sabr | ₹600 | ₹499 | Men; Black/White |
| Chilli Crush | ₹900 | ₹699 | Women, New In |
| Bitchआरी | ₹900 | ₹599 | Women/New In, last; last supplied portrait first |

Product data/prices are duplicated in code today. Add-to-cart stores browser-local entries, not confirmed orders. Existing stock quantities must never be fabricated.
A database password is committed in application.properties. Remove credentials from source, switch to environment configuration and rotate the existing password before any deployment. Removing a line does not remove Git history or make the old password safe. Do not copy the value into docs or logs.

## 3. Launch architecture

Customer/admin browser -> HTTPS reverse proxy -> Next.js storefront/admin
Reverse proxy /api -> Spring Boot -> MySQL
Spring Boot -> payment gateway and transactional email API
Reverse proxy /media -> persistent product upload folder
Backup job -> encrypted private off-server storage

- Public paths: storefront and /media; protected UI /admin and protected APIs /api/admin.
- Use the same origin for API traffic to simplify cookies/CORS. No separate admin hosting subscription.
- Frontend never connects directly to MySQL.
- MySQL is private to the server/container network. Public ingress only for HTTPS and tightly controlled administration.
- Docker Compose or equivalently simple services with automatic restart; no Kubernetes, Redis or microservices for launch.
- Build releases outside the small production server. Deploy versioned artifacts with health checks and retain a known working release.
- Persist database data and uploads outside replaceable application containers.
- Apply migrations using one versioned migration tool, proposed Flyway; use validation, not automatic schema updates, in production.
- Never cache admin/customer/order responses publicly; invalidate product caches when products change.

Advantages: fewer subscriptions, full control, easy-to-understand deployment.
Trade-offs: a single machine is a single point of failure; we own OS/security maintenance, database recovery and capacity planning. This is not managed hosting or high availability.

## 4. Verified cost baseline and assumptions

Sources and published rates:
1. DigitalOcean Basic regular shared-CPU: 2 GiB/1 vCPU/50 GiB disk/2,000 GiB transfer $12/month; 4 GiB/2 vCPU/80 GiB/4,000 GiB transfer $24/month.
   https://www.digitalocean.com/pricing/droplets
2. DigitalOcean basic daily server backup plan adds 30% of server price; weekly adds 20%.
   https://docs.digitalocean.com/products/backups/details/pricing/
3. Backblaze B2 first 10 GB storage free; published storage starts at $6.95/TB-month, with egress allowances/overages.
   https://www.backblaze.com/cloud-storage/pricing
4. Resend transactional Free: 3,000 emails/month and 100/day. Pro currently $20/month for 50,000/month. Authenticate a sending domain.
   https://resend.com/pricing
5. Let's Encrypt TLS certificates are free. Renewal must be automated and monitored.
   https://letsencrypt.org/
6. Razorpay standard published rate: 2% plus applicable GST, no gateway setup/annual maintenance fee. Promotions, account-specific rates, international methods and value-added services must be checked separately.
   https://razorpay.com/pricing/
7. Next.js supports self-hosting on Node.js or Docker; Vercel is optional.
   https://nextjs.org/docs/app/guides/self-hosting

Costing assumptions: $1 = ₹90 for planning, NOT a verified current FX rate; one production server; small media/catalogue; no sustained staging server; no paid apps, SMS, WhatsApp, image hosting, managed database, load balancer or paid monitoring. Storage/transfer allowances are finite. Region and plan availability must be checked in the provider console before purchase.

| Monthly item | 2 GiB trial option | 4 GiB headroom option |
|---|---:|---:|
| Server (published) | $12.00 | $24.00 |
| Daily server backups (published formula) | $3.60 | $7.20 |
| Private off-server DB/media backups (allowance, not quote) | $0–$1 | $0–$1 |
| Transactional email within Free limits | $0 | $0 |
| Separate image service/admin hosting/MySQL subscription | $0 | $0 |
| Total before tax/FX/overages | $15.60–$16.60 | $31.20–$32.20 |
| At assumed ₹90/$ | ₹1,404–₹1,494 | ₹2,808–₹2,898 |

Budget reserve, not guaranteed invoice: ₹1,800–₹2,000/month for the 2 GiB setup; ₹3,300–₹3,600 for 4 GiB, allowing some tax/FX variability. Verify invoice tax treatment rather than treating these allowances as all-inclusive fixed prices.

Excluded from hosting totals:
- Domain renewal (actual registrar invoice, not an assumed price).
- Gateway fees, courier charges, packaging, printing, blank garments, returns and marketing.
- Business tax/invoice obligations and paid professional services.
- Branded inbox subscription if desired. Transactional email sends order messages; it is not a Gmail-like mailbox.
- Developer/operations time. Lower cash cost does not mean lower maintenance workload.
- Temporary second server during rehearsal/recovery/migration, extra storage, bandwidth or paid email tier.

Payment examples using only the published 2% baseline, BEFORE GST on fees:
| Paid order value | Gateway fee |
|---|---:|
| ₹499 | ₹9.98 |
| ₹599 | ₹11.98 |
| ₹699 | ₹13.98 |
| ₹999 | ₹19.98 |

At 50 prepaid orders averaging ₹599, processed value = ₹29,950; base gateway fee = ₹599 plus applicable GST. Hosting plus that base fee is ₹2,003–₹2,093 under the 2 GiB model, before taxes/FX and fulfilment costs. Gateway fees apply to the processed total, including charged shipping where applicable. Refund fee treatment and settlement timing require account-specific verification. No Shopify surcharge in this custom setup.

Do not assume gateway UPI is free merely because standard UPI has zero MDR; Razorpay's published page describes a separate platform fee.
Do not plan a paid storefront on Vercel Hobby, and do not rely on temporary free trials/promotions for recurring budgets.
Turning ordering off does not stop server/backup charges while those services remain provisioned.

## 5. Product and admin management

Admin capabilities:
- Add/edit products: name, stable slug, description, selling/compare-at price, fabric details, media, colour/size variants.
- Draft -> Active -> Archived; remove from sale without deleting past order records.
- Assign Men/Women/New In and reorder listings. Preserve Bitchआरी at the end and its selected first image.
- Upload/reorder photos and video, set first image, view desktop/mobile preview.
- Size chart editor; show only supported sizes. Validate compare-at price >= sale price.
- Edit banners and announcement text; only expose controls that actually work.
- Maintain blank stock, damaged stock and adjustment reasons.
- See orders, printing queue, payment/refund state and tracking.
- Pause all new orders with a public message; separate maintenance storefront switch.
- Audit important product/price/stock/refund changes.

Uploads: whitelist formats, size limits, server-generated filenames, validate image content, generate web-friendly copies. Store DB paths/metadata, not image blobs. New admin uploads live in a persistent folder, not the app's bundled public folder, so product additions do not need deployment. Back up originals/derived files appropriately.

Data groups:
Product, ProductVariant, Media, Collection/CollectionProduct, SizeChart, StoreSettings;
BlankType, BlankStock, VariantBlankMapping, StockReservation, StockMovement;
AdminUser/Session, Order, OrderItem, PaymentAttempt, WebhookEvent, Refund, Shipment, EmailOutbox, AuditEvent.

Money in integer paise; timestamps in UTC; store order item name/price/tax/address snapshots so later product edits do not rewrite old orders.

## 6. Print-after-order inventory and fulfilment

Stock must be shared across designs using the same blank type, colour and size.
Example: Sabr Black-M and Chilli Crush Black-M may both consume one identical Black-M blank. Five blanks means five combined orders, not five per design. Different fabrics/blank types need separate pools.

Proposed workflow:
- Cart does not reserve stock.
- Backend checkout recalculates price/shipping/tax and reserves required blanks atomically for a proposed 15-minute window.
- Gateway payment order is created with server-calculated total.
- Verified successful/captured payment converts reservation into allocated blanks and a confirmed order.
- Printing starts -> consume the allocated blank; log wastage/damage/reprint adjustments.
- Packed -> create courier shipment automatically -> receive AWB/label -> dispatch/pickup -> courier events update tracking -> delivered.
- A cancellation before printing returns allocation once; after printing, a printed item is not added back as a blank.
- Returned printed items need inspection and separate handling; launch admin can mark them unavailable pending review rather than pretend they are unused blanks.
- Paid orders persist even when new sales are paused.
- Blank shortages OR production queue capacity can block new orders.
- Display the agreed print/dispatch lead time before checkout; no invented dispatch promise.

Inventory rules to test: two simultaneous checkouts for the last blank, designs sharing a blank, duplicate cancellation/restock, multi-item all-or-nothing reservation, expiry and late successful payments.
If payment arrives after a released reservation: reallocate only if stock remains; otherwise flag paid-but-unfulfillable and initiate a controlled refund/manual resolution. Never silently discard a paid order.

## 7. Payments, cart and order safety

Guest cart may remain browser-local for convenience, but backend owns prices, discounts, availability and totals. Cart lines carry variant IDs and quantities.
- Create pending order and gateway order server-side; persist an idempotency key.
- Verify payment signature and gateway state; don't trust browser success screen.
- Validate webhooks, deduplicate events and tolerate out-of-order delivery.
- Reconcile stuck/pending payment attempts via gateway API.
- Keep order, payment, printing, shipping and refund statuses separate.
- Failed email does not undo a paid order; use persisted email jobs/retries and admin failure visibility.
- Full/partial refund admin actions require explicit amounts, reasons, audit and gateway confirmation.
- Never store card details/UPI credentials; use gateway-hosted checkout.
- Protect guest order tracking with a non-guessable token and disclose only necessary information.
- Apply request limits/validation to login, checkout and uploads.

Initial notifications: order receipt/payment pending where needed, payment confirmation, dispatch/tracking, cancellation/refund update.
Automatic shipping must prevent duplicate bookings, verify courier callbacks, handle retries and reconcile missed tracking events. A failed booking stays visible for staff resolution; the chosen provider and any fees are not yet confirmed.

## 8. Pausing and recovery

Store modes:
- Open: accept orders subject to stock/production capacity.
- Orders paused: catalogue and admin visible; backend refuses creation of new checkouts.
- Maintenance: customer-facing message; admin access restricted and existing obligations remain.
A new-order pause must not block webhooks/reconciliation for already-started payments; resolve these safely. Never just hide Buy buttons.

Backup proposal:
- Daily provider server snapshots for recovery of system configuration.
- Hourly consistent MySQL logical backups to encrypted private off-server storage; daily media incremental backups plus upload-triggered backup queue.
- Proposed retention: 48 hourly DB copies, 30 daily copies, latest valid media set/version retention. Measure size and prune intentionally.
- Backup credentials separate/limited; encryption keys kept separately from the server.
- Monitor backup failure and periodically restore into an isolated environment.
- Initial recovery target: at most one hour of DB data loss and restore within four hours after an operator starts recovery, to be demonstrated by a rehearsal rather than promised as an SLA.
- Reconcile gateway records after recovery; stock/order allocations require checking.
- Daily server snapshots alone are not a substitute for database-consistent backups.

Operations: uptime/health checks, disk/memory/CPU alerts, log rotation, certificate renewal checks, gateway failure alerts and email queue alerts. Domain/provider payment renewal must not lapse.

## 9. Implementation sequence and completion gates

| Phase | Deliverable | Completion evidence |
|---|---|---|
| 0: Foundation repair | Environment-based secrets; credential rotation checklist; schema migrations; runnable local setup | No committed active credentials; isolated test DB and migrations work |
| 1: Catalogue foundation | Shared product model/API and one reusable detail page; preserve all media/prices/navigation | All six products render consistently; no duplicated price sources |
| 2: Admin and blanks | Owner auth/MFA; product/media editor; collections; blank pools/mappings; stock ledger; pause controls | Product publish/edit/archive works without code; protected APIs reject unauthorised users; shared blank stock correct |
| 3: Shopping | Cart page, guest address/checkout, shipping configuration and stock reservations | Server totals authoritative; stale cart and simultaneous last-blank tests pass |
| 4: Payments/orders | Razorpay test mode, webhook/reconciliation/refunds, print queue, automatic shipment booking/tracking, transactional emails | Success/failure/pending/duplicate/late payments and refunds verified; no duplicate order/stock movement |
| 5: Deployment rehearsal | Single-server deployment, persistent media/DB, HTTPS, backup/restore and load tests | 2 GiB capacity either demonstrated or upgrade documented; rollback/restore rehearsed |
| 6: Launch verification | Approved policies, real stock, live gateway/account/domain and mobile QA | Controlled real payment/refund, correct email/tracking, admin workflow and launch checklist pass |

Work in small reviewable commits. No reliable calendar delivery date is established: dependencies include business decisions, gateway activation, testing and deployment access.

The next implementation task after this plan is Phase 0, then catalogue consolidation. More disconnected product-page copies would increase maintenance cost.

## 10. Testing, capacity and launch gates

- Mobile visual QA at 360/390/430 widths and desktop, including back/continue-shopping exact listing origin, image order, full print close-ups and Devanagari title clipping.
- Customer/admin data validation and permission tests.
- Transactional inventory/payment integration tests with MySQL, including shared blanks.
- Controlled payment test mode followed by live payment/refund verification.
- Upload retention across deployment and database restore.
- Demonstrate pauses stop new checkout server-side while existing orders remain manageable.
- Capacity rehearsal: representative browse/checkout workload with email/background jobs. Provisional starting test: 20 concurrent browsing users and 5 concurrent checkout attempts; not an advertised traffic guarantee.
- Investigate sustained >75% RAM, CPU saturation, swap pressure, slow checkout or crashes. Tune safely or upgrade to 4 GiB before launch; do not assume 2 GiB sufficient from catalogue size alone.
- Disable debug SQL logging in production; scrub secrets/addresses/payment payloads from logs.
- Billing alerts for paid accounts and storage/traffic; notifications are not hard spending caps.

Launch blocked until: accurate products/size chart, real blank stock, capacity/lead time, shipping charges/serviceability, approved policies/tax settings, gateway approval, secure admin, functioning emails, backups/restore, visual QA and tested payment/order handling.

## 11. Deferred features and remaining business costs

Defer customer registration, wishlist, loyalty, reviews, complex filters, SMS/WhatsApp, COD, automatic courier booking, POD/vendor integration, recommendations, multi-warehouse and sophisticated analytics.
Future features must be planned without changing stock/payment safety.

This plan prices website infrastructure and standard gateway use, not T-shirt unit economics. Before launch, calculate per-item blank + print + packaging + shipping + gateway + return allowance and margin for each current price.
The site does not resolve seller tax/registration obligations. Confirm those for the actual business before activating sales.

## Current implementation work

See IMPLEMENTATION_CHECKLIST.md for the temporary completion list and ADMIN_SETUP.md for the first product-management slice. Provider-specific shipping fees and API requirements remain unverified until a courier service is selected.
