import type {
  DashboardOverviewResponse,
  FoodItemResponse,
  OrderAnalyticsResponse,
  PageResponse,
  RecentActivityResponse,
  RestaurantAnalyticsResponse,
  RestaurantResponse,
  RevenueAnalyticsResponse,
  UserAnalyticsResponse,
  UserResponse,
} from '../types/admin'
import { API_BASE, getAuthHeaders } from '../config/api'

export const adminService = {
  // ----------------------------------------------------
  // DASHBOARD ANALYTICS APIS
  // ----------------------------------------------------

  async getOverview(token?: string): Promise<DashboardOverviewResponse> {
    const res = await fetch(`${API_BASE}/admin/dashboard/overview`, {
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to fetch dashboard overview')
    }
    return data.data
  },

  async getRevenueAnalytics(
    period = 'monthly',
    startDate?: string,
    endDate?: string,
    token?: string
  ): Promise<RevenueAnalyticsResponse> {
    const params = new URLSearchParams({ period })
    if (startDate) params.append('startDate', startDate)
    if (endDate) params.append('endDate', endDate)

    const res = await fetch(`${API_BASE}/admin/dashboard/revenue?${params}`, {
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to fetch revenue analytics')
    }
    return data.data
  },

  async getOrderAnalytics(
    startDate?: string,
    endDate?: string,
    token?: string
  ): Promise<OrderAnalyticsResponse> {
    const params = new URLSearchParams()
    if (startDate) params.append('startDate', startDate)
    if (endDate) params.append('endDate', endDate)

    const res = await fetch(`${API_BASE}/admin/dashboard/orders?${params}`, {
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to fetch order analytics')
    }
    return data.data
  },

  async getUserAnalytics(
    startDate?: string,
    endDate?: string,
    token?: string
  ): Promise<UserAnalyticsResponse> {
    const params = new URLSearchParams()
    if (startDate) params.append('startDate', startDate)
    if (endDate) params.append('endDate', endDate)

    const res = await fetch(`${API_BASE}/admin/dashboard/users?${params}`, {
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to fetch user analytics')
    }
    return data.data
  },

  async getRestaurantAnalytics(
    startDate?: string,
    endDate?: string,
    token?: string
  ): Promise<RestaurantAnalyticsResponse> {
    const params = new URLSearchParams()
    if (startDate) params.append('startDate', startDate)
    if (endDate) params.append('endDate', endDate)

    const res = await fetch(`${API_BASE}/admin/dashboard/restaurants?${params}`, {
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to fetch restaurant analytics')
    }
    return data.data
  },

  async getRecentActivity(limit = 20, token?: string): Promise<RecentActivityResponse[]> {
    const res = await fetch(`${API_BASE}/admin/dashboard/recent-activity?limit=${limit}`, {
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to fetch recent activity')
    }
    return data.data
  },

  // ----------------------------------------------------
  // USER MANAGEMENT APIS
  // ----------------------------------------------------

  async getUsers(
    page = 0,
    size = 10,
    role?: string,
    active?: boolean,
    search?: string,
    token?: string
  ): Promise<PageResponse<UserResponse>> {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
    })
    if (role) params.append('role', role)
    if (active !== undefined) params.append('active', String(active))
    if (search) params.append('search', search)

    const res = await fetch(`${API_BASE}/admin/users?${params}`, {
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok) {
      throw new Error(data.message || 'Failed to fetch users')
    }
    // Handle both wrapped ApiResponse and direct list if needed
    return data.data || data
  },

  async activateUser(userId: number, token?: string): Promise<UserResponse> {
    const res = await fetch(`${API_BASE}/admin/users/${userId}/activate`, {
      method: 'PATCH',
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok) {
      throw new Error(data.message || 'Failed to activate user')
    }
    return data.data || data
  },

  async deactivateUser(userId: number, token?: string): Promise<UserResponse> {
    const res = await fetch(`${API_BASE}/admin/users/${userId}/deactivate`, {
      method: 'PATCH',
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok) {
      throw new Error(data.message || 'Failed to deactivate user')
    }
    return data.data || data
  },

  // ----------------------------------------------------
  // RESTAURANT MANAGEMENT APIS
  // ----------------------------------------------------

  async getAllRestaurants(token?: string): Promise<RestaurantResponse[]> {
    const res = await fetch(`${API_BASE}/admin/restaurants`, {
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to fetch restaurants')
    }
    return data.data
  },

  async approveRestaurant(restaurantId: number, token?: string): Promise<RestaurantResponse> {
    const res = await fetch(`${API_BASE}/admin/restaurants/${restaurantId}/approve`, {
      method: 'PATCH',
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to approve restaurant')
    }
    return data.data
  },

  async rejectRestaurant(restaurantId: number, token?: string): Promise<RestaurantResponse> {
    const res = await fetch(`${API_BASE}/admin/restaurants/${restaurantId}/reject`, {
      method: 'PATCH',
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to reject restaurant')
    }
    return data.data
  },

  async activateRestaurant(restaurantId: number, token?: string): Promise<RestaurantResponse> {
    const res = await fetch(`${API_BASE}/admin/restaurants/${restaurantId}/activate`, {
      method: 'PATCH',
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to activate restaurant')
    }
    return data.data
  },

  async deactivateRestaurant(restaurantId: number, token?: string): Promise<RestaurantResponse> {
    const res = await fetch(`${API_BASE}/admin/restaurants/${restaurantId}/deactivate`, {
      method: 'PATCH',
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to deactivate restaurant')
    }
    return data.data
  },

  // ----------------------------------------------------
  // ORDER MANAGEMENT APIS
  // ----------------------------------------------------

  async getAdminOrders(
    page = 0,
    size = 10,
    status?: string,
    orderNumber?: string,
    customer?: string,
    restaurantId?: number,
    token?: string
  ): Promise<PageResponse<any>> {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
    })
    if (status) params.append('status', status)
    if (orderNumber) params.append('orderNumber', orderNumber)
    if (customer) params.append('customer', customer)
    if (restaurantId) params.append('restaurantId', String(restaurantId))

    const res = await fetch(`${API_BASE}/admin/orders?${params}`, {
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to fetch admin orders')
    }
    return data.data
  },

  // ----------------------------------------------------
  // FOOD ITEM MONITORING APIS
  // ----------------------------------------------------

  async getAllFoodItems(token?: string): Promise<FoodItemResponse[]> {
    const res = await fetch(`${API_BASE}/admin/items`, {
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to fetch food items')
    }
    return data.data
  },

  async activateFoodItem(itemId: number, token?: string): Promise<FoodItemResponse> {
    const res = await fetch(`${API_BASE}/admin/items/${itemId}/activate`, {
      method: 'PATCH',
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to activate food item')
    }
    return data.data
  },

  async deactivateFoodItem(itemId: number, token?: string): Promise<FoodItemResponse> {
    const res = await fetch(`${API_BASE}/admin/items/${itemId}/deactivate`, {
      method: 'PATCH',
      headers: getAuthHeaders(token),
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to deactivate food item')
    }
    return data.data
  },
}

