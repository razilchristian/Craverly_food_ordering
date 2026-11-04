import {
  AlertCircle,
  CheckCircle2,
  ChevronRight,
  Clock,
  ExternalLink,
  Eye,
  Filter,
  Package,
  RefreshCw,
  Search,
  ShoppingBag,
  Truck,
} from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import { adminService } from '../../services/adminService'
import type { PageResponse } from '../../types/admin'
import { inr } from '../../utils/format'

interface OrderSummary {
  id: number
  orderNumber: string
  restaurantId: number
  restaurantName: string
  customerId: number
  customerName: string
  orderStatus: string
  paymentStatus: string
  itemCount: number
  totalAmount: number
  createdAt: string
}

export default function AdminOrdersPage() {
  const [ordersPage, setOrdersPage] = useState<PageResponse<OrderSummary> | null>(null)
  const [page, setPage] = useState(0)
  const [size] = useState(10)
  const [statusFilter, setStatusFilter] = useState('')
  const [searchTerm, setSearchTerm] = useState('')
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  const loadOrders = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const statusParam = statusFilter || undefined
      const searchParam = searchTerm.trim() || undefined

      const data = await adminService.getAdminOrders(
        page,
        size,
        statusParam,
        searchParam, // can pass as orderNumber
        searchParam  // or customer
      )
      setOrdersPage(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load orders')
    } finally {
      setLoading(false)
    }
  }, [page, size, statusFilter, searchTerm])

  useEffect(() => {
    loadOrders()
  }, [loadOrders])

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    loadOrders()
  }

  const handleMarkOutForDelivery = async (orderId: number, orderNumber: string) => {
    setActionLoading(orderId)
    setError(null)
    setSuccessMsg(null)
    try {
      const token = localStorage.getItem('cravely_token') || ''
      const res = await fetch(`http://localhost:8081/api/admin/orders/${orderId}/out-for-delivery`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
      })
      const data = await res.json()
      if (!res.ok || !data.success) {
        throw new Error(data.message || 'Failed to update order status')
      }
      setSuccessMsg(`Order ${orderNumber} is now OUT FOR DELIVERY.`)
      loadOrders()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update order')
    } finally {
      setActionLoading(null)
    }
  }

  const handleMarkDelivered = async (orderId: number, orderNumber: string) => {
    setActionLoading(orderId)
    setError(null)
    setSuccessMsg(null)
    try {
      const token = localStorage.getItem('cravely_token') || ''
      const res = await fetch(`http://localhost:8081/api/admin/orders/${orderId}/delivered`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
      })
      const data = await res.json()
      if (!res.ok || !data.success) {
        throw new Error(data.message || 'Failed to mark order as delivered')
      }
      setSuccessMsg(`Order ${orderNumber} has been marked as DELIVERED.`)
      loadOrders()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update order')
    } finally {
      setActionLoading(null)
    }
  }

  const getOrderStatusBadge = (status: string) => {
    switch (status) {
      case 'DELIVERED':
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-0.5 text-[11px] font-bold text-emerald-800">
            DELIVERED
          </span>
        )
      case 'OUT_FOR_DELIVERY':
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-indigo-100 px-2.5 py-0.5 text-[11px] font-bold text-indigo-800">
            <Truck className="size-3" /> OUT FOR DELIVERY
          </span>
        )
      case 'READY_FOR_PICKUP':
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-purple-100 px-2.5 py-0.5 text-[11px] font-bold text-purple-800">
            READY FOR PICKUP
          </span>
        )
      case 'PREPARING':
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2.5 py-0.5 text-[11px] font-bold text-amber-800">
            PREPARING
          </span>
        )
      case 'ACCEPTED':
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-blue-100 px-2.5 py-0.5 text-[11px] font-bold text-blue-800">
            ACCEPTED
          </span>
        )
      case 'PLACED':
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2.5 py-0.5 text-[11px] font-bold text-slate-800">
            PLACED
          </span>
        )
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-rose-100 px-2.5 py-0.5 text-[11px] font-bold text-rose-800">
            CANCELLED
          </span>
        )
      case 'REJECTED':
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-slate-200 px-2.5 py-0.5 text-[11px] font-bold text-slate-700">
            REJECTED
          </span>
        )
      default:
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2.5 py-0.5 text-[11px] font-bold text-slate-800">
            {status}
          </span>
        )
    }
  }

  const getPaymentStatusBadge = (status: string) => {
    switch (status) {
      case 'PAID':
        return (
          <span className="inline-flex items-center rounded bg-emerald-50 px-2 py-0.5 text-[10px] font-bold text-emerald-700 border border-emerald-200">
            PAID
          </span>
        )
      case 'PENDING':
        return (
          <span className="inline-flex items-center rounded bg-amber-50 px-2 py-0.5 text-[10px] font-bold text-amber-700 border border-amber-200">
            PENDING
          </span>
        )
      case 'FAILED':
        return (
          <span className="inline-flex items-center rounded bg-rose-50 px-2 py-0.5 text-[10px] font-bold text-rose-700 border border-rose-200">
            FAILED
          </span>
        )
      default:
        return <span className="text-[10px] text-muted">{status}</span>
    }
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-ink">Global Orders & Logistics</h1>
          <p className="text-xs text-muted mt-0.5">
            Monitor orders across all restaurants, track fulfillment lifecycles, and dispatch logistics
          </p>
        </div>
        <button
          onClick={() => loadOrders()}
          className="inline-flex items-center gap-1.5 rounded-xl border border-line bg-white px-3.5 py-2 text-xs font-semibold text-ink shadow-2xs hover:bg-slate-50 transition"
        >
          <RefreshCw className="size-3.5" /> Refresh Orders
        </button>
      </div>

      {/* Notifications */}
      {error && (
        <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-xs text-rose-800 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0 text-rose-600" />
          <span>{error}</span>
        </div>
      )}

      {successMsg && (
        <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-xs text-emerald-800 flex items-center gap-2">
          <CheckCircle2 className="size-4 shrink-0 text-emerald-600" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="rounded-2xl border border-line bg-white p-4 shadow-xs flex flex-wrap items-center justify-between gap-4">
        <form onSubmit={handleSearchSubmit} className="flex-1 min-w-[260px] flex items-center gap-2">
          <div className="relative flex-1">
            <Search className="size-4 text-muted absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search by Order # or Customer Name..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full rounded-xl border border-line pl-9 pr-3 py-2 text-xs text-ink focus:border-primary focus:outline-none"
            />
          </div>
          <button
            type="submit"
            className="rounded-xl bg-primary px-3.5 py-2 text-xs font-bold text-white shadow-2xs hover:bg-primary-dark transition"
          >
            Search
          </button>
        </form>

        <div className="flex items-center gap-2 text-xs">
          <Filter className="size-4 text-muted" />
          <span className="text-muted font-semibold">Status:</span>
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value)
              setPage(0)
            }}
            className="rounded-lg border border-line px-2.5 py-1.5 text-xs text-ink bg-white focus:outline-none"
          >
            <option value="">All Statuses</option>
            <option value="PLACED">PLACED</option>
            <option value="ACCEPTED">ACCEPTED</option>
            <option value="PREPARING">PREPARING</option>
            <option value="READY_FOR_PICKUP">READY FOR PICKUP</option>
            <option value="OUT_FOR_DELIVERY">OUT FOR DELIVERY</option>
            <option value="DELIVERED">DELIVERED</option>
            <option value="CANCELLED">CANCELLED</option>
            <option value="REJECTED">REJECTED</option>
          </select>
        </div>
      </div>

      {/* Orders Table */}
      <div className="rounded-2xl border border-line bg-white shadow-xs overflow-hidden">
        {loading ? (
          <div className="py-20 text-center">
            <RefreshCw className="mx-auto size-8 animate-spin text-primary" />
            <p className="mt-3 text-xs font-semibold text-muted">Retrieving order database records...</p>
          </div>
        ) : !ordersPage || ordersPage.content.length === 0 ? (
          <div className="py-16 text-center text-xs text-muted">
            <ShoppingBag className="mx-auto size-10 text-slate-300 mb-2" />
            <p className="font-semibold text-ink text-sm">No orders recorded</p>
            <p className="text-slate-400 mt-1">No orders matched the selected filter or query.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-line text-muted uppercase text-[10px] font-bold tracking-wider">
                <tr>
                  <th className="px-5 py-3.5">Order</th>
                  <th className="px-5 py-3.5">Customer</th>
                  <th className="px-5 py-3.5">Restaurant</th>
                  <th className="px-5 py-3.5">Items</th>
                  <th className="px-5 py-3.5">Total Amount</th>
                  <th className="px-5 py-3.5">Payment</th>
                  <th className="px-5 py-3.5">Order Status</th>
                  <th className="px-5 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {ordersPage.content.map((o) => (
                  <tr key={o.id} className="hover:bg-slate-50/50 transition">
                    <td className="px-5 py-4">
                      <div>
                        <span className="font-mono font-bold text-ink">{o.orderNumber}</span>
                        <p className="text-[11px] text-muted">
                          {new Date(o.createdAt).toLocaleDateString('en-IN', {
                            month: 'short',
                            day: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit',
                          })}
                        </p>
                      </div>
                    </td>
                    <td className="px-5 py-4">
                      <p className="font-bold text-ink">{o.customerName || 'Customer'}</p>
                      <p className="text-[11px] text-muted">ID: #{o.customerId}</p>
                    </td>
                    <td className="px-5 py-4">
                      <p className="font-semibold text-ink">{o.restaurantName || 'Restaurant'}</p>
                      <p className="text-[11px] text-muted">ID: #{o.restaurantId}</p>
                    </td>
                    <td className="px-5 py-4 font-semibold text-ink">
                      {o.itemCount} {o.itemCount === 1 ? 'item' : 'items'}
                    </td>
                    <td className="px-5 py-4 font-bold text-ink">{inr(o.totalAmount)}</td>
                    <td className="px-5 py-4">{getPaymentStatusBadge(o.paymentStatus)}</td>
                    <td className="px-5 py-4">{getOrderStatusBadge(o.orderStatus)}</td>
                    <td className="px-5 py-4 text-right space-x-2">
                      {/* Tracking Link */}
                      <Link
                        to={`/orders/${o.id}/tracking`}
                        className="inline-flex items-center gap-1 rounded-lg border border-line bg-white px-2.5 py-1 text-xs font-semibold text-ink hover:bg-slate-50 transition"
                        title="Live Tracking"
                      >
                        <Eye className="size-3" /> Track
                      </Link>

                      {/* State Dispatch Controls */}
                      {o.orderStatus === 'READY_FOR_PICKUP' && (
                        <button
                          onClick={() => handleMarkOutForDelivery(o.id, o.orderNumber)}
                          disabled={actionLoading === o.id}
                          className="inline-flex items-center gap-1 rounded-lg bg-indigo-600 px-2.5 py-1 text-xs font-bold text-white hover:bg-indigo-700 transition disabled:opacity-50"
                        >
                          <Truck className="size-3" /> Dispatch
                        </button>
                      )}

                      {o.orderStatus === 'OUT_FOR_DELIVERY' && (
                        <button
                          onClick={() => handleMarkDelivered(o.id, o.orderNumber)}
                          disabled={actionLoading === o.id}
                          className="inline-flex items-center gap-1 rounded-lg bg-emerald-600 px-2.5 py-1 text-xs font-bold text-white hover:bg-emerald-700 transition disabled:opacity-50"
                        >
                          <CheckCircle2 className="size-3" /> Deliver
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Controls */}
        {ordersPage && ordersPage.totalPages > 1 && (
          <div className="flex items-center justify-between px-5 py-3 border-t border-line bg-slate-50/50 text-xs text-muted">
            <span>
              Showing {ordersPage.number * ordersPage.size + 1} to{' '}
              {Math.min((ordersPage.number + 1) * ordersPage.size, ordersPage.totalElements)} of{' '}
              {ordersPage.totalElements} orders
            </span>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={ordersPage.number === 0}
                className="rounded-lg border border-line bg-white px-3 py-1 font-semibold text-ink shadow-2xs hover:bg-slate-50 disabled:opacity-40"
              >
                Previous
              </button>
              <span className="font-bold text-ink">
                Page {ordersPage.number + 1} of {ordersPage.totalPages}
              </span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={ordersPage.number >= ordersPage.totalPages - 1}
                className="rounded-lg border border-line bg-white px-3 py-1 font-semibold text-ink shadow-2xs hover:bg-slate-50 disabled:opacity-40"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

