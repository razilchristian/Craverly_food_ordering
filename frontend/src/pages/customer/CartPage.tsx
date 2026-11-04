import { CheckCircle2, CreditCard, Loader2, Navigation, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { EmptyState, QuantitySelector } from '../../components/common/ui'
import { useApp } from '../../context/AppContext'
import { paymentService } from '../../services/paymentService'
import { restaurantService } from '../../services/restaurantService'
import type { Coupon } from '../../types'
import type { PaymentResponse } from '../../types/payment'
import { cartTotals, inr } from '../../utils/format'

export default function CartPage() {
  const { cart, setQty, toast } = useApp()
  const [code, setCode] = useState('')
  const [coupon, setCoupon] = useState<Coupon>()
  const [loading, setLoading] = useState(false)
  const [completedPayment, setCompletedPayment] = useState<PaymentResponse | null>(null)
  const [notes, setNotes] = useState('')
  const [address, setAddress] = useState('123 MG Road, Ellis Bridge')
  const [city, setCity] = useState('Ahmedabad')
  const [state, setState] = useState('Gujarat')
  const [pincode, setPincode] = useState('380006')
  const [phone, setPhone] = useState('9876543210')

  if (completedPayment) {
    return (
      <div className="container-app py-16 text-center">
        <div className="mx-auto max-w-md rounded-2xl border border-line bg-white p-8 shadow-sm">
          <CheckCircle2 className="mx-auto size-16 text-emerald-500" />
          <h1 className="mt-4 text-2xl font-black">Payment Successful!</h1>
          <p className="mt-2 text-muted">
            Your order has been placed and payment of <strong className="text-ink">{inr(completedPayment.amount)}</strong> has been verified via Razorpay.
          </p>
          <div className="mt-6 rounded-xl bg-slate-50 p-4 text-left text-sm space-y-1">
            <p><span className="text-muted">Order Number:</span> <strong>{completedPayment.orderNumber}</strong></p>
            <p><span className="text-muted">Payment ID:</span> <strong>{completedPayment.razorpayPaymentId}</strong></p>
            <p><span className="text-muted">Status:</span> <span className="inline-block rounded bg-emerald-100 px-2 py-0.5 text-xs font-bold text-emerald-800">{completedPayment.paymentStatus}</span></p>
          </div>
          <div className="mt-6 flex flex-col gap-3">
            <Link
              to={`/orders/${completedPayment.orderId}/tracking`}
              className="btn-primary w-full text-center flex items-center justify-center gap-2"
            >
              <Navigation className="size-4" /> Track Your Order
            </Link>
            <div className="flex gap-3">
              <Link to="/orders" className="btn-secondary flex-1 text-center text-xs">
                My Orders
              </Link>
              <Link to="/restaurants" className="btn-secondary flex-1 text-center text-xs">
                Explore Menu
              </Link>
            </div>
          </div>
        </div>
      </div>
    )
  }

  if (!cart.length) {
    return (
      <div className="container-app py-16">
        <EmptyState
          title="Your cart is empty"
          text="Good food is just a few taps away."
          action={<Link to="/restaurants" className="btn-primary">Explore restaurants</Link>}
        />
      </div>
    )
  }

  const r = restaurantService.get(cart[0].item.restaurantId)!
  const t = cartTotals(
    cart.reduce((s, c) => s + c.qty * c.item.price, 0),
    r.deliveryFee,
    coupon?.percent,
    coupon?.freeDelivery
  )

  const apply = () => {
    const c = restaurantService.coupon(code)
    setCoupon(c)
    toast(c ? `${c.code} applied: ${c.description}` : 'Invalid coupon code')
  }

  const handleCheckout = async () => {
    setLoading(true)
    try {
      // 1. Check token in localStorage or use default mock customer token if available
      const token = localStorage.getItem('cravely_token') || ''
      if (!token) {
        toast('Please login to complete your payment')
        // In local demo without login, initiate mock Razorpay checkout flow
      }

      // Try placing order on live backend
      const orderPayload = {
        deliveryAddress: address,
        deliveryCity: city,
        deliveryState: state,
        deliveryPincode: pincode,
        customerPhone: phone,
        customerNote: notes,
      }

      const orderRes = await fetch('http://localhost:8081/api/orders', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(orderPayload),
      })

      if (!orderRes.ok) {
        const errData = await orderRes.json().catch(() => ({}))
        throw new Error(errData.message || 'Failed to place order before payment')
      }

      const orderData = await orderRes.json()
      const cravelyOrder = orderData.data

      // 2. Create Razorpay Order
      const rzpOrder = await paymentService.createPaymentOrder(cravelyOrder.id, token)

      // 3. Open Razorpay Checkout modal
      await paymentService.openCheckout(
        rzpOrder,
        token,
        (paymentResponse) => {
          setCompletedPayment(paymentResponse)
          toast('Payment successful!')
        },
        (errorMsg) => {
          toast(errorMsg || 'Payment was not completed')
        }
      )
    } catch (err) {
      toast(err instanceof Error ? err.message : 'Checkout encountered an error')
    } finally {
      setLoading(false)
    }
  }

  const row = (l: string, v: string) => (
    <div className="flex justify-between">
      <dt className="text-muted">{l}</dt>
      <dd>{v}</dd>
    </div>
  )

  return (
    <div className="container-app grid gap-6 py-6 md:gap-8 md:py-10 lg:grid-cols-12">
      <section className="lg:col-span-8">
        <h1 className="text-2xl font-extrabold md:text-3xl">Your cart</h1>
        <p className="mt-1 text-muted">
          From <Link to={`/restaurants/${r.id}`} className="font-semibold text-ink underline">{r.name}</Link>
        </p>

        <ul className="mt-6 divide-y divide-line rounded-2xl border border-line bg-white">
          {cart.map(({ item, qty }) => (
            <li key={item.id} className="flex flex-wrap items-center gap-3 p-4 sm:flex-nowrap sm:gap-4">
              <img src={item.image} alt="" className="size-16 rounded-xl object-cover" />
              <div className="min-w-0 flex-1">
                <p className="font-bold">{item.name}</p>
                <p className="text-sm text-muted">{inr(item.price)} each</p>
              </div>
              <div className="ml-[4.75rem] sm:ml-0">
                <QuantitySelector qty={qty} label={item.name} onChange={(q) => setQty(item.id, q)} />
              </div>
              <span className="hidden w-20 text-right font-semibold sm:block">{inr(qty * item.price)}</span>
              <button aria-label={`Remove ${item.name}`} onClick={() => setQty(item.id, 0)} className="text-muted hover:text-error">
                <Trash2 className="size-4" />
              </button>
            </li>
          ))}
        </ul>

        {/* Delivery Details Form */}
        <div className="mt-8 rounded-2xl border border-line bg-white p-5">
          <h2 className="text-lg font-bold">Delivery Address & Contact</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <label className="text-xs font-semibold text-muted">Street Address</label>
              <input value={address} onChange={(e) => setAddress(e.target.value)} className="mt-1 w-full rounded-xl border border-line p-2.5 text-sm" />
            </div>
            <div>
              <label className="text-xs font-semibold text-muted">City</label>
              <input value={city} onChange={(e) => setCity(e.target.value)} className="mt-1 w-full rounded-xl border border-line p-2.5 text-sm" />
            </div>
            <div>
              <label className="text-xs font-semibold text-muted">Pincode</label>
              <input value={pincode} onChange={(e) => setPincode(e.target.value)} className="mt-1 w-full rounded-xl border border-line p-2.5 text-sm" />
            </div>
            <div>
              <label className="text-xs font-semibold text-muted">State</label>
              <input value={state} onChange={(e) => setState(e.target.value)} className="mt-1 w-full rounded-xl border border-line p-2.5 text-sm" />
            </div>
            <div>
              <label className="text-xs font-semibold text-muted">Phone Number</label>
              <input value={phone} onChange={(e) => setPhone(e.target.value)} className="mt-1 w-full rounded-xl border border-line p-2.5 text-sm" />
            </div>
          </div>
          <label htmlFor="notes" className="mt-4 block text-xs font-semibold text-muted">Order instructions (Optional)</label>
          <textarea id="notes" rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="e.g. Less spicy, leave at door" className="mt-1 w-full rounded-xl border border-line p-2.5 text-sm" />
        </div>

        <Link to={`/restaurants/${r.id}`} className="btn-ghost mt-4 inline-block">Add more items</Link>
      </section>

      <aside className="lg:col-span-4">
        <div className="sticky top-20 rounded-2xl border border-line bg-white p-5">
          <div className="flex gap-2">
            <input value={code} onChange={(e) => setCode(e.target.value)} aria-label="Coupon code" placeholder="Try WELCOME10" className="h-10 flex-1 rounded-xl border border-line px-3 text-sm uppercase" />
            <button onClick={apply} className="btn-outline">Apply</button>
          </div>
          <dl className="mt-5 space-y-2 text-sm">
            {row('Item subtotal', inr(t.subtotal))}
            {row('Delivery fee', t.delivery ? inr(t.delivery) : 'Free')}
            {row('Packaging', inr(t.packaging))}
            {row('GST (5%)', inr(t.tax))}
            {t.discount > 0 && <div className="flex justify-between text-success"><dt>Discount</dt><dd>−{inr(t.discount)}</dd></div>}
            <div className="flex justify-between border-t border-line pt-3 text-base font-bold">
              <dt>Grand total</dt>
              <dd>{inr(t.total)}</dd>
            </div>
          </dl>
          <button
            className="btn-primary mt-5 w-full flex items-center justify-center gap-2"
            disabled={loading}
            onClick={handleCheckout}
          >
            {loading ? <Loader2 className="size-4 animate-spin" /> : <CreditCard className="size-4" />}
            {loading ? 'Initiating Checkout...' : 'Pay with Razorpay'}
          </button>
          <p className="mt-3 text-center text-xs text-muted">
            Secured by Razorpay Standard Checkout (Test Mode)
          </p>
        </div>
      </aside>
    </div>
  )
}
