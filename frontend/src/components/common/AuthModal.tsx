import { AlertCircle, Lock, Mail, Phone, ShieldCheck, User, X } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { authService, type RegisterRequest } from '../../services/authService'

interface AuthModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess?: () => void
}

export default function AuthModal({ isOpen, onClose, onSuccess }: AuthModalProps) {
  const [isRegister, setIsRegister] = useState(false)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fullName, setFullName] = useState('')
  const [phone, setPhone] = useState('')
  const [role, setRole] = useState<'CUSTOMER' | 'RESTAURANT_OWNER'>('CUSTOMER')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const nav = useNavigate()

  if (!isOpen) return null

  const handleQuickFill = (quickEmail: string, quickPass: string) => {
    setEmail(quickEmail)
    setPassword(quickPass)
    setError(null)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)

    try {
      if (isRegister) {
        const payload: RegisterRequest = {
          fullName,
          email,
          phone: phone || undefined,
          password,
          role,
        }
        const res = await authService.register(payload)
        if (onSuccess) onSuccess()
        onClose()
        if (res.user.role === 'ADMIN') nav('/admin/dashboard')
        else if (res.user.role === 'RESTAURANT_OWNER') nav('/owner/dashboard')
      } else {
        const res = await authService.login({ email, password })
        if (onSuccess) onSuccess()
        onClose()
        if (res.user.role === 'ADMIN') nav('/admin/dashboard')
        else if (res.user.role === 'RESTAURANT_OWNER') nav('/owner/dashboard')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Authentication failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs"
    >
      <div className="relative w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl sm:p-8">
        <button
          onClick={onClose}
          className="absolute right-4 top-4 rounded-full p-2 text-muted hover:bg-slate-100 hover:text-ink transition"
          aria-label="Close modal"
        >
          <X className="size-5" />
        </button>

        {/* Brand & Title */}
        <div className="text-center">
          <div className="inline-flex size-12 items-center justify-center rounded-2xl bg-primary/10 text-primary mb-3">
            <ShieldCheck className="size-6" />
          </div>
          <h2 className="text-2xl font-black text-ink">
            {isRegister ? 'Join Cravely' : 'Welcome back'}
          </h2>
          <p className="mt-1 text-xs text-muted">
            {isRegister
              ? 'Create an account to order food or manage your restaurant'
              : 'Sign in to access your orders, cart, and dashboard'}
          </p>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="mt-4 flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-800">
            <AlertCircle className="size-4 shrink-0 text-rose-600" />
            <span>{error}</span>
          </div>
        )}

        {/* Demo Fast Logins (Only shown on Login tab) */}
        {!isRegister && (
          <div className="mt-4 rounded-xl border border-line bg-slate-50/70 p-3">
            <p className="text-[11px] font-bold text-muted uppercase tracking-wider text-center">
              Quick Demo Fill:
            </p>
            <div className="mt-2 flex flex-wrap gap-1.5 justify-center text-xs">
              <button
                type="button"
                onClick={() => handleQuickFill('john@example.com', 'Password123!')}
                className="rounded-lg bg-white border border-line px-2.5 py-1 text-[11px] font-semibold text-ink shadow-2xs hover:border-primary hover:text-primary transition"
              >
                Customer
              </button>
              <button
                type="button"
                onClick={() => handleQuickFill('owner@pizzaparadise.com', 'Password123!')}
                className="rounded-lg bg-white border border-line px-2.5 py-1 text-[11px] font-semibold text-ink shadow-2xs hover:border-primary hover:text-primary transition"
              >
                Restaurant Owner
              </button>
              <button
                type="button"
                onClick={() => handleQuickFill('admin@cravely.com', 'Password123!')}
                className="rounded-lg bg-white border border-line px-2.5 py-1 text-[11px] font-semibold text-ink shadow-2xs hover:border-primary hover:text-primary transition"
              >
                Admin
              </button>
            </div>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="mt-5 space-y-3.5">
          {isRegister && (
            <div>
              <label className="block text-xs font-semibold text-slate-700">Full Name</label>
              <div className="relative mt-1">
                <User className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted" />
                <input
                  required
                  type="text"
                  placeholder="e.g. John Doe"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="w-full rounded-xl border border-line bg-white pl-9 pr-3 py-2.5 text-xs text-ink outline-none focus:border-primary"
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700">Email Address</label>
            <div className="relative mt-1">
              <Mail className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted" />
              <input
                required
                type="email"
                placeholder="name@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full rounded-xl border border-line bg-white pl-9 pr-3 py-2.5 text-xs text-ink outline-none focus:border-primary"
              />
            </div>
          </div>

          {isRegister && (
            <div>
              <label className="block text-xs font-semibold text-slate-700">Phone (Optional)</label>
              <div className="relative mt-1">
                <Phone className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted" />
                <input
                  type="tel"
                  placeholder="9876543210"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="w-full rounded-xl border border-line bg-white pl-9 pr-3 py-2.5 text-xs text-ink outline-none focus:border-primary"
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700">Password</label>
            <div className="relative mt-1">
              <Lock className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted" />
              <input
                required
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full rounded-xl border border-line bg-white pl-9 pr-3 py-2.5 text-xs text-ink outline-none focus:border-primary"
              />
            </div>
          </div>

          {isRegister && (
            <div>
              <label className="block text-xs font-semibold text-slate-700">Account Type</label>
              <div className="mt-1 grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setRole('CUSTOMER')}
                  className={`rounded-xl border p-2 text-xs font-bold transition ${
                    role === 'CUSTOMER'
                      ? 'border-primary bg-primary/10 text-primary'
                      : 'border-line bg-white text-muted hover:border-slate-300'
                  }`}
                >
                  Customer
                </button>
                <button
                  type="button"
                  onClick={() => setRole('RESTAURANT_OWNER')}
                  className={`rounded-xl border p-2 text-xs font-bold transition ${
                    role === 'RESTAURANT_OWNER'
                      ? 'border-primary bg-primary/10 text-primary'
                      : 'border-line bg-white text-muted hover:border-slate-300'
                  }`}
                >
                  Restaurant Owner
                </button>
              </div>
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-xl bg-primary py-3 text-xs font-bold text-white shadow-md hover:bg-primary-dark transition disabled:opacity-50 mt-2"
          >
            {loading ? 'Please wait...' : isRegister ? 'Create Account' : 'Sign In'}
          </button>
        </form>

        {/* Toggle between Login and Register */}
        <div className="mt-5 text-center text-xs text-muted border-t border-line pt-4">
          {isRegister ? (
            <p>
              Already have an account?{' '}
              <button
                type="button"
                onClick={() => {
                  setIsRegister(false)
                  setError(null)
                }}
                className="font-bold text-primary hover:underline"
              >
                Sign In
              </button>
            </p>
          ) : (
            <p>
              Don't have an account?{' '}
              <button
                type="button"
                onClick={() => {
                  setIsRegister(true)
                  setError(null)
                }}
                className="font-bold text-primary hover:underline"
              >
                Create Account
              </button>
            </p>
          )}
        </div>
      </div>
    </div>
  )
}

