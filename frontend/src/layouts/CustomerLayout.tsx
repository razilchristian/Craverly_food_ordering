import {
  ClipboardList,
  Compass,
  Home,
  LogOut,
  MapPin,
  Search,
  ShieldCheck,
  ShoppingBag,
  Store,
  Tag,
  User,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import AuthModal from '../components/common/AuthModal'
import { BrandLogo } from '../components/common/ui'
import { useApp } from '../context/AppContext'
import { authService, type UserInfo } from '../services/authService'

const links = [
  { to: '/', label: 'Home', icon: Home },
  { to: '/restaurants', label: 'Explore', icon: Compass },
  { to: '/#offers', label: 'Offers', icon: Tag },
  { to: '/orders', label: 'Orders', icon: ClipboardList },
  { to: '/cart', label: 'Cart', icon: ShoppingBag },
]

export default function CustomerLayout() {
  const { cart, toast } = useApp()
  const count = cart.reduce((s, c) => s + c.qty, 0)
  const [q, setQ] = useState('')
  const [showAuthModal, setShowAuthModal] = useState(false)
  const [currentUser, setCurrentUser] = useState<UserInfo | null>(() => authService.getStoredUser())
  const nav = useNavigate()

  useEffect(() => {
    const handleAuthChange = () => {
      setCurrentUser(authService.getStoredUser())
    }
    window.addEventListener('cravely-auth-change', handleAuthChange)
    return () => window.removeEventListener('cravely-auth-change', handleAuthChange)
  }, [])

  const handleLogout = () => {
    authService.logout()
    toast('Logged out successfully')
    nav('/')
  }

  const search = (e: React.FormEvent) => {
    e.preventDefault()
    nav(`/restaurants?q=${encodeURIComponent(q)}`)
  }

  return (
    <div className="min-h-screen bg-cream pb-16 md:pb-0">
      <header className="sticky top-0 z-40 border-b border-line bg-white/95 backdrop-blur">
        <div className="container-app flex h-14 items-center gap-3 md:h-16 md:gap-4">
          <BrandLogo />
          <span className="flex items-center gap-1 truncate text-xs font-semibold md:hidden lg:flex lg:text-sm">
            <MapPin className="size-4 shrink-0 text-primary" aria-hidden /> Ahmedabad
          </span>
          <form onSubmit={search} className="relative mx-auto hidden max-w-md flex-1 md:block" role="search">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted" aria-hidden />
            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              aria-label="Search"
              placeholder="Search dishes, restaurants, or cuisines..."
              className="h-10 w-full rounded-xl border border-line bg-cream pl-9 pr-3 text-sm outline-none focus:border-primary"
            />
          </form>
          <nav className="ml-auto hidden items-center gap-6 text-sm font-medium md:flex" aria-label="Main">
            {links.slice(0, 4).map((l) => (
              <NavLink
                key={l.to}
                to={l.to}
                end
                className={({ isActive }) =>
                  isActive && l.to !== '/#offers' ? 'text-primary' : 'text-ink hover:text-primary'
                }
              >
                {l.label}
              </NavLink>
            ))}
          </nav>
          <Link
            to="/cart"
            className="relative ml-auto grid size-10 place-items-center rounded-xl hover:bg-cream md:ml-0"
            aria-label={`Cart, ${count} items`}
          >
            <ShoppingBag className="size-5" />
            {count > 0 && (
              <span className="absolute -right-0.5 -top-0.5 grid size-5 place-items-center rounded-full bg-primary text-[11px] font-bold text-white">
                {count}
              </span>
            )}
          </Link>

          {/* User Auth Section */}
          {currentUser ? (
            <div className="hidden md:flex items-center gap-2">
              {currentUser.role === 'ADMIN' && (
                <Link
                  to="/admin/dashboard"
                  className="inline-flex items-center gap-1 rounded-xl bg-slate-900 px-3 py-1.5 text-xs font-bold text-white hover:bg-slate-800 transition"
                >
                  <ShieldCheck className="size-3.5 text-amber-400" /> Admin
                </Link>
              )}
              {currentUser.role === 'RESTAURANT_OWNER' && (
                <Link
                  to="/owner/dashboard"
                  className="inline-flex items-center gap-1 rounded-xl bg-blue-600 px-3 py-1.5 text-xs font-bold text-white hover:bg-blue-700 transition"
                >
                  <Store className="size-3.5" /> Owner Portal
                </Link>
              )}
              <div className="flex items-center gap-1.5 pl-2 border-l border-line text-xs font-semibold">
                <span className="truncate max-w-[120px] text-ink">{currentUser.fullName}</span>
                <button
                  onClick={handleLogout}
                  title="Sign out"
                  className="p-1 text-muted hover:text-rose-600 rounded-lg transition"
                >
                  <LogOut className="size-4" />
                </button>
              </div>
            </div>
          ) : (
            <button
              className="btn-primary hidden md:inline-flex"
              onClick={() => setShowAuthModal(true)}
            >
              <User className="size-4" /> Sign In
            </button>
          )}
        </div>

        <form onSubmit={search} className="container-app relative pb-3 md:hidden" role="search">
          <Search className="absolute left-7 top-[1.25rem] size-4 -translate-y-1/2 text-muted" aria-hidden />
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            aria-label="Search"
            placeholder="Search dishes, restaurants, or cuisines..."
            className="h-10 w-full rounded-xl border border-line bg-cream pl-9 pr-3 text-base outline-none focus:border-primary"
          />
        </form>
      </header>

      <main>
        <Outlet />
      </main>

      <footer className="mt-20 border-t border-line bg-white">
        <div className="container-app grid gap-8 py-12 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <BrandLogo />
            <p className="mt-3 text-sm text-muted">
              Good food from the best kitchens around you. A full-stack food ordering platform.
            </p>
          </div>
          {([
            ['Explore', ['Restaurants', 'Offers', 'Categories']],
            ['Support', ['Help centre', 'Contact', 'About Us']],
            ['Legal', ['Privacy Policy', 'Terms and Conditions']],
          ] as const).map(([h, items]) => (
            <div key={h}>
              <h3 className="text-sm font-bold">{h}</h3>
              <ul className="mt-3 space-y-2 text-sm text-muted">
                {items.map((i) => (
                  <li key={i}>
                    <Link to="/restaurants" className="hover:text-primary">
                      {i}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
        <p className="border-t border-line py-4 text-center text-xs text-muted">
          © 2026 Cravely Food Ordering. All rights reserved.
        </p>
      </footer>

      {/* Mobile Bottom Navigation */}
      <nav
        className="fixed inset-x-0 bottom-0 z-40 grid grid-cols-5 border-t border-line bg-white md:hidden"
        aria-label="Mobile"
      >
        {links.map((l) => (
          <NavLink
            key={l.to}
            to={l.to}
            end
            className={({ isActive }) =>
              `flex flex-col items-center gap-0.5 py-2 text-[11px] font-medium ${
                isActive ? 'text-primary' : 'text-muted'
              }`
            }
          >
            <l.icon className="size-5" />
            {l.label}
          </NavLink>
        ))}
      </nav>

      {/* Auth Modal */}
      <AuthModal
        isOpen={showAuthModal}
        onClose={() => setShowAuthModal(false)}
        onSuccess={() => toast(`Welcome to Cravely!`)}
      />
    </div>
  )
}
