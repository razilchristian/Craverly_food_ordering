import { API_BASE, getAuthHeaders } from '../config/api'

export interface BackendCartItem {
  id: number
  foodItemId: number
  foodItemName: string
  price: number
  quantity: number
  totalPrice: number
}

export interface BackendCart {
  id: number
  restaurantId: number | null
  restaurantName: string | null
  items: BackendCartItem[]
  totalAmount: number
}

export const cartService = {
  async getCart(token?: string): Promise<BackendCart> {
    const res = await fetch(`${API_BASE}/cart`, {
      headers: getAuthHeaders(token),
    })
    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Failed to fetch cart')
    }
    return body.data
  },

  async addItem(foodItemId: number, quantity: number, token?: string): Promise<BackendCart> {
    const res = await fetch(`${API_BASE}/cart/items`, {
      method: 'POST',
      headers: getAuthHeaders(token),
      body: JSON.stringify({ foodItemId, quantity }),
    })
    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Failed to add item to cart')
    }
    return body.data
  },

  async updateQuantity(cartItemId: number, quantity: number, token?: string): Promise<BackendCart> {
    const res = await fetch(`${API_BASE}/cart/items/${cartItemId}`, {
      method: 'PATCH',
      headers: getAuthHeaders(token),
      body: JSON.stringify({ quantity }),
    })
    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Failed to update item quantity')
    }
    return body.data
  },

  async removeItem(cartItemId: number, token?: string): Promise<BackendCart> {
    const res = await fetch(`${API_BASE}/cart/items/${cartItemId}`, {
      method: 'DELETE',
      headers: getAuthHeaders(token),
    })
    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Failed to remove item')
    }
    return body.data
  },

  async clearCart(token?: string): Promise<void> {
    const res = await fetch(`${API_BASE}/cart`, {
      method: 'DELETE',
      headers: getAuthHeaders(token),
    })
    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Failed to clear cart')
    }
  },

  // Synchronize local frontend items with backend MySQL cart before checkout
  async syncCart(
    items: { item: { id: string | number; name?: string; restaurantId?: string }; qty: number }[],
    token?: string
  ): Promise<BackendCart> {
    const payload = {
      items: items.map((entry) => ({
        foodItemId: typeof entry.item.id === 'number' ? entry.item.id : (Number(entry.item.id) || null),
        itemCode: String(entry.item.id),
        name: entry.item.name || '',
        restaurantId: entry.item.restaurantId || '',
        quantity: entry.qty,
      })),
    }

    const res = await fetch(`${API_BASE}/cart/sync`, {
      method: 'POST',
      headers: getAuthHeaders(token),
      body: JSON.stringify(payload),
    })

    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Failed to sync cart with backend')
    }
    return body.data
  },
}

