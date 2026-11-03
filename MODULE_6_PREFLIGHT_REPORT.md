# CRAVELY — MODULE 6 PREFLIGHT AUDIT & STABILIZATION REPORT

**Document:** `MODULE_6_PREFLIGHT_REPORT.md`  
**Project:** Cravely – Food Ordering System  
**Audit Scope:** Module 6 Payment Integration (Razorpay Test Mode) & Order Management Lifecycle Consistency  
**Date:** 2026-11-04  

---

## 1. Project Inspection Summary

A comprehensive architectural and code-level preflight audit was performed across all Module 6 payment artifacts, Module 5 order management artifacts, security configurations, data persistence entities, repositories, and frontend services.

The inspection verified:
* **Entities & Schemas:** `Payment` (`payments` table), `WebhookEvent` (`processed_webhook_events` table), `Order` (`orders` table), `OrderItem` (`order_items` table).
* **Enums & States:** `OrderStatus` (`PLACED`, `ACCEPTED`, `REJECTED`, `PREPARING`, `READY_FOR_PICKUP`, `OUT_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`), `PaymentStatus` (`PENDING`, `PAID`, `FAILED`, `REFUNDED`).
* **Services & Controllers:** `PaymentService`, `PaymentController`, `RazorpayWebhookController`, `RazorpayClientWrapper`, `RazorpayProperties`, `OrderService`, `OrderController`.
* **Frontend:** `paymentService.ts`, `types/payment.ts`, `CartPage.tsx`.
* **Security & Testing:** `SecurityConfig`, `PaymentIntegrationTest`, `OrderIntegrationTest`, `CartIntegrationTest`, `FoodItemIntegrationTest`, `RestaurantIntegrationTest`, `AuthIntegrationTest`.

---

## 2. Issues Discovered

During Phase 1–5 inspections, the following potential concerns were flagged for investigation:
1. **Status Treatment for Razorpay "authorized":** In `PaymentService.verifyPayment()`, payments with status `"authorized"` were treated identically to `"captured"`, marking them `PaymentStatus.PAID`.
2. **Missing Authoritative Amount and Currency Matching:** During payment signature verification, `fetchPayment()` did not explicitly extract or assert that Razorpay's returned amount and currency matched the expected database order total.
3. **Webhook Safety on Terminated Orders:** Potential risk that a delayed Razorpay webhook could mark an already `CANCELLED` or `REJECTED` order as `PAID`.
4. **Order Status Confusion (Reference to "CONFIRMED"):** Audit of documentation revealed prior textual references to an order status called `CONFIRMED`. Inspection was required to verify whether this was introduced into `OrderStatus` or if order operational statuses were improperly overwritten.
5. **Idempotency Execution Sequence in Verification:** In `verifyPayment()`, calling `validateOrderEligibilityForPayment()` prior to the idempotency check prevented duplicate idempotent calls if the order was already paid.
6. **Linger of Superseded Payment Attempts:** Retrying an order created a new payment attempt while leaving previous attempts indefinitely marked as `PENDING`.

---

## 3. Issues Confirmed as Actual Bugs

1. **Bug 1 (Confirmed): Premature Paid Status on "authorized" Payments:**
   * *Finding:* Razorpay status `"authorized"` signifies card pre-authorization, not successful fund capture. Marking `"authorized"` as `PAID` violated the capture requirement.
   * *Resolution:* Only status `"captured"` transitions a payment to `PaymentStatus.PAID`. If `"authorized"` is received, the payment remains in a non-paid state (`PENDING` with notes) and throws an `IllegalStateException` requiring capture confirmation.

2. **Bug 2 (Confirmed): Absence of Amount & Currency Validation in Payment Verification:**
   * *Finding:* `RazorpayClientWrapper.fetchPayment()` only captured `(id, status, method)` and discarded `amount` and `currency`. Tampered verification requests could bypass amount checks if the signature was valid for an arbitrary amount.
   * *Resolution:* Extended `RazorpayPaymentDetails` to record `amount` and `currency`. In `verifyPayment()`, amount in paise and currency are strictly verified against the database order before capture confirmation. Webhooks also validate amount if present in the payload.

3. **Bug 3 (Confirmed): Verification Idempotency Interrupted by Paid Validation:**
   * *Finding:* In `verifyPayment()`, checking `validateOrderEligibilityForPayment(order)` before `if (payment.getPaymentStatus() == PaymentStatus.PAID)` caused duplicate verification calls to throw `IllegalStateException: Order is already paid` instead of returning the cached `PaymentResponse`.
   * *Resolution:* Moved the idempotency short-circuit ahead of `validateOrderEligibilityForPayment()`.

4. **Bug 4 (Confirmed): Delayed Webhooks on Terminated Orders:**
   * *Finding:* If an order was cancelled or rejected while a payment was in-flight, a delayed `payment.captured` or `order.paid` webhook would mark `order.paymentStatus = PAID`.
   * *Resolution:* Added guard clauses in `processWebhook()`: if `order.getOrderStatus() == CANCELLED || REJECTED`, the payment record is marked `FAILED` with a note, and the order's status and payment status are never reactivated or updated.

---

## 4. Changes Made

1. **`RazorpayClientWrapper.java`:**
   * Updated `RazorpayPaymentDetails` record definition to include `Long amount` (in paise) and `String currency`.
   * Updated `fetchPayment()` to extract `amount` and `currency` from the Razorpay API payment entity.
   * Introduced mock payment override registries (`registerMockPayment`, `clearMockPayments`) enabling mock/simulated test scenarios (such as `"authorized"`, amount mismatch, and capture) without altering production code pathways.
2. **`PaymentService.java`:**
   * Enforced strict capture-only transition: only `"captured"` transitions to `PaymentStatus.PAID`.
   * Added handling for `"authorized"` payments to keep them non-paid and require capture confirmation.
   * Added authoritative amount and currency assertion in `verifyPayment()`.
   * Reordered idempotency check in `verifyPayment()` before order eligibility check so duplicate requests return cleanly.
   * Added guard in `processWebhook()` ensuring `CANCELLED` and `REJECTED` orders are never updated or reactivated by late webhooks.
   * Enhanced `retryPayment()` to mark lingering previous `PENDING` attempts as `FAILED` (with reason `"Superseded by new payment retry"`).
3. **`PaymentIntegrationTest.java`:**
   * Restructured the test suite to explicitly implement all 15 audit scenarios required by the preflight specification.

---

## 5. Files Modified

| File | Type | Changes | Clickable Link |
| :--- | :--- | :--- | :--- |
| `RazorpayClientWrapper.java` | Backend Service | Added amount/currency fields to record, mock override registry for test mode | [`RazorpayClientWrapper.java`](file:///c:/Users/riyac/Downloads/Cravely-Food-Ordering/backend/cravely-backend/src/main/java/com/cravely/backend/service/RazorpayClientWrapper.java) |
| `PaymentService.java` | Backend Service | Fixed capture vs authorized transition, amount assertion, idempotency ordering, delayed webhook guards | [`PaymentService.java`](file:///c:/Users/riyac/Downloads/Cravely-Food-Ordering/backend/cravely-backend/src/main/java/com/cravely/backend/service/PaymentService.java) |
| `PaymentIntegrationTest.java` | Backend Test | Added all 15 audit test scenarios, authorized payment tests, and mismatch validation | [`PaymentIntegrationTest.java`](file:///c:/Users/riyac/Downloads/Cravely-Food-Ordering/backend/cravely-backend/src/test/java/com/cravely/backend/PaymentIntegrationTest.java) |

---

## 6. Payment Status Validation Results

* **Captured Payment:** Evaluated and confirmed. Only `"captured"` transitions `Payment` to `PAID`. Sets `paidAt` timestamp and links `razorpayPaymentId`.
* **Authorized Payment:** Evaluated and confirmed. If Razorpay reports `"authorized"`, the payment is NOT marked `PAID`. An `IllegalStateException` is thrown specifying that capture confirmation is required.
* **Failed Payment:** Evaluated and confirmed. Recorded with failure description in `payments.failure_reason`. Original Cravely order is preserved; payment can be retried.
* **Pending Payment:** Evaluated and confirmed. Payment creation inserts a record with `paymentStatus = PENDING`. Order remains `paymentStatus = PENDING` until cryptographic verification and capture succeed.

---

## 7. Order Lifecycle Consistency Results

* **Separation of Concerns:** `OrderStatus` and `PaymentStatus` are strictly decoupled:
  * `OrderStatus`: `PLACED`, `ACCEPTED`, `REJECTED`, `PREPARING`, `READY_FOR_PICKUP`, `OUT_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`.
  * `PaymentStatus`: `PENDING`, `PAID`, `FAILED`, `REFUNDED`.
* **No `CONFIRMED` Order Status Introduced:** A search across the repository confirmed that `CONFIRMED` does not exist in `OrderStatus.java`. Payment success updates only `order.setPaymentStatus(PaymentStatus.PAID)`. The operational order status remains `PLACED` for restaurant review.
* **Delayed Webhook Protection:** If an order has been `CANCELLED` or `REJECTED`, delayed webhook events (`payment.captured`, `order.paid`) do not reactivate the order or overwrite its status.

---

## 8. Webhook Security Results

* **Signature Verification:** Uses `HMAC_SHA256` keyed with `RAZORPAY_WEBHOOK_SECRET` over the exact raw request payload.
* **Missing / Invalid Headers:** Missing `X-Razorpay-Signature` returns HTTP `400 Bad Request`. Invalid signature returns HTTP `400 Bad Request` (`IllegalArgumentException`).
* **Idempotency & Deduplication:** Webhook event IDs are deduplicated using `WebhookEventRepository` against `processed_webhook_events`. Duplicate events return `200 OK` immediately without repeating state mutations.
* **Transactional Integrity:** Webhook processing runs within `@Transactional`. Unhandled errors rollback all operations and prevent recording the event as processed, enabling safe Razorpay redelivery.

---

## 9. Razorpay Configuration Results

* **Secrets Management:**
  * Configured in `application.properties` via environment variables:
    ```properties
    razorpay.key.id=${RAZORPAY_KEY_ID:rzp_test_placeholder_key_id}
    razorpay.key.secret=${RAZORPAY_KEY_SECRET:placeholder_key_secret_12345}
    razorpay.currency=${RAZORPAY_CURRENCY:INR}
    razorpay.webhook.secret=${RAZORPAY_WEBHOOK_SECRET:placeholder_webhook_secret_12345}
    ```
  * Zero real secrets committed to Git.
  * No secrets exposed in logs, API responses, or reports.
* **Mock / Simulated vs Real Test Mode Separation:**
  * When placeholder credentials are active, `RazorpayClientWrapper` safely simulates orders and captures with clear mock identifiers (`order_mock_<timestamp>_<rand>`).
  * When genuine Razorpay Test Mode keys (`rzp_test_...`) are provided via environment variables, the system executes real API network calls against Razorpay Test servers.

---

## 10. Frontend Payment Flow Results

* **Architecture:**
  * Frontend dynamically loads `https://checkout.razorpay.com/v1/checkout.js`.
  * Order is placed on backend -> Backend generates Razorpay order -> Razorpay checkout modal opens -> Customer authorizes -> Verification request sent to backend -> Backend validates signature and capture -> Confirmation UI updated.
* **Authority:** The backend is the single authoritative source of truth. The frontend never assumes payment status without `/api/payments/verify` confirmation.
* **UX Safety:** Checkout button is disabled with a loading spinner during checkout (`disabled={loading}`) to prevent duplicate clicks and accidental duplicate orders.

---

## 11. Backend Test Results

* **Test Suite:** Maven Surefire running JUnit 5 with Spring Boot Test.
* **Command:** `mvnw test`
* **Result:** **`Tests run: 56, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`)**

### 15 Required Payment Audit Tests Verified in `PaymentIntegrationTest.java`:
1. `test1_PaymentOrderCreation_ForValidOrder_Success` — PASSED
2. `test2_PaymentCreation_ForAnotherCustomerOrder_Rejected` — PASSED
3. `test3_PaymentAmount_DerivedFromDatabase` — PASSED
4. `test4_InvalidPaymentSignature_Rejected` — PASSED
5. `test5_ValidPaymentSignature_Accepted` — PASSED
6. `test6_CapturedPayment_BecomesPaid` — PASSED
7. `test7_AuthorizedPayment_DoesNotBecomePaidPrematurely` — PASSED
8. `test8_FailedPayment_HandledCorrectly` — PASSED
9. `test9_PendingPayment_RemainsPending` — PASSED
10. `test10_DuplicateWebhookEvent_IgnoredSafely` — PASSED
11. `test11_InvalidWebhookSignature_Rejected` — PASSED
12. `test12_PaymentRetry_DoesNotCreateAnotherOrder` — PASSED
13. `test13_CancelledOrder_NotReactivatedByDelayedWebhook` — PASSED
14. `test14_PaymentAmountMismatch_Rejected` — PASSED
15. `test15_DuplicateVerificationRequests_HandledSafely` — PASSED

---

## 12. Frontend Build Results

* **Tool:** Vite v8.3.5 / TypeScript compiler.
* **Command:** `npm run build` inside `frontend/`
* **Result:** **`built in 2.15s`** — Zero compilation errors, zero TypeScript errors.

---

## 13. Regression Test Results

All previous modules were re-verified with zero failures:
* **Module 1 (Auth & Users):** `AuthIntegrationTest` (12 tests) — PASSED
* **Module 2 (Restaurants):** `RestaurantIntegrationTest` (5 tests) — PASSED
* **Module 3 (Food & Menu):** `FoodItemIntegrationTest` (7 tests) — PASSED
* **Module 4 (Cart):** `CartIntegrationTest` (7 tests) — PASSED
* **Module 5 (Orders):** `OrderIntegrationTest` (8 tests) — PASSED
* **Module 6 (Payments):** `PaymentIntegrationTest` (17 tests) — PASSED
* **Total Automated Tests:** 56/56 passing.

---

## 14. Git Status and Commit Details

* **Branch:** `main`
* **Remote:** `https://github.com/razilchristian/Craverly_food_ordering.git`
* **Preflight Fix Commit:** `bf49127`
* **Commit Message:** `Fix Module 6 payment and order consistency`
* **Status:** Clean working tree, pushed to `origin/main`.

---

## 15. Remaining Limitations

1. **Refund Workflow:** Automated refund initiation (e.g. `POST /api/payments/refund`) is not part of Module 6 and is slated for later modules (or admin management).
2. **Auto-Capture Dependency:** The current checkout flow expects automatic capture upon payment authorization (Razorpay standard checkout default). If manual capture mode is configured on the Razorpay dashboard, the backend does not currently invoke Razorpay's `/payments/{id}/capture` API, instead requiring capture confirmation via webhook or dashboard.

---

## 16. Verification Distinction Disclosure

In accordance with strict verification reporting guidelines:
* **Unit & Integration Tests:** 56 automated integration tests were executed via `mvnw test` with Spring Boot test slices, verifying database persistence, HMAC cryptography, error codes, and rollback logic.
* **Mock / Simulated Tests:** The test suite and live verification script ran against the local development environment using safe development placeholder credentials (`rzp_test_placeholder_key_id`) and simulated cryptographic validation via `RazorpayClientWrapper`.
* **Actual Razorpay Sandbox Transactions:** **No live or external Razorpay Sandbox network transactions were performed**, as external merchant credentials (`RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET`) are kept as environment placeholders to protect user credentials and avoid non-reproducible external network calls during local builds. When user credentials are provided via environment variables, the codebase connects directly to Razorpay's live test endpoints.

---

## Conclusion & Stop

Module 6 preflight audit and stabilization is complete. All 15 required audit test cases pass, the frontend builds cleanly, the live backend is running stably on port 8081, and the stabilization commit has been pushed to GitHub.

**Execution is halted. Standing by for user review and approval before starting Module 7.**
