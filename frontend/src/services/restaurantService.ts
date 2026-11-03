// Mock service layer. Replace bodies with fetch(`${API_BASE}/api/...`) calls for the Spring Boot backend.
import { categories, coupons, foodItems, restaurants } from '../data/mock'

export const restaurantService = {
  list: () => restaurants, // GET /api/restaurants
  get: (id: string) => restaurants.find((r) => r.id === id), // GET /api/restaurants/{id}
  menu: (id: string) => foodItems.filter((f) => f.restaurantId === id), // GET /api/restaurants/{id}/menu
  categories: () => categories, // GET /api/categories
  coupon: (code: string) => coupons.find((c) => c.code === code.trim().toUpperCase()),
}
