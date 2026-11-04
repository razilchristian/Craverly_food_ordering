import type {
  PaymentFailureRequest,
  PaymentResponse,
  RazorpayCheckoutOptions,
  RazorpayOrderResponse,
  VerifyPaymentRequest,
} from '../types/payment'

import { API_BASE as BASE_URL } from '../config/api'

const API_BASE = `${BASE_URL}/payments`

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
   * Records payment failure callback on backend.
   */
  async recordPaymentFailure(
    failureData: PaymentFailureRequest,
    token: string
  ): Promise<PaymentResponse> {
    const res = await fetch(`${API_BASE}/fail`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(failureData),
    })

    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to record payment failure')
    }
    return data.data
  },

  /**
   * Simulates successful payment for testing when placeholder keys are active.
   */
  async simulateSuccess(orderId: number, token: string): Promise<PaymentResponse> {
    const res = await fetch(`${API_BASE}/simulate-success`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ orderId }),
    })

    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Simulated payment failed')
    }
    return data.data
  },

  /**
   * Simulates payment failure for testing when placeholder keys are active.
   */
  async simulateFailure(orderId: number, token: string): Promise<PaymentResponse> {
    const res = await fetch(`${API_BASE}/simulate-failure`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ orderId }),
    })

    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Simulated failure recording failed')
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
   * Renders the interactive Test Mode Simulator modal when running with placeholder keys.
   */
  renderSimulatorModal(
    orderData: RazorpayOrderResponse,
    token: string,
    onSuccess: (payment: PaymentResponse) => void,
    onFailure: (error: string) => void
  ) {
    const modalId = 'cravely-razorpay-sim-modal'
    const existing = document.getElementById(modalId)
    if (existing) {
      existing.remove()
    }

    const overlay = document.createElement('div')
    overlay.id = modalId
    overlay.className =
      'fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-200'

    const amountFormatted = (orderData.amount / 100).toFixed(2)

    overlay.innerHTML = `
      <div class="relative w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl border border-line text-left">
        <div class="flex items-center justify-between border-b border-line pb-4">
          <div class="flex items-center gap-2">
            <span class="inline-flex size-3 rounded-full bg-amber-500 animate-pulse"></span>
            <h3 class="text-base font-bold text-ink">Razorpay Standard Checkout</h3>
          </div>
          <span class="rounded bg-amber-100 px-2 py-0.5 text-xs font-bold text-amber-800 uppercase tracking-wider">
            Test Mode (Simulator)
          </span>
        </div>

        <div class="mt-4 space-y-3">
          <div class="rounded-xl bg-slate-50 p-3.5 text-xs space-y-1.5 border border-slate-100">
            <div class="flex justify-between">
              <span class="text-muted font-medium">Order Number:</span>
              <span class="font-mono font-bold text-ink">${orderData.orderNumber}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-muted font-medium">Amount to Pay:</span>
              <span class="text-base font-black text-emerald-600">₹${amountFormatted}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-muted font-medium">Customer:</span>
              <span class="font-medium text-ink">${orderData.customerName || 'Customer'}</span>
            </div>
          </div>

          <div class="rounded-lg bg-amber-50/80 p-3 text-[11px] text-amber-900 border border-amber-200">
            <strong>Development Notice:</strong> Placeholder credentials detected (<code>${orderData.razorpayKeyId}</code>).
            To test with the live Razorpay modal, set genuine <code>RAZORPAY_KEY_ID</code> and <code>RAZORPAY_KEY_SECRET</code> in your <code>.env</code> file.
          </div>
        </div>

        <div class="mt-6 flex flex-col gap-2.5">
          <button id="sim-btn-success" class="w-full rounded-xl bg-emerald-600 py-2.5 px-4 text-xs font-bold text-white shadow-sm hover:bg-emerald-700 active:scale-[0.98] transition">
            ✓ Simulate Successful Payment
          </button>
          <button id="sim-btn-fail" class="w-full rounded-xl bg-rose-50 py-2.5 px-4 text-xs font-bold text-rose-700 border border-rose-200 hover:bg-rose-100 active:scale-[0.98] transition">
            ✕ Simulate Payment Failure
          </button>
          <button id="sim-btn-cancel" class="mt-1 text-center text-xs font-medium text-muted hover:text-ink">
            Cancel and Close
          </button>
        </div>
      </div>
    `

    document.body.appendChild(overlay)

    const cleanup = () => {
      overlay.remove()
    }

    const successBtn = overlay.querySelector('#sim-btn-success') as HTMLButtonElement
    const failBtn = overlay.querySelector('#sim-btn-fail') as HTMLButtonElement
    const cancelBtn = overlay.querySelector('#sim-btn-cancel') as HTMLButtonElement

    successBtn?.addEventListener('click', async () => {
      successBtn.disabled = true
      successBtn.textContent = 'Processing simulated payment...'
      try {
        const verified = await this.simulateSuccess(orderData.orderId, token)
        cleanup()
        onSuccess(verified)
      } catch (err) {
        cleanup()
        onFailure(err instanceof Error ? err.message : 'Simulated payment failed')
      }
    })

    failBtn?.addEventListener('click', async () => {
      failBtn.disabled = true
      failBtn.textContent = 'Recording simulated decline...'
      try {
        await this.simulateFailure(orderData.orderId, token)
        cleanup()
        onFailure('Payment was declined by customer in simulation')
      } catch (err) {
        cleanup()
        onFailure(err instanceof Error ? err.message : 'Simulated payment failure recording failed')
      }
    })

    cancelBtn?.addEventListener('click', () => {
      cleanup()
      onFailure('Payment checkout was closed by the user.')
    })
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
    const isPlaceholderMode =
      !orderData.razorpayKeyId ||
      orderData.razorpayKeyId.includes('placeholder') ||
      orderData.razorpayKeyId.includes('unconfigured') ||
      orderData.razorpayOrderId.startsWith('order_mock_')

    // If running in development with placeholder keys, use the test mode simulator
    // to avoid the real Razorpay script error "Oops! Something went wrong"
    if (isPlaceholderMode) {
      this.renderSimulatorModal(orderData, token, onSuccess, onFailure)
      return
    }

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

    // Register failure callback with backend failure recording
    rzp.on('payment.failed', async (response: any) => {
      const error = response?.error || {}
      const reason = error.description || error.reason || 'Payment failed on Razorpay checkout'
      try {
        await this.recordPaymentFailure(
          {
            razorpayOrderId: orderData.razorpayOrderId,
            razorpayPaymentId: error?.metadata?.payment_id,
            errorCode: error?.code,
            errorDescription: error?.description,
            errorReason: error?.reason,
          },
          token
        )
      } catch (err) {
        console.error('Failed to record payment failure on backend:', err)
      }
      onFailure(reason)
    })

    rzp.open()
  },
}
