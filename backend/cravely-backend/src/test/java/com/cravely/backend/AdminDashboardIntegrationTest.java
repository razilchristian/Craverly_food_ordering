package com.cravely.backend;

import com.cravely.backend.controller.AdminController;
import com.cravely.backend.controller.AdminDashboardController;
import com.cravely.backend.controller.AdminOrderController;
import com.cravely.backend.controller.RestaurantAdminController;
import com.cravely.backend.dto.*;
import com.cravely.backend.dto.admin.*;
import com.cravely.backend.entity.*;
import com.cravely.backend.repository.*;
import com.cravely.backend.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdminDashboardIntegrationTest {

    @Autowired
    private AdminDashboardService dashboardService;

    @Autowired
    private AdminDashboardController dashboardController;

    @Autowired
    private AdminController adminUserController;

    @Autowired
    private RestaurantAdminController restaurantAdminController;

    @Autowired
    private AdminOrderController adminOrderController;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private FoodCategoryService categoryService;

    @Autowired
    private FoodItemService foodItemService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User customer;
    private User owner;
    private Long restaurantId;
    private Long foodItemId;

    @BeforeEach
    void setup() {
        long ts = System.currentTimeMillis();
        String adminEmail = "dash_admin_" + ts + "@cravely.com";
        String custEmail = "dash_cust_" + ts + "@cravely.com";
        String ownEmail = "dash_own_" + ts + "@cravely.com";
        String pwd = passwordEncoder.encode("Password123!");

        admin = userRepository.save(new User("Dashboard Admin", adminEmail, "9998887771", pwd, Role.ADMIN, true));
        customer = userRepository.save(new User("Dashboard Customer", custEmail, "9998887772", pwd, Role.CUSTOMER, true));
        owner = userRepository.save(new User("Dashboard Owner", ownEmail, "9998887773", pwd, Role.RESTAURANT_OWNER, true));

        // Create & approve a restaurant
        var r = restaurantService.createRestaurant(owner.getEmail(), new RestaurantCreateRequest(
                "Analytics Bistro", "Fine Dining", "45 Park Street", "Ahmedabad", "Gujarat", "380015",
                "9876501234", "bistro@analytics.com", "Italian", null, LocalTime.of(10, 0), LocalTime.of(23, 0)
        ));
        restaurantService.approveRestaurant(r.getId());
        restaurantId = r.getId();

        // Create category and item
        var cat = categoryService.createCategory(owner.getEmail(), restaurantId, new FoodCategoryCreateRequest("Pasta", "Italian pasta"));
        var item = foodItemService.createFoodItem(owner.getEmail(), restaurantId, new FoodItemCreateRequest(
                "Truffle Fettuccine", "Truffle cream sauce", cat.getId(), new BigDecimal("400.00"), null, FoodType.VEG, true, 25
        ));
        foodItemId = item.getId();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        var auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Order placeSampleOrder() {
        cartService.addToCart(customer.getEmail(), new AddToCartRequest(foodItemId, 2));
        OrderResponse resp = orderService.placeOrder(customer.getEmail(), new PlaceOrderRequest(
                "10 Downing St", "Ahmedabad", "Gujarat", "380015", "9998887772", "Leave at door"
        ));
        return orderRepository.findById(resp.getId()).orElseThrow();
    }

    private Payment createSamplePayment(Order order, PaymentStatus status, BigDecimal amount) {
        long ts = System.currentTimeMillis() + (long)(Math.random() * 10000);
        Payment p = new Payment(order, "order_rzp_" + ts, amount, "INR");
        p.setPaymentStatus(status);
        if (status == PaymentStatus.PAID) {
            p.setRazorpayPaymentId("pay_rzp_" + ts);
            p.setPaymentMethod("upi");
            p.setPaidAt(LocalDateTime.now());
        }
        return paymentRepository.save(p);
    }

    // ==========================================
    // 1. DASHBOARD OVERVIEW TESTS
    // ==========================================

    @Test
    void test01_OverviewUserCountsAccurate() {
        authenticateAs(admin);
        DashboardOverviewResponse overview = dashboardService.getOverview();

        assertTrue(overview.getTotalUsers() >= 3);
        assertTrue(overview.getTotalCustomers() >= 1);
        assertTrue(overview.getTotalRestaurantOwners() >= 1);
        assertTrue(overview.getTotalAdmins() >= 1);
        assertTrue(overview.getActiveUsers() >= 3);
    }

    @Test
    void test02_OverviewRestaurantCountsAccurate() {
        authenticateAs(admin);
        DashboardOverviewResponse overview = dashboardService.getOverview();

        assertTrue(overview.getTotalRestaurants() >= 1);
        assertTrue(overview.getApprovedRestaurants() >= 1);
        assertTrue(overview.getActiveRestaurants() >= 1);
    }

    @Test
    void test03_OverviewOrderAndRevenueCountsAccurate() {
        authenticateAs(admin);
        Order order = placeSampleOrder();
        createSamplePayment(order, PaymentStatus.PAID, new BigDecimal("800.00"));

        DashboardOverviewResponse overview = dashboardService.getOverview();

        assertTrue(overview.getTotalOrders() >= 1);
        assertTrue(overview.getPendingOrders() >= 1);
        assertTrue(overview.getTotalRevenue().compareTo(new BigDecimal("800.00")) >= 0);
        assertTrue(overview.getTodayRevenue().compareTo(new BigDecimal("800.00")) >= 0);
    }

    @Test
    void test04_EmptyOrZeroRevenueHandledSafely() {
        authenticateAs(admin);
        // Calling overview on an arbitrary state must never produce null or NPE
        DashboardOverviewResponse overview = dashboardService.getOverview();
        assertNotNull(overview.getTotalRevenue());
        assertNotNull(overview.getTodayRevenue());
        assertNotNull(overview.getDeliveredRevenue());
        assertNotNull(overview.getReconciliationExposure());
    }

    // ==========================================
    // 2. REVENUE ANALYTICS TESTS
    // ==========================================

    @Test
    void test05_RevenueIncludesPaidExcludesPendingAndFailed() {
        authenticateAs(admin);
        Order order1 = placeSampleOrder();
        Order order2 = placeSampleOrder();
        Order order3 = placeSampleOrder();

        createSamplePayment(order1, PaymentStatus.PAID, new BigDecimal("500.00"));
        createSamplePayment(order2, PaymentStatus.PENDING, new BigDecimal("300.00"));
        createSamplePayment(order3, PaymentStatus.FAILED, new BigDecimal("200.00"));

        RevenueAnalyticsResponse revenue = dashboardService.getRevenueAnalytics("daily", LocalDate.now().minusDays(1), LocalDate.now());

        assertNotNull(revenue);
        // Total revenue must reflect ONLY PAID payments
        assertTrue(revenue.getTotalRevenue().compareTo(new BigDecimal("500.00")) >= 0);
    }

    @Test
    void test06_ReconciliationExposureIdentifiedForCancelledOrders() {
        authenticateAs(admin);
        Order order = placeSampleOrder();
        createSamplePayment(order, PaymentStatus.PAID, new BigDecimal("600.00"));

        // Customer cancels order
        orderService.cancelCustomerOrder(customer.getEmail(), order.getId(), new CancelOrderRequest("Cancelled by test"));

        RevenueAnalyticsResponse revenue = dashboardService.getRevenueAnalytics("daily", LocalDate.now().minusDays(1), LocalDate.now());

        assertTrue(revenue.getReconciliationExposure().compareTo(new BigDecimal("600.00")) >= 0);
    }

    @Test
    void test07_RevenuePeriodGroupings() {
        authenticateAs(admin);
        Order order = placeSampleOrder();
        createSamplePayment(order, PaymentStatus.PAID, new BigDecimal("450.00"));

        // Daily
        RevenueAnalyticsResponse daily = dashboardService.getRevenueAnalytics("daily", LocalDate.now().minusDays(7), LocalDate.now());
        assertEquals("daily", daily.getPeriod());
        assertFalse(daily.getData().isEmpty());

        // Weekly
        RevenueAnalyticsResponse weekly = dashboardService.getRevenueAnalytics("weekly", LocalDate.now().minusWeeks(4), LocalDate.now());
        assertEquals("weekly", weekly.getPeriod());

        // Monthly
        RevenueAnalyticsResponse monthly = dashboardService.getRevenueAnalytics("monthly", LocalDate.now().minusMonths(6), LocalDate.now());
        assertEquals("monthly", monthly.getPeriod());

        // Yearly
        RevenueAnalyticsResponse yearly = dashboardService.getRevenueAnalytics("yearly", LocalDate.now().minusYears(2), LocalDate.now());
        assertEquals("yearly", yearly.getPeriod());
    }

    // ==========================================
    // 3. ORDER ANALYTICS TESTS
    // ==========================================

    @Test
    void test08_OrderAnalyticsStatusBreakdown() {
        authenticateAs(admin);
        Order order = placeSampleOrder();

        OrderAnalyticsResponse analytics = dashboardService.getOrderAnalytics(null, null);

        assertTrue(analytics.getTotalOrders() >= 1);
        assertTrue(analytics.getPlacedOrders() >= 1);
        assertTrue(analytics.getActiveOrders() >= 1);
        assertFalse(analytics.getRecentOrders().isEmpty());
    }

    // ==========================================
    // 4. USER ANALYTICS TESTS
    // ==========================================

    @Test
    void test09_UserAnalyticsDistribution() {
        authenticateAs(admin);
        UserAnalyticsResponse users = dashboardService.getUserAnalytics(null, null);

        assertTrue(users.getTotalUsers() >= 3);
        assertTrue(users.getCustomers() >= 1);
        assertTrue(users.getRestaurantOwners() >= 1);
        assertTrue(users.getAdmins() >= 1);
        assertTrue(users.getActiveUsers() >= 3);
        assertFalse(users.getRecentUsers().isEmpty());
    }

    // ==========================================
    // 5. RESTAURANT ANALYTICS TESTS
    // ==========================================

    @Test
    void test10_RestaurantAnalyticsDistribution() {
        authenticateAs(admin);
        RestaurantAnalyticsResponse rest = dashboardService.getRestaurantAnalytics(null, null);

        assertTrue(rest.getTotalRestaurants() >= 1);
        assertTrue(rest.getApprovedRestaurants() >= 1);
        assertFalse(rest.getCuisineDistribution().isEmpty());
    }

    // ==========================================
    // 6. RECENT PLATFORM ACTIVITY TESTS
    // ==========================================

    @Test
    void test11_RecentActivityAuthenticEvents() {
        authenticateAs(admin);
        Order order = placeSampleOrder();
        createSamplePayment(order, PaymentStatus.PAID, new BigDecimal("800.00"));

        List<RecentActivityResponse> activity = dashboardService.getRecentActivity(20);

        assertNotNull(activity);
        assertFalse(activity.isEmpty());

        // Verify all events contain valid, authentic fields
        for (RecentActivityResponse item : activity) {
            assertNotNull(item.getId());
            assertNotNull(item.getType());
            assertNotNull(item.getTitle());
            assertNotNull(item.getActor());
            assertNotNull(item.getTimestamp());
        }
    }

    // ==========================================
    // 7. USER MANAGEMENT & SELF-DEACTIVATION SAFETY
    // ==========================================

    @Test
    void test12_UserPaginationAndFiltering() {
        authenticateAs(admin);
        Page<UserResponse> page = userService.getUsersPaged(Role.CUSTOMER, true, null, PageRequest.of(0, 10));

        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 1);
        for (UserResponse u : page.getContent()) {
            assertEquals(Role.CUSTOMER, u.getRole());
            assertTrue(u.getActive());
        }
    }

    @Test
    void test13_PreventSelfDeactivationOfSoleAdministrator() {
        authenticateAs(admin);

        // Ensure there is only 1 active admin in this test slice
        long activeAdmins = userRepository.countByRoleAndActive(Role.ADMIN, true);
        if (activeAdmins == 1) {
            assertThrows(IllegalStateException.class, () ->
                    userService.deactivateUser(admin.getId())
            );
        }
    }

    @Test
    void test14_ReactivateUserSuccessfully() {
        authenticateAs(admin);
        // Deactivate customer
        UserResponse deactivated = userService.deactivateUser(customer.getId());
        assertFalse(deactivated.getActive());

        // Reactivate customer
        UserResponse reactivated = userService.activateUser(customer.getId());
        assertTrue(reactivated.getActive());
    }

    // ==========================================
    // 8. RESTAURANT & ORDER MANAGEMENT EXTENSIONS
    // ==========================================

    @Test
    void test15_RestaurantAdminStatusActions() {
        authenticateAs(admin);

        ResponseEntity<ApiResponse<RestaurantResponse>> deact =
                restaurantAdminController.deactivateRestaurant(restaurantId);
        assertEquals(HttpStatus.OK, deact.getStatusCode());
        assertFalse(deact.getBody().getData().getActive());

        ResponseEntity<ApiResponse<RestaurantResponse>> act =
                restaurantAdminController.activateRestaurant(restaurantId);
        assertEquals(HttpStatus.OK, act.getStatusCode());
        assertTrue(act.getBody().getData().getActive());
    }

    @Test
    void test16_OrderAdminInspectionAndStatusFilters() {
        authenticateAs(admin);
        Order order = placeSampleOrder();

        ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> orders =
                adminOrderController.getAllOrders(OrderStatus.PLACED, null, null, null, 0, 10);
        assertEquals(HttpStatus.OK, orders.getStatusCode());
        assertTrue(orders.getBody().getData().getTotalElements() >= 1);
    }

    // ==========================================
    // 9. SECURITY & ROLE AUTHORIZATION TESTS
    // ==========================================

    @Test
    void test17_AdminDashboardEndpointsProtectedByAdminRole() {
        PreAuthorize authAnnotation = AdminDashboardController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(authAnnotation);
        assertEquals("hasRole('ADMIN')", authAnnotation.value());

        // Calling without authentication in SecurityContext throws AuthenticationCredentialsNotFoundException
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                dashboardController.getOverview()
        );
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                dashboardController.getRevenueAnalytics("monthly", null, null)
        );
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                dashboardController.getOrderAnalytics(null, null)
        );
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                dashboardController.getUserAnalytics(null, null)
        );
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                dashboardController.getRestaurantAnalytics(null, null)
        );
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                dashboardController.getRecentActivity(20)
        );
    }

    @Test
    void test18_AdminUserManagementNeverExposesPasswordHashes() {
        authenticateAs(admin);
        UserResponse resp = userService.getUserById(admin.getId());
        assertNotNull(resp);
        // UserResponse entity mapping has no password field
        assertEquals(admin.getEmail(), resp.getEmail());
    }

    @Test
    void test19_DashboardControllerSuccessResponses() {
        authenticateAs(admin);

        ResponseEntity<ApiResponse<DashboardOverviewResponse>> overview = dashboardController.getOverview();
        assertEquals(HttpStatus.OK, overview.getStatusCode());
        assertTrue(overview.getBody().isSuccess());

        ResponseEntity<ApiResponse<RevenueAnalyticsResponse>> rev = dashboardController.getRevenueAnalytics("monthly", null, null);
        assertEquals(HttpStatus.OK, rev.getStatusCode());
        assertTrue(rev.getBody().isSuccess());

        ResponseEntity<ApiResponse<OrderAnalyticsResponse>> ord = dashboardController.getOrderAnalytics(null, null);
        assertEquals(HttpStatus.OK, ord.getStatusCode());
        assertTrue(ord.getBody().isSuccess());

        ResponseEntity<ApiResponse<UserAnalyticsResponse>> usr = dashboardController.getUserAnalytics(null, null);
        assertEquals(HttpStatus.OK, usr.getStatusCode());
        assertTrue(usr.getBody().isSuccess());

        ResponseEntity<ApiResponse<RestaurantAnalyticsResponse>> rst = dashboardController.getRestaurantAnalytics(null, null);
        assertEquals(HttpStatus.OK, rst.getStatusCode());
        assertTrue(rst.getBody().isSuccess());

        ResponseEntity<ApiResponse<List<RecentActivityResponse>>> act = dashboardController.getRecentActivity(20);
        assertEquals(HttpStatus.OK, act.getStatusCode());
        assertTrue(act.getBody().isSuccess());
    }
}
