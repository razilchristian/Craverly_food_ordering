import {
  AlertCircle,
  Check,
  CheckCircle2,
  Clock,
  MapPin,
  Phone,
  Power,
  RefreshCw,
  Search,
  Store,
  User,
  X,
} from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { adminService } from '../../services/adminService'
import type { RestaurantResponse } from '../../types/admin'

export default function AdminRestaurantsPage() {
  const [restaurants, setRestaurants] = useState<RestaurantResponse[]>([])
  const [tab, setTab] = useState<'ALL' | 'PENDING' | 'APPROVED' | 'REJECTED'>('ALL')
  const [searchTerm, setSearchTerm] = useState('')
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  const loadRestaurants = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await adminService.getAllRestaurants()
      setRestaurants(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch restaurants')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadRestaurants()
  }, [loadRestaurants])

  const handleApprove = async (id: number, name: string) => {
    setActionLoading(id)
    setError(null)
    setSuccessMsg(null)
    try {
      await adminService.approveRestaurant(id)
      setSuccessMsg(`Restaurant "${name}" has been APPROVED.`)
      loadRestaurants()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to approve restaurant')
    } finally {
      setActionLoading(null)
    }
  }

  const handleReject = async (id: number, name: string) => {
    if (!window.confirm(`Are you sure you want to REJECT the application for "${name}"?`)) {
      return
    }
    setActionLoading(id)
    setError(null)
    setSuccessMsg(null)
    try {
      await adminService.rejectRestaurant(id)
      setSuccessMsg(`Restaurant "${name}" has been REJECTED.`)
      loadRestaurants()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to reject restaurant')
    } finally {
      setActionLoading(null)
    }
  }

  const handleToggleActive = async (id: number, currentActive: boolean, name: string) => {
    setActionLoading(id)
    setError(null)
    setSuccessMsg(null)
    try {
      if (currentActive) {
        await adminService.deactivateRestaurant(id)
        setSuccessMsg(`Restaurant "${name}" has been deactivated.`)
      } else {
        await adminService.activateRestaurant(id)
        setSuccessMsg(`Restaurant "${name}" has been activated.`)
      }
      loadRestaurants()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update restaurant status')
    } finally {
      setActionLoading(null)
    }
  }

  const filteredRestaurants = restaurants.filter((r) => {
    if (tab !== 'ALL' && r.status !== tab) return false
    if (!searchTerm.trim()) return true
    const term = searchTerm.toLowerCase()
    return (
      r.name.toLowerCase().includes(term) ||
      (r.cuisineType && r.cuisineType.toLowerCase().includes(term)) ||
      (r.city && r.city.toLowerCase().includes(term)) ||
      (r.ownerName && r.ownerName.toLowerCase().includes(term)) ||
      (r.ownerEmail && r.ownerEmail.toLowerCase().includes(term))
    )
  })

  const pendingCount = restaurants.filter((r) => r.status === 'PENDING').length
  const approvedCount = restaurants.filter((r) => r.status === 'APPROVED').length
  const rejectedCount = restaurants.filter((r) => r.status === 'REJECTED').length

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-ink">Restaurant Verification & Operations</h1>
          <p className="text-xs text-muted mt-0.5">
            Review partner enrollment requests, approve restaurants, and manage listing statuses
          </p>
        </div>
        <button
          onClick={() => loadRestaurants()}
          className="inline-flex items-center gap-1.5 rounded-xl border border-line bg-white px-3.5 py-2 text-xs font-semibold text-ink shadow-2xs hover:bg-slate-50 transition"
        >
          <RefreshCw className="size-3.5" /> Refresh List
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

      {/* Tabs and Search Bar */}
      <div className="rounded-2xl border border-line bg-white p-4 shadow-xs flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-1.5 border border-line p-1 rounded-xl bg-slate-50 text-xs font-semibold">
          <button
            onClick={() => setTab('ALL')}
            className={`px-3 py-1.5 rounded-lg transition ${
              tab === 'ALL' ? 'bg-white shadow-2xs text-ink font-bold' : 'text-muted hover:text-ink'
            }`}
          >
            All ({restaurants.length})
          </button>
          <button
            onClick={() => setTab('PENDING')}
            className={`px-3 py-1.5 rounded-lg transition flex items-center gap-1.5 ${
              tab === 'PENDING'
                ? 'bg-amber-500 text-white shadow-2xs font-bold'
                : 'text-amber-800 hover:text-amber-900'
            }`}
          >
            Pending Review ({pendingCount})
          </button>
          <button
            onClick={() => setTab('APPROVED')}
            className={`px-3 py-1.5 rounded-lg transition ${
              tab === 'APPROVED' ? 'bg-white shadow-2xs text-ink font-bold' : 'text-muted hover:text-ink'
            }`}
          >
            Approved ({approvedCount})
          </button>
          <button
            onClick={() => setTab('REJECTED')}
            className={`px-3 py-1.5 rounded-lg transition ${
              tab === 'REJECTED' ? 'bg-white shadow-2xs text-ink font-bold' : 'text-muted hover:text-ink'
            }`}
          >
            Rejected ({rejectedCount})
          </button>
        </div>

        <div className="relative min-w-[240px] flex-1 max-w-md">
          <Search className="size-4 text-muted absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search by restaurant, cuisine, city, or owner..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full rounded-xl border border-line pl-9 pr-3 py-2 text-xs text-ink focus:border-primary focus:outline-none"
          />
        </div>
      </div>

      {/* Restaurant Cards / List */}
      {loading ? (
        <div className="py-20 text-center">
          <RefreshCw className="mx-auto size-8 animate-spin text-primary" />
          <p className="mt-3 text-xs font-semibold text-muted">Loading restaurants...</p>
        </div>
      ) : filteredRestaurants.length === 0 ? (
        <div className="rounded-2xl border border-line bg-white p-16 text-center text-xs text-muted shadow-xs">
          <Store className="mx-auto size-10 text-slate-300 mb-2" />
          <p className="font-semibold text-ink text-sm">No restaurants found</p>
          <p className="text-slate-400 mt-1">No establishments matched the chosen filter or search query.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredRestaurants.map((r) => (
            <div
              key={r.id}
              className="rounded-2xl border border-line bg-white p-5 shadow-xs flex flex-col justify-between space-y-4 hover:shadow-sm transition"
            >
              <div className="space-y-3">
                {/* Title & Badges */}
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <h3 className="text-base font-bold text-ink">{r.name}</h3>
                    <p className="text-xs font-semibold text-primary">{r.cuisineType || 'Cuisine Unspecified'}</p>
                  </div>
                  <div className="flex flex-col items-end gap-1">
                    {r.status === 'PENDING' && (
                      <span className="rounded-full bg-amber-100 px-2.5 py-0.5 text-[10px] font-extrabold text-amber-800">
                        PENDING APPROVAL
                      </span>
                    )}
                    {r.status === 'APPROVED' && (
                      <span className="rounded-full bg-emerald-100 px-2.5 py-0.5 text-[10px] font-extrabold text-emerald-800">
                        APPROVED
                      </span>
                    )}
                    {r.status === 'REJECTED' && (
                      <span className="rounded-full bg-rose-100 px-2.5 py-0.5 text-[10px] font-extrabold text-rose-800">
                        REJECTED
                      </span>
                    )}
                    {r.status === 'APPROVED' && (
                      <span
                        className={`text-[10px] font-semibold ${
                          r.active ? 'text-emerald-700' : 'text-slate-400'
                        }`}
                      >
                        {r.active ? '● Active in Catalog' : '○ Suspended'}
                      </span>
                    )}
                  </div>
                </div>

                {r.description && (
                  <p className="text-xs text-muted line-clamp-2 leading-relaxed">{r.description}</p>
                )}

                {/* Details list */}
                <div className="pt-2 border-t border-line space-y-1.5 text-xs text-muted">
                  <div className="flex items-center gap-2">
                    <MapPin className="size-3.5 shrink-0 text-slate-400" />
                    <span className="truncate">
                      {r.address ? `${r.address}, ${r.city || ''}` : r.city || 'Address not provided'}
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <User className="size-3.5 shrink-0 text-slate-400" />
                    <span className="truncate">
                      Owner: {r.ownerName || 'Unknown'} ({r.ownerEmail || 'No email'})
                    </span>
                  </div>
                  {r.phone && (
                    <div className="flex items-center gap-2">
                      <Phone className="size-3.5 shrink-0 text-slate-400" />
                      <span>{r.phone}</span>
                    </div>
                  )}
                  {(r.openingTime || r.closingTime) && (
                    <div className="flex items-center gap-2">
                      <Clock className="size-3.5 shrink-0 text-slate-400" />
                      <span>
                        Hours: {r.openingTime || '09:00'} - {r.closingTime || '22:00'}
                      </span>
                    </div>
                  )}
                </div>
              </div>

              {/* Action Buttons */}
              <div className="pt-3 border-t border-line flex items-center gap-2">
                {r.status === 'PENDING' && (
                  <>
                    <button
                      onClick={() => handleApprove(r.id, r.name)}
                      disabled={actionLoading === r.id}
                      className="flex-1 inline-flex items-center justify-center gap-1.5 rounded-xl bg-emerald-600 px-3 py-2 text-xs font-bold text-white shadow-2xs hover:bg-emerald-700 transition disabled:opacity-50"
                    >
                      <Check className="size-3.5" /> Approve
                    </button>
                    <button
                      onClick={() => handleReject(r.id, r.name)}
                      disabled={actionLoading === r.id}
                      className="flex-1 inline-flex items-center justify-center gap-1.5 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-xs font-bold text-rose-700 hover:bg-rose-100 transition disabled:opacity-50"
                    >
                      <X className="size-3.5" /> Reject
                    </button>
                  </>
                )}

                {r.status === 'APPROVED' && (
                  <button
                    onClick={() => handleToggleActive(r.id, r.active, r.name)}
                    disabled={actionLoading === r.id}
                    className={`w-full inline-flex items-center justify-center gap-1.5 rounded-xl px-3 py-2 text-xs font-bold transition shadow-2xs disabled:opacity-50 ${
                      r.active
                        ? 'border border-amber-200 bg-amber-50 text-amber-800 hover:bg-amber-100'
                        : 'bg-emerald-600 text-white hover:bg-emerald-700'
                    }`}
                  >
                    <Power className="size-3.5" />
                    {r.active ? 'Deactivate Listing' : 'Reactivate Listing'}
                  </button>
                )}

                {r.status === 'REJECTED' && (
                  <button
                    onClick={() => handleApprove(r.id, r.name)}
                    disabled={actionLoading === r.id}
                    className="w-full inline-flex items-center justify-center gap-1.5 rounded-xl border border-line bg-slate-50 px-3 py-2 text-xs font-bold text-ink hover:bg-slate-100 transition disabled:opacity-50"
                  >
                    Reconsider & Approve
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
