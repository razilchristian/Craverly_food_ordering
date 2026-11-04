export interface DashboardOverviewResponse {
  totalUsers: number
  totalCustomers: number
  totalRestaurantOwners: number
  totalAdmins: number
  activeUsers: number
  deactivatedUsers: number

  totalRestaurants: number
  pendingRestaurants: number
  approvedRestaurants: number
  rejectedRestaurants: number
  activeRestaurants: number

  totalFoodItems: number
  availableFoodItems: number

  totalOrders: number
  pendingOrders: number
  deliveredOrders: number
  cancelledOrders: number
  rejectedOrders: number

  totalRevenue: number
  todayRevenue: number
  deliveredRevenue: number
  reconciliationExposure: number
}

export interface RevenueDataPoint {
  label: string
  date: string
  revenue: number
  orders: number
}

export interface RevenueAnalyticsResponse {
  period: string
  currency: string
  totalRevenue: number
  deliveredRevenue: number
  reconciliationExposure: number
  data: RevenueDataPoint[]
}

export interface OrderAnalyticsResponse {
  totalOrders: number
  placedOrders: number
  acceptedOrders: number
  preparingOrders: number
  readyForPickupOrders: number
  outForDeliveryOrders: number
  deliveredOrders: number
  cancelledOrders: number
  rejectedOrders: number
  activeOrders: number
  recentOrders: {
    id: number
    orderNumber: string
    restaurantName: string
    totalAmount: number
    orderStatus: string
    paymentStatus: string
    itemCount: number
    createdAt: string
  }[]
}

export interface UserResponse {
  id: number
  fullName: string
  email: string
  phone: string
  role: 'CUSTOMER' | 'RESTAURANT_OWNER' | 'ADMIN'
  active: boolean
  createdAt: string
  updatedAt?: string
}

export interface UserAnalyticsResponse {
  totalUsers: number
  customers: number
  restaurantOwners: number
  admins: number
  activeUsers: number
  deactivatedUsers: number
  newUsersInPeriod: number
  recentUsers: UserResponse[]
}

export interface CuisineDistribution {
  cuisine: string
  count: number
}

export interface RestaurantAnalyticsResponse {
  totalRestaurants: number
  pendingApprovals: number
  approvedRestaurants: number
  rejectedRestaurants: number
  activeRestaurants: number
  inactiveRestaurants: number
  cuisineDistribution: CuisineDistribution[]
}

export interface RecentActivityResponse {
  id: string
  type: string
  title: string
  description: string
  actor: string
  actorRole: string
  timestamp: string
  referenceId?: string
  referenceType?: string
}

export interface RestaurantResponse {
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
  ownerName: string
  ownerEmail: string
  openingTime: string
  closingTime: string
}

export interface FoodItemResponse {
  id: number
  restaurantId: number
  restaurantName?: string
  name: string
  description?: string
  price: number
  imageUrl?: string
  foodType: 'VEG' | 'NON_VEG'
  available: boolean
  preparationTime: number
  categoryName?: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

