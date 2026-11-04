import type {
  OrderStatusHistoryResponse,
  OrderSummaryResponse,
  OrderTrackingResponse,
  PageResponse,
} from '../types/tracking'
import { API_BASE, getAuthHeaders } from '../config/api'

export const orderService = {
  /**
   * Fetch customer order tracking details with visual timeline and live status.
   */
  async trackOrder(orderId: number, token?: string): Promise<OrderTrackingResponse> {
    const res = await fetch(`${API_BASE}/orders/${orderId}/tracking`, {
      method: 'GET',
      headers: getAuthHeaders(token),
    })

    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to fetch order tracking details')
    }
    return data.data
  },

  /**
   * Fetch customer order status history log.
   */
  async getOrderHistory(orderId: number, token?: string): Promise<OrderStatusHistoryResponse[]> {
    const res = await fetch(`${API_BASE}/orders/${orderId}/tracking/history`, {
      method: 'GET',
      headers: getAuthHeaders(token),
    })

    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to fetch order status history')
    }
    return data.data
  },

  /**
   * Fetch paginated order history for customer.
   */
  async getMyOrders(
    page = 0,
    size = 10,
    token?: string
  ): Promise<PageResponse<OrderSummaryResponse>> {
    const res = await fetch(`${API_BASE}/orders?page=${page}&size=${size}`, {
      method: 'GET',
      headers: getAuthHeaders(token),
    })

    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to fetch customer orders')
    }
    return data.data
  },

  /**
   * Cancel an order while it is in PLACED or ACCEPTED status.
   */
  async cancelOrder(orderId: number, reason: string, token?: string): Promise<void> {
    const res = await fetch(`${API_BASE}/orders/${orderId}/cancel`, {
      method: 'PATCH',
      headers: getAuthHeaders(token),
      body: JSON.stringify({ cancellationReason: reason }),
    })

    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to cancel order')
    }
    return data.data
  },

  /**
   * Owner tracking endpoint.
   */
  async trackOwnerOrder(orderId: number, token?: string): Promise<OrderTrackingResponse> {
    const res = await fetch(`${API_BASE}/owner/orders/${orderId}/tracking`, {
      method: 'GET',
      headers: getAuthHeaders(token),
    })

    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to fetch owner order tracking')
    }
    return data.data
  },

  /**
   * Admin tracking endpoint.
   */
  async trackAdminOrder(orderId: number, token?: string): Promise<OrderTrackingResponse> {
    const res = await fetch(`${API_BASE}/admin/orders/${orderId}/tracking`, {
      method: 'GET',
      headers: getAuthHeaders(token),
    })

    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to fetch admin order tracking')
    }
    return data.data
  },
}

