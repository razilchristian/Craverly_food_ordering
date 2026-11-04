package com.cravely.backend.repository;

import com.cravely.backend.entity.OrderStatus;
import com.cravely.backend.entity.Payment;
import com.cravely.backend.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findFirstByOrderIdAndPaymentStatusOrderByCreatedAtDesc(Long orderId, PaymentStatus status);

    boolean existsByOrderIdAndPaymentStatus(Long orderId, PaymentStatus status);

    long countByPaymentStatus(PaymentStatus paymentStatus);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentStatus = :status")
    BigDecimal sumAmountByPaymentStatus(@Param("status") PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentStatus = :status AND p.createdAt >= :startDate")
    BigDecimal sumAmountByPaymentStatusAndCreatedAtAfter(@Param("status") PaymentStatus status, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p JOIN p.order o WHERE p.paymentStatus = :status AND o.orderStatus = :orderStatus")
    BigDecimal sumAmountByPaymentStatusAndOrderStatus(@Param("status") PaymentStatus status, @Param("orderStatus") OrderStatus orderStatus);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p JOIN p.order o WHERE p.paymentStatus = :status AND o.orderStatus IN :orderStatuses")
    BigDecimal sumAmountByPaymentStatusAndOrderStatusIn(@Param("status") PaymentStatus status, @Param("orderStatuses") Collection<OrderStatus> orderStatuses);

    List<Payment> findByPaymentStatusAndCreatedAtBetweenOrderByCreatedAtAsc(PaymentStatus status, LocalDateTime start, LocalDateTime end);

    List<Payment> findTop10ByPaymentStatusOrderByCreatedAtDesc(PaymentStatus status);
}
