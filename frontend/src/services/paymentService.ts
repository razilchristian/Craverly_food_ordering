import type {
  PaymentResponse,
  RazorpayCheckoutOptions,
  RazorpayOrderResponse,
  VerifyPaymentRequest,
} from '../types/payment'

const API_BASE = 'http://localhost:8081/api/payments'

export const paymentService = {
  /**
   * Securely loads the Razorpay checkout script if not already present.
   */
  loadRazorpayScript(): Promise<boolean> {
    return new Promise((resolve) => {
      if (typeof window !== 'undefined' && window.Razorpay) {
        resolve(true)
        return
      }
      const script = document.createElement('script')
      script.src = 'https://checkout.razorpay.com/v1/checkout.js'
      script.async = true
      script.onload = () => resolve(true)
      script.onerror = () => resolve(false)
      document.body.appendChild(script)
    })
  },

  /**
   * Initiates payment order creation on backend.
   */
  async createPaymentOrder(orderId: number, token: string): Promise<RazorpayOrderResponse> {
    const res = await fetch(`${API_BASE}/create-order`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ orderId }),
    })

    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to initiate payment')
    }
    return data.data
  },

  /**
   * Verifies payment signature with the backend.
   */
  async verifyPayment(verification: VerifyPaymentRequest, token: string): Promise<PaymentResponse> {
    const res = await fetch(`${API_BASE}/verify`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(verification),
    })

    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Payment signature verification failed')
    }
    return data.data
  },

  /**
   * Retries payment for an unpaid Cravely order.
   */
  async retryPayment(orderId: number, token: string): Promise<RazorpayOrderResponse> {
    const res = await fetch(`${API_BASE}/retry/${orderId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
    })

    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to retry payment')
    }
    return data.data
  },

  /**
   * Fetches payment history for an order.
   */
  async getPaymentHistory(orderId: number, token: string): Promise<PaymentResponse[]> {
    const res = await fetch(`${API_BASE}/order/${orderId}`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })

    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to fetch payment history')
    }
    return data.data
  },

  /**
   * Opens Razorpay Standard Checkout modal.
   */
  async openCheckout(
    orderData: RazorpayOrderResponse,
    token: string,
    onSuccess: (payment: PaymentResponse) => void,
    onFailure: (error: string) => void
  ) {
    const loaded = await this.loadRazorpayScript()
    if (!loaded) {
      onFailure('Razorpay Checkout SDK failed to load. Please check your internet connection.')
      return
    }

    const options: RazorpayCheckoutOptions = {
      key: orderData.razorpayKeyId,
      amount: orderData.amount,
      currency: orderData.currency,
      name: 'Cravely Food Ordering',
      description: `Payment for Order #${orderData.orderNumber}`,
      order_id: orderData.razorpayOrderId,
      prefill: {
        name: orderData.customerName,
        email: orderData.customerEmail,
        contact: orderData.customerPhone,
      },
      theme: {
        color: '#E23744',
      },
      handler: async (response) => {
        try {
          const verified = await this.verifyPayment(
            {
              razorpay_order_id: response.razorpay_order_id,
              razorpay_payment_id: response.razorpay_payment_id,
              razorpay_signature: response.razorpay_signature,
            },
            token
          )
          onSuccess(verified)
        } catch (err) {
          onFailure(err instanceof Error ? err.message : 'Payment verification failed')
        }
      },
      modal: {
        ondismiss: () => {
          onFailure('Payment checkout was closed by the user.')
        },
      },
    }

    const rzp = new window.Razorpay(options)
    rzp.open()
  },
}
