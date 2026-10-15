# Shiprocket checkout integration

Checkout checks prepaid Delhivery serviceability and rates. Admin shipment booking can create a real Shiprocket order and assign a Delhivery AWB for a paid LIVE order only. Pickup scheduling and labels remain manual in Shiprocket.

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
- Delhivery only; blocked couriers, missing/invalid rates and all other courier names are excluded.
- Choose the lowest valid Delhivery rate, rounded up to the next whole rupee.
- Product subtotal >= 999 means customer shipping is zero, but Delhivery serviceability must still pass.
- Checkout rechecks shipping before reserving stock and rejects a mismatched expected total.
- Shipping provider errors fail closed; there is no free-shipping fallback on failure.
- Credentials and auth response bodies are not logged. Cached auth is renewed after one day; an expired token is retried once after a 401.

## Verify locally

1. Restart the backend after setting credentials. Start the frontend.
2. Add one ₹599 tee, enter delivery details and check a serviceable PIN. The summary must show a Delhivery charge and the correct final total.
3. Create the pending checkout. Its shipping and total must match the displayed quote and Razorpay amount.
4. Cancel that checkout, then test a two-tee cart >= ₹999. Serviceability must still be checked; shipping is free.
5. Test an unsupported PIN, a changed PIN after a successful check, and unavailable credentials. Payment must remain inaccessible.
6. Test a three-item cart: it should request support rather than invent a parcel rate.
7. Run `gradlew.bat test` from backend on Windows with Java 21. New tests cover rate filtering/rounding, authoritative shipping totals, tampered totals and unavailable-courier rejection.

KYC is pending; account restrictions can prevent live rate responses. No real shipment needs to be booked for this stage.

## Admin shipment booking

- Flyway V8 adds one durable shipment booking per store order.
- Open Admin > Orders > View details > Shipment.
- Test/unpaid/refunded/review orders cannot call the provider through this feature.
- Booking is off by default. Only after KYC verification and live-payment readiness, set SHIPROCKET_BOOKING_ENABLED=true in backend/.env and restart the backend. Leave it false during Razorpay TEST-mode development.
- A live order first displays the Delhivery quote, parcel dimensions, pickup location and customer shipping amount. Admin must explicitly confirm the real booking.
- Confirmation rechecks courier and price. A changed quote must be reviewed again. The quoted provider cost does not change the amount already paid by the customer.
- Shiprocket IDs and the AWB are saved separately from payment status. BOOKED means an AWB was assigned, not that pickup or delivery occurred.
- The committed CREATING marker prevents repeated or concurrent submissions from creating a second shipment.
- If any external mutation times out or gives an uncertain response, REVIEW_REQUIRED prevents automatic retries. Search Shiprocket by the full store order reference and resolve there. If the process crashes at CREATING, ORDER_CREATED or ASSIGNING, also review in Shiprocket; there is deliberately no reset/rebook button.
- Do not enable booking against test payments by changing their payment mode in the database. Test this feature with mocked Shiprocket responses using ShipmentIntegrationTests.

Run `gradlew.bat test` locally with Java 21. Shipment tests cover test/live eligibility, idempotency, concurrent clicks, price changes, timeout review, stored order payloads, admin authorization and CSRF enforcement. No test sends a real shipment request.
