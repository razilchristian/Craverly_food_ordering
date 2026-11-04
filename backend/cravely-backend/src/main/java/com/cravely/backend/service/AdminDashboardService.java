package com.cravely.backend.service;

import com.cravely.backend.dto.OrderSummaryResponse;
import com.cravely.backend.dto.UserResponse;
import com.cravely.backend.dto.admin.*;
import com.cravely.backend.entity.*;
import com.cravely.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final FoodItemRepository foodItemRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;

    public AdminDashboardService(
            UserRepository userRepository,
            RestaurantRepository restaurantRepository,
            FoodItemRepository foodItemRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            OrderStatusHistoryRepository statusHistoryRepository) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.foodItemRepository = foodItemRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    /**
     * Platform-wide high-level statistics calculated strictly from the database.
     */
    public DashboardOverviewResponse getOverview() {
        DashboardOverviewResponse res = new DashboardOverviewResponse();

        // User aggregates
        res.setTotalUsers(userRepository.count());
        res.setTotalCustomers(userRepository.countByRole(Role.CUSTOMER));
        res.setTotalRestaurantOwners(userRepository.countByRole(Role.RESTAURANT_OWNER));
        res.setTotalAdmins(userRepository.countByRole(Role.ADMIN));
        res.setActiveUsers(userRepository.countByActive(true));
        res.setDeactivatedUsers(userRepository.countByActive(false));

        // Restaurant aggregates
        res.setTotalRestaurants(restaurantRepository.count());
        res.setPendingRestaurants(restaurantRepository.countByStatus(RestaurantStatus.PENDING));
        res.setApprovedRestaurants(restaurantRepository.countByStatus(RestaurantStatus.APPROVED));
        res.setRejectedRestaurants(restaurantRepository.countByStatus(RestaurantStatus.REJECTED));
        res.setActiveRestaurants(restaurantRepository.countByActive(true));

        // Food item aggregates
        res.setTotalFoodItems(foodItemRepository.count());
        res.setAvailableFoodItems(foodItemRepository.countByAvailable(true));

        // Order aggregates
        res.setTotalOrders(orderRepository.count());
        res.setPendingOrders(orderRepository.countByOrderStatus(OrderStatus.PLACED));
        res.setDeliveredOrders(orderRepository.countByOrderStatus(OrderStatus.DELIVERED));
        res.setCancelledOrders(orderRepository.countByOrderStatus(OrderStatus.CANCELLED));
        res.setRejectedOrders(orderRepository.countByOrderStatus(OrderStatus.REJECTED));

        // Financial aggregates (only captured payments: PAID)
        BigDecimal totalRev = paymentRepository.sumAmountByPaymentStatus(PaymentStatus.PAID);
        res.setTotalRevenue(totalRev != null ? totalRev : BigDecimal.ZERO);

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        BigDecimal todayRev = paymentRepository.sumAmountByPaymentStatusAndCreatedAtAfter(PaymentStatus.PAID, startOfToday);
        res.setTodayRevenue(todayRev != null ? todayRev : BigDecimal.ZERO);

        BigDecimal deliveredRev = paymentRepository.sumAmountByPaymentStatusAndOrderStatus(PaymentStatus.PAID, OrderStatus.DELIVERED);
        res.setDeliveredRevenue(deliveredRev != null ? deliveredRev : BigDecimal.ZERO);

        BigDecimal exposure = paymentRepository.sumAmountByPaymentStatusAndOrderStatusIn(
                PaymentStatus.PAID, List.of(OrderStatus.CANCELLED, OrderStatus.REJECTED));
        res.setReconciliationExposure(exposure != null ? exposure : BigDecimal.ZERO);

        return res;
    }

    /**
     * Financial analytics grouped by daily, weekly, monthly, or yearly periods.
     * Revenue is derived strictly from captured payments (PAID).
     */
    public RevenueAnalyticsResponse getRevenueAnalytics(String periodParam, LocalDate startDate, LocalDate endDate) {
        String period = (periodParam != null && !periodParam.isBlank()) ? periodParam.trim().toLowerCase() : "monthly";

        LocalDate end = (endDate != null) ? endDate : LocalDate.now();
        LocalDate start = startDate;
        if (start == null) {
            switch (period) {
                case "daily":
                    start = end.minusDays(29); // 30 days
                    break;
                case "weekly":
                    start = end.minusWeeks(11); // 12 weeks
                    break;
                case "yearly":
                    start = end.minusYears(4); // 5 years
                    break;
                case "monthly":
                default:
                    start = end.minusMonths(11); // 12 months
                    break;
            }
        }

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

        List<Payment> paidPayments = paymentRepository
                .findByPaymentStatusAndCreatedAtBetweenOrderByCreatedAtAsc(PaymentStatus.PAID, startDateTime, endDateTime);

        RevenueAnalyticsResponse response = new RevenueAnalyticsResponse();
        response.setPeriod(period);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal deliveredRevenue = BigDecimal.ZERO;
        BigDecimal exposure = BigDecimal.ZERO;

        // Group into linked hash map to preserve chronological order
        Map<String, BucketAccumulator> buckets = new LinkedHashMap<>();

        DateTimeFormatter dailyKeyFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dailyLabelFormat = DateTimeFormatter.ofPattern("dd MMM yyyy");

        DateTimeFormatter monthKeyFormat = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter monthLabelFormat = DateTimeFormatter.ofPattern("MMM yyyy");

        DateTimeFormatter yearFormat = DateTimeFormatter.ofPattern("yyyy");

        for (Payment payment : paidPayments) {
            BigDecimal amt = payment.getAmount();
            totalRevenue = totalRevenue.add(amt);

            if (payment.getOrder() != null) {
                OrderStatus oStatus = payment.getOrder().getOrderStatus();
                if (oStatus == OrderStatus.DELIVERED) {
                    deliveredRevenue = deliveredRevenue.add(amt);
                } else if (oStatus == OrderStatus.CANCELLED || oStatus == OrderStatus.REJECTED) {
                    exposure = exposure.add(amt);
                }
            }

            LocalDateTime dt = payment.getCreatedAt();
            String key;
            String label;

            switch (period) {
                case "daily":
                    key = dt.format(dailyKeyFormat);
                    label = dt.format(dailyLabelFormat);
                    break;
                case "weekly":
                    int week = dt.get(WeekFields.ISO.weekOfWeekBasedYear());
                    int year = dt.get(WeekFields.ISO.weekBasedYear());
                    key = String.format("%04d-W%02d", year, week);
                    label = "W" + week + " " + year;
                    break;
                case "yearly":
                    key = dt.format(yearFormat);
                    label = key;
                    break;
                case "monthly":
                default:
                    key = dt.format(monthKeyFormat);
                    label = dt.format(monthLabelFormat);
                    break;
            }

            BucketAccumulator acc = buckets.computeIfAbsent(key, k -> new BucketAccumulator(label, key));
            acc.revenue = acc.revenue.add(amt);
            acc.orders += 1;
        }

        response.setTotalRevenue(totalRevenue);
        response.setDeliveredRevenue(deliveredRevenue);
        response.setReconciliationExposure(exposure);

        List<RevenueAnalyticsResponse.RevenueDataPoint> dataPoints = buckets.values().stream()
                .map(b -> new RevenueAnalyticsResponse.RevenueDataPoint(b.label, b.key, b.revenue, b.orders))
                .collect(Collectors.toList());
        response.setData(dataPoints);

        return response;
    }

    private static class BucketAccumulator {
        String label;
        String key;
        BigDecimal revenue = BigDecimal.ZERO;
        long orders = 0;

        BucketAccumulator(String label, String key) {
            this.label = label;
            this.key = key;
        }
    }

    /**
     * Order statistics and status distribution.
     */
    public OrderAnalyticsResponse getOrderAnalytics(LocalDate startDate, LocalDate endDate) {
        OrderAnalyticsResponse res = new OrderAnalyticsResponse();

        if (startDate != null || endDate != null) {
            LocalDate start = startDate != null ? startDate : LocalDate.of(2000, 1, 1);
            LocalDate end = endDate != null ? endDate : LocalDate.now();
            LocalDateTime startDateTime = start.atStartOfDay();
            LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

            res.setTotalOrders(orderRepository.countByCreatedAtBetween(startDateTime, endDateTime));
            res.setPlacedOrders(orderRepository.countByOrderStatusAndCreatedAtBetween(OrderStatus.PLACED, startDateTime, endDateTime));
            res.setAcceptedOrders(orderRepository.countByOrderStatusAndCreatedAtBetween(OrderStatus.ACCEPTED, startDateTime, endDateTime));
            res.setPreparingOrders(orderRepository.countByOrderStatusAndCreatedAtBetween(OrderStatus.PREPARING, startDateTime, endDateTime));
            res.setReadyForPickupOrders(orderRepository.countByOrderStatusAndCreatedAtBetween(OrderStatus.READY_FOR_PICKUP, startDateTime, endDateTime));
            res.setOutForDeliveryOrders(orderRepository.countByOrderStatusAndCreatedAtBetween(OrderStatus.OUT_FOR_DELIVERY, startDateTime, endDateTime));
            res.setDeliveredOrders(orderRepository.countByOrderStatusAndCreatedAtBetween(OrderStatus.DELIVERED, startDateTime, endDateTime));
            res.setCancelledOrders(orderRepository.countByOrderStatusAndCreatedAtBetween(OrderStatus.CANCELLED, startDateTime, endDateTime));
            res.setRejectedOrders(orderRepository.countByOrderStatusAndCreatedAtBetween(OrderStatus.REJECTED, startDateTime, endDateTime));
        } else {
            res.setTotalOrders(orderRepository.count());
            res.setPlacedOrders(orderRepository.countByOrderStatus(OrderStatus.PLACED));
            res.setAcceptedOrders(orderRepository.countByOrderStatus(OrderStatus.ACCEPTED));
            res.setPreparingOrders(orderRepository.countByOrderStatus(OrderStatus.PREPARING));
            res.setReadyForPickupOrders(orderRepository.countByOrderStatus(OrderStatus.READY_FOR_PICKUP));
            res.setOutForDeliveryOrders(orderRepository.countByOrderStatus(OrderStatus.OUT_FOR_DELIVERY));
            res.setDeliveredOrders(orderRepository.countByOrderStatus(OrderStatus.DELIVERED));
            res.setCancelledOrders(orderRepository.countByOrderStatus(OrderStatus.CANCELLED));
            res.setRejectedOrders(orderRepository.countByOrderStatus(OrderStatus.REJECTED));
        }

        res.setActiveOrders(
                res.getPlacedOrders() +
                res.getAcceptedOrders() +
                res.getPreparingOrders() +
                res.getReadyForPickupOrders() +
                res.getOutForDeliveryOrders()
        );

        List<OrderSummaryResponse> recent = orderRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(OrderSummaryResponse::fromEntity)
                .collect(Collectors.toList());
        res.setRecentOrders(recent);

        return res;
    }

    /**
     * User registration distribution and activity metrics.
     */
    public UserAnalyticsResponse getUserAnalytics(LocalDate startDate, LocalDate endDate) {
        UserAnalyticsResponse res = new UserAnalyticsResponse();

        res.setTotalUsers(userRepository.count());
        res.setCustomers(userRepository.countByRole(Role.CUSTOMER));
        res.setRestaurantOwners(userRepository.countByRole(Role.RESTAURANT_OWNER));
        res.setAdmins(userRepository.countByRole(Role.ADMIN));
        res.setActiveUsers(userRepository.countByActive(true));
        res.setDeactivatedUsers(userRepository.countByActive(false));

        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        res.setNewUsersInPeriod(userRepository.countByCreatedAtBetween(start.atStartOfDay(), end.atTime(LocalTime.MAX)));

        List<UserResponse> recent = userRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
        res.setRecentUsers(recent);

        return res;
    }

    /**
     * Restaurant approval and cuisine distribution statistics.
     */
    public RestaurantAnalyticsResponse getRestaurantAnalytics(LocalDate startDate, LocalDate endDate) {
        RestaurantAnalyticsResponse res = new RestaurantAnalyticsResponse();

        res.setTotalRestaurants(restaurantRepository.count());
        res.setPendingApprovals(restaurantRepository.countByStatus(RestaurantStatus.PENDING));
        res.setApprovedRestaurants(restaurantRepository.countByStatus(RestaurantStatus.APPROVED));
        res.setRejectedRestaurants(restaurantRepository.countByStatus(RestaurantStatus.REJECTED));
        res.setActiveRestaurants(restaurantRepository.countByActive(true));
        res.setInactiveRestaurants(restaurantRepository.countByActive(false));

        List<Object[]> cuisineGroups = restaurantRepository.countByCuisineTypeGroup();
        List<RestaurantAnalyticsResponse.CuisineDistribution> cuisines = new ArrayList<>();
        if (cuisineGroups != null) {
            for (Object[] row : cuisineGroups) {
                if (row.length >= 2 && row[0] != null) {
                    cuisines.add(new RestaurantAnalyticsResponse.CuisineDistribution(
                            (String) row[0],
                            ((Number) row[1]).longValue()
                    ));
                }
            }
        }
        res.setCuisineDistribution(cuisines);

        return res;
    }

    /**
     * Consolidated chronological audit trail of authentic platform events.
     */
    public List<RecentActivityResponse> getRecentActivity(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 50));
        List<RecentActivityResponse> events = new ArrayList<>();

        // 1. Order Status Transitions & History
        List<OrderStatusHistory> historyEntries = statusHistoryRepository.findTop20ByOrderByChangedAtDesc();
        for (OrderStatusHistory h : historyEntries) {
            String type = (h.getNewStatus() == OrderStatus.PLACED) ? "ORDER_PLACED" : "ORDER_STATUS_CHANGED";
            String title = "Order " + (h.getOrder() != null ? h.getOrder().getOrderNumber() : "") + " " + h.getNewStatus();
            String desc = h.getRemarks() != null ? h.getRemarks() : ("Order transitioned to " + h.getNewStatus());
            String actor = h.getChangedBy() != null ? h.getChangedBy().getFullName() : "System";
            String role = (h.getChangedBy() != null && h.getChangedBy().getRole() != null)
                    ? h.getChangedBy().getRole().name() : "SYSTEM";

            events.add(new RecentActivityResponse(
                    "osh-" + h.getId(),
                    type,
                    title,
                    desc,
                    actor,
                    role,
                    h.getChangedAt(),
                    h.getOrder() != null ? String.valueOf(h.getOrder().getId()) : null,
                    "ORDER"
            ));
        }

        // 2. Verified Successful Payments
        List<Payment> recentPayments = paymentRepository.findTop10ByPaymentStatusOrderByCreatedAtDesc(PaymentStatus.PAID);
        for (Payment p : recentPayments) {
            String actor = (p.getOrder() != null && p.getOrder().getCustomer() != null)
                    ? p.getOrder().getCustomer().getFullName() : "Customer";

            events.add(new RecentActivityResponse(
                    "pay-" + p.getId(),
                    "PAYMENT_CAPTURED",
                    "Payment of ₹" + p.getAmount() + " Received",
                    "Order " + (p.getOrder() != null ? p.getOrder().getOrderNumber() : "") + " verified via Razorpay",
                    actor,
                    "CUSTOMER",
                    p.getCreatedAt(),
                    String.valueOf(p.getId()),
                    "PAYMENT"
            ));
        }

        // 3. Restaurant Submissions & Approvals
        List<Restaurant> recentRestaurants = restaurantRepository.findTop10ByOrderByCreatedAtDesc();
        for (Restaurant r : recentRestaurants) {
            String type = (r.getStatus() == RestaurantStatus.APPROVED) ? "RESTAURANT_APPROVED" : "RESTAURANT_CREATED";
            String title = "Restaurant: " + r.getName();
            String desc = r.getCuisineType() + " in " + r.getCity() + " (" + r.getStatus() + ")";
            String actor = r.getOwner() != null ? r.getOwner().getFullName() : "Restaurant Owner";

            events.add(new RecentActivityResponse(
                    "rest-" + r.getId(),
                    type,
                    title,
                    desc,
                    actor,
                    "RESTAURANT_OWNER",
                    r.getCreatedAt(),
                    String.valueOf(r.getId()),
                    "RESTAURANT"
            ));
        }

        // 4. User Registrations
        List<User> recentUsers = userRepository.findTop10ByOrderByCreatedAtDesc();
        for (User u : recentUsers) {
            events.add(new RecentActivityResponse(
                    "user-" + u.getId(),
                    "USER_REGISTERED",
                    "New User: " + u.getFullName(),
                    "Registered as " + u.getRole().name() + " (" + u.getEmail() + ")",
                    u.getFullName(),
                    u.getRole().name(),
                    u.getCreatedAt(),
                    String.valueOf(u.getId()),
                    "USER"
            ));
        }

        // Sort descending by timestamp and take up to boundedLimit
        return events.stream()
                .filter(e -> e.getTimestamp() != null)
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(boundedLimit)
                .collect(Collectors.toList());
    }
}
