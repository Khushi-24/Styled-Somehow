# Audit remediation status — 19 October 2026

This is the current implementation record. It supersedes old checklist implementation claims. Backend source has NOT been compiled/tested in this workspace: the Gradle distribution download fails with Network unreachable. New integration tests and MySQL CI are supplied, not claimed passing. Keep real payments/booking disabled until verification.

## Implemented changes

| Audit item | Result |
| --- | --- |
| F01 LIVE payments | Matching TEST/LIVE order/UI/ledger mode, explicit RAZORPAY_LIVE_ENABLED switch default false, mode-mismatch protection. No paid provider action performed. |
| F02 reservation abuse | Durable hourly IP/browser/email/global creation limits, cart quote/PIN/shipping/customer payment limits and maximum two outstanding checkouts per email. Trusted-proxy/edge deployment remains required. Distributed botnets are not fully prevented by application quotas. |
| F03 network/stock locking | Gateway order preparation and refund workers use durable leases; provider HTTP occurs outside inventory transactions. Fetched payment state applies in a short stock-locked transaction. Stock/order state refreshes under locks. Global stock locking remains conservative for the small shared-blank pool; real MySQL concurrency still needs verification. |
| F04 / F10 parcels | Checkout and admin defaults now agree: 11 x 13 inches, height +1 inch per tee, 0.5 kg per started pair, maximum 20 tees. Shipping request includes dimensions. Actual pack verification is required; admin can override actual measurements. Quote differences are merchant-side, never silently charged to an already paid customer. |
| F05 tracking | Original audit corrected: TrackingPoller already existed. Added safe failure logging; retained its ten-minute per-shipment refresh eligibility. |
| F06 old confirmation recovery | A nonempty new cart does not restore the previous confirmation on reload. Shared policy function has passing Node regression tests. |
| F07 production | Added APP_PRODUCTION startup checks, database readiness route, encrypted off-server backup script and deployment/restore guide. No hosting/backup job was installed and no restore was performed. |
| F08 cancellations/returns | Owner-reviewed full refunds for pre-dispatch cancellations and inspected delivered/returned items; active courier booking blocks premature refund. Durable existing refund worker handles provider requests. No automatic blank-stock restoration. Partial refunds and exchanges remain reviewed manual provider/support operations. |
| F09 shipment recovery | Read-only existing-order/shipment verification and reconciliation by internal Shiprocket ID. Verifies store reference and shipment ownership; never blindly creates/assigns another courier. Carrier cancellation/rebooking, label and pickup operations remain in Shiprocket. Unknown/unsupported responses fail closed. |
| F11 navigation | Removed links to six nonexistent collections and inactive mobile Search. Menu exposes real New In/Men/Women routes only. |
| F12 new stock colours | Admin creates colour/size variants with zero stock, then uses existing versioned, reasoned inventory adjustments for opening stock. |
| F13 pause orders | Persisted admin pause/resume control. New checkout/payment starts stop; existing payment verification, callbacks/refunds and order access continue. |
| F14 OTP lock | Quota/challenge transaction ends before SMTP. Failed sends invalidate the challenge and retain quota usage. Verification locks only its challenge. |
| F15 background queues | Independent scheduling pool (4) with per-order locks/leases retained. Production durations/backlogs must be monitored. |
| F16 old records | Server-side order/payment search and pagination; older stock movement pagination. |
| F17 failure visibility | Admin mail queue shows configuration, attempts and next retry, with guarded/audited retry scheduling. Payment/refund/tracking job failures log safe categories. External alert destination still needs setup. |
| F18 media | Full ImageIO decoding for JPEG/PNG/WebP (WebP reader dependency), 8 MB/12 MP/8000px bounds; Next responsive display optimization of public product media. Originals retained. Automated deletion intentionally not enabled: historical orders may still reference images. |
| F19 SEO | Robots, dynamic catalogue sitemap, product canonical/social metadata and escaped JSON-LD offers/availability. Archived products are noindex. |
| F20 accessibility | Homepage reduced-motion timer check, pause control and hover/focus suspension; menu Escape, focus cycling and body scroll lock. Men/Women banners now have explicit pause controls. Full browser/screen-reader audit remains outstanding. |
| F21 archived links | Archived products remain readable from historical orders and cannot be purchased. Drafts stay private and archived products stay out of listings. |
| F22 CI | Active/main branch triggers, frontend regression tests, backend and separate real MySQL suite configured. Workflow execution is not assumed successful. |
| F23 policies | Added factual privacy/shopping terms and footer/checkout links. Seller legal identity/address, invoice/tax details and final policy approval require owner input. No personal details invented. |
| F24 performance | Responsive optimized hero sources and optimized collection/product images; original source assets preserved. Measured 750px mobile lineup response 152,187 bytes vs 1,364,050-byte original in local test. Actual mobile LCP/CLS not measured. |
| F25 temporary failures | Order refresh retains last known authorized details on network/5xx failure; clears only for access failures. |
| F26 event ordering | New events get strictly increasing microsecond timestamps per locked order, avoiding UUID tie ordering. Historical identical timestamps cannot reliably be reconstructed. |
| F27/F28 UI | Rack font aligned, cart summary uses white/border styling, exception stages do not display an empty normal timeline; fully refunded orders label the original refunded payment. Rendered visual QA still needed. |
| F29 delivery guarantee | SMTP acceptance plus DB commit cannot offer exactly-once delivery. Retained stable Message-IDs, durable retries and operator visibility. This is documented, not falsely claimed eliminated. |
| F30 documentation | This status record and current deployment guide supersede historical implementation notes; corrected erroneous manual-only tracking finding. |

## Verification performed here

- Frontend ESLint and production build: passed.
- Two checkout-recovery regression tests: passed.
- Production HTTP probes with a mock catalogue: home, Men/Women collections, product JSON-LD/canonical rendering, privacy, terms, robots and sitemap: passed. These do not exercise real backend checkout/payment APIs.
- Product JSON-LD escapes angle brackets and parses back to the original fixture value.
- Responsive Next image endpoint: 750px mobile lineup returned 200 and 152,187 bytes. Original file preserved.
- Backup shell syntax: passed `bash -n`. No backup/provider call executed.
- Backend suite attempted: blocked before compilation by Gradle distribution network access.

## Still required outside this workspace

1. Pull the branch, run Java 21 `bash gradlew test` (PowerShell: `.\gradlew.bat test`), and apply Flyway V13–V15 to the local MySQL database by restarting the backend. Back up first; do not modify old migration checksums.
2. Verify pause/resume, quotas, new colour creation, paginated searches, OTP send/failure/retry, refund review and shipment reconciliation with the real local backend. Do not use live credentials for routine tests.
3. Confirm actual parcels, prices/availability for representative PINs and quantities, and legal seller/invoice/policy details.
4. Configure hosting, HTTPS/private backend/database, secure cookies, trusted-proxy/edge limits, persistent media, alerting, encrypted backup schedule and a successful isolated restore drill.
5. Complete controlled LIVE payment/refund, public webhook and genuine Shiprocket booking/AWB/pickup/tracking verification before enabling public paid orders. Check desktop/mobile appearance, keyboard/screen reader, private browsing/storage failures and multi-tab behavior.
