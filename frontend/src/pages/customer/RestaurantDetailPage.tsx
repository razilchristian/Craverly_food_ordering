import { Clock, Heart } from 'lucide-react'
import { Link, useParams } from 'react-router'
import { AddControl, EmptyState, RatingBadge, VegIndicator } from '../../components/common/ui'
import { useApp } from '../../context/AppContext'
import { restaurantService } from '../../services/restaurantService'
import type { MenuSection } from '../../types'
import { inr } from '../../utils/format'

const sections: MenuSection[] = ['Recommended', 'Starters', 'Main Course', 'Rice and Biryani', 'Breads', 'Desserts', 'Beverages']

export default function RestaurantDetailPage() {
  const { id = '' } = useParams()
  const r = restaurantService.get(id)
  const { cart, favorites, toggleFav } = useApp()
  if (!r) return <div className="container-app py-16"><EmptyState title="Restaurant not found" text="It may have moved." action={<Link to="/restaurants" className="btn-primary">Explore restaurants</Link>} /></div>
  const menu = restaurantService.menu(id)
  const present = sections.filter((s) => menu.some((m) => m.section === s))
  const count = cart.reduce((s, c) => s + c.qty, 0)
  const total = cart.reduce((s, c) => s + c.qty * c.item.price, 0)
  const fav = favorites.includes(r.id)

  return (
    <div className="container-app pb-36 pt-4 md:py-6 lg:pb-6">
      <div className="relative overflow-hidden rounded-3xl">
        <img src={r.image} alt={`${r.name} cover`} className="h-48 w-full object-cover sm:h-64 md:h-80" />
        <div className="absolute inset-0 bg-gradient-to-t from-black/70 to-transparent" />
        <div className="absolute bottom-0 p-4 text-white md:p-6">
          <h1 className="text-3xl font-extrabold md:text-5xl">{r.name}</h1>
          <p className="mt-1 opacity-90">{r.cuisines.join(' · ')}</p>
        </div>
        <button onClick={() => toggleFav(r.id)} aria-pressed={fav} aria-label="Toggle favorite" className="absolute right-4 top-4 grid size-10 place-items-center rounded-full bg-white"><Heart className={`size-5 ${fav ? 'fill-primary text-primary' : ''}`} /></button>
      </div>
      <div className="mt-4 flex flex-wrap gap-x-6 gap-y-2 text-sm text-muted">
        <span className="flex items-center gap-2"><RatingBadge value={r.rating} /> {r.reviews.toLocaleString('en-IN')} reviews</span>
        <span className="flex items-center gap-1"><Clock className="size-4" aria-hidden /> {r.deliveryMins} min</span>
        <span>{inr(r.deliveryFee)} delivery</span><span>Min order {inr(r.minOrder)}</span><span>Open {r.hours}</span>
      </div>
      <nav className="sticky top-[6.75rem] z-30 -mx-4 md:top-16 mt-6 flex gap-2 overflow-x-auto border-b border-line bg-cream px-4 py-3" aria-label="Menu sections">
        {present.map((s) => <a key={s} href={`#${s}`} className="shrink-0 rounded-full bg-white px-4 py-1.5 text-sm font-medium shadow-sm hover:text-primary">{s}</a>)}
      </nav>
      <div className="mt-6 grid gap-8 lg:grid-cols-12">
        <div className="space-y-10 lg:col-span-8">
          {present.map((s) => (
            <section key={s} id={s} className="scroll-mt-48 md:scroll-mt-32">
              <h2 className="section-title">{s}</h2>
              <ul className="mt-4 divide-y divide-line rounded-2xl border border-line bg-white">
                {menu.filter((m) => m.section === s).map((m) => (
                  <li key={m.id} className="flex gap-3 p-4 md:gap-4">
                    <div className="flex-1">
                      <div className="flex items-center gap-2"><VegIndicator veg={m.veg} />{m.bestseller && <span className="text-xs font-bold text-secondary">Bestseller</span>}</div>
                      <h3 className="mt-1 font-bold">{m.name}</h3>
                      <p className="font-semibold text-primary">{inr(m.price)}</p>
                      <p className="mt-1 text-sm text-muted">{m.description}</p>
                    </div>
                    <div className="flex w-24 shrink-0 flex-col sm:w-28 items-center gap-2">
                      <img src={m.image} alt={m.name} loading="lazy" className="aspect-square w-full rounded-xl object-cover" />
                      <AddControl item={m} />
                    </div>
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </div>
        {count > 0 && (
          <aside className="hidden lg:col-span-4 lg:block">
            <div className="sticky top-36 rounded-2xl border border-line bg-white p-5">
              <h2 className="font-bold">Your cart</h2>
              <ul className="mt-3 space-y-2 text-sm">{cart.map((c) => <li key={c.item.id} className="flex justify-between"><span>{c.qty} × {c.item.name}</span><span>{inr(c.qty * c.item.price)}</span></li>)}</ul>
              <Link to="/cart" className="btn-primary mt-4 w-full">View cart · {inr(total)}</Link>
            </div>
          </aside>
        )}
      </div>
      {count > 0 && (
        <Link to="/cart" className="fixed inset-x-3 bottom-16 z-40 flex items-center justify-between rounded-2xl bg-primary px-5 py-3 font-semibold text-white shadow-xl lg:hidden">
          <span>{count} items · {inr(total)}</span><span>View cart</span>
        </Link>
      )}
    </div>
  )
}
