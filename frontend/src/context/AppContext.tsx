import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import type { CartItem, FoodItem } from '../types'

interface Toast { id: number; message: string }
interface Ctx {
  cart: CartItem[]
  favorites: string[]
  add: (item: FoodItem) => void
  setQty: (id: string, qty: number) => void
  clear: () => void
  toggleFav: (id: string) => void
  toast: (message: string) => void
}
const AppContext = createContext<Ctx | null>(null)

const load = <T,>(k: string, d: T): T => {
  try { return (JSON.parse(localStorage.getItem(k) ?? 'null') as T) ?? d } catch { return d }
}

export function AppProvider({ children }: { children: ReactNode }) {
  const [cart, setCart] = useState<CartItem[]>(() => load('cravely-cart', []))
  const [favorites, setFavs] = useState<string[]>(() => load('cravely-favs', []))
  const [toasts, setToasts] = useState<Toast[]>([])
  const [pending, setPending] = useState<FoodItem | null>(null)

  useEffect(() => localStorage.setItem('cravely-cart', JSON.stringify(cart)), [cart])
  useEffect(() => localStorage.setItem('cravely-favs', JSON.stringify(favorites)), [favorites])

  const toast = (message: string) => {
    const id = Date.now() + Math.random()
    setToasts((t) => [...t, { id, message }])
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 2600)
  }
  const doAdd = (item: FoodItem, replace = false) => {
    setCart((c) => {
      const base = replace ? [] : c
      const found = base.find((x) => x.item.id === item.id)
      return found ? base.map((x) => (x.item.id === item.id ? { ...x, qty: x.qty + 1 } : x)) : [...base, { item, qty: 1 }]
    })
    toast(`${item.name} added to cart`)
  }
  const add = (item: FoodItem) => {
    if (cart.length && cart[0].item.restaurantId !== item.restaurantId) setPending(item)
    else doAdd(item)
  }
  const setQty = (id: string, qty: number) =>
    setCart((c) => (qty <= 0 ? c.filter((x) => x.item.id !== id) : c.map((x) => (x.item.id === id ? { ...x, qty } : x))))
  const toggleFav = (id: string) => {
    setFavs((f) => (f.includes(id) ? f.filter((x) => x !== id) : [...f, id]))
    toast(favorites.includes(id) ? 'Removed from favorites' : 'Added to favorites')
  }

  return (
    <AppContext.Provider value={{ cart, favorites, add, setQty, clear: () => setCart([]), toggleFav, toast }}>
      {children}
      {pending && (
        <div role="dialog" aria-modal="true" aria-labelledby="replace-title" className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4" onKeyDown={(e) => e.key === 'Escape' && setPending(null)}>
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-xl">
            <h2 id="replace-title" className="text-lg font-bold">Replace cart items?</h2>
            <p className="mt-2 text-sm text-muted">Your cart has items from another restaurant. Orders can include one restaurant at a time.</p>
            <div className="mt-6 flex justify-end gap-3">
              <button autoFocus className="btn-ghost" onClick={() => setPending(null)}>Keep cart</button>
              <button className="btn-primary" onClick={() => { doAdd(pending, true); setPending(null) }}>Replace</button>
            </div>
          </div>
        </div>
      )}
      <div aria-live="polite" className="fixed bottom-24 left-1/2 z-50 flex -translate-x-1/2 flex-col gap-2 md:bottom-6">
        {toasts.map((t) => (
          <div key={t.id} className="rounded-xl bg-ink px-4 py-3 text-sm font-medium text-white shadow-lg">{t.message}</div>
        ))}
      </div>
    </AppContext.Provider>
  )
}

export const useApp = () => {
  const c = useContext(AppContext)
  if (!c) throw new Error('useApp outside provider')
  return c
}
