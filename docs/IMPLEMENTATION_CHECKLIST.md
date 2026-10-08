# Temporary website implementation checklist

Owner: Khushi. Updated 8 October 2026.
Remove this file after all launch items are completed and verified. Keep setup and operational documentation.

`[x]` means implementation exists; verification is listed separately. It does not mean production approval.

## Catalogue and admin — first active milestone
- [x] `/admin` owner login and backend authorization, hashed password, session expiry, CSRF protection and basic login throttling.
- [x] Persistent product catalogue and one-time import of the six existing designs.
- [x] Add/edit products, draft/publish/archive, collection assignment, New In, prices and calculated savings.
- [x] S–XL selection, colours, composition/GSM and descriptions.
- [x] Upload photos to persistent local storage, reorder photos, first-photo listing image, full-artwork display option.
- [x] Storefront lists and detail routes read published products from the same catalogue.
- [ ] Run backend integration suite with Java 21, reachable dependency repositories and MySQL.
- [ ] Verify real login → upload → draft → publish → storefront → edit → archive → restart persistence.
- [ ] Desktop/mobile browser QA, gallery, colours and exact back-navigation regression checks.
- [ ] Add a real new design through admin together with Khushi; do not create dummy published merchandise.
- [ ] MFA, production edge rate limiting, audit log and admin credential rotation workflow.
- [ ] Video upload/transcoding and image optimization; current Sabr video remains supported.

## Inventory and fulfilment
- [x] One shared oversized blank type confirmed; all designs share colour/size stock. Opening: White/Black S/M/L 12 each; XL 0.
- [x] Admin stock adjustments with reasons, movement ledger and low-stock indicators (3 or fewer).
- [x] Product size buttons reflect shared stock; cart addition rechecks current availability.
- [ ] Verify adjustment/history/restart and unavailable sizes against real MySQL.
- [ ] Atomic checkout reservation, expiry and payment-time stock consumption.
- [ ] Printing queue/capacity, dispatch lead time and fulfilment statuses.
- [ ] Pause orders independently of browsing; maintenance mode where needed.

## Cart, checkout, payments and orders
- [ ] Cart page, quantity changes/removal, guest checkout and address validation.
- [ ] Authoritative backend price/stock/shipping calculation; no trust in local cart prices.
- [ ] Razorpay approval, test integration, server payment verification and signed webhooks.
- [ ] Idempotent orders/payment processing, retries, reconciliation and late-payment handling.
- [ ] Admin orders, cancellation/refunds and customer support workflow.
- [ ] Receipts/invoices, payment/order/refund emails and delivery monitoring.

## Automatic shipping and tracking — confirmed choice
- [ ] Select courier/aggregator, account/API credentials, pickup address and commercial terms.
- [ ] Serviceable PIN codes, shipping rates, package weight/dimensions and shipping rules.
- [ ] Automatic shipment creation after ready-to-ship; prevent duplicate bookings.
- [ ] AWB/label, pickup booking, cancellation/rebooking and visible failure/retry states.
- [ ] Verified courier callbacks, event deduplication and periodic tracking reconciliation.
- [ ] Secure customer tracking page and dispatch/delivery notifications.
- [ ] Delivery failures, return-to-origin and operational exception handling.

## Launch and operations
- [ ] Confirm genuine garment measurements, material/GSM, stock and dispatch promises.
- [ ] Approved seller/contact, privacy, shipping, cancellation and return/refund pages; exchange policy only when approved.
- [ ] Tax/invoice requirements and compare-at pricing confirmed.
- [ ] Deploy Next.js/admin + Spring Boot + MySQL on one server; domain and HTTPS.
- [ ] Secrets, private database, secure cookies, database migrations and monitoring.
- [ ] Database/media off-server backups and tested restore.
- [ ] End-to-end payment/refund/shipping checks, concurrency checks and mobile QA.
- [ ] Reconfirm actual hosting/courier/email costs before paid activation.
- [ ] Remove this temporary checklist once every launch item is verified.
