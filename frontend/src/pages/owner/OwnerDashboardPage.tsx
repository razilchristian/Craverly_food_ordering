import {
  AlertCircle,
  Check,
  CheckCircle2,
  Clock,
  Eye,
  Filter,
  Plus,
  Power,
  RefreshCw,
  Search,
  ShoppingBag,
  Store,
  UtensilsCrossed,
  X,
} from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import { ownerService, type OwnerFoodItem, type OwnerOrderSummary, type OwnerRestaurant } from '../../services/ownerService'
import { inr } from '../../utils/format'

export default function OwnerDashboardPage() {
  const [restaurants, setRestaurants] = useState<OwnerRestaurant[]>([])
  const [selectedRestaurant, setSelectedRestaurant] = useState<OwnerRestaurant | null>(null)
  const [orders, setOrders] = useState<OwnerOrderSummary[]>([])
  const [items, setItems] = useState<OwnerFoodItem[]>([])
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  // Modals
  const [showCreateRestaurant, setShowCreateRestaurant] = useState(false)
  const [showAddItem, setShowAddItem] = useState(false)
  const [rejectModalOrder, setRejectModalOrder] = useState<OwnerOrderSummary | null>(null)
  const [rejectReason, setRejectReason] = useState('')

  // Create restaurant form state
  const [restName, setRestName] = useState('')
  const [restCuisine, setRestCuisine] = useState('')
  const [restAddress, setRestAddress] = useState('')
  const [restCity, setRestCity] = useState('Ahmedabad')
  const [restState, setRestState] = useState('Gujarat')
  const [restPincode, setRestPincode] = useState('380001')
  const [restPhone, setRestPhone] = useState('9876543201')
  const [restEmail, setRestEmail] = useState('kitchen@example.com')

  // Add item form state
  const [itemName, setItemName] = useState('')
  const [itemPrice, setItemPrice] = useState('199')
  const [itemType, setItemType] = useState<'VEG' | 'NON_VEG'>('VEG')
  const [itemPrepTime, setItemPrepTime] = useState('20')
  const [itemDesc, setItemDesc] = useState('')

  const loadData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const myRests = await ownerService.getMyRestaurants()
      setRestaurants(myRests)
      if (myRests.length > 0) {
        const activeRest = selectedRestaurant
          ? myRests.find((r) => r.id === selectedRestaurant.id) || myRests[0]
          : myRests[0]
        setSelectedRestaurant(activeRest)

        const [orderPage, foodItems] = await Promise.all([
          ownerService.getRestaurantOrders(activeRest.id),
          ownerService.getRestaurantItems(activeRest.id).catch(() => []),
        ])
        setOrders(orderPage.content || [])
        setItems(foodItems || [])
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load restaurant data')
    } finally {
      setLoading(false)
    }
  }, [selectedRestaurant])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleSelectRestaurant = async (r: OwnerRestaurant) => {
    setSelectedRestaurant(r)
    setLoading(true)
    try {
      const [orderPage, foodItems] = await Promise.all([
        ownerService.getRestaurantOrders(r.id),
        ownerService.getRestaurantItems(r.id).catch(() => []),
      ])
      setOrders(orderPage.content || [])
      setItems(foodItems || [])
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to switch restaurant')
    } finally {
      setLoading(false)
    }
  }

  // Order state actions
  const handleAcceptOrder = async (orderId: number) => {
    setActionLoading(orderId)
    setError(null)
    setSuccessMsg(null)
    try {
      await ownerService.acceptOrder(orderId)
      setSuccessMsg('Order accepted and sent to kitchen!')
      loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to accept order')
    } finally {
      setActionLoading(null)
    }
  }

  const handleRejectOrder = async () => {
    if (!rejectModalOrder) return
    setActionLoading(rejectModalOrder.id)
    setError(null)
    setSuccessMsg(null)
    try {
      await ownerService.rejectOrder(rejectModalOrder.id, rejectReason || 'Kitchen is currently overwhelmed')
      setSuccessMsg(`Order ${rejectModalOrder.orderNumber} rejected.`)
      setRejectModalOrder(null)
      setRejectReason('')
      loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to reject order')
    } finally {
      setActionLoading(null)
    }
  }

  const handleMarkPreparing = async (orderId: number) => {
    setActionLoading(orderId)
    setError(null)
    setSuccessMsg(null)
    try {
      await ownerService.markPreparing(orderId)
      setSuccessMsg('Order marked as PREPARING.')
      loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to mark preparing')
    } finally {
      setActionLoading(null)
    }
  }

  const handleMarkReady = async (orderId: number) => {
    setActionLoading(orderId)
    setError(null)
    setSuccessMsg(null)
    try {
      await ownerService.markReady(orderId)
      setSuccessMsg('Order is READY FOR PICKUP. Platform admin has been notified for dispatch.')
      loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to mark ready')
    } finally {
      setActionLoading(null)
    }
  }

  const handleToggleAvailability = async (item: OwnerFoodItem) => {
    setActionLoading(item.id)
    try {
      await ownerService.toggleFoodAvailability(item.id, !item.available)
      setSuccessMsg(`Availability updated for ${item.name}`)
      loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update item availability')
    } finally {
      setActionLoading(null)
    }
  }

  const handleCreateRestaurant = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const newRest = await ownerService.createRestaurant({
        name: restName,
        cuisineType: restCuisine,
        address: restAddress,
        city: restCity,
        state: restState,
        pincode: restPincode,
        phone: restPhone,
        email: restEmail,
        openingTime: '09:00',
        closingTime: '23:00',
      })
      setSuccessMsg(`Restaurant "${newRest.name}" registered! Submitted for Administrator review.`)
      setShowCreateRestaurant(false)
      loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to register restaurant')
    } finally {
      setLoading(false)
    }
  }

  const handleAddFoodItem = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedRestaurant) return
    setLoading(true)
    setError(null)
    try {
      await ownerService.addFoodItem(selectedRestaurant.id, {
        name: itemName,
        price: Number(itemPrice),
        foodType: itemType,
        preparationTime: Number(itemPrepTime),
        description: itemDesc,
        available: true,
      })
      setSuccessMsg(`Added "${itemName}" to menu!`)
      setShowAddItem(false)
      setItemName('')
      setItemDesc('')
      loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add food item')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-8 max-w-7xl mx-auto">
      {/* Top Banner & Refresh */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-ink">Kitchen & Store Operations</h1>
          <p className="text-xs text-muted mt-0.5">
            Process incoming orders, manage kitchen statuses, and control your live catalog
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowCreateRestaurant(true)}
            className="inline-flex items-center gap-1.5 rounded-xl bg-blue-600 px-3.5 py-2 text-xs font-bold text-white shadow-2xs hover:bg-blue-700 transition"
          >
            <Plus className="size-3.5" /> Register New Restaurant
          </button>
          <button
            onClick={() => loadData()}
            className="inline-flex items-center gap-1.5 rounded-xl border border-line bg-white px-3.5 py-2 text-xs font-semibold text-ink shadow-2xs hover:bg-slate-50 transition"
          >
            <RefreshCw className="size-3.5" /> Refresh
          </button>
        </div>
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

      {/* Restaurant Selection & Verification Status */}
      {restaurants.length === 0 ? (
        <div className="rounded-3xl border border-line bg-white p-12 text-center shadow-xs">
          <Store className="mx-auto size-12 text-blue-500 mb-3" />
          <h2 className="text-lg font-bold text-ink">No Restaurant Registered Yet</h2>
          <p className="mt-1 text-xs text-muted max-w-md mx-auto">
            Get started by registering your establishment on Cravely. Administrators will review and
            approve your listing for customer orders.
          </p>
          <button
            onClick={() => setShowCreateRestaurant(true)}
            className="mt-5 inline-flex items-center gap-2 rounded-xl bg-blue-600 px-5 py-2.5 text-xs font-bold text-white shadow-md hover:bg-blue-700 transition"
          >
            <Plus className="size-4" /> Register Restaurant Now
          </button>
        </div>
      ) : (
        <div className="rounded-3xl border border-line bg-white p-6 shadow-xs space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-4 border-b border-line pb-4">
            <div className="flex items-center gap-3">
              <div className="size-10 rounded-2xl bg-blue-50 border border-blue-200 flex items-center justify-center text-blue-600 font-bold">
                <Store className="size-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-lg font-extrabold text-ink">{selectedRestaurant?.name}</h2>
                  {selectedRestaurant?.status === 'APPROVED' && (
                    <span className="rounded-full bg-emerald-100 px-2.5 py-0.5 text-[10px] font-black text-emerald-800">
                      APPROVED
                    </span>
                  )}
                  {selectedRestaurant?.status === 'PENDING' && (
                    <span className="rounded-full bg-amber-100 px-2.5 py-0.5 text-[10px] font-black text-amber-800">
                      UNDER ADMIN REVIEW
                    </span>
                  )}
                  {selectedRestaurant?.status === 'REJECTED' && (
                    <span className="rounded-full bg-rose-100 px-2.5 py-0.5 text-[10px] font-black text-rose-800">
                      REJECTED
                    </span>
                  )}
                </div>
                <p className="text-xs text-muted">
                  {selectedRestaurant?.cuisineType} · {selectedRestaurant?.city},{' '}
                  {selectedRestaurant?.state}
                </p>
              </div>
            </div>

            {restaurants.length > 1 && (
              <div className="flex items-center gap-2 text-xs">
                <span className="text-muted font-semibold">Switch Restaurant:</span>
                <select
                  value={selectedRestaurant?.id}
                  onChange={(e) => {
                    const r = restaurants.find((x) => x.id === Number(e.target.value))
                    if (r) handleSelectRestaurant(r)
                  }}
                  className="rounded-xl border border-line bg-slate-50 px-3 py-1.5 font-bold text-ink outline-none"
                >
                  {restaurants.map((r) => (
                    <option key={r.id} value={r.id}>
                      {r.name} ({r.status})
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>

          {/* Operational Kitchen Orders */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <ShoppingBag className="size-4 text-blue-600" />
                <h3 className="text-base font-bold text-ink">Live Incoming Kitchen Orders</h3>
              </div>
              <span className="text-xs font-semibold text-muted">
                {orders.length} order{orders.length === 1 ? '' : 's'} recorded
              </span>
            </div>

            {orders.length === 0 ? (
              <div className="rounded-2xl border border-line bg-slate-50/50 p-8 text-center text-xs text-muted">
                No orders placed for this restaurant yet. Once a customer checks out, orders will
                appear here for kitchen processing.
              </div>
            ) : (
              <div className="overflow-x-auto rounded-2xl border border-line bg-white">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 border-b border-line text-muted uppercase text-[10px] font-bold tracking-wider">
                    <tr>
                      <th className="px-5 py-3.5">Order #</th>
                      <th className="px-5 py-3.5">Customer</th>
                      <th className="px-5 py-3.5">Items</th>
                      <th className="px-5 py-3.5">Total</th>
                      <th className="px-5 py-3.5">Status</th>
                      <th className="px-5 py-3.5 text-right">Kitchen Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-line">
                    {orders.map((o) => (
                      <tr key={o.id} className="hover:bg-slate-50/60 transition">
                        <td className="px-5 py-4 font-mono font-bold text-ink">{o.orderNumber}</td>
                        <td className="px-5 py-4">
                          <p className="font-semibold text-ink">{o.customerName || 'Customer'}</p>
                          <p className="text-[11px] text-muted">ID: #{o.customerId}</p>
                        </td>
                        <td className="px-5 py-4 font-medium text-ink">{o.itemCount} item(s)</td>
                        <td className="px-5 py-4 font-bold text-ink">{inr(o.totalAmount)}</td>
                        <td className="px-5 py-4">
                          <span
                            className={`inline-block rounded-full px-2.5 py-0.5 text-[10px] font-black ${
                              o.orderStatus === 'PLACED'
                                ? 'bg-slate-100 text-slate-800'
                                : o.orderStatus === 'ACCEPTED'
                                ? 'bg-blue-100 text-blue-800'
                                : o.orderStatus === 'PREPARING'
                                ? 'bg-amber-100 text-amber-800'
                                : o.orderStatus === 'READY_FOR_PICKUP'
                                ? 'bg-purple-100 text-purple-800'
                                : o.orderStatus === 'DELIVERED'
                                ? 'bg-emerald-100 text-emerald-800'
                                : 'bg-rose-100 text-rose-800'
                            }`}
                          >
                            {o.orderStatus}
                          </span>
                        </td>
                        <td className="px-5 py-4 text-right space-x-2">
                          {o.orderStatus === 'PLACED' && (
                            <>
                              <button
                                onClick={() => handleAcceptOrder(o.id)}
                                disabled={actionLoading === o.id}
                                className="rounded-lg bg-blue-600 px-3 py-1 font-bold text-white hover:bg-blue-700 transition disabled:opacity-50"
                              >
                                Accept Order
                              </button>
                              <button
                                onClick={() => setRejectModalOrder(o)}
                                disabled={actionLoading === o.id}
                                className="rounded-lg border border-rose-200 bg-rose-50 px-2.5 py-1 font-bold text-rose-700 hover:bg-rose-100 transition disabled:opacity-50"
                              >
                                Reject
                              </button>
                            </>
                          )}

                          {o.orderStatus === 'ACCEPTED' && (
                            <button
                              onClick={() => handleMarkPreparing(o.id)}
                              disabled={actionLoading === o.id}
                              className="rounded-lg bg-amber-600 px-3 py-1 font-bold text-white hover:bg-amber-700 transition disabled:opacity-50"
                            >
                              Start Preparation
                            </button>
                          )}

                          {o.orderStatus === 'PREPARING' && (
                            <button
                              onClick={() => handleMarkReady(o.id)}
                              disabled={actionLoading === o.id}
                              className="rounded-lg bg-purple-600 px-3 py-1 font-bold text-white hover:bg-purple-700 transition disabled:opacity-50"
                            >
                              Mark Ready for Pickup
                            </button>
                          )}

                          {o.orderStatus === 'READY_FOR_PICKUP' && (
                            <span className="text-[11px] font-semibold text-purple-700 italic">
                              Ready for dispatch (Admin logistics)
                            </span>
                          )}

                          {['OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED', 'REJECTED'].includes(
                            o.orderStatus
                          ) && (
                            <Link
                              to={`/orders/${o.id}/tracking`}
                              className="inline-flex items-center gap-1 text-[11px] font-bold text-blue-600 hover:underline"
                            >
                              <Eye className="size-3" /> View Timeline
                            </Link>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Menu Management Section */}
          <div className="space-y-4 pt-6 border-t border-line">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <UtensilsCrossed className="size-4 text-blue-600" />
                <h3 className="text-base font-bold text-ink">Menu & Dishes Catalog</h3>
              </div>
              <button
                onClick={() => setShowAddItem(true)}
                className="inline-flex items-center gap-1 rounded-xl bg-slate-900 px-3 py-1.5 text-xs font-bold text-white hover:bg-slate-800 transition"
              >
                <Plus className="size-3.5" /> Add Dish
              </button>
            </div>

            {items.length === 0 ? (
              <div className="rounded-2xl border border-line bg-slate-50/50 p-8 text-center text-xs text-muted">
                No food items added yet. Click "Add Dish" to expand your restaurant's menu.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {items.map((item) => (
                  <div
                    key={item.id}
                    className="rounded-2xl border border-line bg-white p-4 shadow-2xs flex flex-col justify-between space-y-3"
                  >
                    <div>
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-sm text-ink">{item.name}</span>
                        <span
                          className={`size-2.5 rounded-full ${
                            item.foodType === 'VEG' ? 'bg-emerald-500' : 'bg-rose-500'
                          }`}
                          title={item.foodType}
                        />
                      </div>
                      <p className="font-black text-sm text-blue-600 mt-1">{inr(item.price)}</p>
                      {item.description && (
                        <p className="text-xs text-muted line-clamp-2 mt-1">{item.description}</p>
                      )}
                    </div>

                    <div className="pt-2 border-t border-line flex items-center justify-between text-xs">
                      <span
                        className={`font-semibold ${
                          item.available ? 'text-emerald-700' : 'text-slate-400'
                        }`}
                      >
                        {item.available ? '● Available' : '○ Out of stock'}
                      </span>
                      <button
                        onClick={() => handleToggleAvailability(item)}
                        className={`rounded-lg px-2.5 py-1 text-[11px] font-bold border transition ${
                          item.available
                            ? 'border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100'
                            : 'border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                        }`}
                      >
                        <Power className="size-3 inline mr-1" />
                        {item.available ? 'Mark Out of Stock' : 'Mark Available'}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Create Restaurant Modal */}
      {showCreateRestaurant && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs">
          <div className="w-full max-w-lg rounded-3xl bg-white p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-line pb-3">
              <h3 className="text-base font-bold text-ink">Register Your Restaurant</h3>
              <button
                onClick={() => setShowCreateRestaurant(false)}
                className="text-muted hover:text-ink"
              >
                <X className="size-5" />
              </button>
            </div>
            <form onSubmit={handleCreateRestaurant} className="space-y-3 text-xs">
              <div>
                <label className="font-semibold text-slate-700">Restaurant Name</label>
                <input
                  required
                  type="text"
                  placeholder="e.g. Spice Route"
                  value={restName}
                  onChange={(e) => setRestName(e.target.value)}
                  className="w-full mt-1 rounded-xl border border-line p-2.5 text-xs outline-none focus:border-blue-600"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-semibold text-slate-700">Cuisine Type</label>
                  <input
                    required
                    type="text"
                    placeholder="e.g. North Indian"
                    value={restCuisine}
                    onChange={(e) => setRestCuisine(e.target.value)}
                    className="w-full mt-1 rounded-xl border border-line p-2.5 text-xs outline-none focus:border-blue-600"
                  />
                </div>
                <div>
                  <label className="font-semibold text-slate-700">Phone</label>
                  <input
                    required
                    type="tel"
                    value={restPhone}
                    onChange={(e) => setRestPhone(e.target.value)}
                    className="w-full mt-1 rounded-xl border border-line p-2.5 text-xs outline-none focus:border-blue-600"
                  />
                </div>
              </div>
              <div>
                <label className="font-semibold text-slate-700">Street Address</label>
                <input
                  required
                  type="text"
                  placeholder="e.g. 45 CG Road"
                  value={restAddress}
                  onChange={(e) => setRestAddress(e.target.value)}
                  className="w-full mt-1 rounded-xl border border-line p-2.5 text-xs outline-none focus:border-blue-600"
                />
              </div>
              <div className="grid grid-cols-3 gap-2">
                <div>
                  <label className="font-semibold text-slate-700">City</label>
                  <input
                    required
                    type="text"
                    value={restCity}
                    onChange={(e) => setRestCity(e.target.value)}
                    className="w-full mt-1 rounded-xl border border-line p-2.5 text-xs outline-none"
                  />
                </div>
                <div>
                  <label className="font-semibold text-slate-700">State</label>
                  <input
                    required
                    type="text"
                    value={restState}
                    onChange={(e) => setRestState(e.target.value)}
                    className="w-full mt-1 rounded-xl border border-line p-2.5 text-xs outline-none"
                  />
                </div>
                <div>
                  <label className="font-semibold text-slate-700">Pincode</label>
                  <input
                    required
                    type="text"
                    value={restPincode}
                    onChange={(e) => setRestPincode(e.target.value)}
                    className="w-full mt-1 rounded-xl border border-line p-2.5 text-xs outline-none"
                  />
                </div>
              </div>
              <div className="flex justify-end gap-2 pt-3 border-t border-line">
                <button
                  type="button"
                  onClick={() => setShowCreateRestaurant(false)}
                  className="rounded-xl border border-line px-4 py-2 font-semibold text-muted hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="rounded-xl bg-blue-600 px-4 py-2 font-bold text-white hover:bg-blue-700 transition"
                >
                  Submit Registration
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Food Item Modal */}
      {showAddItem && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs">
          <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-line pb-3">
              <h3 className="text-base font-bold text-ink">Add Dish to Menu</h3>
              <button onClick={() => setShowAddItem(false)} className="text-muted hover:text-ink">
                <X className="size-5" />
              </button>
            </div>
            <form onSubmit={handleAddFoodItem} className="space-y-3 text-xs">
              <div>
                <label className="font-semibold text-slate-700">Dish Name</label>
                <input
                  required
                  type="text"
                  placeholder="e.g. Paneer Butter Masala"
                  value={itemName}
                  onChange={(e) => setItemName(e.target.value)}
                  className="w-full mt-1 rounded-xl border border-line p-2.5 text-xs outline-none focus:border-blue-600"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-semibold text-slate-700">Price (INR)</label>
                  <input
                    required
                    type="number"
                    min="1"
                    value={itemPrice}
                    onChange={(e) => setItemPrice(e.target.value)}
                    className="w-full mt-1 rounded-xl border border-line p-2.5 text-xs outline-none focus:border-blue-600"
                  />
                </div>
                <div>
                  <label className="font-semibold text-slate-700">Diet Type</label>
                  <select
                    value={itemType}
                    onChange={(e) => setItemType(e.target.value as any)}
                    className="w-full mt-1 rounded-xl border border-line p-2.5 text-xs outline-none bg-white"
                  >
                    <option value="VEG">Vegetarian</option>
                    <option value="NON_VEG">Non-Vegetarian</option>
                  </select>
                </div>
              </div>
              <div>
                <label className="font-semibold text-slate-700">Prep Time (Minutes)</label>
                <input
                  type="number"
                  min="5"
                  value={itemPrepTime}
                  onChange={(e) => setItemPrepTime(e.target.value)}
                  className="w-full mt-1 rounded-xl border border-line p-2.5 text-xs outline-none"
                />
              </div>
              <div>
                <label className="font-semibold text-slate-700">Description</label>
                <textarea
                  rows={2}
                  placeholder="Fresh cottage cheese cubes in rich creamy tomato gravy..."
                  value={itemDesc}
                  onChange={(e) => setItemDesc(e.target.value)}
                  className="w-full mt-1 rounded-xl border border-line p-2.5 text-xs outline-none"
                />
              </div>
              <div className="flex justify-end gap-2 pt-3 border-t border-line">
                <button
                  type="button"
                  onClick={() => setShowAddItem(false)}
                  className="rounded-xl border border-line px-4 py-2 font-semibold text-muted hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="rounded-xl bg-blue-600 px-4 py-2 font-bold text-white hover:bg-blue-700 transition"
                >
                  Add Dish
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Reject Order Reason Modal */}
      {rejectModalOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-sm rounded-3xl bg-white p-6 shadow-2xl space-y-4">
            <h3 className="text-base font-bold text-rose-800">Reject Order Confirmation</h3>
            <p className="text-xs text-muted">
              Specify reason for rejecting order #{rejectModalOrder.orderNumber}:
            </p>
            <input
              type="text"
              placeholder="e.g. Ingredients out of stock"
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              className="w-full rounded-xl border border-line p-2.5 text-xs outline-none focus:border-rose-500"
            />
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setRejectModalOrder(null)}
                className="rounded-xl border border-line px-3.5 py-1.5 text-xs font-semibold text-muted hover:bg-slate-50"
              >
                Cancel
              </button>
              <button
                onClick={handleRejectOrder}
                className="rounded-xl bg-rose-600 px-3.5 py-1.5 text-xs font-bold text-white hover:bg-rose-700"
              >
                Confirm Reject
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

