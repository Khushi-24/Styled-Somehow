> Current audit fixes and operational limits: see [AUDIT_FIX_STATUS.md](AUDIT_FIX_STATUS.md). Historical details below may be superseded, including the old dimension-free/two-item shipping rule.

# Shipping rules

- Pickup: Home, PIN 380015. API credentials remain in backend/.env.
- Approved shortlist: Delhivery, DTDC, Xpressbees and Blue Dart. Select the cheapest valid, unblocked quote.
- Maximum provider quote: ₹110 inclusive. Quotes above ₹110 require contacting contact@styledsomehow.in; unavailable PIN codes are also blocked.
- This maximum applies even when customer shipping is free (product subtotal ₹999+).
- Below ₹999, customer shipping is the selected quote rounded up to a whole rupee. There is no ₹60 flat fee.
- Updated user estimate: up to two T-shirts weigh at most 500 g. Use 0.5 kg for both one- and two-item quote requests. Verify the packed weight before dispatch.
- Old dimensions and old 700 g / 1,500 g weights have been removed. Checkout quotes omit dimensions and remain weight-only estimates: final courier billing may use volumetric weight.
- More than two items require confirmed packaging details; checkout remains blocked for those parcels.
- Real booking requires confirmed packed weight and positive dimensions entered per order in admin. Legacy configuration keys shiprocket.parcel-length-cm, shiprocket.parcel-breadth-cm and shiprocket.parcel-height-cm remain unset for the old preview flow. Do not set guessed dimensions. Booking also requires paid LIVE status and explicit booking enablement.
- Test PIN checks with a supported courier, with all quotes above ₹110, with no supported courier, and both below/above ₹999. Changing PIN must invalidate the prior result.
- Backend tests: run gradlew.bat test with Java 21. Integration tests use artificial parcel dimensions solely for mocked shipment fixtures.

## Admin courier comparison
- Paid orders: open the shipment panel, enter actual packed weight in kg and dimensions in cm, then Compare couriers. Paid TEST orders can compare read-only; they cannot book real shipments.
- Returned supported, unblocked courier options are sorted by API quote, rounded up to whole rupees. Delivery days and rating are shown only when supplied by Shiprocket. Missing or inactive options are not invented.
- Comparison sends actual parcel measurements. Editing any measurement clears the previous comparison. A confirmation shows the selected courier, cost and parcel.
- Booking re-fetches options with the same parcel and requires the selected courier ID and expected price still to match. An administrator may explicitly select a quote above ₹110; checkout still enforces its ₹110 limit. Customer shipping payment never changes.
- Real booking remains disabled by default and requires a paid LIVE order. Duplicate protection and ambiguous-result review remain in place. Labels and pickup stay in Shiprocket.
- Legacy /preview and service overloads remain for compatibility; the admin UI uses /compare and supplies per-order measurements, without relying on global parcel dimensions.
- API quotes may differ from dashboard or final billing. No unexplained ₹5 adjustment is added. Confirm the rate discrepancy with Shiprocket before enabling live bookings.

## Read-only rate diagnostics
- In the admin shipment panel, expand Diagnose Shiprocket dashboard / API differences. Enter an existing numeric Shiprocket order ID and compare.
- Makes two serviceability GET calls using the store order's route: one with the current parcel, prepaid flag and stored subtotal; the other with the supplied Shiprocket order ID (without overriding its weight/payment parameters). Confirm the existing order matches the same route and shipment details.
- Displays allowlisted raw courier/price fields from both responses, including blocked or unsupported couriers, without rounding. No full provider payload, credentials or customer details are exposed.
- Admin authentication and CSRF are required. No order, AWB or pickup is created; diagnostic results cannot be selected for booking and do not modify the normal comparison.

- Extended diagnostic also GETs /orders/show/{id}. Shows the exact PIN request and a strict allowlist of saved shipment fields (PINs, pickup identifiers, channel, payment codes, values and parcel measurements). Names, emails, phone numbers and street addresses are excluded. Missing values remain unknown. Failure to read details does not discard the two courier result tables.
- Screenshot confirmed the dashboard adds ₹5 for Smart Order to existing-order API freight. No universal fee is hardcoded into checkout or booking.

## Checkout and PIN validation
- Checkout uses Contact → Delivery → Review & pay. Guest checkout remains available and the existing Gmail restriction is retained. Contact/address values survive step navigation; the final review prominently shows the delivery PIN.
- GET /api/checkout/location/{pin} proxies Shiprocket /open/postcode/details with postcode. No new external account or client-side provider credentials are needed. The backend caches successful results for 24 hours, bounded to 4,096 PINs.
- City/town must be selected from the returned postal city or locality names; state is read-only. Postal city/district is labelled separately because it may not be the town name. House/street and landmark remain manually entered.
- Changing the PIN immediately clears mapped city/state, previous lookup and delivery validation. Stale lookup and shipping responses cannot overwrite the new PIN. A failed lookup offers retry and blocks continuation; missing data is never guessed.
- CheckoutService validates the submitted city and state against the lookup before reserving inventory. Case and whitespace variations are accepted. A mismatched PIN/city/state or provider outage blocks order creation. Existing paid orders are not rewritten.
- Pay securely prepares an idempotent checkout and opens Razorpay without a separate pending-checkout action. A pending checkout is recovered after refresh; editing its delivery cancels the prior reservation first. Payment status can still be checked when its payment window has expired.
- Test mode remains explicit. Price and shipping recalculation, CSRF, payment verification, reservation expiry, late payments and stock protections remain server-controlled. No real payments or shipments are initiated by tests here.
- Verify 362720 using real provider data: city/state options must belong to that PIN, and a forged Ahmedabad city must be rejected. Repeat after changing PIN and for lookup/shipping failures.

- Customer delivery messages use `Estimated delivery in X days`, with two preparation days added once to the courier's positive transit estimate. Admin courier estimates remain raw. Missing estimates do not get invented dates.
- Finished checkout pointers (PAID, CANCELLED, EXPIRED, REFUNDED) are cleared. A completed-order pointer discovered while a new cart exists no longer restores the old confirmation or subtracts products from the new cart. Pending and payment-review states still resume for safe payment recovery. The redundant always-visible pending-checkout link is removed from the bag.


## Order reference recovery
Paid confirmation references are retained in tab session storage, separately from active checkout. Reload restores the confirmation, including when other cart items remain; normal navigation with a fresh cart starts checkout as before. Session-bound checkout APIs still require the original session.

On /orders, “Forgot your order reference?” sends up to 20 recent paid/refund order references and a code for the newest order to the exact checkout email (case insensitive). An older reference can be entered separately for a new code. No references are exposed in the request response. Existing OTP request budgets, cooldown, browser binding, attempts, expiry, and email block apply to recovery too. Delivery failures cannot grant access. This recovery flow is historical and its public endpoints are now disabled. Current confirmation emails use Hostinger SMTP and private order links; see email-setup.md.

Manual checks: pay a TEST order; refresh the confirmation; start a fresh cart; use recovery with its checkout email; verify code in the same browser; request an older reference separately; ensure a non-matching email receives a generic response and no mail. Backend integration tests cover recovery ownership, browser binding, failed email delivery, and shared cooldown, but require a working Gradle/Java environment.


## Current order access (supersedes earlier OTP/recovery notes)
Public OTP/recovery requests are retired. New confirmation emails contain private signed order links; lost emails are handled via contact@styledsomehow.in. See email-setup.md for token expiry, session behavior and test steps. The latest admin UI removes the temporary dashboard/API diagnostic panel and allows TEST courier review without booking. Review refreshes the selected courier quote and shows parcel, route and shipping difference; real LIVE booking requires the existing booking flag and explicit acknowledgement.
