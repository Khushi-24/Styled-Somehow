# Shipping rules

- Pickup: Home, PIN 380015. API credentials remain in backend/.env.
- Approved shortlist: Delhivery, DTDC, Xpressbees and Blue Dart. Select the cheapest valid, unblocked quote.
- Maximum provider quote: ₹110 inclusive. Quotes above ₹110 require contacting contact@styledsomehow.in; unavailable PIN codes are also blocked.
- This maximum applies even when customer shipping is free (product subtotal ₹999+).
- Below ₹999, customer shipping is the selected quote rounded up to a whole rupee. There is no ₹60 flat fee.
- Updated user estimate: up to two T-shirts weigh at most 500 g. Use 0.5 kg for both one- and two-item quote requests. Verify the packed weight before dispatch.
- Old dimensions and old 700 g / 1,500 g weights have been removed. Checkout quotes omit dimensions and remain weight-only estimates: final courier billing may use volumetric weight.
- More than two items require confirmed packaging details; checkout remains blocked for those parcels.
- Real booking requires confirmed positive dimensions. Defaults are unset (zero), so booking cannot proceed until those are known. Configuration keys are shiprocket.parcel-length-cm, shiprocket.parcel-breadth-cm and shiprocket.parcel-height-cm. Do not set guessed dimensions. Booking also requires paid LIVE status and explicit booking enablement.
- Test PIN checks with a supported courier, with all quotes above ₹110, with no supported courier, and both below/above ₹999. Changing PIN must invalidate the prior result.
- Backend tests: run gradlew.bat test with Java 21. Integration tests use artificial parcel dimensions solely for mocked shipment fixtures.
