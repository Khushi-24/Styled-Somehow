# Fulfilment and tracking

Payment status remains separate from fulfilment. Verified payment starts fulfilment at PAID. Admin stages are PAID → PRINTING → PACKED → READY_TO_SHIP. TEST orders can exercise these steps as explicitly labelled simulations; they cannot book real couriers or mark parcels dispatched.

Real shipment booking requires PAID/LIVE, the existing booking-enabled flag, a READY_TO_SHIP stage, confirmed parcel details and courier cost acknowledgement. A successful AWB assignment records COURIER_BOOKED. It does not mean pickup happened. Admin can mark SHIPPED only after a BOOKED AWB exists and a handover note is entered. The UI also requires physical-handover acknowledgement. No labels or pickup API is called automatically.

Stage updates lock the order and require the expected current stage. Stale updates and skipping preparation stages are rejected. History records stage, actor, note and time. Customer DTOs exclude internal notes, actors and courier costs. Pay/refund/review status gates manual fulfilment actions.

## Customer tracking
The private order page loads the session-protected /api/order-access/order/{id}/fulfilment DTO and refreshes its cached data every minute. The response contains preparation stage, stage-event timestamps, assigned courier/AWB, last known courier status, a trusted HTTPS tracking URL if supplied, last-check time and an unavailable flag. Customer refresh never directly calls Shiprocket.

Tracking uses GET /courier/track/awb/{awb} via the existing authenticated Shiprocket client. The response must identify the matching AWB and track_status=1. Only explicit picked-up/in-transit/out-for-delivery statuses advance SHIPPED; explicit DELIVERED advances DELIVERED; known NDR/RTO states expose exceptions. Unknown statuses are shown as received without inventing movement. AWB assignment and pickup scheduling do not imply SHIPPED.

A scheduler runs each minute and checks up to 10 due booked shipments. Each shipment is normally checked after 10 minutes. Admin can request refresh, with a one-minute minimum between attempts. Terminal courier states stop polling. Failed tracking requests retain previous data and show an unavailable notice. A known terminal fulfilment stage cannot regress. Tracking requires a LIVE booking; TEST records never make tracking calls.

Only HTTPS tracking URLs on allowed Shiprocket, Delhivery, DTDC, Xpressbees or Blue Dart domains are exposed; unexpected URLs are omitted. Provider payloads and customer data are not logged.

Flyway V10 adds the stage, audit history and tracking cache columns. Existing paid orders start at PAID; existing successfully booked paid orders start at COURIER_BOOKED. No fake historical events or shipment times are generated.

## Local verification
1. Pull and restart the backend; confirm Flyway V10 completes.
2. In admin, open a PAID TEST order and simulate PRINTING, PACKED, READY_TO_SHIP.
3. Open its private order page: simulated progress should appear, with no real courier data.
4. For a genuine LIVE paid order after Razorpay live setup, prepare it through READY_TO_SHIP, select/review a courier and create a shipment.
5. Confirm AWB/courier in admin and the customer page. Do not mark shipped before handover.
6. Record physical handover or wait for a real courier movement scan. Check SHIPPED/DELIVERED updates and history.
7. Run tests with Java 21: gradlew.bat test --tests "*FulfilmentTests" --tests "*FulfilmentIntegrationTests" --tests "*TrackingTests" --tests "*ShipmentIntegrationTests" --tests "*OrderAccessIntegrationTests".

Frontend lint/build are checked here. Backend execution is blocked by the Gradle download in this environment; migration, actual API response and end-to-end LIVE courier behaviour require local verification. Tests cover transitions, stale writes, TEST/refund restrictions, AWB and handover-note gates, idempotent updates, private DTOs, CSRF/auth, provider outages, terminal states and tracking URL validation.

Remaining shipping work: public webhook setup (polling currently handles updates), label/pickup integration if wanted in admin, shipment cancellation/rebooking recovery, dispatch emails, live operational verification and parcels over two T-shirts. No paid provider action is performed by installing these changes.
