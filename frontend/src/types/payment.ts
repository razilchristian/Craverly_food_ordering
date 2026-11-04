export interface RazorpayOrderResponse {
  razorpayOrderId: string
  razorpayKeyId: string
  amount: number // in paise
  amountInRupees: number
  currency: string
  orderId: number
  orderNumber: string
  customerName?: string
  customerEmail?: string
  customerPhone?: string
}

export interface VerifyPaymentRequest {
  razorpay_order_id: string
  razorpay_payment_id: string
  razorpay_signature: string
}

export interface PaymentFailureRequest {
  razorpayOrderId: string
  razorpayPaymentId?: string
  errorCode?: string
  errorDescription?: string
  errorReason?: string
}

export interface PaymentResponse {
  id: number
  orderId: number
  orderNumber: string
  razorpayOrderId: string
  razorpayPaymentId?: string
  amount: number
  currency: string
  paymentStatus: 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED'
  paymentMethod?: string
  failureReason?: string
  paidAt?: string
  createdAt: string
}

export interface RazorpayCheckoutOptions {
  key: string
  amount: number
  currency: string
  name: string
  description?: string
  order_id: string
  prefill?: {
    name?: string
    email?: string
    contact?: string
  }
  theme?: {
    color?: string
  }
  handler: (response: {
    razorpay_payment_id: string
    razorpay_order_id: string
    razorpay_signature: string
  }) => void
  modal?: {
    ondismiss?: () => void
  }
}

declare global {
  interface Window {
    Razorpay: new (options: RazorpayCheckoutOptions) => {
      open: () => void
      on: (event: string, callback: (response: any) => void) => void
    }
  }
}
