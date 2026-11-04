import {
  AlertCircle,
  CheckCircle2,
  Clock,
  Eye,
  Filter,
  Package,
  Power,
  RefreshCw,
  Search,
  Store,
  UtensilsCrossed,
} from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { adminService } from '../../services/adminService'
import type { FoodItemResponse } from '../../types/admin'
import { inr } from '../../utils/format'

export default function AdminFoodItemsPage() {
  const [items, setItems] = useState<FoodItemResponse[]>([])
  const [searchTerm, setSearchTerm] = useState('')
  const [typeFilter, setTypeFilter] = useState<'ALL' | 'VEG' | 'NON_VEG'>('ALL')
  const [availabilityFilter, setAvailabilityFilter] = useState<'ALL' | 'AVAILABLE' | 'UNAVAILABLE'>('ALL')
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  const loadFoodItems = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await adminService.getAllFoodItems()
      setItems(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch food items')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadFoodItems()
  }, [loadFoodItems])

  const handleToggleStatus = async (item: FoodItemResponse) => {
    setActionLoading(item.id)
    setError(null)
    setSuccessMsg(null)
    try {
      if (item.available) {
        await adminService.deactivateFoodItem(item.id)
        setSuccessMsg(`"${item.name}" has been DEACTIVATED from menus.`)
      } else {
        await adminService.activateFoodItem(item.id)
        setSuccessMsg(`"${item.name}" is now ACTIVATED and visible to customers.`)
      }
      loadFoodItems()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update item availability')
    } finally {
      setActionLoading(null)
    }
  }

  const filteredItems = items.filter((item) => {
    if (typeFilter !== 'ALL' && item.foodType !== typeFilter) return false
    if (availabilityFilter === 'AVAILABLE' && !item.available) return false
    if (availabilityFilter === 'UNAVAILABLE' && item.available) return false

    if (!searchTerm.trim()) return true
    const term = searchTerm.toLowerCase()
    return (
      item.name.toLowerCase().includes(term) ||
      (item.restaurantName && item.restaurantName.toLowerCase().includes(term)) ||
      (item.categoryName && item.categoryName.toLowerCase().includes(term)) ||
      (item.description && item.description.toLowerCase().includes(term))
    )
  })

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-ink">Food Catalog & Menu Moderation</h1>
          <p className="text-xs text-muted mt-0.5">
            Monitor dishes across all partner restaurants, verify listings, and manage availability
          </p>
        </div>
        <button
          onClick={() => loadFoodItems()}
          className="inline-flex items-center gap-1.5 rounded-xl border border-line bg-white px-3.5 py-2 text-xs font-semibold text-ink shadow-2xs hover:bg-slate-50 transition"
        >
          <RefreshCw className="size-3.5" /> Refresh Items
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
        <div className="relative min-w-[260px] flex-1 max-w-md">
          <Search className="size-4 text-muted absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search by dish name, restaurant, or category..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full rounded-xl border border-line pl-9 pr-3 py-2 text-xs text-ink focus:border-primary focus:outline-none"
          />
        </div>

        <div className="flex flex-wrap items-center gap-3 text-xs">
          <div className="flex items-center gap-1.5">
            <span className="text-muted font-semibold">Diet:</span>
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value as any)}
              className="rounded-lg border border-line px-2.5 py-1.5 text-xs text-ink bg-white focus:outline-none"
            >
              <option value="ALL">All Types</option>
              <option value="VEG">Vegetarian Only</option>
              <option value="NON_VEG">Non-Vegetarian Only</option>
            </select>
          </div>

          <div className="flex items-center gap-1.5">
            <span className="text-muted font-semibold">Availability:</span>
            <select
              value={availabilityFilter}
              onChange={(e) => setAvailabilityFilter(e.target.value as any)}
              className="rounded-lg border border-line px-2.5 py-1.5 text-xs text-ink bg-white focus:outline-none"
            >
              <option value="ALL">All Statuses</option>
              <option value="AVAILABLE">Available Only</option>
              <option value="UNAVAILABLE">Suspended / Out of Stock</option>
            </select>
          </div>
        </div>
      </div>

      {/* Items Grid */}
      {loading ? (
        <div className="py-20 text-center">
          <RefreshCw className="mx-auto size-8 animate-spin text-primary" />
          <p className="mt-3 text-xs font-semibold text-muted">Retrieving food catalog...</p>
        </div>
      ) : filteredItems.length === 0 ? (
        <div className="rounded-2xl border border-line bg-white p-16 text-center text-xs text-muted shadow-xs">
          <UtensilsCrossed className="mx-auto size-10 text-slate-300 mb-2" />
          <p className="font-semibold text-ink text-sm">No food items found</p>
          <p className="text-slate-400 mt-1">Adjust search query or filter parameters to find items.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredItems.map((item) => (
            <div
              key={item.id}
              className="rounded-2xl border border-line bg-white p-5 shadow-xs flex flex-col justify-between space-y-4 hover:shadow-sm transition"
            >
              <div className="space-y-3">
                {/* Header & Badges */}
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span
                      className={`inline-flex items-center justify-center size-4 rounded-sm border shrink-0 ${
                        item.foodType === 'VEG'
                          ? 'border-emerald-600 text-emerald-600'
                          : 'border-rose-600 text-rose-600'
                      }`}
                      title={item.foodType}
                    >
                      <span
                        className={`size-2 rounded-full ${
                          item.foodType === 'VEG' ? 'bg-emerald-600' : 'bg-rose-600'
                        }`}
                      />
                    </span>
                    <h3 className="text-sm font-bold text-ink truncate">{item.name}</h3>
                  </div>

                  {item.available ? (
                    <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 border border-emerald-200 px-2 py-0.5 text-[10px] font-bold text-emerald-700">
                      Available
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 rounded-full bg-rose-50 border border-rose-200 px-2 py-0.5 text-[10px] font-bold text-rose-700">
                      Suspended
                    </span>
                  )}
                </div>

                {item.description && (
                  <p className="text-xs text-muted line-clamp-2 leading-relaxed">
                    {item.description}
                  </p>
                )}

                {/* Details */}
                <div className="pt-2 border-t border-line space-y-1.5 text-xs text-muted">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-500">Price:</span>
                    <span className="font-extrabold text-ink">{inr(item.price)}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-slate-500">Restaurant:</span>
                    <span className="font-semibold text-ink truncate max-w-[180px]">
                      {item.restaurantName || `Restaurant #${item.restaurantId}`}
                    </span>
                  </div>
                  {item.categoryName && (
                    <div className="flex items-center justify-between">
                      <span className="text-slate-500">Category:</span>
                      <span className="font-medium text-slate-700">{item.categoryName}</span>
                    </div>
                  )}
                  {item.preparationTime && (
                    <div className="flex items-center justify-between">
                      <span className="text-slate-500">Prep Time:</span>
                      <span className="font-medium text-slate-700">{item.preparationTime} mins</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Action Button */}
              <div className="pt-3 border-t border-line">
                <button
                  onClick={() => handleToggleStatus(item)}
                  disabled={actionLoading === item.id}
                  className={`w-full inline-flex items-center justify-center gap-1.5 rounded-xl px-3 py-2 text-xs font-bold transition shadow-2xs disabled:opacity-50 ${
                    item.available
                      ? 'border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100'
                      : 'border border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                  }`}
                >
                  <Power className="size-3.5" />
                  {item.available ? 'Deactivate Dish' : 'Activate Dish'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

