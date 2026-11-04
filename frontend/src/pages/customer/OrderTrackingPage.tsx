import {
  AlertCircle,
  ArrowLeft,
  Bike,
  Check,
  CheckCircle2,
  Clock,
  History,
  MapPin,
  PackageCheck,
  Phone,
  ReceiptText,
  RefreshCw,
  Utensils,
  XCircle,
} from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import { orderService } from '../../services/orderService'
import type { OrderStatus, OrderTrackingResponse } from '../../types/tracking'
import { inr } from '../../utils/format'

interface StepConfig {
  status: OrderStatus
  label: string
  description: string
  icon: typeof Check
}

const ORDER_STEPS: StepConfig[] = [
  {
    status: 'PLACED',
    label: 'Order Placed',
    description: 'We received your order and sent it to the restaurant',
    icon: ReceiptText,
  },
  {
    status: 'ACCEPTED',
    label: 'Accepted',
    description: 'The restaurant confirmed and accepted your order',
    icon: CheckCircle2,
  },
  {
    status: 'PREPARING',
    label: 'Preparing',
    description: 'Chef is cooking your fresh and delicious meal',
    icon: Utensils,
  },
  {
    status: 'READY_FOR_PICKUP',
    label: 'Ready for Pickup',
    description: 'Food is packaged and awaiting delivery partner dispatch',
    icon: PackageCheck,
  },
  {
    status: 'OUT_FOR_DELIVERY',
    label: 'Out for Delivery',
    description: 'Your food is on the way to your doorstep',
    icon: Bike,
  },
  {
    status: 'DELIVERED',
    label: 'Delivered',
    description: 'Order delivered! Enjoy your meal',
    icon: Check,
  },
]

const TERMINAL_STATUSES: OrderStatus[] = ['DELIVERED', 'CANCELLED', 'REJECTED']

export default function OrderTrackingPage() {
  const { orderId } = useParams<{ orderId: string }>()
  const parsedId = Number(orderId)

  const [tracking, setTracking] = useState<OrderTrackingResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date())

  const isTerminalRef = useRef(false)

  const fetchTracking = useCallback(
    async (isManual = false) => {
      if (!parsedId || isNaN(parsedId)) {
        setError('Invalid order ID')
        setLoading(false)
        return
      }

      if (isManual) setRefreshing(true)

      try {
        const data = await orderService.trackOrder(parsedId)
        setTracking(data)
        setError(null)
        setLastUpdated(new Date())

        if (TERMINAL_STATUSES.includes(data.orderStatus)) {
          isTerminalRef.current = true
        }
      } catch (err) {
        // If not found in mock/backend, provide fallback helpful message
        setError(err instanceof Error ? err.message : 'Unable to load order tracking')
      } finally {
        setLoading(false)
        if (isManual) setRefreshing(false)
      }
    },
    [parsedId]
  )

  // Initial fetch and 10s polling interval
  useEffect(() => {
    isTerminalRef.current = false
    fetchTracking()

    const intervalId = setInterval(() => {
      if (!isTerminalRef.current) {
        fetchTracking(false)
      }
    }, 10000)

    return () => {
      clearInterval(intervalId)
    }
  }, [fetchTracking])

  if (loading) {
    return (
      <div className="container-app py-16 text-center">
        <RefreshCw className="mx-auto size-10 animate-spin text-primary" />
        <p className="mt-4 text-base font-semibold text-muted">Loading live tracking details...</p>
      </div>
    )
  }

  if (error || !tracking) {
    return (
      <div className="container-app py-16 text-center">
        <div className="mx-auto max-w-md rounded-2xl border border-line bg-white p-8 shadow-sm">
          <AlertCircle className="mx-auto size-12 text-rose-500" />
          <h2 className="mt-4 text-xl font-bold text-ink">Order Tracking Unavailable</h2>
          <p className="mt-2 text-sm text-muted">{error || 'Order could not be found or tracked.'}</p>
          <div className="mt-6 flex justify-center gap-3">
            <Link to="/orders" className="btn-secondary">
              View All Orders
            </Link>
            <button onClick={() => fetchTracking(true)} className="btn-primary">
              Retry
            </button>
          </div>
        </div>
      </div>
    )
  }

  const isCancelled = tracking.orderStatus === 'CANCELLED'
  const isRejected = tracking.orderStatus === 'REJECTED'
  const isTerminal = TERMINAL_STATUSES.includes(tracking.orderStatus)

  // Determine current active step index in standard flow
  const currentStepIndex = ORDER_STEPS.findIndex((s) => s.status === tracking.orderStatus)

  return (
    <div className="container-app py-6 md:py-10 max-w-5xl">
      {/* Top Navigation */}
      <div className="mb-6 flex items-center justify-between">
        <Link
          to="/orders"
          className="inline-flex items-center gap-2 text-sm font-semibold text-muted hover:text-ink transition"
        >
          <ArrowLeft className="size-4" /> Back to My Orders
        </Link>
        <div className="flex items-center gap-3 text-xs text-muted">
          <span>Updated {lastUpdated.toLocaleTimeString()}</span>
          <button
            onClick={() => fetchTracking(true)}
            disabled={refreshing}
            className="inline-flex items-center gap-1.5 rounded-lg border border-line bg-white px-3 py-1.5 font-medium text-ink shadow-2xs hover:bg-cream transition disabled:opacity-50"
            title="Refresh tracking status"
          >
            <RefreshCw className={`size-3.5 ${refreshing ? 'animate-spin text-primary' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {/* Order Header Card */}
      <div className="rounded-2xl border border-line bg-white p-6 shadow-sm mb-8">
        <div className="flex flex-wrap items-start justify-between gap-4 border-b border-line pb-6">
          <div>
            <div className="flex items-center gap-3 flex-wrap">
              <h1 className="text-2xl font-black text-ink">{tracking.orderNumber}</h1>
              {/* Order Status Badge */}
              <span
                className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider ${tracking.orderStatus === 'DELIVERED'
                    ? 'bg-emerald-100 text-emerald-800'
                    : isCancelled || isRejected
                      ? 'bg-rose-100 text-rose-800'
                      : 'bg-primary/15 text-primary'
                  }`}
              >
                {tracking.orderStatus.replace(/_/g, ' ')}
              </span>

              {/* Payment Status Badge */}
              <span
                className={`inline-flex items-center px-2.5 py-0.5 rounded text-xs font-semibold ${tracking.paymentStatus === 'PAID'
                    ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                    : 'bg-amber-50 text-amber-700 border border-amber-200'
                  }`}
              >
                Payment: {tracking.paymentStatus}
              </span>
            </div>

            <p className="mt-2 text-sm text-muted">
              From <strong className="text-ink font-semibold">{tracking.restaurantName}</strong> • Placed on{' '}
              {new Date(tracking.orderedAt).toLocaleString('en-IN', {
                dateStyle: 'medium',
                timeStyle: 'short',
              })}
            </p>
          </div>

          <div className="text-right">
            <span className="text-xs uppercase tracking-wider text-muted font-bold block">Estimated Delivery</span>
            <span className="text-lg font-black text-ink">
              {isTerminal ? '—' : '30 – 40 mins'}
            </span>
            {!isTerminal && (
              <span className="inline-flex items-center gap-1.5 text-xs text-emerald-600 font-medium block mt-1">
                <span className="size-2 rounded-full bg-emerald-500 animate-pulse" />
                Live Polling (every 10s)
              </span>
            )}
          </div>
        </div>

        {/* Terminal Alert Banners */}
        {isCancelled && (
          <div className="mt-6 rounded-xl border border-rose-200 bg-rose-50 p-4 flex items-start gap-3 text-rose-900">
            <XCircle className="size-5 text-rose-600 shrink-0 mt-0.5" />
            <div>
              <h3 className="font-bold text-sm">Order Cancelled</h3>
              <p className="text-xs mt-1 text-rose-700">
                Reason: {tracking.cancellationReason || 'Cancelled by customer'}
              </p>
            </div>
          </div>
        )}

        {isRejected && (
          <div className="mt-6 rounded-xl border border-amber-200 bg-amber-50 p-4 flex items-start gap-3 text-amber-900">
            <AlertCircle className="size-5 text-amber-600 shrink-0 mt-0.5" />
            <div>
              <h3 className="font-bold text-sm">Order Rejected by Restaurant</h3>
              <p className="text-xs mt-1 text-amber-700">
                Reason: {tracking.rejectionReason || 'Unable to fulfill order at this time'}
              </p>
            </div>
          </div>
        )}

        {/* Visual Timeline (Standard 6-stage lifecycle) */}
        {!isCancelled && !isRejected && (
          <div className="mt-8 pt-2">
            <h2 className="text-sm font-bold uppercase tracking-wider text-muted mb-6">Order Status Timeline</h2>

            <div className="relative">
              {/* Mobile Timeline (Vertical) */}
              <div className="md:hidden space-y-6 relative before:absolute before:left-5 before:top-3 before:bottom-3 before:w-0.5 before:bg-line">
                {ORDER_STEPS.map((step, idx) => {
                  const isCompleted = currentStepIndex > idx
                  const isCurrent = currentStepIndex === idx
                  const StepIcon = step.icon

                  return (
                    <div key={step.status} className="relative flex items-start gap-4">
                      <div
                        className={`size-10 rounded-full flex items-center justify-center shrink-0 z-10 transition-colors ${isCompleted
                            ? 'bg-emerald-500 text-white shadow-sm'
                            : isCurrent
                              ? 'bg-primary text-white ring-4 ring-primary/20 shadow-md animate-pulse'
                              : 'bg-slate-100 text-slate-400 border border-line'
                          }`}
                      >
                        <StepIcon className="size-5" />
                      </div>
                      <div className="pt-1">
                        <p className={`font-bold text-sm ${isCurrent ? 'text-primary' : isCompleted ? 'text-ink' : 'text-muted'}`}>
                          {step.label}
                        </p>
                        <p className="text-xs text-muted mt-0.5">{step.description}</p>
                      </div>
                    </div>
                  )
                })}
              </div>

              {/* Desktop Timeline (Horizontal) */}
              <div className="hidden md:grid md:grid-cols-6 gap-2 relative">
                {/* Connecting Line */}
                <div className="absolute top-5 left-8 right-8 h-1 bg-line -z-0">
                  <div
                    className="h-full bg-emerald-500 transition-all duration-500"
                    style={{
                      width: `${currentStepIndex < 0
                          ? 0
                          : Math.min(100, (currentStepIndex / (ORDER_STEPS.length - 1)) * 100)
                        }%`,
                    }}
                  />
                </div>

                {ORDER_STEPS.map((step, idx) => {
                  const isCompleted = currentStepIndex > idx
                  const isCurrent = currentStepIndex === idx
                  const StepIcon = step.icon

                  return (
                    <div key={step.status} className="flex flex-col items-center text-center relative z-10">
                      <div
                        className={`size-11 rounded-full flex items-center justify-center transition-all ${isCompleted
                            ? 'bg-emerald-500 text-white shadow-sm'
                            : isCurrent
                              ? 'bg-primary text-white ring-4 ring-primary/20 shadow-md scale-110'
                              : 'bg-white border-2 border-line text-slate-400'
                          }`}
                      >
                        <StepIcon className="size-5" />
                      </div>
                      <p
                        className={`mt-3 text-xs font-bold leading-tight ${isCurrent ? 'text-primary font-black' : isCompleted ? 'text-ink' : 'text-slate-400'
                          }`}
                      >
                        {step.label}
                      </p>
                      <p className="text-[11px] text-muted mt-1 leading-snug px-1 line-clamp-2">
                        {step.description}
                      </p>
                    </div>
                  )
                })}
              </div>
            </div>
          </div>
        )}
      </div>

      <div className="grid gap-8 md:grid-cols-12">
        {/* Left Column: Order Items & Delivery Info */}
        <div className="md:col-span-7 space-y-6">
          {/* Order Items */}
          <div className="rounded-2xl border border-line bg-white p-6 shadow-sm">
            <h2 className="text-base font-bold text-ink mb-4 flex items-center gap-2">
              <Utensils className="size-4 text-primary" /> Items in this Order ({tracking.items.length})
            </h2>
            <ul className="divide-y divide-line">
              {tracking.items.map((item) => (
                <li key={item.id} className="py-3 flex items-center justify-between gap-4">
                  <div className="flex items-center gap-3">
                    <span
                      className={`size-4 rounded-sm border flex items-center justify-center shrink-0 ${item.foodType === 'VEG' ? 'border-emerald-600' : 'border-rose-600'
                        }`}
                    >
                      <span
                        className={`size-2 rounded-full ${item.foodType === 'VEG' ? 'bg-emerald-600' : 'bg-rose-600'
                          }`}
                      />
                    </span>
                    <div>
                      <p className="text-sm font-bold text-ink">{item.foodItemName}</p>
                      <p className="text-xs text-muted">
                        Qty: {item.quantity} × {inr(item.priceAtOrder)}
                      </p>
                    </div>
                  </div>
                  <span className="text-sm font-semibold text-ink">{inr(item.totalPrice)}</span>
                </li>
              ))}
            </ul>

            {/* Bill Summary */}
            <div className="mt-4 pt-4 border-t border-line space-y-2 text-xs">
              <div className="flex justify-between text-muted">
                <span>Subtotal</span>
                <span>{inr(tracking.subtotal)}</span>
              </div>
              <div className="flex justify-between text-muted">
                <span>Delivery Fee</span>
                <span>{tracking.deliveryFee > 0 ? inr(tracking.deliveryFee) : 'FREE'}</span>
              </div>
              {tracking.discountAmount > 0 && (
                <div className="flex justify-between text-emerald-600 font-medium">
                  <span>Discount</span>
                  <span>- {inr(tracking.discountAmount)}</span>
                </div>
              )}
              <div className="flex justify-between text-muted">
                <span>Taxes & Charges</span>
                <span>{inr(tracking.taxAmount)}</span>
              </div>
              <div className="flex justify-between text-sm font-black text-ink pt-2 border-t border-line">
                <span>Total Paid</span>
                <span>{inr(tracking.totalAmount)}</span>
              </div>
            </div>
          </div>

          {/* Delivery & Contact Information */}
          <div className="rounded-2xl border border-line bg-white p-6 shadow-sm">
            <h2 className="text-base font-bold text-ink mb-4 flex items-center gap-2">
              <MapPin className="size-4 text-primary" /> Delivery Information
            </h2>
            <div className="space-y-3 text-sm">
              <div>
                <span className="text-xs uppercase text-muted font-semibold block">Delivery Address</span>
                <p className="font-medium text-ink mt-0.5">
                  {tracking.deliveryAddress}, {tracking.deliveryCity}, {tracking.deliveryState} –{' '}
                  {tracking.deliveryPincode}
                </p>
              </div>

              <div className="grid grid-cols-2 gap-4 pt-2 border-t border-line">
                <div>
                  <span className="text-xs uppercase text-muted font-semibold block">Contact Phone</span>
                  <p className="font-medium text-ink mt-0.5 flex items-center gap-1.5">
                    <Phone className="size-3.5 text-muted" /> {tracking.customerPhone}
                  </p>
                </div>
                <div>
                  <span className="text-xs uppercase text-muted font-semibold block">Restaurant Phone</span>
                  <p className="font-medium text-ink mt-0.5 flex items-center gap-1.5">
                    <Phone className="size-3.5 text-muted" /> {tracking.restaurantPhone}
                  </p>
                </div>
              </div>

              {tracking.customerNote && (
                <div className="pt-2 border-t border-line">
                  <span className="text-xs uppercase text-muted font-semibold block">Delivery Instructions</span>
                  <p className="text-xs text-muted mt-0.5 italic">"{tracking.customerNote}"</p>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right Column: Status Transition History Audit Log */}
        <div className="md:col-span-5">
          <div className="rounded-2xl border border-line bg-white p-6 shadow-sm sticky top-24">
            <h2 className="text-base font-bold text-ink mb-4 flex items-center gap-2">
              <History className="size-4 text-primary" /> Status History Log
            </h2>

            <div className="flow-root">
              <ul className="-mb-6">
                {tracking.statusHistory.map((historyItem, idx) => {
                  const isLast = idx === tracking.statusHistory.length - 1

                  return (
                    <li key={historyItem.id || idx}>
                      <div className="relative pb-6">
                        {!isLast && (
                          <span
                            className="absolute left-3.5 top-4 -ml-px h-full w-0.5 bg-line"
                            aria-hidden="true"
                          />
                        )}
                        <div className="relative flex items-start space-x-3">
                          <div className="relative">
                            <span className="size-7 rounded-full bg-primary/10 border border-primary/30 flex items-center justify-center text-primary">
                              <Clock className="size-3.5" />
                            </span>
                          </div>
                          <div className="min-w-0 flex-1">
                            <div className="text-xs">
                              <span className="font-bold text-ink uppercase tracking-wide">
                                {historyItem.newStatus.replace(/_/g, ' ')}
                              </span>
                              <span className="text-muted ml-2">
                                by {historyItem.changedBy} ({historyItem.changedByRole})
                              </span>
                            </div>
                            {historyItem.remarks && (
                              <p className="mt-1 text-xs text-slate-600 bg-slate-50 p-2 rounded-lg border border-slate-100">
                                {historyItem.remarks}
                              </p>
                            )}
                            <p className="mt-1 text-[11px] text-muted">
                              {new Date(historyItem.changedAt).toLocaleTimeString('en-IN', {
                                hour: '2-digit',
                                minute: '2-digit',
                                second: '2-digit',
                              })}
                            </p>
                          </div>
                        </div>
                      </div>
                    </li>
                  )
                })}
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

