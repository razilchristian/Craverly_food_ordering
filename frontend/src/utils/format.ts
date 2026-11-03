export const inr = (n: number) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n)

export const cartTotals = (subtotal: number, deliveryFee: number, discountPct = 0, freeDelivery = false) => {
  const packaging = subtotal > 0 ? 20 : 0
  const delivery = subtotal > 0 && !freeDelivery ? deliveryFee : 0
  const discount = Math.round((subtotal * discountPct) / 100)
  const tax = Math.round((subtotal - discount) * 0.05)
  return { subtotal, packaging, delivery, discount, tax, total: subtotal - discount + packaging + delivery + tax }
}
