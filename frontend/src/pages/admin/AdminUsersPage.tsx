import {
  AlertCircle,
  CheckCircle2,
  Lock,
  RefreshCw,
  Search,
  Shield,
  ShieldAlert,
  Unlock,
  UserCheck,
  UserX,
  Users,
} from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { adminService } from '../../services/adminService'
import type { PageResponse, UserResponse } from '../../types/admin'

export default function AdminUsersPage() {
  const [usersPage, setUsersPage] = useState<PageResponse<UserResponse> | null>(null)
  const [page, setPage] = useState(0)
  const [size] = useState(10)
  const [roleFilter, setRoleFilter] = useState('')
  const [activeFilter, setActiveFilter] = useState<string>('')
  const [searchTerm, setSearchTerm] = useState('')
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  // Confirmation modal state
  const [confirmModal, setConfirmModal] = useState<{
    user: UserResponse
    action: 'activate' | 'deactivate'
  } | null>(null)

  const loadUsers = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const activeParam = activeFilter === '' ? undefined : activeFilter === 'true'
      const roleParam = roleFilter === '' ? undefined : roleFilter
      const searchParam = searchTerm.trim() || undefined

      const data = await adminService.getUsers(page, size, roleParam, activeParam, searchParam)
      setUsersPage(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch users')
    } finally {
      setLoading(false)
    }
  }, [page, size, roleFilter, activeFilter, searchTerm])

  useEffect(() => {
    loadUsers()
  }, [loadUsers])

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    loadUsers()
  }

  const handleToggleStatus = async () => {
    if (!confirmModal) return
    const { user, action } = confirmModal
    setActionLoading(user.id)
    setError(null)
    setSuccessMsg(null)

    try {
      if (action === 'activate') {
        await adminService.activateUser(user.id)
        setSuccessMsg(`User "${user.fullName}" (${user.email}) has been activated successfully.`)
      } else {
        await adminService.deactivateUser(user.id)
        setSuccessMsg(`User "${user.fullName}" (${user.email}) has been deactivated.`)
      }
      setConfirmModal(null)
      loadUsers()
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to ${action} user`)
      setConfirmModal(null)
    } finally {
      setActionLoading(null)
    }
  }

  const getRoleBadge = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-rose-100 px-2.5 py-0.5 text-xs font-bold text-rose-800">
            <Shield className="size-3" /> ADMIN
          </span>
        )
      case 'RESTAURANT_OWNER':
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-blue-100 px-2.5 py-0.5 text-xs font-bold text-blue-800">
            RESTAURANT OWNER
          </span>
        )
      default:
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-bold text-slate-800">
            CUSTOMER
          </span>
        )
    }
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-ink">User Account Management</h1>
          <p className="text-xs text-muted mt-0.5">
            View, search, filter, and manage platform customer, owner, and administrator accounts
          </p>
        </div>
        <button
          onClick={() => loadUsers()}
          className="inline-flex items-center gap-1.5 rounded-xl border border-line bg-white px-3.5 py-2 text-xs font-semibold text-ink shadow-2xs hover:bg-slate-50 transition"
        >
          <RefreshCw className="size-3.5" /> Refresh List
        </button>
      </div>

      {/* Notifications */}
      {error && (
        <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-xs text-rose-800 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0 text-rose-600" />
          <span>{error}</span>
        </div>
      )}

      {successMsg && (
        <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-xs text-emerald-800 flex items-center gap-2">
          <CheckCircle2 className="size-4 shrink-0 text-emerald-600" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="rounded-2xl border border-line bg-white p-4 shadow-xs flex flex-wrap items-center justify-between gap-4">
        <form onSubmit={handleSearchSubmit} className="flex-1 min-w-[260px] flex items-center gap-2">
          <div className="relative flex-1">
            <Search className="size-4 text-muted absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search by full name, email, or phone..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full rounded-xl border border-line pl-9 pr-3 py-2 text-xs text-ink focus:border-primary focus:outline-none"
            />
          </div>
          <button
            type="submit"
            className="rounded-xl bg-primary px-3.5 py-2 text-xs font-bold text-white shadow-2xs hover:bg-primary-dark transition"
          >
            Search
          </button>
        </form>

        <div className="flex flex-wrap items-center gap-3 text-xs">
          <div className="flex items-center gap-1.5">
            <span className="text-muted font-semibold">Role:</span>
            <select
              value={roleFilter}
              onChange={(e) => {
                setRoleFilter(e.target.value)
                setPage(0)
              }}
              className="rounded-lg border border-line px-2.5 py-1.5 text-xs text-ink bg-white focus:outline-none"
            >
              <option value="">All Roles</option>
              <option value="CUSTOMER">Customer</option>
              <option value="RESTAURANT_OWNER">Restaurant Owner</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>

          <div className="flex items-center gap-1.5">
            <span className="text-muted font-semibold">Status:</span>
            <select
              value={activeFilter}
              onChange={(e) => {
                setActiveFilter(e.target.value)
                setPage(0)
              }}
              className="rounded-lg border border-line px-2.5 py-1.5 text-xs text-ink bg-white focus:outline-none"
            >
              <option value="">All Statuses</option>
              <option value="true">Active Only</option>
              <option value="false">Deactivated Only</option>
            </select>
          </div>
        </div>
      </div>

      {/* Users Table */}
      <div className="rounded-2xl border border-line bg-white shadow-xs overflow-hidden">
        {loading ? (
          <div className="py-20 text-center">
            <RefreshCw className="mx-auto size-8 animate-spin text-primary" />
            <p className="mt-3 text-xs font-semibold text-muted">Loading user accounts...</p>
          </div>
        ) : !usersPage || usersPage.content.length === 0 ? (
          <div className="py-16 text-center text-xs text-muted">
            <Users className="mx-auto size-10 text-slate-300 mb-2" />
            <p className="font-semibold text-ink text-sm">No users found</p>
            <p className="text-slate-400 mt-1">Try adjusting your filters or search keywords.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-line text-muted uppercase text-[10px] font-bold tracking-wider">
                <tr>
                  <th className="px-5 py-3.5">User</th>
                  <th className="px-5 py-3.5">Contact Details</th>
                  <th className="px-5 py-3.5">Role</th>
                  <th className="px-5 py-3.5">Status</th>
                  <th className="px-5 py-3.5">Registered</th>
                  <th className="px-5 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {usersPage.content.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-50/50 transition">
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-3">
                        <div className="size-8 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center font-bold text-ink text-xs shrink-0">
                          {u.fullName ? u.fullName.charAt(0).toUpperCase() : 'U'}
                        </div>
                        <div>
                          <p className="font-bold text-ink">{u.fullName}</p>
                          <p className="text-[11px] text-muted">ID: #{u.id}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-4">
                      <p className="text-ink font-medium">{u.email}</p>
                      <p className="text-[11px] text-muted">{u.phone || 'No phone provided'}</p>
                    </td>
                    <td className="px-5 py-4">{getRoleBadge(u.role)}</td>
                    <td className="px-5 py-4">
                      {u.active ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-0.5 text-[11px] font-bold text-emerald-800">
                          <span className="size-1.5 rounded-full bg-emerald-500" /> Active
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 rounded-full bg-rose-100 px-2.5 py-0.5 text-[11px] font-bold text-rose-800">
                          <span className="size-1.5 rounded-full bg-rose-500" /> Deactivated
                        </span>
                      )}
                    </td>
                    <td className="px-5 py-4 text-muted">
                      {new Date(u.createdAt).toLocaleDateString('en-IN', {
                        year: 'numeric',
                        month: 'short',
                        day: 'numeric',
                      })}
                    </td>
                    <td className="px-5 py-4 text-right">
                      {u.active ? (
                        <button
                          onClick={() => setConfirmModal({ user: u, action: 'deactivate' })}
                          disabled={actionLoading === u.id}
                          className="inline-flex items-center gap-1 rounded-lg border border-rose-200 bg-rose-50 px-2.5 py-1 text-xs font-semibold text-rose-700 hover:bg-rose-100 transition disabled:opacity-50"
                        >
                          <Lock className="size-3" /> Deactivate
                        </button>
                      ) : (
                        <button
                          onClick={() => setConfirmModal({ user: u, action: 'activate' })}
                          disabled={actionLoading === u.id}
                          className="inline-flex items-center gap-1 rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-700 hover:bg-emerald-100 transition disabled:opacity-50"
                        >
                          <Unlock className="size-3" /> Activate
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        {usersPage && usersPage.totalPages > 1 && (
          <div className="flex items-center justify-between px-5 py-3 border-t border-line bg-slate-50/50 text-xs text-muted">
            <span>
              Showing {usersPage.number * usersPage.size + 1} to{' '}
              {Math.min((usersPage.number + 1) * usersPage.size, usersPage.totalElements)} of{' '}
              {usersPage.totalElements} users
            </span>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={usersPage.number === 0}
                className="rounded-lg border border-line bg-white px-3 py-1 font-semibold text-ink shadow-2xs hover:bg-slate-50 disabled:opacity-40"
              >
                Previous
              </button>
              <span className="font-bold text-ink">
                Page {usersPage.number + 1} of {usersPage.totalPages}
              </span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={usersPage.number >= usersPage.totalPages - 1}
                className="rounded-lg border border-line bg-white px-3 py-1 font-semibold text-ink shadow-2xs hover:bg-slate-50 disabled:opacity-40"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Confirmation Modal */}
      {confirmModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl border border-line space-y-4">
            <div className="flex items-center gap-3">
              <div
                className={`size-10 rounded-full flex items-center justify-center shrink-0 ${
                  confirmModal.action === 'deactivate'
                    ? 'bg-rose-100 text-rose-700'
                    : 'bg-emerald-100 text-emerald-700'
                }`}
              >
                {confirmModal.action === 'deactivate' ? (
                  <ShieldAlert className="size-5" />
                ) : (
                  <UserCheck className="size-5" />
                )}
              </div>
              <div>
                <h3 className="text-base font-bold text-ink capitalize">
                  {confirmModal.action} User Account
                </h3>
                <p className="text-xs text-muted">Confirm user authorization state change</p>
              </div>
            </div>

            <p className="text-xs text-slate-600 leading-relaxed">
              Are you sure you want to <strong>{confirmModal.action}</strong> the account of{' '}
              <strong>{confirmModal.user.fullName}</strong> ({confirmModal.user.email})?
              {confirmModal.action === 'deactivate' && (
                <span className="block mt-2 text-rose-700 font-medium">
                  Note: Deactivated users will be prevented from logging in or placing orders. If
                  this is the sole active administrator, the system will prevent deactivation.
                </span>
              )}
            </p>

            <div className="flex items-center justify-end gap-3 pt-3 border-t border-line">
              <button
                onClick={() => setConfirmModal(null)}
                className="rounded-xl border border-line px-4 py-2 text-xs font-semibold text-muted hover:bg-slate-50"
              >
                Cancel
              </button>
              <button
                onClick={handleToggleStatus}
                className={`rounded-xl px-4 py-2 text-xs font-bold text-white shadow-2xs transition capitalize ${
                  confirmModal.action === 'deactivate'
                    ? 'bg-rose-600 hover:bg-rose-700'
                    : 'bg-emerald-600 hover:bg-emerald-700'
                }`}
              >
                Confirm {confirmModal.action}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
