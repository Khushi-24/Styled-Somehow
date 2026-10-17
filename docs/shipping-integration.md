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
