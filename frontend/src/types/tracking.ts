export type OrderStatus =
  | 'PLACED'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'PREPARING'
  | 'READY_FOR_PICKUP'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED'

export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED'

export interface OrderItemResponse {
  id: number
  foodItemId: number
  foodItemName: string
  foodItemImage?: string
  foodType: 'VEG' | 'NON_VEG'
  quantity: number
  priceAtOrder: number
  totalPrice: number
}

export interface OrderStatusHistoryResponse {
  id: number
  previousStatus: OrderStatus | null
  newStatus: OrderStatus
  changedBy: string
  changedByRole: string
  changedAt: string
  remarks?: string
}

export interface OrderTrackingResponse {
  orderId: number
  orderNumber: string
  orderStatus: OrderStatus
  paymentStatus: PaymentStatus
  restaurantId: number
  restaurantName: string
  restaurantPhone: string
  deliveryAddress: string
  deliveryCity: string
  deliveryState: string
  deliveryPincode: string
  customerPhone: string
  customerNote?: string
  rejectionReason?: string
  cancellationReason?: string
  subtotal: number
  deliveryFee: number
  taxAmount: number
  discountAmount: number
  totalAmount: number
  orderedAt: string
  items: OrderItemResponse[]
  statusHistory: OrderStatusHistoryResponse[]
}

export interface OrderSummaryResponse {
  id: number
  orderNumber: string
  restaurantId: number
  restaurantName: string
  orderStatus: OrderStatus
  paymentStatus: PaymentStatus
  totalAmount: number
  itemCount: number
  createdAt: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

