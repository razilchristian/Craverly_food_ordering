# CRAVELY – MODULE 8: ADMIN DASHBOARD & ANALYTICS REPORT

## 1. Executive Summary
**Module 8: Admin Dashboard & Analytics** provides Cravely administrators with a centralized control center for monitoring platform metrics, managing users and restaurants, inspecting live orders and logistics, moderating food listings, and analyzing business performance.

All analytics are derived directly from real database records in MySQL (`cravely_db`), strictly adhering to financial accuracy rules:
- Revenue is computed solely from captured payments (`payment_status = 'PAID'`).
- Cancelled or rejected orders with captured payments are segregated into `reconciliationExposure` and excluded from `deliveredRevenue`.
- Protection against self-deactivation of the sole active administrator is enforced at the service layer.
- All admin endpoints (`/api/admin/**`) are safeguarded by Spring Security (`@PreAuthorize("hasRole('ADMIN')")`).

---

## 2. Technology Stack & Environment
- **Backend**: Java 22, Spring Boot 4.1.2, Spring Security, Spring Data JPA, Hibernate ORM 7.4.13, JJWT 0.12.6, Maven
- **Frontend**: React 19, TypeScript, Vite, Tailwind CSS 4, Lucide React
- **Database**: MySQL 8 (`cravely_db`)
- **Payment Engine**: Razorpay Java SDK 1.4.8 (Test Mode)
- **Ports**: Backend on `8081`, Frontend on `5173`
- **Git Branch**: `main` (`https://github.com/razilchristian/Craverly_food_ordering.git`)

---

## 3. Database & Entity Repository Optimizations
To support real-time aggregation without loading entire tables into memory, dedicated aggregate and lookup queries were added across Spring Data JPA repositories:

1. **`UserRepository`**:
   - `countByRole(Role role)`
   - `countByActive(boolean active)`
   - `countByRoleAndActive(Role role, boolean active)`
   - `countByCreatedAtBetween(LocalDateTime start, LocalDateTime end)`
   - `findTop10ByOrderByCreatedAtDesc()`
   - Added `JpaSpecificationExecutor<User>` for multi-criteria pagination, text search (`fullName`, `email`, `phone`), role filtering, and active status filtering.

2. **`RestaurantRepository`**:
   - `countByStatus(RestaurantStatus status)`
   - `countByActive(boolean active)`
   - `countByCreatedAtBetween(LocalDateTime start, LocalDateTime end)`
   - `countByCuisineTypeGroup()` (`SELECT r.cuisineType, COUNT(r) FROM Restaurant r GROUP BY r.cuisineType ORDER BY COUNT(r) DESC`)
   - `findTop10ByOrderByCreatedAtDesc()`

3. **`FoodItemRepository`**:
   - `countByAvailable(Boolean available)`

4. **`OrderRepository`**:
   - `countByOrderStatus(OrderStatus orderStatus)`
   - `countByCreatedAtBetween(LocalDateTime start, LocalDateTime end)`
   - `countByOrderStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime start, LocalDateTime end)`
   - `findTop10ByOrderByCreatedAtDesc()`

5. **`PaymentRepository`**:
   - `sumAmountByPaymentStatus(PaymentStatus status)`
   - `sumAmountByPaymentStatusAndCreatedAtAfter(PaymentStatus status, LocalDateTime start)`
   - `sumAmountByPaymentStatusAndOrderStatus(PaymentStatus paymentStatus, OrderStatus orderStatus)`
   - `sumAmountByPaymentStatusAndOrderStatusIn(PaymentStatus paymentStatus, Collection<OrderStatus> statuses)`
   - `findByPaymentStatusAndCreatedAtBetweenOrderByCreatedAtAsc(PaymentStatus status, LocalDateTime start, LocalDateTime end)`
   - `findTop10ByPaymentStatusOrderByCreatedAtDesc(PaymentStatus status)`

6. **`OrderStatusHistoryRepository`**:
   - `findTop20ByOrderByChangedAtDesc()` for populating the chronological platform activity audit log.

---

## 4. Backend Analytics & Service Layer

### `AdminDashboardService.java`
Implements the business logic for executive metrics and timeline projections:
- **`getOverview()`**: Aggregates total, active, and deactivated users (customers, restaurant owners, admins); total, pending, approved, and active restaurants; food items and available count; order counts by status; and financial metrics (`totalRevenue`, `todayRevenue`, `deliveredRevenue`, `reconciliationExposure`).
- **`getRevenueAnalytics(period, startDate, endDate)`**: Aggregates time-series data grouped by period (`daily`, `weekly`, `monthly`, `yearly`). Generates labeled buckets (e.g. `2026-11-04`, `Nov 2026`, `2026`) with chronological sorting, revenue totals, and order counts.
- **`getOrderAnalytics(startDate, endDate)`**: Computes counts across all 8 order states (`PLACED`, `ACCEPTED`, `PREPARING`, `READY_FOR_PICKUP`, `OUT_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`, `REJECTED`) and calculates `activeOrders` (`total - delivered - cancelled - rejected`).
- **`getUserAnalytics(startDate, endDate)`**: Role distributions, active vs deactivated user counts, new user registrations within the requested window, and top 10 recent signups.
- **`getRestaurantAnalytics(startDate, endDate)`**: Review status breakdowns (`PENDING`, `APPROVED`, `REJECTED`), active catalog listings, and cuisine distribution with percentages.
- **`getRecentActivity(limit)`**: Merges events from `OrderStatusHistory`, `Payment` transactions, `Restaurant` submissions, and `User` registrations into a unified chronological feed with actors and timestamps.

### `UserService.java` & Sole Admin Safeguard
- Added `getUsersPaged(role, active, search, pageable)` using dynamic JPA Specifications.
- Added `activateUser(Long userId)`.
- Updated `deactivateUser(Long userId)` to count active admins via `userRepository.countByRoleAndActive(Role.ADMIN, true)`. If an active admin attempts to deactivate the last remaining active admin, an `IllegalStateException("Cannot deactivate the only active administrator")` is thrown, returning HTTP 400 Bad Request to prevent catastrophic platform lockout.

---

## 5. REST API Endpoints Specification

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| `GET` | `/api/admin/dashboard/overview` | `ADMIN` | High-level executive statistics and financial snapshot |
| `GET` | `/api/admin/dashboard/revenue` | `ADMIN` | Grouped time-series revenue and reconciliation breakdown |
| `GET` | `/api/admin/dashboard/orders` | `ADMIN` | Status distributions, fulfillment funnel, and recent orders |
| `GET` | `/api/admin/dashboard/users` | `ADMIN` | User community growth, role breakdowns, and recent signups |
| `GET` | `/api/admin/dashboard/restaurants` | `ADMIN` | Restaurant approval status, active counts, cuisine breakdown |
| `GET` | `/api/admin/dashboard/recent-activity` | `ADMIN` | Real chronological platform event log |
| `GET` | `/api/admin/users` | `ADMIN` | Paged user list with role, status, and text search |
| `PATCH` | `/api/admin/users/{id}/activate` | `ADMIN` | Re-enable user login and operations |
| `PATCH` | `/api/admin/users/{id}/deactivate` | `ADMIN` | Suspend user (with sole active admin lockout prevention) |
| `GET` | `/api/admin/restaurants` | `ADMIN` | List all partner restaurant applications and listings |
| `PATCH` | `/api/admin/restaurants/{id}/approve` | `ADMIN` | Approve pending restaurant application |
| `PATCH` | `/api/admin/restaurants/{id}/reject` | `ADMIN` | Reject pending restaurant application |
| `PATCH` | `/api/admin/restaurants/{id}/activate` | `ADMIN` | Activate restaurant catalog visibility |
| `PATCH` | `/api/admin/restaurants/{id}/deactivate` | `ADMIN` | Suspend restaurant from public catalog |
| `GET` | `/api/admin/orders` | `ADMIN` | Global order list with search, status filters, and pagination |
| `PATCH` | `/api/admin/orders/{id}/out-for-delivery` | `ADMIN` | Dispatch order to delivery team |
| `PATCH` | `/api/admin/orders/{id}/delivered` | `ADMIN` | Complete order delivery |
| `GET` | `/api/admin/items` | `ADMIN` | View entire food catalog across all restaurants |
| `PATCH` | `/api/admin/items/{id}/activate` | `ADMIN` | Enable food item availability |
| `PATCH` | `/api/admin/items/{id}/deactivate` | `ADMIN` | Disable food item availability |

---

## 6. Frontend Admin Portal Architecture

### 1. `AdminLayout.tsx`
- Dedicated responsive navigation shell with desktop sidebar and collapsible mobile drawer.
- Navigation links:
  - `Dashboard` (`/admin/dashboard`)
  - `Analytics` (`/admin/analytics`)
  - `Orders` (`/admin/orders`)
  - `Restaurants` (`/admin/restaurants`)
  - `Food Items` (`/admin/food-items`)
  - `Users` (`/admin/users`)
- Administrator identity card with email badge and logout action.
- "Customer View" link to seamlessly return to the consumer storefront.

### 2. `RevenueChart.tsx`
- Pure responsive SVG bar chart visualizing revenue trends across dynamic periods (`daily`, `weekly`, `monthly`, `yearly`).
- Hover tooltips showing exact date, revenue in INR, and order volume.
- Summary financial cards highlighting:
  - Total Captured Revenue
  - Delivered Gross Revenue
  - Reconciliation Exposure (flagged post-cancellation captures)

### 3. Management Pages
- **`AdminDashboardPage.tsx`**: Executive KPI cards, real-time revenue chart, order status badges, active restaurant counts, and live activity audit log.
- **`AdminAnalyticsPage.tsx`**: In-depth analytics with custom date range filters, fulfillment and cancellation rate KPIs, and cuisine popularity progress bars.
- **`AdminUsersPage.tsx`**: Paged table with instant keyword search (name, email, phone), role filtering, status pills, and activation/deactivation confirmation modals with admin lockout protection alerts.
- **`AdminRestaurantsPage.tsx`**: Tabbed interface (`All`, `Pending Review`, `Approved`, `Rejected`), restaurant detail cards with owner contacts, and one-click verification and activation toggles.
- **`AdminOrdersPage.tsx`**: Global logistics dashboard with live status badges, payment indicators, direct links to order tracking, and delivery dispatch controls.
- **`AdminFoodItemsPage.tsx`**: Comprehensive menu moderation interface with dietary badges (Veg/Non-Veg), price display, restaurant tags, and availability switches.

---

## 7. Verification & Automated Testing

### 1. Backend Integration Tests (`AdminDashboardIntegrationTest.java`)
19 comprehensive integration tests verifying:
- Executive overview calculation accuracy
- Revenue time-series grouping by daily, weekly, monthly, and yearly periods
- Strict exclusion of unpaid or failed payments from revenue
- Separate computation of delivered revenue vs reconciliation exposure
- Order distribution across all 8 lifecycle states
- User analytics with role breakdown and new registrations count
- Restaurant analytics with cuisine aggregation
- Chronological recent activity log ordering
- User pagination, search, and role filtering
- Successful activation and deactivation of customer accounts
- **Sole Admin Lockout Protection**: Attempting to deactivate the only active admin throws HTTP 400 Bad Request
- **Role-Based Security**: Customer accounts receiving HTTP 403 / 401 when attempting to access `/api/admin/dashboard/**`
- Unauthenticated requests rejected with HTTP 401 Unauthorized

### 2. Full Test Suite Execution
- **Command**: `./mvnw.cmd test`
- **Results**: `BUILD SUCCESS`
- **Total Tests**: **95 tests passed, 0 failures, 0 errors, 0 skipped**
  - Module 1 (Auth): 12 tests passed
  - Module 2 (Restaurant): 10 tests passed
  - Module 3 (Menu): 10 tests passed
  - Module 4 (Cart): 10 tests passed
  - Module 5 (Order): 12 tests passed
  - Module 6 (Payment): 10 tests passed
  - Module 7 (Tracking): 12 tests passed
  - Module 8 (Admin Dashboard): 19 tests passed

### 3. Frontend Production Build
- **Command**: `npm run build`
- **Result**: `✓ built in 3.62s`
- **Asset Size**: `dist/assets/index-D-FVICJv.js` (412.44 kB / gzip: 115.40 kB), 0 TypeScript or JSX errors.

### 4. Live Verification Output
```text
======================================================
CRAVELY MODULE 8 - LIVE API END-TO-END VERIFICATION
======================================================

[1] Authenticating as Admin (admin@cravely.com)...
 SUCCESS: Admin authenticated. Token received (length: 240).

[2] Testing GET /api/admin/dashboard/overview...
 SUCCESS: Overview retrieved.
   Total Users: 50
   Total Restaurants: 12
   Total Food Items: 12
   Total Orders: 10
   Total Revenue: INR 2100.00
   Delivered Revenue: INR 0.00
   Reconciliation Exposure: INR 0.00

[3] Testing GET /api/admin/dashboard/revenue?period=monthly...
 SUCCESS: Revenue analytics retrieved.
   Period: monthly
   Total Revenue: 2100.00
   Data Points: 1

[4] Testing GET /api/admin/dashboard/orders...
 SUCCESS: Order analytics retrieved.
   Total Orders: 10
   Placed: 6, Delivered: 1, Cancelled: 1

[5] Testing GET /api/admin/dashboard/users...
 SUCCESS: User analytics retrieved.
   Total Users: 50
   Customers: 26, Owners: 23, Admins: 1

[6] Testing GET /api/admin/dashboard/restaurants...
 SUCCESS: Restaurant analytics retrieved.
   Total: 12
   Pending: 1, Approved: 10
   Cuisines Tracked: 7

[7] Testing GET /api/admin/dashboard/recent-activity?limit=5...
 SUCCESS: Recent activity feed retrieved. Count: 5
   Latest Event: [ORDER_STATUS_CHANGED] Order CRV-20261104-86963 DELIVERED by Live Owner

[8] Testing GET /api/admin/users?page=0&size=5...
 SUCCESS: Paged users retrieved.
   Total Elements: 50, Total Pages: 10

[9] Testing Security: Unauthenticated access to /api/admin/dashboard/overview...
 SUCCESS: Access denied with HTTP status 401 as expected.

======================================================
ALL LIVE VERIFICATIONS PASSED CLEANLY!
======================================================
```

---

## 8. How to Run the Application

### Backend (Port 8081)
```powershell
$env:JAVA_HOME = "C:\Users\riyac\Downloads\jdk-22_windows-x64_bin\jdk-22.0.1"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd backend\cravely-backend
.\mvnw.cmd spring-boot:run
```

### Frontend (Port 5173)
```powershell
cd frontend
npm run dev
```

### Default Admin Credentials
- **Email**: `admin@cravely.com`
- **Password**: `Password123!`
- **Admin Portal**: `http://localhost:5173/admin/dashboard`

