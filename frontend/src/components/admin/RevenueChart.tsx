import { AlertTriangle, CheckCircle, TrendingUp } from 'lucide-react'
import { useState } from 'react'
import type { RevenueDataPoint } from '../../types/admin'
import { inr } from '../../utils/format'

interface RevenueChartProps {
  data: RevenueDataPoint[]
  totalRevenue: number
  deliveredRevenue: number
  reconciliationExposure: number
  period: string
  onPeriodChange: (period: string) => void
  loading?: boolean
}

export default function RevenueChart({
  data,
  totalRevenue,
  deliveredRevenue,
  reconciliationExposure,
  period,
  onPeriodChange,
  loading = false,
}: RevenueChartProps) {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null)

  const maxVal = Math.max(1, ...data.map((d) => d.revenue))
  const chartHeight = 180

  return (
    <div className="rounded-2xl border border-line bg-white p-6 shadow-sm">
      {/* Header with Title and Period Filter */}
      <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
        <div>
          <h2 className="text-base font-bold text-ink flex items-center gap-2">
            <TrendingUp className="size-4 text-primary" /> Revenue Analytics (Captured Payments)
          </h2>
          <p className="text-xs text-muted mt-0.5">
            Calculated strictly from verified Razorpay payments (excluding pending/failed attempts)
          </p>
        </div>

        {/* Period Selector */}
        <div className="flex items-center rounded-xl bg-slate-100 p-1 text-xs font-semibold">
          {(['daily', 'weekly', 'monthly', 'yearly'] as const).map((p) => (
            <button
              key={p}
              onClick={() => onPeriodChange(p)}
              className={`px-3 py-1.5 rounded-lg capitalize transition ${
                period === p ? 'bg-white text-ink shadow-2xs font-bold' : 'text-muted hover:text-ink'
              }`}
            >
              {p}
            </button>
          ))}
        </div>
      </div>

      {/* Financial Split Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6 pt-2 border-t border-line">
        <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100">
          <span className="text-[11px] font-bold text-muted uppercase tracking-wider block">
            Total Captured Revenue
          </span>
          <span className="text-xl font-black text-ink block mt-1">{inr(totalRevenue)}</span>
          <span className="text-[11px] text-muted block mt-0.5">All successful payments</span>
        </div>

        <div className="p-3.5 rounded-xl bg-emerald-50/60 border border-emerald-100">
          <span className="text-[11px] font-bold text-emerald-800 uppercase tracking-wider flex items-center gap-1">
            <CheckCircle className="size-3 text-emerald-600" /> Delivered Revenue
          </span>
          <span className="text-xl font-black text-emerald-700 block mt-1">
            {inr(deliveredRevenue)}
          </span>
          <span className="text-[11px] text-emerald-700/80 block mt-0.5">Fulfilled orders</span>
        </div>

        <div className="p-3.5 rounded-xl bg-amber-50/60 border border-amber-100">
          <span className="text-[11px] font-bold text-amber-800 uppercase tracking-wider flex items-center gap-1">
            <AlertTriangle className="size-3 text-amber-600" /> Reconciliation Exposure
          </span>
          <span className="text-xl font-black text-amber-700 block mt-1">
            {inr(reconciliationExposure)}
          </span>
          <span className="text-[11px] text-amber-700/80 block mt-0.5">Captured on Cancelled/Rejected</span>
        </div>
      </div>

      {/* Interactive Bar Chart */}
      {loading ? (
        <div className="h-48 flex items-center justify-center text-xs font-semibold text-muted">
          Loading revenue chart...
        </div>
      ) : data.length === 0 ? (
        <div className="h-48 flex items-center justify-center text-xs text-muted border border-dashed border-line rounded-xl">
          No revenue recorded in this period.
        </div>
      ) : (
        <div className="relative pt-6">
          {/* Tooltip display */}
          {hoveredIndex !== null && data[hoveredIndex] && (
            <div className="absolute top-0 left-1/2 -translate-x-1/2 z-10 px-3 py-1.5 rounded-lg bg-slate-900 text-white text-xs font-semibold shadow-md flex items-center gap-2 pointer-events-none">
              <span>{data[hoveredIndex].label}:</span>
              <strong className="text-emerald-400">{inr(data[hoveredIndex].revenue)}</strong>
              <span className="text-slate-400">({data[hoveredIndex].orders} orders)</span>
            </div>
          )}

          {/* SVG Bars Container */}
          <div className="flex items-end gap-2 md:gap-3 h-48 border-b border-line px-2">
            {data.map((item, idx) => {
              const heightPercent = Math.max(8, (item.revenue / maxVal) * 100)
              const isHovered = hoveredIndex === idx

              return (
                <div
                  key={item.date || idx}
                  className="flex-1 flex flex-col items-center h-full justify-end group cursor-pointer"
                  onMouseEnter={() => setHoveredIndex(idx)}
                  onMouseLeave={() => setHoveredIndex(null)}
                >
                  <div
                    className={`w-full max-w-[40px] rounded-t-md transition-all duration-200 ${
                      isHovered
                        ? 'bg-primary ring-2 ring-primary/30 shadow-md'
                        : 'bg-emerald-500/80 group-hover:bg-primary'
                    }`}
                    style={{ height: `${heightPercent}%` }}
                  />
                  <span className="text-[10px] text-muted truncate max-w-[48px] mt-2 block text-center">
                    {item.label}
                  </span>
                </div>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}
