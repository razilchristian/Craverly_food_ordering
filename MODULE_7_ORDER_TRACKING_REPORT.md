# CRAVELY – MODULE 7: ORDER TRACKING SYSTEM REPORT

**Project:** Cravely – Food Ordering System  
**Module:** Module 7 – Order Tracking System  
**Author:** Senior Java Spring Boot Backend Developer & Full-Stack Architect  
**Status:** Completed, Verified, and Ready for Review  
**Git Branch:** `main`  
**Test Suite:** 76 Tests Total (19 Module 7 + 57 Regression) — 100% Pass Rate  

---

## 1. Executive Summary

Module 7 implements an end-to-end, high-integrity **Order Tracking System** across the Cravely backend and frontend. The system provides real-time, chronological visibility into every stage of an order's lifecycle for customers, restaurant owners, and administrators. 

### Key Deliverables Completed:
1. **Audit-Grade Status History Data Model (`order_status_history`):** Complete JPA entity mapping with foreign keys, composite indexes (`order_id`, `changed_at`), immutability of historical records, and explicit actor auditing (`changed_by_user_id`, `changed_at`, `remarks`).
2. **Automatic Lifecycle Transition Recording:** Integrated directly into `OrderService` for every valid status change (`PLACED`, `ACCEPTED`, `REJECTED`, `PREPARING`, `READY_FOR_PICKUP`, `OUT_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`). The initial order placement creates the root event (`previous_status = null`). Failed or unauthorized status attempts never write audit records.
3. **Role-Enforced Tracking Endpoints:**
   - **Customer:** `GET /api/orders/{orderId}/tracking` and `GET /api/orders/{orderId}/tracking/history` with strict ownership validation (returning `404 Not Found` for unauthorized or cross-customer access to prevent information disclosure).
   - **Restaurant Owner:** `GET /api/owner/orders/{orderId}/tracking`, `PATCH /api/owner/orders/{orderId}/out-for-delivery`, and `PATCH /api/owner/orders/{orderId}/delivered` (enforcing restaurant ownership; returning `403 Forbidden` if attempting to manage another restaurant's order).
   - **Administrator:** `GET /api/admin/orders/{orderId}/tracking`, `PATCH /api/admin/orders/{orderId}/out-for-delivery`, and `PATCH /api/admin/orders/{orderId}/delivered` with system-wide oversight.
4. **Rich Customer Frontend:**
   - **Live Order Tracking Page (`/orders/:orderId/tracking`):** Visual horizontal/vertical timeline, completed checkmarks, current stage pulse animation, upcoming muted steps, terminal banners for `CANCELLED` and `REJECTED`, itemized order summary, price breakdown, delivery information, and complete status history audit log.
   - **Automatic Polling:** 10-second polling interval that automatically terminates when a terminal status (`DELIVERED`, `CANCELLED`, `REJECTED`) is reached, with proper `clearInterval` cleanup upon component unmount.
   - **Orders Page (`/orders`):** Customer order history overview with direct "Track Order" navigation buttons and pagination.
   - **Post-Checkout Navigation:** Direct "Track Your Order" link integrated into `CartPage.tsx` upon verified Razorpay payment.
5. **Comprehensive Automated Testing:** 19 automated integration tests covering the complete tracking lifecycle, security isolation, actor auditing, chronological ordering, and failed transition protection, plus 57 regression tests across Modules 1–6 (76 total tests, 0 failures).

---

## 2. Database Schema & Data Model

### Table: `order_status_history`
```sql
CREATE TABLE order_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    previous_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NOT NULL,
    changed_by_user_id BIGINT NULL,
    changed_at DATETIME(6) NOT NULL,
    remarks VARCHAR(500) NULL,
    CONSTRAINT fk_osh_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_osh_user FOREIGN KEY (changed_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_order_status_history_order (order_id),
    INDEX idx_order_status_history_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Entity Mapping (`OrderStatusHistory.java`)
- **`@Entity` & `@Table`**: Annotated with composite indexes on `order_id` and `changed_at`.
- **`@ManyToOne(fetch = FetchType.LAZY)`**: Relationships to `Order` and `User`.
- **`@CreationTimestamp`**: Sets `changed_at` automatically on insert with `updatable = false`.
- **`@Enumerated(EnumType.STRING)`**: Persists enum constants (`OrderStatus`) as clear, readable strings.

### Entity Bidirectional Association (`Order.java`)
```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("changedAt ASC")
private List<OrderStatusHistory> statusHistory = new ArrayList<>();

public void addStatusHistory(OrderStatusHistory history) {
    this.statusHistory.add(history);
    history.setOrder(this);
}
```

---

## 3. Order Status State Machine & Transitions

The order lifecycle adheres strictly to the defined operational sequence:

```
[Customer Place Order]
       │
       ▼
    PLACED ─────────── (Customer Cancel) ───────────► CANCELLED (Terminal)
       │
       ├────────────── (Owner Reject) ──────────────► REJECTED (Terminal)
       ▼
    ACCEPTED ───────── (Customer Cancel) ───────────► CANCELLED (Terminal)
       │
       ▼
    PREPARING
       │
       ▼
READY_FOR_PICKUP
       │
       ▼
OUT_FOR_DELIVERY (Owner or Admin)
       │
       ▼
   DELIVERED (Owner or Admin - Terminal)
```

### Transition Invariants:
1. **Initial Event:** `PLACED` is recorded when the customer submits the order (`previousStatus = null`).
2. **Terminal Boundaries:** Once an order reaches `DELIVERED`, `CANCELLED`, or `REJECTED`, no further status transitions or edits are allowed.
3. **Strict Validation:** Direct status jumps (e.g., `PLACED` $\rightarrow$ `PREPARING` or `PLACED` $\rightarrow$ `DELIVERED`) throw `IllegalStateException` and never write history entries.
4. **Cancellation Restrictions:** Customers may cancel an order only while in `PLACED` or `ACCEPTED` status. Once preparation begins (`PREPARING`), cancellations are prohibited.
5. **Decoupled Financial Status:** `PaymentStatus` (`PAID`, `PENDING`, `FAILED`) remains independent of `OrderStatus`. Orders cancelled or rejected post-payment retain `PAID` for reconciliation while transitioning operational status to `CANCELLED` or `REJECTED`.

---

## 4. API Endpoints Specification

### 4.1 Customer Endpoints (`/api/orders`)
- **`GET /api/orders/{orderId}/tracking`**
  - **Security:** `@PreAuthorize("hasRole('CUSTOMER')")`
  - **Behavior:** Verifies the authenticated customer owns `orderId`. Returns `OrderTrackingResponse` including current status, restaurant details, delivery address, items snapshot, price breakdown, and full timeline.
  - **Isolation:** If `orderId` belongs to another customer or does not exist, returns `404 Not Found` (via `ResourceNotFoundException`) to prevent enumeration attacks.
- **`GET /api/orders/{orderId}/tracking/history`**
  - **Security:** `@PreAuthorize("hasRole('CUSTOMER')")`
  - **Behavior:** Returns `List<OrderStatusHistoryResponse>` in ascending chronological order (`changedAt ASC`).

### 4.2 Restaurant Owner Endpoints (`/api/owner/orders`)
- **`GET /api/owner/orders/{orderId}/tracking`**
  - **Security:** `@PreAuthorize("hasRole('RESTAURANT_OWNER')")`
  - **Behavior:** Returns tracking details for an order belonging to the owner's restaurant. Returns `403 Forbidden` (`AccessDeniedException`) if the order belongs to another restaurant.
- **`PATCH /api/owner/orders/{orderId}/out-for-delivery`**
  - **Security:** `@PreAuthorize("hasRole('RESTAURANT_OWNER')")`
  - **Behavior:** Transitions order from `READY_FOR_PICKUP` to `OUT_FOR_DELIVERY` and records status history with owner credentials.
- **`PATCH /api/owner/orders/{orderId}/delivered`**
  - **Security:** `@PreAuthorize("hasRole('RESTAURANT_OWNER')")`
  - **Behavior:** Transitions order from `OUT_FOR_DELIVERY` to `DELIVERED` and records status history with owner credentials.

### 4.3 Administrator Endpoints (`/api/admin/orders`)
- **`GET /api/admin/orders/{orderId}/tracking`**
  - **Security:** `@PreAuthorize("hasRole('ADMIN')")`
  - **Behavior:** Unrestricted global tracking access across all restaurants.
- **`PATCH /api/admin/orders/{orderId}/out-for-delivery`**
  - **Security:** `@PreAuthorize("hasRole('ADMIN')")`
  - **Behavior:** Admin override to dispatch order.
- **`PATCH /api/admin/orders/{orderId}/delivered`**
  - **Security:** `@PreAuthorize("hasRole('ADMIN')")`
  - **Behavior:** Admin override to complete delivery.

---

## 5. DTO Specifications

### `OrderTrackingResponse`
```json
{
  "orderId": 292,
  "orderNumber": "CRV-20261104-86963",
  "orderStatus": "DELIVERED",
  "paymentStatus": "PAID",
  "restaurantId": 885,
  "restaurantName": "Live Tracking Bistro",
  "restaurantPhone": "9876500001",
  "deliveryAddress": "504 Sunset Heights, SG Highway",
  "deliveryCity": "Ahmedabad",
  "deliveryState": "Gujarat",
  "deliveryPincode": "380054",
  "customerPhone": "9876543210",
  "customerNote": "Ring bell twice",
  "rejectionReason": null,
  "cancellationReason": null,
  "subtotal": 900.00,
  "deliveryFee": 0.00,
  "taxAmount": 45.00,
  "discountAmount": 0.00,
  "totalAmount": 945.00,
  "orderedAt": "2026-11-04T11:30:27.949",
  "items": [
    {
      "id": 412,
      "foodItemId": 945,
      "foodItemName": "Truffle Risotto",
      "foodType": "VEG",
      "quantity": 2,
      "priceAtOrder": 450.00,
      "totalPrice": 900.00
    }
  ],
  "statusHistory": [
    {
      "id": 101,
      "previousStatus": null,
      "newStatus": "PLACED",
      "changedBy": "Live Customer",
      "changedByRole": "CUSTOMER",
      "changedAt": "2026-11-04T11:30:27.967",
      "remarks": "Order placed successfully"
    }
  ]
}
```

---

## 6. Frontend Implementation

### 6.1 Tracking Page (`frontend/src/pages/customer/OrderTrackingPage.tsx`)
- **Visual Lifecycle Stepper:** 6-step progress pipeline (`PLACED`, `ACCEPTED`, `PREPARING`, `READY_FOR_PICKUP`, `OUT_FOR_DELIVERY`, `DELIVERED`). Completed steps display green checkmark badges with connecting lines; active step pulses with primary branding; upcoming steps remain muted.
- **Terminal State Banners:** Dedicated high-visibility alert cards for `CANCELLED` and `REJECTED` orders detailing the recorded reason and timestamp.
- **Live Polling Engine:** Background polling every 10 seconds via `setInterval`. Polling automatically disengages once the order reaches any terminal state (`DELIVERED`, `CANCELLED`, `REJECTED`) or when navigating away (`useEffect` cleanup).
- **Audit Log Table:** Chronological timeline showing actor name, role badge, timestamp, and optional remarks for complete transparency.

### 6.2 Customer Orders Page (`frontend/src/pages/customer/OrdersPage.tsx`)
- Clean list of all customer orders with status badges, price totals, and direct "Track Order" action buttons linking to `/orders/:orderId/tracking`.
- Paginated interface with smooth navigation.

### 6.3 Routing & Navigation Updates
- **`App.tsx`:** Registered `/orders` (`OrdersPage`) and `/orders/:orderId/tracking` (`OrderTrackingPage`).
- **`CustomerLayout.tsx`:** Added "Orders" tab to both desktop header navbar and mobile navigation bar.
- **`CartPage.tsx`:** Added prominent "Track Your Order" CTA immediately following successful Razorpay checkout.

---

## 7. Automated Test Suite Results

### Maven Test Execution Summary
- **Module 7 Tests:** `OrderTrackingIntegrationTest` (19 Tests)
- **Regression Tests:** 57 Tests (`AuthIntegrationTest`, `RestaurantIntegrationTest`, `CartIntegrationTest`, `OrderIntegrationTest`, `PaymentIntegrationTest`)
- **Total Tests Run:** 76
- **Failures / Errors:** 0
- **Pass Rate:** 100%

### Test Scenarios Covered in `OrderTrackingIntegrationTest`:
1. `test01_InitialPlacedHistoryGeneratedOnOrderPlacement`: Root `PLACED` audit entry with `previousStatus = null`, customer actor, and timestamp.
2. `test02_OrderAcceptanceCreatesAcceptedHistoryRecord`: `ACCEPTED` transition records owner actor and remarks.
3. `test03_OrderRejectionCreatesRejectedHistoryRecord`: `REJECTED` transition persists rejection reason.
4. `test04_PreparingTransitionCreatesHistoryRecord`: `PREPARING` transition audit logging.
5. `test05_ReadyForPickupTransitionCreatesHistoryRecord`: `READY_FOR_PICKUP` transition audit logging.
6. `test06_OutForDeliveryTransitionCreatesHistoryRecord`: `OUT_FOR_DELIVERY` transition audit logging.
7. `test07_DeliveredTransitionCreatesHistoryRecord`: `DELIVERED` terminal status audit logging.
8. `test08_CancellationCreatesCancelledHistoryRecord`: `CANCELLED` transition records customer reason.
9. `test09_FailedTransitionDoesNotCreateHistory`: Invalid jumps (e.g., `PLACED` $\rightarrow$ `PREPARING`) fail cleanly with `IllegalStateException` and create 0 audit entries.
10. `test10_RepeatedUnchangedStatusDoesNotCreateDuplicateHistory`: Idempotent state re-trigger prevents duplicate history entries.
11. `test11_StatusHistoryReturnedInChronologicalOrder`: Verifies strict chronological ordering (`changedAt ASC`) across all 6 stages.
12. `test12_ActorInformationRecordedAccurately`: Verifies actor names, roles (`CUSTOMER`, `RESTAURANT_OWNER`, `ADMIN`), and IDs.
13. `test13_CustomerCanTrackTheirOwnOrder`: Customer retrieval returns 200 OK and full payload.
14. `test14_CustomerCannotTrackAnotherCustomersOrder`: Cross-customer tracking blocked with 404 Not Found.
15. `test15_CustomerCannotAccessAnotherCustomersHistory`: Cross-customer history access blocked with 404 Not Found.
16. `test16_OwnerCanTrackOrderForTheirRestaurant`: Owner access returns 200 OK.
17. `test17_OwnerCannotTrackOrderForAnotherRestaurant`: Cross-owner access blocked with 403 Forbidden.
18. `test18_AdminCanInspectTrackingForAnyOrder`: Admin global access returns 200 OK.
19. `test19_RoleAndSecurityAnnotationsConfigured`: Controller security annotations and unauthenticated access rejection verified.

---

## 8. Live End-to-End Verification

The complete live workflow was executed against the running backend server on port 8081 and MySQL database:
1. User registrations: Customer, Owner 1, Owner 2, Customer 2.
2. Authentication & JWT issuance.
3. Restaurant creation, admin approval, category and food item setup.
4. Cart population and order placement $\rightarrow$ Order ID: `292` (`CRV-20261104-86963`).
5. Customer tracking verified (`PLACED`, 1 initial history item).
6. Consecutive transitions executed: `ACCEPTED` $\rightarrow$ `PREPARING` $\rightarrow$ `READY_FOR_PICKUP` $\rightarrow$ `OUT_FOR_DELIVERY` $\rightarrow$ `DELIVERED`.
7. Chronological timeline validated (all 6 history entries verified with accurate timestamps and actor roles).
8. Admin tracking inspection verified.
9. Cross-customer isolation verified (Customer 2 received `404 Not Found`).
10. Cross-owner isolation verified (Owner 2 received `403 Forbidden`).
11. Unauthenticated request verified (received `401 Unauthorized`).

---

## 9. Verification Commands Reference

```powershell
# 1. Run all backend tests
cd backend\cravely-backend
$env:JAVA_HOME = "C:\Users\riyac\Downloads\jdk-22_windows-x64_bin\jdk-22.0.1"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test

# 2. Build frontend production assets
cd frontend
npm run build

# 3. Check backend health
Invoke-RestMethod -Uri "http://localhost:8081/api/health" -Method Get
```
