export type Role = 'customer' | 'restaurant' | 'admin'
export interface User { id: string; name: string; email: string; role: Role; joined: string; active: boolean }
export interface Category { id: string; name: string; image: string }
export interface Restaurant {
  id: string; name: string; image: string; rating: number; reviews: number; cuisines: string[]
  deliveryMins: number; deliveryFee: number; priceForTwo: number; veg: boolean; offer?: string
  category: string; minOrder: number; hours: string
}
export type MenuSection = 'Recommended' | 'Starters' | 'Main Course' | 'Rice and Biryani' | 'Breads' | 'Desserts' | 'Beverages'
export interface FoodItem {
  id: string; restaurantId: string; name: string; description: string; price: number; image: string
  veg: boolean; bestseller?: boolean; rating: number; section: MenuSection; available: boolean
}
export interface CartItem { item: FoodItem; qty: number }
export interface Coupon { code: string; description: string; percent?: number; freeDelivery?: boolean }
