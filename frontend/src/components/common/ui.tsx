import { Heart, Minus, Plus, Star } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { useApp } from '../../context/AppContext'
import type { FoodItem, Restaurant } from '../../types'
import { inr } from '../../utils/format'

export function BrandLogo() {
  return (
    <Link to="/" className="flex items-center gap-2" aria-label="Cravely home">
      <span className="grid size-9 place-items-center rounded-xl bg-primary text-white">
        <svg viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round"><path d="M4 13a8 8 0 0 0 16 0H4Z" /><path d="M9 9c0-2 1-3 3-4M14 9c0-1.5.6-2.4 2-3" /></svg>
      </span>
      <span className="text-xl font-extrabold tracking-tight">cravely</span>
    </Link>
  )
}

export const VegIndicator = ({ veg }: { veg: boolean }) => (
  <span title={veg ? 'Vegetarian' : 'Non-vegetarian'} className={`inline-grid size-4 place-items-center rounded-[3px] border-2 ${veg ? 'border-success' : 'border-error'}`}>
    <span className={`size-1.5 rounded-full ${veg ? 'bg-success' : 'bg-error'}`} />
    <span className="sr-only">{veg ? 'Vegetarian' : 'Non-vegetarian'}</span>
  </span>
)

export const RatingBadge = ({ value }: { value: number }) => (
  <span className="inline-flex items-center gap-1 rounded-md bg-success px-1.5 py-0.5 text-xs font-bold text-white">
    <Star className="size-3 fill-current" aria-hidden /> {value.toFixed(1)}
  </span>
)

export function QuantitySelector({ qty, onChange, label }: { qty: number; onChange: (q: number) => void; label: string }) {
  return (
    <div className="inline-flex items-center rounded-xl border border-primary bg-white text-primary">
      <button className="grid size-10 place-items-center" aria-label={`Decrease ${label}`} onClick={() => onChange(qty - 1)}><Minus className="size-4" /></button>
      <span className="w-6 text-center text-sm font-bold" aria-live="polite">{qty}</span>
      <button className="grid size-10 place-items-center" aria-label={`Increase ${label}`} onClick={() => onChange(qty + 1)}><Plus className="size-4" /></button>
    </div>
  )
}

export function AddControl({ item }: { item: FoodItem }) {
  const { cart, add, setQty } = useApp()
  const line = cart.find((c) => c.item.id === item.id)
  if (line) return <QuantitySelector qty={line.qty} label={item.name} onChange={(q) => setQty(item.id, q)} />
  return <button className="btn-outline" onClick={() => add(item)} aria-label={`Add ${item.name}`}>Add</button>
}

export function RestaurantCard({ r }: { r: Restaurant }) {
  const { favorites, toggleFav } = useApp()
  const fav = favorites.includes(r.id)
  return (
    <article className="group relative overflow-hidden rounded-2xl border border-line bg-white transition hover:-translate-y-0.5 hover:shadow-lg">
      <Link to={`/restaurants/${r.id}`} className="block">
        <div className="relative aspect-[16/10] overflow-hidden">
          <img src={r.image} alt={`Food at ${r.name}`} loading="lazy" className="size-full object-cover transition duration-500 group-hover:scale-105" />
          {r.offer && <span className="absolute bottom-3 left-3 rounded-lg bg-white px-2 py-1 text-xs font-bold text-primary shadow">{r.offer}</span>}
        </div>
        <div className="p-4">
          <div className="flex items-start justify-between gap-2">
            <h3 className="font-bold">{r.name}</h3>
            <RatingBadge value={r.rating} />
          </div>
          <p className="mt-1 text-sm text-muted">{r.cuisines.join(' · ')}</p>
          <p className="mt-3 flex flex-wrap gap-x-3 gap-y-1 border-t border-line pt-3 text-xs font-medium text-muted">
            <span>{r.deliveryMins} min</span><span>{inr(r.deliveryFee)} delivery</span><span>{inr(r.priceForTwo)} for two</span>
          </p>
        </div>
      </Link>
      <button onClick={() => toggleFav(r.id)} aria-pressed={fav} aria-label={fav ? `Remove ${r.name} from favorites` : `Add ${r.name} to favorites`} className="absolute right-3 top-3 grid size-9 place-items-center rounded-full bg-white/90 shadow">
        <Heart className={`size-4 ${fav ? 'fill-primary text-primary' : 'text-ink'}`} />
      </button>
    </article>
  )
}

export function FoodItemCard({ item, restaurantName }: { item: FoodItem; restaurantName?: string }) {
  return (
    <article className="flex flex-col overflow-hidden rounded-2xl border border-line bg-white">
      <img src={item.image} alt={item.name} loading="lazy" className="aspect-[4/3] w-full object-cover" />
      <div className="flex flex-1 flex-col p-3 sm:p-4">
        <div className="flex items-center gap-2"><VegIndicator veg={item.veg} /><RatingBadge value={item.rating} /></div>
        <h3 className="mt-2 text-sm font-bold leading-snug sm:text-base">{item.name}</h3>
        {restaurantName && <p className="truncate text-xs text-muted sm:text-sm">{restaurantName}</p>}
        <div className="mt-auto flex flex-wrap items-center justify-between gap-2 pt-3">
          <span className="font-bold text-primary">{inr(item.price)}</span>
          <AddControl item={item} />
        </div>
      </div>
    </article>
  )
}

export const EmptyState = ({ title, text, action }: { title: string; text: string; action?: ReactNode }) => (
  <div className="rounded-2xl border border-dashed border-line bg-white px-6 py-16 text-center">
    <div className="mx-auto grid size-16 place-items-center rounded-full bg-cream text-secondary" aria-hidden>
      <svg viewBox="0 0 24 24" className="size-8" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M4 13a8 8 0 0 0 16 0H4Z" /></svg>
    </div>
    <h2 className="mt-4 text-lg font-bold">{title}</h2>
    <p className="mt-1 text-sm text-muted">{text}</p>
    {action && <div className="mt-6">{action}</div>}
  </div>
)
