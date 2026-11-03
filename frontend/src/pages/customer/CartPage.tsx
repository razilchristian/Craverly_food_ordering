import { Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { EmptyState, QuantitySelector } from '../../components/common/ui'
import { useApp } from '../../context/AppContext'
import { restaurantService } from '../../services/restaurantService'
import type { Coupon } from '../../types'
import { cartTotals, inr } from '../../utils/format'

export default function CartPage() {
  const { cart, setQty, toast } = useApp()
  const [code, setCode] = useState('')
  const [coupon, setCoupon] = useState<Coupon>()
  if (!cart.length) return <div className="container-app py-16"><EmptyState title="Your cart is empty" text="Good food is just a few taps away." action={<Link to="/restaurants" className="btn-primary">Explore restaurants</Link>} /></div>
  const r = restaurantService.get(cart[0].item.restaurantId)!
  const t = cartTotals(cart.reduce((s, c) => s + c.qty * c.item.price, 0), r.deliveryFee, coupon?.percent, coupon?.freeDelivery)
  const apply = () => {
    const c = restaurantService.coupon(code)
    setCoupon(c)
    toast(c ? `${c.code} applied: ${c.description}` : 'Invalid coupon code')
  }
  const row = (l: string, v: string) => <div className="flex justify-between"><dt className="text-muted">{l}</dt><dd>{v}</dd></div>

  return (
    <div className="container-app grid gap-6 py-6 md:gap-8 md:py-10 lg:grid-cols-12">
      <section className="lg:col-span-8">
        <h1 className="text-2xl font-extrabold md:text-3xl">Your cart</h1>
        <p className="mt-1 text-muted">From <Link to={`/restaurants/${r.id}`} className="font-semibold text-ink underline">{r.name}</Link></p>
        <ul className="mt-6 divide-y divide-line rounded-2xl border border-line bg-white">
          {cart.map(({ item, qty }) => (
            <li key={item.id} className="flex flex-wrap items-center gap-3 p-4 sm:flex-nowrap sm:gap-4">
              <img src={item.image} alt="" className="size-16 rounded-xl object-cover" />
              <div className="min-w-0 flex-1"><p className="font-bold">{item.name}</p><p className="text-sm text-muted">{inr(item.price)} each</p></div>
              <div className="ml-[4.75rem] sm:ml-0"><QuantitySelector qty={qty} label={item.name} onChange={(q) => setQty(item.id, q)} /></div>
              <span className="hidden w-20 text-right font-semibold sm:block">{inr(qty * item.price)}</span>
              <button aria-label={`Remove ${item.name}`} onClick={() => setQty(item.id, 0)} className="text-muted hover:text-error"><Trash2 className="size-4" /></button>
            </li>
          ))}
        </ul>
        <label htmlFor="notes" className="mt-6 block text-sm font-semibold">Order instructions</label>
        <textarea id="notes" rows={3} placeholder="e.g. Less spicy, no onions" className="mt-2 w-full rounded-xl border border-line bg-white p-3 text-sm" />
        <Link to={`/restaurants/${r.id}`} className="btn-ghost mt-4">Add more items</Link>
      </section>
      <aside className="lg:col-span-4">
        <div className="sticky top-20 rounded-2xl border border-line bg-white p-5">
          <div className="flex gap-2">
            <input value={code} onChange={(e) => setCode(e.target.value)} aria-label="Coupon code" placeholder="Try WELCOME10" className="h-10 flex-1 rounded-xl border border-line px-3 text-sm uppercase" />
            <button onClick={apply} className="btn-outline">Apply</button>
          </div>
          <dl className="mt-5 space-y-2 text-sm">
            {row('Item subtotal', inr(t.subtotal))}{row('Delivery fee', t.delivery ? inr(t.delivery) : 'Free')}{row('Packaging', inr(t.packaging))}{row('GST (5%)', inr(t.tax))}
            {t.discount > 0 && <div className="flex justify-between text-success"><dt>Discount</dt><dd>−{inr(t.discount)}</dd></div>}
            <div className="flex justify-between border-t border-line pt-3 text-base font-bold"><dt>Grand total</dt><dd>{inr(t.total)}</dd></div>
          </dl>
          <button className="btn-primary mt-5 w-full" onClick={() => toast('Checkout arrives in the next build step')}>Proceed to checkout</button>
        </div>
      </aside>
    </div>
  )
}
