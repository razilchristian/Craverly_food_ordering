import {
  AlertCircle,
  Calendar,
  DollarSign,
  PieChart,
  RefreshCw,
  ShoppingBag,
  Store,
  TrendingUp,
  Users,
} from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import RevenueChart from '../../components/admin/RevenueChart'
import { adminService } from '../../services/adminService'
import type {
  OrderAnalyticsResponse,
  RestaurantAnalyticsResponse,
  RevenueAnalyticsResponse,
  UserAnalyticsResponse,
} from '../../types/admin'
import { inr } from '../../utils/format'

export default function AdminAnalyticsPage() {
  const [period, setPeriod] = useState('monthly')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')

  const [revenue, setRevenue] = useState<RevenueAnalyticsResponse | null>(null)
  const [orders, setOrders] = useState<OrderAnalyticsResponse | null>(null)
  const [restaurants, setRestaurants] = useState<RestaurantAnalyticsResponse | null>(null)
  const [users, setUsers] = useState<UserAnalyticsResponse | null>(null)

  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadAnalytics = useCallback(async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true)
    setError(null)
    try {
      const [revData, ordData, restData, userData] = await Promise.all([
        adminService.getRevenueAnalytics(period, startDate || undefined, endDate || undefined),
        adminService.getOrderAnalytics(startDate || undefined, endDate || undefined),
        adminService.getRestaurantAnalytics(startDate || undefined, endDate || undefined),
        adminService.getUserAnalytics(startDate || undefined, endDate || undefined),
      ])
      setRevenue(revData)
      setOrders(ordData)
      setRestaurants(restData)
      setUsers(userData)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load business analytics')
    } finally {
      setLoading(false)
      if (isRefresh) setRefreshing(false)
    }
  }, [period, startDate, endDate])

  useEffect(() => {
    loadAnalytics()
  }, [loadAnalytics])

  const handlePeriodChange = (newPeriod: string) => {
    setPeriod(newPeriod)
  }

  const handleApplyDateFilter = (e: React.FormEvent) => {
    e.preventDefault()
    loadAnalytics()
  }

  const handleClearDateFilter = () => {
    setStartDate('')
    setEndDate('')
  }

  if (loading && !revenue) {
    return (
      <div className="py-24 text-center">
        <RefreshCw className="mx-auto size-10 animate-spin text-primary" />
        <p className="mt-4 text-sm font-semibold text-muted">Aggregating platform analytics...</p>
      </div>
    )
  }

  const totalOrdersCount = orders?.totalOrders || 0
  const deliveryRate = totalOrdersCount > 0 ? (((orders?.deliveredOrders || 0) / totalOrdersCount) * 100).toFixed(1) : '0'
  const cancellationRate = totalOrdersCount > 0 ? (((orders?.cancelledOrders || 0) / totalOrdersCount) * 100).toFixed(1) : '0'
  const rejectionRate = totalOrdersCount > 0 ? (((orders?.rejectedOrders || 0) / totalOrdersCount) * 100).toFixed(1) : '0'

  return (
    <div className="space-y-8 max-w-7xl mx-auto">
      {/* Page Header */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-ink">Business & Operations Analytics</h1>
          <p className="text-xs text-muted mt-0.5">
            Deep financial breakdowns, order lifecycle distributions, and user engagement metrics
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => loadAnalytics(true)}
            disabled={refreshing}
            className="inline-flex items-center gap-1.5 rounded-xl border border-line bg-white px-3.5 py-2 text-xs font-semibold text-ink shadow-2xs hover:bg-slate-50 transition disabled:opacity-50"
          >
            <RefreshCw className={`size-3.5 ${refreshing ? 'animate-spin text-primary' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {/* Date Filter Bar */}
      <form
        onSubmit={handleApplyDateFilter}
        className="rounded-2xl border border-line bg-white p-4 shadow-xs flex flex-wrap items-center gap-4 text-xs font-semibold"
      >
        <div className="flex items-center gap-2 text-muted">
          <Calendar className="size-4" />
          <span>Date Range Filter:</span>
        </div>
        <div className="flex items-center gap-2">
          <label htmlFor="startDate" className="text-muted text-[11px]">Start:</label>
          <input
            id="startDate"
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="rounded-lg border border-line px-2.5 py-1.5 text-xs text-ink focus:border-primary focus:outline-none"
          />
        </div>
        <div className="flex items-center gap-2">
          <label htmlFor="endDate" className="text-muted text-[11px]">End:</label>
          <input
            id="endDate"
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="rounded-lg border border-line px-2.5 py-1.5 text-xs text-ink focus:border-primary focus:outline-none"
          />
        </div>
        <button
          type="submit"
          className="rounded-lg bg-primary px-3 py-1.5 font-bold text-white shadow-2xs hover:bg-primary-dark transition"
        >
          Apply Filter
        </button>
        {(startDate || endDate) && (
          <button
            type="button"
            onClick={handleClearDateFilter}
            className="text-muted hover:text-ink underline transition"
          >
            Clear Filter
          </button>
        )}
      </form>

      {error && (
        <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-xs text-rose-800 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0 text-rose-600" />
          <span>{error}</span>
        </div>
      )}

      {/* Interactive Revenue Chart */}
      <section>
        <RevenueChart data={revenue} period={period} onPeriodChange={handlePeriodChange} />
      </section>

      {/* Order Status Distribution & Lifecycle Funnel */}
      <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 rounded-2xl border border-line bg-white p-6 shadow-xs space-y-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <ShoppingBag className="size-5 text-primary" />
              <h2 className="text-base font-bold text-ink">Order Lifecycle Distribution</h2>
            </div>
            <span className="text-xs font-semibold text-muted">
              Total Recorded: <strong className="text-ink">{orders?.totalOrders || 0}</strong>
            </span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="rounded-xl border border-line bg-slate-50/70 p-3">
              <p className="text-[11px] font-semibold text-muted">PLACED</p>
              <p className="text-lg font-black text-ink mt-1">{orders?.placedOrders || 0}</p>
            </div>
            <div className="rounded-xl border border-line bg-blue-50/70 p-3">
              <p className="text-[11px] font-semibold text-blue-800">ACCEPTED</p>
              <p className="text-lg font-black text-blue-900 mt-1">{orders?.acceptedOrders || 0}</p>
            </div>
            <div className="rounded-xl border border-line bg-amber-50/70 p-3">
              <p className="text-[11px] font-semibold text-amber-800">PREPARING</p>
              <p className="text-lg font-black text-amber-900 mt-1">{orders?.preparingOrders || 0}</p>
            </div>
            <div className="rounded-xl border border-line bg-purple-50/70 p-3">
              <p className="text-[11px] font-semibold text-purple-800">READY</p>
              <p className="text-lg font-black text-purple-900 mt-1">{orders?.readyForPickupOrders || 0}</p>
            </div>
            <div className="rounded-xl border border-line bg-indigo-50/70 p-3">
              <p className="text-[11px] font-semibold text-indigo-800">OUT FOR DELIVERY</p>
              <p className="text-lg font-black text-indigo-900 mt-1">{orders?.outForDeliveryOrders || 0}</p>
            </div>
            <div className="rounded-xl border border-line bg-emerald-50/70 p-3">
              <p className="text-[11px] font-semibold text-emerald-800">DELIVERED</p>
              <p className="text-lg font-black text-emerald-900 mt-1">{orders?.deliveredOrders || 0}</p>
            </div>
            <div className="rounded-xl border border-line bg-rose-50/70 p-3">
              <p className="text-[11px] font-semibold text-rose-800">CANCELLED</p>
              <p className="text-lg font-black text-rose-900 mt-1">{orders?.cancelledOrders || 0}</p>
            </div>
            <div className="rounded-xl border border-line bg-slate-100 p-3">
              <p className="text-[11px] font-semibold text-slate-700">REJECTED</p>
              <p className="text-lg font-black text-slate-900 mt-1">{orders?.rejectedOrders || 0}</p>
            </div>
          </div>

          {/* Operational Success Metrics */}
          <div className="pt-4 border-t border-line grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="p-3 bg-emerald-50/50 rounded-xl border border-emerald-100">
              <p className="text-[11px] font-semibold text-emerald-800">Fulfillment Rate</p>
              <p className="text-xl font-black text-emerald-700 mt-1">{deliveryRate}%</p>
              <p className="text-[10px] text-emerald-600 mt-0.5">Delivered / Total Orders</p>
            </div>
            <div className="p-3 bg-rose-50/50 rounded-xl border border-rose-100">
              <p className="text-[11px] font-semibold text-rose-800">Customer Cancellation Rate</p>
              <p className="text-xl font-black text-rose-700 mt-1">{cancellationRate}%</p>
              <p className="text-[10px] text-rose-600 mt-0.5">Cancelled / Total Orders</p>
            </div>
            <div className="p-3 bg-amber-50/50 rounded-xl border border-amber-100">
              <p className="text-[11px] font-semibold text-amber-800">Restaurant Rejection Rate</p>
              <p className="text-xl font-black text-amber-700 mt-1">{rejectionRate}%</p>
              <p className="text-[10px] text-amber-600 mt-0.5">Rejected / Total Orders</p>
            </div>
          </div>
        </div>

        {/* User Breakdown Card */}
        <div className="rounded-2xl border border-line bg-white p-6 shadow-xs space-y-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-2 mb-4">
              <Users className="size-5 text-primary" />
              <h2 className="text-base font-bold text-ink">User Community</h2>
            </div>

            <div className="space-y-3">
              <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-line">
                <span className="text-xs font-semibold text-slate-700">Customers</span>
                <span className="text-sm font-bold text-ink">{users?.customers || 0}</span>
              </div>
              <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-line">
                <span className="text-xs font-semibold text-slate-700">Restaurant Owners</span>
                <span className="text-sm font-bold text-ink">{users?.restaurantOwners || 0}</span>
              </div>
              <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-line">
                <span className="text-xs font-semibold text-slate-700">System Administrators</span>
                <span className="text-sm font-bold text-ink">{users?.admins || 0}</span>
              </div>
            </div>
          </div>

          <div className="pt-4 border-t border-line space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="text-muted">Active Users:</span>
              <span className="font-bold text-emerald-700">{users?.activeUsers || 0}</span>
            </div>
            <div className="flex items-center justify-between text-xs">
              <span className="text-muted">Deactivated Users:</span>
              <span className="font-bold text-rose-700">{users?.deactivatedUsers || 0}</span>
            </div>
            <div className="flex items-center justify-between text-xs pt-1 border-t border-line">
              <span className="text-muted font-bold">Total Platform Users:</span>
              <span className="font-extrabold text-ink">{users?.totalUsers || 0}</span>
            </div>
          </div>
        </div>
      </section>

      {/* Restaurant Network & Cuisine Popularity */}
      <section className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Restaurant Status */}
        <div className="rounded-2xl border border-line bg-white p-6 shadow-xs space-y-4">
          <div className="flex items-center gap-2">
            <Store className="size-5 text-primary" />
            <h2 className="text-base font-bold text-ink">Restaurant Network Health</h2>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
            <div className="p-3.5 rounded-xl border border-line bg-slate-50">
              <p className="text-[11px] font-semibold text-muted">Total Enrolled</p>
              <p className="text-xl font-black text-ink mt-1">{restaurants?.totalRestaurants || 0}</p>
            </div>
            <div className="p-3.5 rounded-xl border border-amber-200 bg-amber-50">
              <p className="text-[11px] font-semibold text-amber-800">Pending Review</p>
              <p className="text-xl font-black text-amber-900 mt-1">{restaurants?.pendingApprovals || 0}</p>
            </div>
            <div className="p-3.5 rounded-xl border border-emerald-200 bg-emerald-50">
              <p className="text-[11px] font-semibold text-emerald-800">Approved</p>
              <p className="text-xl font-black text-emerald-900 mt-1">{restaurants?.approvedRestaurants || 0}</p>
            </div>
            <div className="p-3.5 rounded-xl border border-rose-200 bg-rose-50">
              <p className="text-[11px] font-semibold text-rose-800">Rejected</p>
              <p className="text-xl font-black text-rose-900 mt-1">{restaurants?.rejectedRestaurants || 0}</p>
            </div>
            <div className="p-3.5 rounded-xl border border-emerald-200 bg-emerald-50/50">
              <p className="text-[11px] font-semibold text-emerald-700">Currently Active</p>
              <p className="text-xl font-black text-emerald-800 mt-1">{restaurants?.activeRestaurants || 0}</p>
            </div>
            <div className="p-3.5 rounded-xl border border-line bg-slate-100">
              <p className="text-[11px] font-semibold text-slate-600">Inactive / Suspended</p>
              <p className="text-xl font-black text-slate-800 mt-1">{restaurants?.inactiveRestaurants || 0}</p>
            </div>
          </div>
        </div>

        {/* Cuisine Distribution */}
        <div className="rounded-2xl border border-line bg-white p-6 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <PieChart className="size-5 text-primary" />
              <h2 className="text-base font-bold text-ink">Cuisine Distribution</h2>
            </div>
            <span className="text-xs text-muted">Variety on Cravely</span>
          </div>

          {(!restaurants?.cuisineDistribution || restaurants.cuisineDistribution.length === 0) ? (
            <div className="py-8 text-center text-xs text-muted">No cuisine data recorded yet.</div>
          ) : (
            <div className="space-y-3">
              {restaurants.cuisineDistribution.map((c) => {
                const totalR = restaurants.totalRestaurants || 1
                const pct = Math.round((c.count / totalR) * 100)
                return (
                  <div key={c.cuisine} className="space-y-1">
                    <div className="flex items-center justify-between text-xs font-semibold">
                      <span className="text-ink">{c.cuisine || 'Unspecified'}</span>
                      <span className="text-muted">
                        {c.count} ({pct}%)
                      </span>
                    </div>
                    <div className="h-2 w-full rounded-full bg-slate-100 overflow-hidden">
                      <div
                        className="h-full rounded-full bg-primary transition-all duration-500"
                        style={{ width: `${Math.min(pct, 100)}%` }}
                      />
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </section>
    </div>
  )
}
