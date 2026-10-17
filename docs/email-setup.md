# Hostinger order email setup

The backend supports Hostinger SMTP for OTPs, forgotten-order reference recovery and automatic paid-order confirmation emails. The existing Brevo provider remains available.

Add these entries to your ignored backend/.env file (not frontend/.env):

```dotenv
EMAIL_PROVIDER=smtp
SMTP_HOST=smtp.hostinger.com
SMTP_PORT=465
SMTP_USERNAME=contact@styledsomehow.in
SMTP_PASSWORD=REPLACE_WITH_MAILBOX_PASSWORD
ORDER_CONFIRMATION_EMAIL_ENABLED=true
STORE_URL=http://localhost:3000
```

Keep your existing ORDER_ACCESS_SECRET (at least 32 characters). The password is the mailbox password used in the Gmail app, not the Hostinger account password. Never commit this file. This project imports backend/.env as a properties file; enter the literal value without shell quotes, and escape any backslashes as required by Java properties. Use the public HTTPS storefront URL for STORE_URL when deployed. EMAIL_SENDER and BREVO_API_KEY are not needed for SMTP.

SMTP uses SSL on port 465 with hostname verification; other ports use required STARTTLS. Connection and read/write timeouts are set. No credentials are logged.

Restart the backend after changing these values. Flyway V9 adds confirmation delivery state. It skips historical terminal orders; only newly paid orders, and existing still-pending orders that later become paid, are eligible. No schema changes to existing migrations.

The dispatcher checks every 30 seconds, up to 10 orders at a time. It only sends PAID orders, after the payment transaction has committed. Attempts are row-locked to prevent simultaneous workers sending the same order. A failure retains PAID status and schedules a retry with increasing delay. Sent orders are skipped. SMTP delivery is at-least-once: an ambiguous network failure after the provider accepted an email, or a crash before its sent marker commits, can cause a duplicate. SMTP acceptance does not guarantee inbox placement.

TEST confirmation subjects start with [TEST] and the body says no real money was charged and the order will not dispatch. Emails include full reference, product/size/colour/quantity, subtotal/shipping/total, address and the order-access link. No OTP or password is placed in that URL. Order details still require OTP verification.

Recovery: on /orders choose Forgot your order reference, enter the exact checkout email, and receive up to 20 recent paid/refund references plus a code for the newest order. Existing OTP budgets, expiry, browser binding and attempt limits apply.

## Local verification
1. Run backend tests: gradlew.bat test --tests "*OrderConfirmationTests" --tests "*OrderAccessIntegrationTests".
2. Restart backend and frontend.
3. Complete a Razorpay TEST order. Within roughly 30 seconds plus mail delivery time, check Gmail and spam for [TEST] confirmation.
4. Confirm full reference and amounts match. Open View order from the email and complete the OTP.
5. Refresh the confirmation page and ensure the reference remains. A fresh cart must start a new checkout.
6. Test forgotten-reference recovery using the same Gmail address.
7. A duplicate payment webhook/reconciliation must not produce another confirmation after one has been recorded as sent.

Automated tests were added for sent-order suppression, failed-send retry, non-paid/config-disabled behavior and confirmation content. This environment could not download Gradle; backend compilation/tests and actual SMTP delivery must be verified locally. Frontend recovery lint/build passed in the preceding change.
