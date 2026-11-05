import {
  ChevronRight,
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
  X,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import AuthModal from '../components/common/AuthModal'
import { BrandLogo } from '../components/common/ui'
import { useApp } from '../context/AppContext'
import { authService, type UserInfo } from '../services/authService'

const desktopLinks = [
  { to: '/', label: 'Home', icon: Home },
  { to: '/restaurants', label: 'Explore', icon: Compass },
  { to: '/#offers', label: 'Offers', icon: Tag },
  { to: '/orders', label: 'Orders', icon: ClipboardList },
]

export default function CustomerLayout() {
  const { cart, toast } = useApp()
  const count = cart.reduce((s, c) => s + c.qty, 0)
  const [q, setQ] = useState('')
  const [showAuthModal, setShowAuthModal] = useState(false)
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login')
  const [showAccountSheet, setShowAccountSheet] = useState(false)
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
    setShowAccountSheet(false)
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
        <div className="container-app flex h-14 items-center gap-2 sm:gap-3 md:h-16 md:gap-4">
          <BrandLogo />
          <span className="hidden sm:inline-flex lg:inline-flex items-center gap-1 truncate text-xs font-semibold">
            <MapPin className="size-3.5 shrink-0 text-primary" aria-hidden /> Ahmedabad
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
            {desktopLinks.map((l) => (
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

          {/* Mobile Auth Buttons (Visible in top bar on mobile screens) */}
          {!currentUser ? (
            <div className="ml-auto flex items-center gap-1.5 md:hidden">
              <button
                onClick={() => {
                  setAuthMode('login')
                  setShowAuthModal(true)
                }}
                className="rounded-xl border border-line bg-white px-2.5 py-1.5 text-xs font-bold text-ink hover:border-primary hover:text-primary transition shadow-2xs"
              >
                Sign In
              </button>
              <button
                onClick={() => {
                  setAuthMode('register')
                  setShowAuthModal(true)
                }}
                className="rounded-xl bg-primary px-2.5 py-1.5 text-xs font-bold text-white hover:bg-primary-dark transition shadow-2xs"
              >
                Sign Up
              </button>
            </div>
          ) : (
            <button
              onClick={() => setShowAccountSheet(true)}
              className="ml-auto flex items-center gap-1.5 rounded-xl border border-line bg-white px-2 py-1 text-xs font-bold text-ink shadow-2xs md:hidden"
              aria-label="My Account"
            >
              <div className="grid size-6 place-items-center rounded-full bg-primary text-[10px] font-bold text-white">
                {currentUser.fullName ? currentUser.fullName.charAt(0).toUpperCase() : 'U'}
              </div>
              <span className="truncate max-w-[60px] text-[11px]">
                {currentUser.fullName ? currentUser.fullName.split(' ')[0] : 'Account'}
              </span>
            </button>
          )}

          {/* Cart Icon Button */}
          <Link
            to="/cart"
            className="relative grid size-10 place-items-center rounded-xl hover:bg-cream"
            aria-label={`Cart, ${count} items`}
          >
            <ShoppingBag className="size-5" />
            {count > 0 && (
              <span className="absolute -right-0.5 -top-0.5 grid size-5 place-items-center rounded-full bg-primary text-[11px] font-bold text-white">
                {count}
              </span>
            )}
          </Link>

          {/* Desktop User Auth Section */}
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
            <div className="hidden md:flex items-center gap-2">
              <button
                className="btn-ghost text-xs font-bold"
                onClick={() => {
                  setAuthMode('login')
                  setShowAuthModal(true)
                }}
              >
                Sign In
              </button>
              <button
                className="btn-primary text-xs"
                onClick={() => {
                  setAuthMode('register')
                  setShowAuthModal(true)
                }}
              >
                <User className="size-4" /> Create Account
              </button>
            </div>
          )}
        </div>

        {/* Mobile Search Bar */}
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
        className="fixed inset-x-0 bottom-0 z-40 grid grid-cols-5 border-t border-line bg-white/95 backdrop-blur md:hidden"
        aria-label="Mobile Navigation"
      >
        <NavLink
          to="/"
          end
          className={({ isActive }) =>
            `flex flex-col items-center gap-0.5 py-2 text-[11px] font-medium transition ${isActive ? 'text-primary font-bold' : 'text-muted hover:text-ink'
            }`
          }
        >
          <Home className="size-5" />
          Home
        </NavLink>
        <NavLink
          to="/restaurants"
          end
          className={({ isActive }) =>
            `flex flex-col items-center gap-0.5 py-2 text-[11px] font-medium transition ${isActive ? 'text-primary font-bold' : 'text-muted hover:text-ink'
            }`
          }
        >
          <Compass className="size-5" />
          Explore
        </NavLink>
        <NavLink
          to="/orders"
          end
          className={({ isActive }) =>
            `flex flex-col items-center gap-0.5 py-2 text-[11px] font-medium transition ${isActive ? 'text-primary font-bold' : 'text-muted hover:text-ink'
            }`
          }
        >
          <ClipboardList className="size-5" />
          Orders
        </NavLink>
        <NavLink
          to="/cart"
          end
          className={({ isActive }) =>
            `relative flex flex-col items-center gap-0.5 py-2 text-[11px] font-medium transition ${isActive ? 'text-primary font-bold' : 'text-muted hover:text-ink'
            }`
          }
        >
          <div className="relative">
            <ShoppingBag className="size-5" />
            {count > 0 && (
              <span className="absolute -right-2 -top-1 grid size-4 place-items-center rounded-full bg-primary text-[9px] font-bold text-white">
                {count}
              </span>
            )}
          </div>
          Cart
        </NavLink>
        <button
          onClick={() => {
            if (currentUser) {
              setShowAccountSheet(true)
            } else {
              setAuthMode('login')
              setShowAuthModal(true)
            }
          }}
          className={`flex flex-col items-center gap-0.5 py-2 text-[11px] font-medium transition ${showAccountSheet ? 'text-primary font-bold' : 'text-muted hover:text-ink'
            }`}
          aria-label={currentUser ? 'My Account' : 'Sign In or Sign Up'}
        >
          <User className="size-5" />
          {currentUser ? 'Account' : 'Sign In'}
        </button>
      </nav>

      {/* Mobile My Account Sheet */}
      {showAccountSheet && currentUser && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/60 p-0 sm:p-4 backdrop-blur-xs"
          onClick={() => setShowAccountSheet(false)}
        >
          <div
            className="w-full max-w-md rounded-t-3xl sm:rounded-3xl bg-white p-6 shadow-2xl animate-in slide-in-from-bottom duration-200"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between pb-3 border-b border-line">
              <h2 className="text-lg font-black text-ink">My Account</h2>
              <button
                onClick={() => setShowAccountSheet(false)}
                className="rounded-full p-1.5 text-muted hover:bg-slate-100 hover:text-ink transition"
                aria-label="Close"
              >
                <X className="size-5" />
              </button>
            </div>

            {/* Profile Info Card */}
            <div className="mt-4 flex items-center gap-3 rounded-2xl bg-slate-50 p-4 border border-line">
              <div className="grid size-12 place-items-center rounded-2xl bg-primary text-white font-black text-lg shadow-sm">
                {currentUser.fullName ? currentUser.fullName.charAt(0).toUpperCase() : 'U'}
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate font-black text-ink text-base">{currentUser.fullName}</p>
                <p className="truncate text-xs text-muted">{currentUser.email}</p>
                <div className="mt-1">
                  {currentUser.role === 'ADMIN' ? (
                    <span className="inline-flex items-center gap-1 rounded-md bg-slate-900 px-2 py-0.5 text-[10px] font-bold text-amber-400">
                      <ShieldCheck className="size-3 text-amber-400" /> Administrator
                    </span>
                  ) : currentUser.role === 'RESTAURANT_OWNER' ? (
                    <span className="inline-flex items-center gap-1 rounded-md bg-blue-100 px-2 py-0.5 text-[10px] font-bold text-blue-800">
                      <Store className="size-3 text-blue-600" /> Restaurant Partner
                    </span>
                  ) : (
                    <span className="inline-block rounded-md bg-emerald-100 px-2 py-0.5 text-[10px] font-bold text-emerald-800">
                      Customer Account
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Account Quick Links */}
            <div className="mt-4 space-y-1 text-sm font-semibold text-ink">
              <Link
                to="/orders"
                onClick={() => setShowAccountSheet(false)}
                className="flex items-center justify-between rounded-xl p-3 hover:bg-slate-50 transition"
              >
                <div className="flex items-center gap-3">
                  <ClipboardList className="size-5 text-primary" />
                  <span>My Orders</span>
                </div>
                <ChevronRight className="size-4 text-muted" />
              </Link>

              <Link
                to="/cart"
                onClick={() => setShowAccountSheet(false)}
                className="flex items-center justify-between rounded-xl p-3 hover:bg-slate-50 transition"
              >
                <div className="flex items-center gap-3">
                  <ShoppingBag className="size-5 text-primary" />
                  <span>My Cart</span>
                </div>
                <div className="flex items-center gap-2">
                  {count > 0 && (
                    <span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-bold text-primary">
                      {count} items
                    </span>
                  )}
                  <ChevronRight className="size-4 text-muted" />
                </div>
              </Link>

              <Link
                to="/restaurants"
                onClick={() => setShowAccountSheet(false)}
                className="flex items-center justify-between rounded-xl p-3 hover:bg-slate-50 transition"
              >
                <div className="flex items-center gap-3">
                  <Compass className="size-5 text-primary" />
                  <span>Explore Restaurants</span>
                </div>
                <ChevronRight className="size-4 text-muted" />
              </Link>

              {currentUser.role === 'ADMIN' && (
                <Link
                  to="/admin/dashboard"
                  onClick={() => setShowAccountSheet(false)}
                  className="flex items-center justify-between rounded-xl p-3 bg-slate-900 text-white hover:bg-slate-800 transition"
                >
                  <div className="flex items-center gap-3">
                    <ShieldCheck className="size-5 text-amber-400" />
                    <span>Admin Dashboard</span>
                  </div>
                  <ChevronRight className="size-4 text-slate-400" />
                </Link>
              )}

              {currentUser.role === 'RESTAURANT_OWNER' && (
                <Link
                  to="/owner/dashboard"
                  onClick={() => setShowAccountSheet(false)}
                  className="flex items-center justify-between rounded-xl p-3 bg-blue-50 text-blue-900 hover:bg-blue-100 transition"
                >
                  <div className="flex items-center gap-3">
                    <Store className="size-5 text-blue-600" />
                    <span>Owner Portal</span>
                  </div>
                  <ChevronRight className="size-4 text-blue-400" />
                </Link>
              )}
            </div>

            {/* Logout Action */}
            <button
              onClick={handleLogout}
              className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl border border-rose-200 bg-rose-50 py-3 text-xs font-bold text-rose-700 hover:bg-rose-100 transition"
            >
              <LogOut className="size-4" /> Sign Out
            </button>
          </div>
        </div>
      )}

      {/* Auth Modal */}
      <AuthModal
        isOpen={showAuthModal}
        initialMode={authMode}
        onClose={() => setShowAuthModal(false)}
        onSuccess={() => toast(`Welcome to Cravely!`)}
      />
    </div>
  )
}
