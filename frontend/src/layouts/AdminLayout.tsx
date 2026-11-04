import {
  ArrowLeft,
  LayoutDashboard,
  LogOut,
  Menu,
  ShieldCheck,
  ShoppingBag,
  Store,
  TrendingUp,
  Users,
  UtensilsCrossed,
  X,
} from 'lucide-react'
import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { BrandLogo } from '../components/common/ui'

const navLinks = [
  { to: '/admin/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/admin/analytics', label: 'Analytics', icon: TrendingUp },
  { to: '/admin/orders', label: 'Orders', icon: ShoppingBag },
  { to: '/admin/restaurants', label: 'Restaurants', icon: Store },
  { to: '/admin/food-items', label: 'Food Items', icon: UtensilsCrossed },
  { to: '/admin/users', label: 'Users', icon: Users },
]

export default function AdminLayout() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const nav = useNavigate()

  // Retrieve user/admin status from localStorage
  const adminEmail = localStorage.getItem('cravely_admin_email') || 'admin@cravely.com'

  const handleLogout = () => {
    localStorage.removeItem('cravely_token')
    localStorage.removeItem('cravely_admin_email')
    nav('/')
  }

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col md:flex-row">
      {/* Mobile Top Header */}
      <div className="md:hidden flex items-center justify-between px-4 py-3 bg-white border-b border-line sticky top-0 z-50">
        <BrandLogo />
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center gap-1 rounded bg-rose-100 px-2 py-0.5 text-[11px] font-bold text-rose-800">
            <ShieldCheck className="size-3" /> ADMIN
          </span>
          <button
            onClick={() => setMobileOpen(!mobileOpen)}
            className="p-2 rounded-lg border border-line text-ink"
            aria-label="Toggle navigation menu"
          >
            {mobileOpen ? <X className="size-5" /> : <Menu className="size-5" />}
          </button>
        </div>
      </div>

      {/* Sidebar Navigation */}
      <aside
        className={`fixed inset-y-0 left-0 z-40 w-64 bg-slate-900 text-white flex flex-col transition-transform md:translate-x-0 md:static md:inset-auto ${
          mobileOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Brand & Badge */}
        <div className="p-6 border-b border-slate-800 flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xl font-black tracking-tight text-white">Cravely</span>
              <span className="rounded bg-primary/20 px-2 py-0.5 text-[10px] font-bold text-primary border border-primary/30 uppercase tracking-widest">
                ADMIN
              </span>
            </div>
            <p className="text-[11px] text-slate-400 mt-1">Management Portal</p>
          </div>
          <button
            onClick={() => setMobileOpen(false)}
            className="md:hidden text-slate-400 hover:text-white"
          >
            <X className="size-5" />
          </button>
        </div>

        {/* Navigation Links */}
        <nav className="flex-1 px-3 py-6 space-y-1 overflow-y-auto">
          {navLinks.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={() => setMobileOpen(false)}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-semibold transition ${
                  isActive
                    ? 'bg-primary text-white shadow-sm'
                    : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                }`
              }
            >
              <item.icon className="size-4 shrink-0" />
              {item.label}
            </NavLink>
          ))}
        </nav>

        {/* Footer Actions */}
        <div className="p-4 border-t border-slate-800 space-y-2">
          <Link
            to="/"
            className="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-semibold text-slate-400 hover:text-white hover:bg-slate-800 transition"
          >
            <ArrowLeft className="size-3.5" /> Customer View
          </Link>
          <div className="flex items-center justify-between pt-2 border-t border-slate-800/60 px-1">
            <div className="truncate mr-2">
              <p className="text-xs font-bold text-white truncate">Administrator</p>
              <p className="text-[10px] text-slate-400 truncate">{adminEmail}</p>
            </div>
            <button
              onClick={handleLogout}
              className="text-slate-400 hover:text-rose-400 p-1.5 rounded-lg transition"
              title="Logout from Admin"
            >
              <LogOut className="size-4" />
            </button>
          </div>
        </div>
      </aside>

      {/* Backdrop for mobile sidebar */}
      {mobileOpen && (
        <div
          onClick={() => setMobileOpen(false)}
          className="fixed inset-0 bg-black/50 z-30 md:hidden"
        />
      )}

      {/* Main Content Area */}
      <main className="flex-1 flex flex-col min-w-0 overflow-y-auto">
        <header className="hidden md:flex items-center justify-between px-8 py-4 bg-white border-b border-line sticky top-0 z-20">
          <div>
            <h1 className="text-base font-bold text-ink">Administrative Dashboard</h1>
            <p className="text-xs text-muted">Platform metrics, operations, and control panel</p>
          </div>
          <div className="flex items-center gap-3">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 border border-emerald-200 px-3 py-1 text-xs font-bold text-emerald-800">
              <span className="size-2 rounded-full bg-emerald-500 animate-pulse" />
              API Connected
            </span>
            <div className="size-8 rounded-full bg-slate-900 text-white flex items-center justify-center font-bold text-xs">
              AD
            </div>
          </div>
        </header>

        <div className="flex-1 p-4 md:p-8">
          <Outlet />
        </div>
      </main>
    </div>
  )
}

