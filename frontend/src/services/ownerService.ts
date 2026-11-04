import { API_BASE, getAuthHeaders } from '../config/api'
import type { PageResponse } from '../types/admin'

export interface OwnerRestaurant {
  id: number
  name: string
  description: string
  address: string
  city: string
  state: string
  pincode: string
  phone: string
  email: string
  cuisineType: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  active: boolean
  openingTime: string
  closingTime: string
}

export interface OwnerOrderSummary {
  id: number
  orderNumber: string
  restaurantId: number
  restaurantName: string
  customerId: number
  customerName: string
  orderStatus: string
  paymentStatus: string
  itemCount: number
  totalAmount: number
  createdAt: string
}

export interface OwnerFoodItem {
  id: number
  name: string
  description?: string
  price: number
  imageUrl?: string
  foodType: 'VEG' | 'NON_VEG'
  available: boolean
  preparationTime: number
  categoryName?: string
}

export const ownerService = {
  async getMyRestaurants(token?: string): Promise<OwnerRestaurant[]> {
    const res = await fetch(`${API_BASE}/owner/restaurants`, {
      headers: getAuthHeaders(token),
    })
    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Failed to fetch owner restaurants')
    }
    return body.data
  },

  async createRestaurant(data: any, token?: string): Promise<OwnerRestaurant> {
    const res = await fetch(`${API_BASE}/owner/restaurants`, {
      method: 'POST',
      headers: getAuthHeaders(token),
      body: JSON.stringify(data),
    })
    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Failed to create restaurant')
    }
    return body.data
  },

  async getRestaurantOrders(
    restaurantId: number,
    status?: string,
    page = 0,
    size = 10,
    token?: string
  ): Promise<PageResponse<OwnerOrderSummary>> {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
    })
    if (status) params.append('status', status)

    const res = await fetch(`${API_BASE}/owner/restaurants/${restaurantId}/orders?${params}`, {
      headers: getAuthHeaders(token),
    })
    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Failed to fetch restaurant orders')
    }
    return body.data
  },

  async acceptOrder(orderId: number, token?: string): Promise<any> {
    const res = await fetch(`${API_BASE}/owner/orders/${orderId}/accept`, {
      method: 'PATCH',
      headers: getAuthHeaders(token),
    })
    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Failed to accept order')
    }
    return body.data
  },

  async rejectOrder(orderId: number, reason: string, token?: string): Promise<any> {
    const res = await fetch(`${API_BASE}/owner/orders/${orderId}/reject`, {
      method: 'PATCH',
      headers: getAuthHeaders(token),
      body: JSON.stringify({ rejectionReason: reason }),
    })
    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Failed to reject order')
    }
    return body.data
  },

  async markPreparing(orderId: number, token?: string): Promise<any> {
    const res = await fetch(`${API_BASE}/owner/orders/${orderId}/preparing`, {
      method: 'PATCH',
      headers: getAuthHeaders(token),
    })
    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Failed to mark order preparing')
    }
    return body.data
  },

  async markReady(orderId: number, token?: string): Promise<any> {
    const res = await fetch(`${API_BASE}/owner/orders/${orderId}/ready`, {
      method: 'PATCH',
      headers: getAuthHeaders(token),
    })
    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Failed to mark order ready for pickup')
    }
    return body.data
  },

  async getRestaurantItems(restaurantId: number, token?: string): Promise<OwnerFoodItem[]> {
    const res = await fetch(`${API_BASE}/owner/restaurants/${restaurantId}/items`, {
      headers: getAuthHeaders(token),
    })
    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Failed to fetch food items')
    }
    return body.data
  },

  async addFoodItem(restaurantId: number, data: any, token?: string): Promise<OwnerFoodItem> {
    const res = await fetch(`${API_BASE}/owner/restaurants/${restaurantId}/items`, {
      method: 'POST',
      headers: getAuthHeaders(token),
      body: JSON.stringify(data),
    })
    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Failed to add food item')
    }
    return body.data
  },

  async toggleFoodAvailability(itemId: number, available: boolean, token?: string): Promise<any> {
    const res = await fetch(`${API_BASE}/owner/items/${itemId}/availability?available=${available}`, {
      method: 'PATCH',
      headers: getAuthHeaders(token),
    })
    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Failed to update food item availability')
    }
    return body.data
  },
}

