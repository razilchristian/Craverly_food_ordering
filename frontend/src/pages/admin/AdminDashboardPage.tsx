import {
  AlertCircle,
  ArrowRight,
  CheckCircle2,
  Clock,
  DollarSign,
  PackageCheck,
  RefreshCw,
  ShoppingBag,
  Store,
  Users,
  UtensilsCrossed,
} from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import RevenueChart from '../../components/admin/RevenueChart'
import { adminService } from '../../services/adminService'
import type {
  DashboardOverviewResponse,
  OrderAnalyticsResponse,
  RecentActivityResponse,
  RevenueAnalyticsResponse,
} from '../../types/admin'
import { inr } from '../../utils/format'

export default function AdminDashboardPage() {
  const [overview, setOverview] = useState<DashboardOverviewResponse | null>(null)
  const [revenueData, setRevenueData] = useState<RevenueAnalyticsResponse | null>(null)
  const [orderAnalytics, setOrderAnalytics] = useState<OrderAnalyticsResponse | null>(null)
  const [recentActivity, setRecentActivity] = useState<RecentActivityResponse[]>([])

  const [period, setPeriod] = useState('monthly')
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadDashboardData = useCallback(async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true)
    setError(null)
    try {
      const [ov, rev, ord, act] = await Promise.all([
        adminService.getOverview(),
        adminService.getRevenueAnalytics(period),
        adminService.getOrderAnalytics(),
        adminService.getRecentActivity(10),
      ])
      setOverview(ov)
      setRevenueData(rev)
      setOrderAnalytics(ord)
      setRecentActivity(act)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load dashboard metrics')
    } finally {
      setLoading(false)
      if (isRefresh) setRefreshing(false)
    }
  }, [period])

  useEffect(() => {
    loadDashboardData()
  }, [loadDashboardData])

  const handlePeriodChange = async (newPeriod: string) => {
    setPeriod(newPeriod)
    try {
      const rev = await adminService.getRevenueAnalytics(newPeriod)
      setRevenueData(rev)
    } catch (err) {
      console.error(err)
    }
  }

  if (loading && !overview) {
    return (
      <div className="py-24 text-center">
        <RefreshCw className="mx-auto size-10 animate-spin text-primary" />
        <p className="mt-4 text-sm font-semibold text-muted">Loading live platform dashboard...</p>
      </div>
    )
  }

  return (
    <div className="space-y-8 max-w-7xl mx-auto">
      {/* Top Banner / Refresh */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-ink">Platform Executive Overview</h1>
          <p className="text-xs text-muted mt-0.5">
            Aggregated in real-time from MySQL database records
          </p>
        </div>
        <button
          onClick={() => loadDashboardData(true)}
          disabled={refreshing}
          className="inline-flex items-center gap-1.5 rounded-xl border border-line bg-white px-3.5 py-2 text-xs font-semibold text-ink shadow-2xs hover:bg-slate-50 transition disabled:opacity-50"
        >
          <RefreshCw className={`size-3.5 ${refreshing ? 'animate-spin text-primary' : ''}`} />
          Refresh Data
        </button>
      </div>

      {error && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Summary KPI Cards Grid */}
      {overview && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Revenue KPI */}
          <div className="rounded-2xl border border-line bg-white p-5 shadow-sm hover:shadow-md transition">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-muted uppercase tracking-wider">
                Total Revenue
              </span>
              <span className="size-9 rounded-xl bg-emerald-100 text-emerald-800 flex items-center justify-center">
                <DollarSign className="size-5" />
              </span>
            </div>
            <p className="text-2xl font-black text-ink mt-2">{inr(overview.totalRevenue)}</p>
            <div className="mt-3 pt-3 border-t border-line flex items-center justify-between text-[11px] text-muted">
              <span>Today: {inr(overview.todayRevenue)}</span>
              <span className="text-emerald-700 font-semibold">Captured</span>
            </div>
          </div>

          {/* Orders KPI */}
          <div className="rounded-2xl border border-line bg-white p-5 shadow-sm hover:shadow-md transition">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-muted uppercase tracking-wider">
                Total Orders
              </span>
              <span className="size-9 rounded-xl bg-blue-100 text-blue-800 flex items-center justify-center">
                <ShoppingBag className="size-5" />
              </span>
            </div>
            <p className="text-2xl font-black text-ink mt-2">{overview.totalOrders}</p>
            <div className="mt-3 pt-3 border-t border-line flex items-center justify-between text-[11px] text-muted">
              <span>{overview.pendingOrders} Active / Pending</span>
              <span>{overview.deliveredOrders} Delivered</span>
            </div>
          </div>

          {/* Restaurants KPI */}
          <div className="rounded-2xl border border-line bg-white p-5 shadow-sm hover:shadow-md transition">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-muted uppercase tracking-wider">
                Restaurants
              </span>
              <span className="size-9 rounded-xl bg-purple-100 text-purple-800 flex items-center justify-center">
                <Store className="size-5" />
              </span>
            </div>
            <p className="text-2xl font-black text-ink mt-2">{overview.totalRestaurants}</p>
            <div className="mt-3 pt-3 border-t border-line flex items-center justify-between text-[11px]">
              <span className="text-muted">{overview.approvedRestaurants} Approved</span>
              {overview.pendingRestaurants > 0 ? (
                <Link
                  to="/admin/restaurants"
                  className="font-bold text-amber-700 bg-amber-50 px-2 py-0.5 rounded border border-amber-200"
                >
                  {overview.pendingRestaurants} Pending
                </Link>
              ) : (
                <span className="text-muted">0 Pending</span>
              )}
            </div>
          </div>

          {/* Users KPI */}
          <div className="rounded-2xl border border-line bg-white p-5 shadow-sm hover:shadow-md transition">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-muted uppercase tracking-wider">
                Platform Users
              </span>
              <span className="size-9 rounded-xl bg-amber-100 text-amber-800 flex items-center justify-center">
                <Users className="size-5" />
              </span>
            </div>
            <p className="text-2xl font-black text-ink mt-2">{overview.totalUsers}</p>
            <div className="mt-3 pt-3 border-t border-line flex items-center justify-between text-[11px] text-muted">
              <span>{overview.totalCustomers} Cust • {overview.totalRestaurantOwners} Owners</span>
              <span className="font-semibold text-emerald-700">{overview.activeUsers} Active</span>
            </div>
          </div>
        </div>
      )}

      {/* Revenue Analytics Chart */}
      {revenueData && (
        <RevenueChart
          data={revenueData.data}
          totalRevenue={revenueData.totalRevenue}
          deliveredRevenue={revenueData.deliveredRevenue}
          reconciliationExposure={revenueData.reconciliationExposure}
          period={period}
          onPeriodChange={handlePeriodChange}
        />
      )}

      {/* Order Status Distribution & Quick Actions Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Order Lifecycle Pipeline Distribution */}
        <div className="lg:col-span-7 rounded-2xl border border-line bg-white p-6 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-base font-bold text-ink flex items-center gap-2">
              <PackageCheck className="size-4 text-primary" /> Order Status Distribution
            </h2>
            <Link
              to="/admin/orders"
              className="text-xs font-semibold text-primary hover:underline flex items-center gap-1"
            >
              View All Orders <ArrowRight className="size-3" />
            </Link>
          </div>

          {orderAnalytics && (
            <div className="space-y-3.5 pt-2">
              {[
                { label: 'Placed', count: orderAnalytics.placedOrders, color: 'bg-blue-500' },
                { label: 'Accepted', count: orderAnalytics.acceptedOrders, color: 'bg-indigo-500' },
                { label: 'Preparing', count: orderAnalytics.preparingOrders, color: 'bg-amber-500' },
                { label: 'Ready for Pickup', count: orderAnalytics.readyForPickupOrders, color: 'bg-purple-500' },
                { label: 'Out for Delivery', count: orderAnalytics.outForDeliveryOrders, color: 'bg-sky-500' },
                { label: 'Delivered', count: orderAnalytics.deliveredOrders, color: 'bg-emerald-500' },
                { label: 'Cancelled', count: orderAnalytics.cancelledOrders, color: 'bg-rose-500' },
                { label: 'Rejected', count: orderAnalytics.rejectedOrders, color: 'bg-red-700' },
              ].map((stage) => {
                const total = orderAnalytics.totalOrders || 1
                const pct = Math.round((stage.count / total) * 100)

                return (
                  <div key={stage.label}>
                    <div className="flex justify-between text-xs font-semibold mb-1">
                      <span className="text-ink">{stage.label}</span>
                      <span className="text-muted">
                        {stage.count} ({pct}%)
                      </span>
                    </div>
                    <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
                      <div
                        className={`h-full ${stage.color} rounded-full transition-all duration-300`}
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>

        {/* Recent Reliable Platform Activity */}
        <div className="lg:col-span-5 rounded-2xl border border-line bg-white p-6 shadow-sm flex flex-col">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-base font-bold text-ink flex items-center gap-2">
              <Clock className="size-4 text-primary" /> Recent Platform Activity
            </h2>
            <span className="text-[10px] uppercase font-bold text-muted bg-slate-100 px-2 py-0.5 rounded">
              Audited
            </span>
          </div>

          {recentActivity.length === 0 ? (
            <p className="text-xs text-muted text-center py-12">No activity logged yet.</p>
          ) : (
            <div className="flow-root flex-1">
              <ul className="-mb-4 divide-y divide-line">
                {recentActivity.map((act) => (
                  <li key={act.id} className="py-3">
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0 flex-1">
                        <p className="text-xs font-bold text-ink truncate">{act.title}</p>
                        <p className="text-[11px] text-muted truncate mt-0.5">{act.description}</p>
                        <div className="flex items-center gap-2 mt-1">
                          <span className="inline-block rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-medium text-slate-700">
                            {act.actor} ({act.actorRole})
                          </span>
                        </div>
                      </div>
                      <span className="text-[10px] text-muted shrink-0">
                        {new Date(act.timestamp).toLocaleTimeString([], {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </span>
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

