import { ArrowRight, Clock, MapPin } from 'lucide-react'
import { Link } from 'react-router'
import { FoodItemCard, RestaurantCard } from '../../components/common/ui'
import { categories, foodItems, popularDishIds, restaurants } from '../../data/mock'

export default function HomePage() {
  const popular = popularDishIds.map((id) => foodItems.find((f) => f.id === id)!)
  return (
    <>
      <section className="container-app grid items-center gap-8 pb-10 pt-6 md:gap-10 md:py-16 lg:grid-cols-12">
        <div className="lg:col-span-6">
          <span className="inline-flex items-center gap-2 rounded-full bg-white px-3 py-1 text-xs font-semibold text-muted shadow-sm"><MapPin className="size-3.5 text-primary" /> Delivering across Ahmedabad</span>
          <h1 className="mt-5 text-[2.75rem] font-extrabold leading-[1.02] tracking-tight sm:text-6xl lg:text-7xl">Good food.<br /><span className="text-primary">Great mood.</span></h1>
          <p className="mt-4 max-w-md text-base text-muted md:mt-5 md:text-lg">Discover your next favorite meal from the best restaurants around you.</p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to="/restaurants" className="btn-primary h-12 flex-1 px-6 sm:flex-none">Explore Restaurants <ArrowRight className="size-4" /></Link>
            <a href="#popular" className="btn-ghost h-12 flex-1 px-6 sm:flex-none">Browse Popular Dishes</a>
          </div>
        </div>
        <div className="relative grid grid-cols-5 grid-rows-2 gap-3 lg:col-span-6">
          <img src="https://images.unsplash.com/photo-1788843304145-bec146aa83c5?auto=format&fit=crop&w=900&q=75" alt="Grilled paneer tikka skewers with peppers and mushrooms" className="col-span-3 row-span-2 h-full min-h-64 sm:min-h-80 w-full rounded-2xl object-cover md:rounded-3xl" />
          <img src={restaurants[3].image} alt="Wood-fired pizza" className="col-span-2 aspect-square w-full rounded-2xl object-cover md:rounded-3xl" />
          <img src={foodItems[10].image} alt="Indian thali" className="col-span-2 aspect-square w-full rounded-2xl object-cover md:rounded-3xl" />
          <div className="absolute -bottom-4 left-4 flex items-center gap-3 rounded-2xl bg-white p-3 pr-5 shadow-xl">
            <span className="grid size-10 place-items-center rounded-xl bg-secondary/15 text-secondary"><Clock className="size-5" /></span>
            <div><p className="text-xs text-muted">Average delivery</p><p className="font-bold">28 minutes</p></div>
          </div>
        </div>
      </section>

      <section className="container-app mt-8" aria-labelledby="cat-h">
        <h2 id="cat-h" className="section-title">What's on your mind?</h2>
        <div className="-mx-4 mt-5 flex gap-3 overflow-x-auto sm:gap-4 px-4 pb-2">
          {categories.map((c) => (
            <Link key={c.id} to={`/restaurants?cat=${c.id}`} className="group w-20 shrink-0 sm:w-28 text-center">
              <img src={c.image} alt="" className="aspect-square w-full rounded-full object-cover ring-4 ring-white transition group-hover:ring-secondary/40" />
              <span className="mt-2 block text-sm font-semibold">{c.name}</span>
            </Link>
          ))}
        </div>
      </section>

      <section className="container-app mt-14">
        <div className="flex items-end justify-between"><h2 className="section-title">Featured restaurants</h2><Link to="/restaurants" className="text-sm font-semibold text-primary">See all</Link></div>
        <div className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">{restaurants.slice(0, 8).map((r) => <RestaurantCard key={r.id} r={r} />)}</div>
      </section>

      <section id="popular" className="container-app mt-14 scroll-mt-20">
        <h2 className="section-title">Popular dishes</h2>
        <div className="mt-5 grid grid-cols-2 gap-3 md:grid-cols-3 md:gap-4 xl:grid-cols-6">
          {popular.map((f) => <FoodItemCard key={f.id} item={f} restaurantName={restaurants.find((r) => r.id === f.restaurantId)?.name} />)}
        </div>
      </section>

      <section id="offers" className="container-app mt-14 scroll-mt-20">
        <h2 className="section-title">Offers for you <span className="ml-2 align-middle text-xs font-medium text-muted">(demo offers)</span></h2>
        <div className="mt-5 grid gap-4 md:grid-cols-3">
          {[['WELCOME10', '10% off your first order', 'bg-primary text-white'], ['FREE DELIVERY', 'On selected orders · code FREEDEL', 'bg-secondary text-ink'], ['COMBO DEALS', 'More food, better value · code COMBO15', 'bg-ink text-white']].map(([t, d, c]) => (
            <div key={t} className={`rounded-2xl p-6 ${c}`}><p className="text-2xl font-extrabold tracking-tight">{t}</p><p className="mt-1 text-sm opacity-90">{d}</p></div>
          ))}
        </div>
      </section>

      <section className="container-app mt-14">
        <h2 className="section-title">How it works</h2>
        <ol className="mt-5 grid gap-4 md:grid-cols-3">
          {['Discover your favorites', 'Add to cart and place your order', 'Enjoy your meal'].map((s, i) => (
            <li key={s} className="rounded-2xl border border-line bg-white p-6"><span className="text-4xl font-extrabold text-secondary">0{i + 1}</span><p className="mt-3 font-bold">{s}</p></li>
          ))}
        </ol>
      </section>
    </>
  )
}
