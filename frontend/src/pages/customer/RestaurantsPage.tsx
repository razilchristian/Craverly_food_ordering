import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router'
import { EmptyState, RestaurantCard } from '../../components/common/ui'
import { restaurantService } from '../../services/restaurantService'
import type { Restaurant } from '../../types'

const sorters: Record<string, (a: Restaurant, b: Restaurant) => number> = {
  rating: (a, b) => b.rating - a.rating,
  time: (a, b) => a.deliveryMins - b.deliveryMins,
  price: (a, b) => a.priceForTwo - b.priceForTwo,
}

export default function RestaurantsPage() {
  const [params] = useSearchParams()
  const [q, setQ] = useState(params.get('q') ?? '')
  const [cat, setCat] = useState(params.get('cat') ?? '')
  const [veg, setVeg] = useState(false)
  const [minRating, setMinRating] = useState(0)
  const [maxTime, setMaxTime] = useState(60)
  const [sort, setSort] = useState('relevance')
  const cats = restaurantService.categories()

  const list = useMemo(() => {
    const t = q.toLowerCase()
    const r = restaurantService.list().filter((x) =>
      (!t || x.name.toLowerCase().includes(t) || x.cuisines.some((c) => c.toLowerCase().includes(t))) &&
      (!cat || x.category === cat) && (!veg || x.veg) && x.rating >= minRating && x.deliveryMins <= maxTime)
    return sorters[sort] ? [...r].sort(sorters[sort]) : r
  }, [q, cat, veg, minRating, maxTime, sort])

  const clear = () => { setQ(''); setCat(''); setVeg(false); setMinRating(0); setMaxTime(60); setSort('relevance') }
  const sel = 'h-10 rounded-xl border border-line bg-white px-3 text-sm'

  return (
    <div className="container-app py-6 md:py-10">
      <h1 className="text-3xl font-extrabold tracking-tight md:text-4xl">Find your next favorite.</h1>
      <p className="mt-2 text-muted">Explore restaurants and discover something delicious.</p>
      <div className="mt-6 flex flex-col gap-3 md:flex-row md:flex-wrap md:items-center">
        <input value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search restaurants or cuisines" placeholder="Search restaurants or cuisines" className={`${sel} w-full text-base md:w-auto md:min-w-60 md:flex-1 md:text-sm`} />
        <div className="-mx-4 flex items-center gap-3 overflow-x-auto px-4 pb-1 md:mx-0 md:flex-wrap md:overflow-visible md:px-0 md:pb-0">
        <select aria-label="Minimum rating" className={sel} value={minRating} onChange={(e) => setMinRating(+e.target.value)}><option value={0}>Any rating</option><option value={4}>4.0+</option><option value={4.5}>4.5+</option></select>
        <select aria-label="Delivery time" className={sel} value={maxTime} onChange={(e) => setMaxTime(+e.target.value)}><option value={60}>Any time</option><option value={30}>Under 30 min</option><option value={20}>Under 20 min</option></select>
        <select aria-label="Sort by" className={sel} value={sort} onChange={(e) => setSort(e.target.value)}><option value="relevance">Relevance</option><option value="rating">Rating</option><option value="time">Delivery time</option><option value="price">Price: low to high</option></select>
        <label className="flex shrink-0 items-center gap-2 text-sm font-medium"><input type="checkbox" checked={veg} onChange={(e) => setVeg(e.target.checked)} className="size-4 accent-success" /> Veg only</label>
        </div>
      </div>
      <div className="-mx-4 mt-4 flex gap-2 overflow-x-auto px-4 pb-1 md:mx-0 md:flex-wrap md:px-0">
        {[{ id: '', name: 'All' }, ...cats].map((c) => (
          <button key={c.id} onClick={() => setCat(c.id)} aria-pressed={cat === c.id} className={`shrink-0 rounded-full border px-4 py-2 text-sm font-medium ${cat === c.id ? 'border-ink bg-ink text-white' : 'border-line bg-white'}`}>{c.name}</button>
        ))}
      </div>
      <div className="mt-6 flex items-center justify-between text-sm"><p className="text-muted">{list.length} restaurants</p><button onClick={clear} className="font-semibold text-primary">Clear all filters</button></div>
      {list.length ? (
        <div className="mt-4 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">{list.map((r) => <RestaurantCard key={r.id} r={r} />)}</div>
      ) : (
        <div className="mt-4"><EmptyState title="No restaurants match" text="Try removing a filter or searching something else." action={<button className="btn-primary" onClick={clear}>Clear filters</button>} /></div>
      )}
    </div>
  )
}
