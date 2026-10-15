# Shiprocket checkout integration

This stage checks prepaid Blue Dart serviceability and rates. It does not create shipments, debit the Shiprocket wallet, assign AWBs, or schedule pickups.

## Local configuration

Set SHIPROCKET_EMAIL and SHIPROCKET_PASSWORD in backend/.env using the API user's credentials. Do not commit .env. Defaults are pickup postcode 380015 and pickup location Home; overrides are SHIPROCKET_PICKUP_POSTCODE and SHIPROCKET_PICKUP_LOCATION.

Known packed parcels:

| T-shirts | Actual weight | Measured size, cm | API size rounded up, cm |
| --- | --- | --- | --- |
| 1 | 0.7 kg | 30.48 × 38.10 × 3.81 | 31 × 39 × 4 |
| 2 | 1.5 kg | 30.48 × 38.10 × 7.62 | 31 × 39 × 8 |

Orders over two items are blocked pending measured packaging details. Never extrapolate parcel sizes silently.

## Rules

- POST /api/checkout/shipping-quote accepts cart items and pinCode; CSRF protection remains enabled.
- Catalog prices and stock are checked by the backend. Client-supplied prices are never used.
- Blue Dart only; blocked couriers, missing/invalid rates and all other courier names are excluded.
- Choose the lowest valid Blue Dart rate, rounded up to the next whole rupee.
- Product subtotal >= 999 means customer shipping is zero, but Blue Dart serviceability must still pass.
- Checkout rechecks shipping before reserving stock and rejects a mismatched expected total.
- Shipping provider errors fail closed; there is no free-shipping fallback on failure.
- Credentials and auth response bodies are not logged. Cached auth is renewed after one day; an expired token is retried once after a 401.

## Verify locally

1. Restart the backend after setting credentials. Start the frontend.
2. Add one ₹599 tee, enter delivery details and check a serviceable PIN. The summary must show a Blue Dart charge and the correct final total.
3. Create the pending checkout. Its shipping and total must match the displayed quote and Razorpay amount.
4. Cancel that checkout, then test a two-tee cart >= ₹999. Serviceability must still be checked; shipping is free.
5. Test an unsupported PIN, a changed PIN after a successful check, and unavailable credentials. Payment must remain inaccessible.
6. Test a three-item cart: it should request support rather than invent a parcel rate.
7. Run `gradlew.bat test` from backend on Windows with Java 21. New tests cover rate filtering/rounding, authoritative shipping totals, tampered totals and unavailable-courier rejection.

KYC is pending; account restrictions can prevent live rate responses. No real shipment needs to be booked for this stage.
