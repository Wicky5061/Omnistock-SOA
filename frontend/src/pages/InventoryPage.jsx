import { useState, useEffect } from 'react'

function InventoryPage() {
  const [inventory, setInventory] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [filter, setFilter] = useState('all') // 'all' | 'low'

  useEffect(() => {
    const fetchInventory = async () => {
      try {
        setError(null)
        const res = await fetch('/inventory')
        if (!res.ok) throw new Error(`Failed to fetch inventory (${res.status})`)
        const data = await res.json()
        setInventory(data)
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }
    fetchInventory()
  }, [])

  const isLowStock = (item) => {
    return item.stockQuantity != null && item.lowStockThreshold != null && item.stockQuantity <= item.lowStockThreshold
  }

  const filtered = filter === 'low' ? inventory.filter(isLowStock) : inventory
  const lowCount = inventory.filter(isLowStock).length

  const getStockColor = (item) => {
    if (isLowStock(item)) return '#e94560'
    if (item.stockQuantity <= item.lowStockThreshold * 2) return '#f0a500'
    return '#27ae60'
  }

  const getStockPercent = (item) => {
    const max = Math.max(item.stockQuantity, item.lowStockThreshold * 5, 100)
    return Math.min((item.stockQuantity / max) * 100, 100)
  }

  return (
    <div>
      <div className="page-header">
        <h1>Inventory</h1>
        <div style={{ display: 'flex', gap: '8px' }}>
          {lowCount > 0 && (
            <span className="status status-low">⚠ {lowCount} low stock</span>
          )}
          <span className="badge">{inventory.length} items</span>
        </div>
      </div>

      {error && <div className="error">⚠ {error}</div>}

      {/* Filter Tabs */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
        <button
          className="btn"
          style={{ background: filter === 'all' ? '#1a1a2e' : '#e0e0e8', color: filter === 'all' ? '#fff' : '#555' }}
          onClick={() => setFilter('all')}
        >
          All ({inventory.length})
        </button>
        <button
          className="btn"
          style={{ background: filter === 'low' ? '#e94560' : '#e0e0e8', color: filter === 'low' ? '#fff' : '#555' }}
          onClick={() => setFilter('low')}
        >
          ⚠ Low Stock ({lowCount})
        </button>
      </div>

      {loading ? (
        <div className="loading">Loading inventory...</div>
      ) : filtered.length === 0 ? (
        <div className="empty">
          {filter === 'low' ? 'No low-stock items — everything looks good!' : 'No inventory records found.'}
        </div>
      ) : (
        <div className="card">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Product</th>
                <th className="text-right">Stock</th>
                <th className="text-right">Threshold</th>
                <th style={{ width: '160px' }}>Level</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((item) => (
                <tr key={item.id}>
                  <td style={{ color: '#999' }}>#{item.id}</td>
                  <td><strong>{item.productName}</strong></td>
                  <td className="text-right" style={{ fontWeight: 600, color: getStockColor(item) }}>
                    {item.stockQuantity}
                  </td>
                  <td className="text-right" style={{ color: '#999' }}>{item.lowStockThreshold}</td>
                  <td>
                    <div className="stock-bar">
                      <div
                        className="stock-bar-fill"
                        style={{
                          width: `${getStockPercent(item)}%`,
                          background: getStockColor(item),
                        }}
                      />
                    </div>
                  </td>
                  <td>
                    {isLowStock(item) ? (
                      <span className="status status-low">⚠ Low Stock</span>
                    ) : (
                      <span className="status status-ok">✓ OK</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

export default InventoryPage
