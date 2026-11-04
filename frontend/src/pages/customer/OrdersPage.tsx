import { ArrowRight, Clock, Navigation, RefreshCw, ShoppingBag } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { EmptyState } from '../../components/common/ui'
import { orderService } from '../../services/orderService'
import type { OrderSummaryResponse } from '../../types/tracking'
import { inr } from '../../utils/format'

export default function OrdersPage() {
  const [orders, setOrders] = useState<OrderSummaryResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)

  const fetchOrders = async (targetPage = 0) => {
    setLoading(true)
    setError(null)
    try {
      const data = await orderService.getMyOrders(targetPage, 10)
      setOrders(data.content)
      setTotalPages(data.totalPages)
      setPage(data.number)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch orders')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchOrders(0)
  }, [])

  if (loading && orders.length === 0) {
    return (
      <div className="container-app py-16 text-center">
        <RefreshCw className="mx-auto size-10 animate-spin text-primary" />
        <p className="mt-4 text-base font-semibold text-muted">Loading your past orders...</p>
      </div>
    )
  }

  return (
    <div className="container-app py-8 md:py-12 max-w-4xl">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-black text-ink">My Orders</h1>
          <p className="mt-1 text-sm text-muted">Track active deliveries and review your order history</p>
        </div>
        <button
          onClick={() => fetchOrders(page)}
          className="inline-flex items-center gap-1.5 rounded-xl border border-line bg-white px-3.5 py-2 text-xs font-semibold text-ink shadow-2xs hover:bg-cream transition"
        >
          <RefreshCw className={`size-3.5 ${loading ? 'animate-spin text-primary' : ''}`} />
          Refresh
        </button>
      </div>

      {error && (
        <div className="mb-6 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">
          {error}
        </div>
      )}

      {orders.length === 0 ? (
        <EmptyState
          title="No orders yet"
          text="You haven't placed any delicious orders yet. Explore our curated restaurants!"
          action={
            <Link to="/restaurants" className="btn-primary">
              Explore Restaurants
            </Link>
          }
        />
      ) : (
        <div className="space-y-4">
          {orders.map((order) => (
            <div
              key={order.id}
              className="rounded-2xl border border-line bg-white p-5 shadow-sm hover:shadow-md transition"
            >
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <div className="flex items-center gap-3">
                    <span className="font-mono text-base font-bold text-ink">{order.orderNumber}</span>
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider ${
                        order.orderStatus === 'DELIVERED'
                          ? 'bg-emerald-100 text-emerald-800'
                          : order.orderStatus === 'CANCELLED' || order.orderStatus === 'REJECTED'
                            ? 'bg-rose-100 text-rose-800'
                            : 'bg-primary/15 text-primary'
                      }`}
                    >
                      {order.orderStatus.replace(/_/g, ' ')}
                    </span>
                    <span
                      className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-semibold ${
                        order.paymentStatus === 'PAID'
                          ? 'bg-emerald-50 text-emerald-700'
                          : 'bg-amber-50 text-amber-700'
                      }`}
                    >
                      {order.paymentStatus}
                    </span>
                  </div>

                  <h3 className="mt-2 text-lg font-bold text-ink">{order.restaurantName}</h3>
                  <div className="mt-1 flex items-center gap-3 text-xs text-muted">
                    <span className="flex items-center gap-1">
                      <Clock className="size-3.5" />
                      {new Date(order.createdAt).toLocaleString('en-IN', {
                        dateStyle: 'medium',
                        timeStyle: 'short',
                      })}
                    </span>
                    <span>•</span>
                    <span className="flex items-center gap-1">
                      <ShoppingBag className="size-3.5" /> {order.itemCount} items
                    </span>
                  </div>
                </div>

                <div className="flex flex-col items-end gap-3">
                  <span className="text-lg font-black text-ink">{inr(order.totalAmount)}</span>
                  <Link
                    to={`/orders/${order.id}/tracking`}
                    className="btn-primary inline-flex items-center gap-2 text-xs py-2 px-4 shadow-sm"
                  >
                    <Navigation className="size-3.5" /> Track Order
                  </Link>
                </div>
              </div>
            </div>
          ))}

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="mt-8 flex justify-center gap-3">
              <button
                disabled={page <= 0}
                onClick={() => fetchOrders(page - 1)}
                className="btn-secondary text-xs disabled:opacity-40"
              >
                Previous
              </button>
              <span className="inline-flex items-center px-4 text-xs font-semibold text-muted">
                Page {page + 1} of {totalPages}
              </span>
              <button
                disabled={page >= totalPages - 1}
                onClick={() => fetchOrders(page + 1)}
                className="btn-secondary text-xs disabled:opacity-40"
              >
                Next
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

