import { useEffect, useRef, useState } from 'react'
import { createChart, CandlestickSeries, HistogramSeries } from 'lightweight-charts'
import api from '../api/axios'

const fmt = (n) => Number(n).toLocaleString('ko-KR')

const POLL_INTERVAL = 10_000 // 10초

// 장 운영 시간 여부 (평일 09:00~15:30)
function isMarketOpen() {
  const now = new Date()
  const day = now.getDay()
  if (day === 0 || day === 6) return false
  const h = now.getHours()
  const m = now.getMinutes()
  const total = h * 60 + m
  return total >= 9 * 60 && total < 15 * 60 + 30
}

export default function Stocks() {
  const [stocks, setStocks] = useState([])
  const [selected, setSelected] = useState(null)
  const [currentPrice, setCurrentPrice] = useState(null)
  const [chartPrices, setChartPrices] = useState([])   // 차트용 전체 데이터
  const [prices, setPrices] = useState([])              // 테이블용 현재 페이지 데이터
  const [pagination, setPagination] = useState({ page: 0, totalPages: 0, totalElements: 0 })
  const [loading, setLoading] = useState(false)
  const [fetchForm, setFetchForm] = useState({ startDate: '', endDate: '' })
  const [fetching, setFetching] = useState(false)
  const [fetchMsg, setFetchMsg] = useState('')
  const [polling, setPolling] = useState(false)

  const candleRef = useRef(null)
  const volumeRef = useRef(null)
  const candleChart = useRef(null)
  const volumeChart = useRef(null)
  const pollTimer = useRef(null)

  useEffect(() => {
    api.get('/stocks').then(({ data }) => {
      const list = data.data || []
      setStocks(list)
      if (list.length > 0) setSelected(list[0])
    }).catch(() => {})
  }, [])

  useEffect(() => {
    if (!selected) return
    setLoading(true)
    setCurrentPrice(null)
    setChartPrices([])
    setPrices([])
    setPagination({ page: 0, totalPages: 0, totalElements: 0 })

    api.get(`/stocks/${selected.code}`).then(({ data }) => {
      setCurrentPrice(data.data)
    }).catch(() => {})

    // 차트용 전체 데이터
    api.get(`/stocks/${selected.code}/prices/chart`).then(({ data }) => {
      setChartPrices(data.data || [])
    }).catch(() => {})

    // 테이블용 첫 페이지
    fetchPage(selected.code, 0).finally(() => setLoading(false))

    // 폴링 시작 — 종목 변경 시 이전 타이머 정리 후 재시작
    clearInterval(pollTimer.current)
    if (isMarketOpen()) {
      setPolling(true)
      pollTimer.current = setInterval(() => {
        if (!isMarketOpen()) {
          clearInterval(pollTimer.current)
          setPolling(false)
          return
        }
        api.get(`/stocks/${selected.code}`).then(({ data }) => {
          setCurrentPrice(data.data)
        }).catch(() => {})
      }, POLL_INTERVAL)
    } else {
      setPolling(false)
    }

    return () => clearInterval(pollTimer.current)
  }, [selected])

  async function fetchPage(code, page) {
    const { data } = await api.get(`/stocks/${code}/prices`, { params: { page, size: 50 } })
    const pageData = data.data
    setPrices(pageData.content || [])
    setPagination({
      page: pageData.number,
      totalPages: pageData.totalPages,
      totalElements: pageData.totalElements,
    })
  }

  function handlePageChange(page) {
    if (!selected) return
    fetchPage(selected.code, page)
  }

  // 차트 렌더링
  useEffect(() => {
    if (!chartPrices.length || !candleRef.current || !volumeRef.current) return

    if (candleChart.current) { candleChart.current.remove(); candleChart.current = null }
    if (volumeChart.current) { volumeChart.current.remove(); volumeChart.current = null }

    const sorted = [...chartPrices].reverse()
    const commonOpts = {
      layout: { background: { color: '#263445' }, textColor: '#a0b0c0' },
      grid: { vertLines: { color: '#2c3e50' }, horzLines: { color: '#2c3e50' } },
      timeScale: { borderColor: '#2c3e50', timeVisible: true, secondsVisible: false },
      rightPriceScale: { borderColor: '#2c3e50' },
    }

    const cc = createChart(candleRef.current, { ...commonOpts, width: candleRef.current.offsetWidth, height: 360 })
    const cs = cc.addSeries(CandlestickSeries, { upColor: '#e74c3c', downColor: '#3498db', borderUpColor: '#e74c3c', borderDownColor: '#3498db', wickUpColor: '#e74c3c', wickDownColor: '#3498db' })
    cs.setData(sorted.map(d => ({ time: String(d.tradeDate), open: d.openPrice, high: d.highPrice, low: d.lowPrice, close: d.closePrice })))

    const vc = createChart(volumeRef.current, { ...commonOpts, width: volumeRef.current.offsetWidth, height: 100, timeScale: { ...commonOpts.timeScale, visible: false } })
    const vs = vc.addSeries(HistogramSeries, { priceFormat: { type: 'volume' }, priceScaleId: '', scaleMargins: { top: 0.1, bottom: 0 } })
    vs.setData(sorted.map(d => ({ time: String(d.tradeDate), value: d.volume, color: d.closePrice >= d.openPrice ? 'rgba(231,76,60,0.5)' : 'rgba(52,152,219,0.5)' })))

    cc.timeScale().subscribeVisibleLogicalRangeChange(r => r && vc.timeScale().setVisibleLogicalRange(r))
    vc.timeScale().subscribeVisibleLogicalRangeChange(r => r && cc.timeScale().setVisibleLogicalRange(r))
    cc.timeScale().fitContent()

    const onResize = () => {
      cc.applyOptions({ width: candleRef.current.offsetWidth })
      vc.applyOptions({ width: volumeRef.current.offsetWidth })
    }
    window.addEventListener('resize', onResize)

    candleChart.current = cc
    volumeChart.current = vc

    return () => window.removeEventListener('resize', onResize)
  }, [chartPrices])

  const latest = prices[0]
  const prev = prices[1]
  const diff = latest && prev ? latest.closePrice - prev.closePrice : 0
  const rate = prev ? ((diff / prev.closePrice) * 100).toFixed(2) : '0.00'

  function priceColor(v) {
    return v > 0 ? 'text-up' : v < 0 ? 'text-down' : 'text-muted'
  }

  function signIcon(v) {
    return v > 0 ? '▲' : v < 0 ? '▼' : '-'
  }

  async function handleFetch(e) {
    e.preventDefault()
    if (!selected) return
    setFetching(true)
    setFetchMsg('')
    try {
      const { data } = await api.post(`/stocks/${selected.code}/fetch`, null, {
        params: { startDate: fetchForm.startDate.replace(/-/g, ''), endDate: fetchForm.endDate.replace(/-/g, '') }
      })
      setFetchMsg(`${data.data.savedCount}건 저장 완료`)
      const [chartRes] = await Promise.all([
        api.get(`/stocks/${selected.code}/prices/chart`),
        fetchPage(selected.code, 0),
      ])
      setChartPrices(chartRes.data.data || [])
    } catch (err) {
      setFetchMsg(err.response?.data?.message || '수집 실패')
    } finally {
      setFetching(false)
    }
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">

      {/* 종목 선택 + 일봉 수집 */}
      <div className="flex flex-wrap gap-3 items-end mb-6">
        <select
          className="bg-card border border-border text-white rounded-lg px-4 py-2.5 min-w-64 focus:outline-none focus:border-primary cursor-pointer"
          value={selected?.code || ''}
          onChange={(e) => {
            const s = stocks.find(s => s.code === e.target.value)
            if (s) setSelected(s)
            setFetchMsg('')
          }}
        >
          {stocks.length === 0 && <option value="">종목 불러오는 중...</option>}
          {stocks.map(s => (
            <option key={s.code} value={s.code}>{s.name} ({s.code})</option>
          ))}
        </select>

        {/* 일봉 수집 폼 */}
        <form onSubmit={handleFetch} className="flex gap-2 items-end flex-wrap">
          <div className="flex flex-col gap-1">
            <label className="text-muted text-xs">시작일</label>
            <input
              type="date"
              value={fetchForm.startDate}
              onChange={(e) => setFetchForm({ ...fetchForm, startDate: e.target.value })}
              className="bg-card border border-border text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-primary"
              required
            />
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-muted text-xs">종료일</label>
            <input
              type="date"
              value={fetchForm.endDate}
              onChange={(e) => setFetchForm({ ...fetchForm, endDate: e.target.value })}
              className="bg-card border border-border text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-primary"
              required
            />
          </div>
          <button
            type="submit"
            disabled={fetching || !selected}
            className="bg-card border border-border text-muted px-4 py-2 rounded-lg text-sm hover:text-white transition-colors disabled:opacity-50"
          >
            {fetching ? '수집 중...' : '일봉 수집'}
          </button>
          {fetchMsg && <span className="text-sm text-green-400">{fetchMsg}</span>}
        </form>
      </div>

      {/* 현재가 카드 */}
      <div className="bg-card border border-border rounded-xl p-6 mb-6">
        {!selected && <p className="text-muted text-center">종목을 선택해주세요.</p>}
        {selected && !currentPrice && !latest && loading && <p className="text-muted text-center">불러오는 중...</p>}
        {selected && (currentPrice || latest) && (() => {
          const price = currentPrice?.stck_prpr ? Number(currentPrice.stck_prpr) : latest?.closePrice
          const isRealtime = !!currentPrice?.stck_prpr
          return (
            <>
              <p className="text-lg font-bold text-white">{selected.name}</p>
              <p className="text-sm text-muted mb-3">
                {selected.code} · {isRealtime ? '실시간' : `기준일: ${latest?.tradeDate}`}
                {polling && <span className="ml-2 text-green-400">● 10초 갱신 중</span>}
                {!polling && <span className="ml-2 text-muted">● 장 마감</span>}
              </p>
              <p className={`text-3xl font-bold ${priceColor(diff)}`}>{fmt(price)} <span className="text-base">원</span></p>
              <div className="flex gap-6 mt-3 text-sm text-muted">
                <span className={priceColor(diff)}>{signIcon(diff)} {fmt(Math.abs(diff))} ({rate}%)</span>
                {isRealtime ? (
                  <>
                    <span>고가 {fmt(currentPrice.stck_hgpr)}</span>
                    <span>저가 {fmt(currentPrice.stck_lwpr)}</span>
                    <span>거래량 {fmt(currentPrice.acml_vol)}</span>
                  </>
                ) : latest && (
                  <>
                    <span>고가 {fmt(latest.highPrice)}</span>
                    <span>저가 {fmt(latest.lowPrice)}</span>
                    <span>거래량 {fmt(latest.volume)}</span>
                  </>
                )}
              </div>
            </>
          )
        })()}
      </div>

      {/* 캔들차트 */}
      {chartPrices.length > 0 && (
        <div className="bg-card border border-border rounded-xl overflow-hidden mb-6">
          <div className="px-6 py-4 border-b border-border text-sm text-muted font-semibold">
            캔들차트 · {selected?.name} ({selected?.code})
          </div>
          <div ref={candleRef} />
          <div ref={volumeRef} className="border-t border-border" />
        </div>
      )}

      {/* 일봉 테이블 */}
      <div className="bg-card border border-border rounded-xl overflow-hidden">
        <div className="px-6 py-4 border-b border-border text-sm text-muted font-semibold flex justify-between items-center">
          <span>일봉 데이터 · {selected?.name || ''}</span>
          {pagination.totalElements > 0 && (
            <span className="text-xs text-muted">총 {pagination.totalElements.toLocaleString()}건</span>
          )}
        </div>
        {loading && <p className="text-muted text-center py-12">불러오는 중...</p>}
        {!loading && prices.length === 0 && <p className="text-muted text-center py-12">저장된 데이터가 없습니다.</p>}
        {prices.length > 0 && (
          <>
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-muted">
                  <th className="text-left px-4 py-3">날짜</th>
                  <th className="text-right px-4 py-3">시가</th>
                  <th className="text-right px-4 py-3">고가</th>
                  <th className="text-right px-4 py-3">저가</th>
                  <th className="text-right px-4 py-3">종가</th>
                  <th className="text-right px-4 py-3">거래량</th>
                </tr>
              </thead>
              <tbody>
                {prices.map((d) => (
                  <tr key={d.tradeDate} className="border-b border-surface hover:bg-surface transition-colors">
                    <td className="text-left px-4 py-2.5 text-muted">{d.tradeDate}</td>
                    <td className="text-right px-4 py-2.5">{fmt(d.openPrice)}</td>
                    <td className="text-right px-4 py-2.5 text-up">{fmt(d.highPrice)}</td>
                    <td className="text-right px-4 py-2.5 text-down">{fmt(d.lowPrice)}</td>
                    <td className={`text-right px-4 py-2.5 font-medium ${d.closePrice >= d.openPrice ? 'text-up' : 'text-down'}`}>{fmt(d.closePrice)}</td>
                    <td className="text-right px-4 py-2.5 text-muted">{fmt(d.volume)}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* 페이지네이션 버튼 */}
            {pagination.totalPages > 1 && (
              <div className="flex justify-center items-center gap-2 px-4 py-4 border-t border-border">
                <button
                  onClick={() => handlePageChange(pagination.page - 1)}
                  disabled={pagination.page === 0}
                  className="px-3 py-1.5 rounded text-sm bg-surface text-muted hover:text-white disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                >
                  이전
                </button>
                {Array.from({ length: pagination.totalPages }, (_, i) => i)
                  .filter(i => Math.abs(i - pagination.page) <= 2)
                  .map(i => (
                    <button
                      key={i}
                      onClick={() => handlePageChange(i)}
                      className={`px-3 py-1.5 rounded text-sm transition-colors ${
                        i === pagination.page
                          ? 'bg-primary text-white'
                          : 'bg-surface text-muted hover:text-white'
                      }`}
                    >
                      {i + 1}
                    </button>
                  ))}
                <button
                  onClick={() => handlePageChange(pagination.page + 1)}
                  disabled={pagination.page === pagination.totalPages - 1}
                  className="px-3 py-1.5 rounded text-sm bg-surface text-muted hover:text-white disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                >
                  다음
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
