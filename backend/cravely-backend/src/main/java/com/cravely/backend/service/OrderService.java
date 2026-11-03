package com.cravely.backend.service;

import com.cravely.backend.dto.*;
import com.cravely.backend.entity.*;
import com.cravely.backend.exception.ResourceNotFoundException;
import com.cravely.backend.repository.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final FoodItemRepository foodItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartRepository cartRepository,
                        FoodItemRepository foodItemRepository,
                        RestaurantRepository restaurantRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.foodItemRepository = foodItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
    }

    // ==========================================
    // 1. CUSTOMER ORDER OPERATIONS
    // ==========================================

    @Transactional
    public OrderResponse placeOrder(String customerEmail, PlaceOrderRequest request) {
        User customer = getCustomer(customerEmail);

        Cart cart = cartRepository.findByUserId(customer.getId())
                .orElseThrow(() -> new IllegalStateException("Cannot place order with an empty cart"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot place order with an empty cart");
        }

        Restaurant restaurant = cart.getRestaurant();
        if (restaurant == null || restaurant.getStatus() != RestaurantStatus.APPROVED || !Boolean.TRUE.equals(restaurant.getActive())) {
            throw new IllegalStateException("Restaurant is currently inactive or unapproved");
        }

        String orderNumber = generateUniqueOrderNumber();

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setOrderStatus(OrderStatus.PLACED);
        order.setPaymentStatus(PaymentStatus.PENDING);

        // Snapshot delivery details
        order.setDeliveryAddress(request.getDeliveryAddress().trim());
        order.setDeliveryCity(request.getDeliveryCity().trim());
        order.setDeliveryState(request.getDeliveryState().trim());
        order.setDeliveryPincode(request.getDeliveryPincode().trim());
        order.setCustomerPhone(request.getCustomerPhone().trim());
        order.setCustomerNote(request.getCustomerNote() != null ? request.getCustomerNote().trim() : null);

        BigDecimal subtotal = BigDecimal.ZERO;

        // Process cart items and create historical snapshot
        for (CartItem cartItem : cart.getItems()) {
            FoodItem liveFoodItem = foodItemRepository.findById(cartItem.getFoodItem().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + cartItem.getFoodItem().getId()));

            if (!Boolean.TRUE.equals(liveFoodItem.getAvailable())) {
                throw new IllegalStateException("Food item '" + liveFoodItem.getName() + "' is currently unavailable");
            }

            if (liveFoodItem.getCategory() == null || !Boolean.TRUE.equals(liveFoodItem.getCategory().getActive())) {
                throw new IllegalStateException("Category for food item '" + liveFoodItem.getName() + "' is currently inactive");
            }

            BigDecimal unitPrice = liveFoodItem.getPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(itemTotal);

            OrderItem orderItem = new OrderItem(
                    order,
                    liveFoodItem,
                    liveFoodItem.getName(),
                    liveFoodItem.getDescription(),
                    liveFoodItem.getFoodType(),
                    unitPrice,
                    cartItem.getQuantity(),
                    itemTotal
            );

            order.addItem(orderItem);
        }

        // Configure charges (kept at 0.00 until pricing/payment module)
        BigDecimal deliveryFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(deliveryFee).add(taxAmount).subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);

        order.setSubtotal(subtotal);
        order.setDeliveryFee(deliveryFee);
        order.setTaxAmount(taxAmount);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        // Transactional cart clear: Cart is only cleared after successful order creation
        cart.clearItems();
        cartRepository.save(cart);

        return OrderResponse.fromEntity(savedOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(String customerEmail, OrderStatus status,
                                                  LocalDateTime startDate, LocalDateTime endDate,
                                                  Pageable pageable) {
        User customer = getCustomer(customerEmail);

        Page<Order> orders;
        if (status != null && startDate != null && endDate != null) {
            orders = orderRepository.findByCustomerIdAndOrderStatusAndCreatedAtBetween(customer.getId(), status, startDate, endDate, pageable);
        } else if (status != null) {
            orders = orderRepository.findByCustomerIdAndOrderStatus(customer.getId(), status, pageable);
        } else if (startDate != null && endDate != null) {
            orders = orderRepository.findByCustomerIdAndCreatedAtBetween(customer.getId(), startDate, endDate, pageable);
        } else {
            orders = orderRepository.findByCustomerId(customer.getId(), pageable);
        }

        return orders.map(OrderSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public OrderResponse getCustomerOrderDetails(String customerEmail, Long orderId) {
        User customer = getCustomer(customerEmail);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        return OrderResponse.fromEntity(order);
    }

    @Transactional
    public OrderResponse cancelCustomerOrder(String customerEmail, Long orderId, CancelOrderRequest request) {
        User customer = getCustomer(customerEmail);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        // Permitted only while order is PLACED or ACCEPTED
        if (order.getOrderStatus() != OrderStatus.PLACED && order.getOrderStatus() != OrderStatus.ACCEPTED) {
            throw new IllegalStateException("Order cannot be cancelled in status '" + order.getOrderStatus()
                    + "'. Cancellation is only permitted when the order is PLACED or ACCEPTED.");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(request.getReason() != null ? request.getReason().trim() : "Cancelled by customer");

        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    // ==========================================
    // 2. RESTAURANT OWNER ORDER OPERATIONS
    // ==========================================

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getRestaurantOrders(String ownerEmail, Long restaurantId, OrderStatus status, Pageable pageable) {
        validateOwnerRestaurantAccess(ownerEmail, restaurantId);

        Page<Order> orders;
        if (status != null) {
            orders = orderRepository.findByRestaurantIdAndOrderStatus(restaurantId, status, pageable);
        } else {
            orders = orderRepository.findByRestaurantId(restaurantId, pageable);
        }

        return orders.map(OrderSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOwnerOrderDetails(String ownerEmail, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        validateOwnerRestaurantAccess(ownerEmail, order.getRestaurant().getId());

        return OrderResponse.fromEntity(order);
    }

    @Transactional
    public OrderResponse acceptOrder(String ownerEmail, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        validateOwnerRestaurantAccess(ownerEmail, order.getRestaurant().getId());

        if (order.getOrderStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException("Only orders in PLACED status can be accepted. Current status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.ACCEPTED);
        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    @Transactional
    public OrderResponse rejectOrder(String ownerEmail, Long orderId, RejectOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        validateOwnerRestaurantAccess(ownerEmail, order.getRestaurant().getId());

        if (order.getOrderStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException("Only orders in PLACED status can be rejected. Current status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.REJECTED);
        order.setRejectionReason(request.getReason() != null ? request.getReason().trim() : "Rejected by restaurant");
        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    @Transactional
    public OrderResponse markOrderPreparing(String ownerEmail, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        validateOwnerRestaurantAccess(ownerEmail, order.getRestaurant().getId());

        if (order.getOrderStatus() != OrderStatus.ACCEPTED) {
            throw new IllegalStateException("Only orders in ACCEPTED status can move to PREPARING. Current status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.PREPARING);
        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    @Transactional
    public OrderResponse markOrderReady(String ownerEmail, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        validateOwnerRestaurantAccess(ownerEmail, order.getRestaurant().getId());

        if (order.getOrderStatus() != OrderStatus.PREPARING) {
            throw new IllegalStateException("Only orders in PREPARING status can move to READY_FOR_PICKUP. Current status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.READY_FOR_PICKUP);
        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    // ==========================================
    // 3. ADMIN ORDER OPERATIONS
    // ==========================================

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAllOrders(OrderStatus status, String orderNumber,
                                                  String customerQuery, Long restaurantId,
                                                  Pageable pageable) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("orderStatus"), status));
            }
            if (orderNumber != null && !orderNumber.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("orderNumber")), "%" + orderNumber.trim().toLowerCase() + "%"));
            }
            if (restaurantId != null) {
                predicates.add(cb.equal(root.get("restaurant").get("id"), restaurantId));
            }
            if (customerQuery != null && !customerQuery.trim().isEmpty()) {
                String pattern = "%" + customerQuery.trim().toLowerCase() + "%";
                Join<Order, User> customerJoin = root.join("customer");
                predicates.add(cb.or(
                        cb.like(cb.lower(customerJoin.get("fullName")), pattern),
                        cb.like(cb.lower(customerJoin.get("email")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return orderRepository.findAll(spec, pageable).map(OrderSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public OrderResponse getAdminOrderDetails(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return OrderResponse.fromEntity(order);
    }

    // ==========================================
    // HELPER & VALIDATION METHODS
    // ==========================================

    private User getCustomer(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer account not found"));

        if (user.getRole() != Role.CUSTOMER) {
            throw new AccessDeniedException("Only customers are authorized to perform this operation");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("Customer account is deactivated");
        }

        return user;
    }

    private void validateOwnerRestaurantAccess(String ownerEmail, Long restaurantId) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant owner account not found"));

        if (owner.getRole() != Role.RESTAURANT_OWNER) {
            throw new AccessDeniedException("Only restaurant owners are authorized to perform this operation");
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + restaurantId));

        if (!restaurant.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("You do not have permission to manage orders for this restaurant");
        }
    }

    private String generateUniqueOrderNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 0; i < 20; i++) {
            int randomNum = ThreadLocalRandom.current().nextInt(100000);
            String candidate = String.format("CRV-%s-%05d", datePart, randomNum);
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        return "CRV-" + datePart + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }
}
