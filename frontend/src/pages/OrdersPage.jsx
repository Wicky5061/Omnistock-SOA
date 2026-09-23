import { useState, useEffect } from 'react'

const EMPTY_ORDER = { productId: '', productName: '', quantity: '' }

function OrdersPage() {
  const [orders, setOrders] = useState([])
  const [products, setProducts] = useState([])
  const [form, setForm] = useState(EMPTY_ORDER)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [successMsg, setSuccessMsg] = useState(null)

  const fetchOrders = async () => {
    try {
      setError(null)
      const res = await fetch('/orders')
      if (!res.ok) throw new Error(`Failed to fetch orders (${res.status})`)
      const data = await res.json()
      setOrders(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const fetchProducts = async () => {
    try {
      const res = await fetch('/products')
      if (res.ok) {
        const data = await res.json()
        setProducts(data)
      }
    } catch {
      // Products list is optional — for the dropdown
    }
  }

  useEffect(() => {
    fetchOrders()
    fetchProducts()
  }, [])

  const handleProductSelect = (e) => {
    const productId = e.target.value
    const product = products.find((p) => String(p.id) === productId)
    setForm({
      ...form,
      productId: productId,
      productName: product ? product.name : '',
    })
  }

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    setSuccessMsg(null)
    try {
      const body = {
        productId: parseInt(form.productId, 10),
        productName: form.productName,
        quantity: parseInt(form.quantity, 10),
      }
      const res = await fetch('/orders', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      })
      if (!res.ok) throw new Error(`Failed to place order (${res.status})`)
      const newOrder = await res.json()
      if (newOrder.status === 'CONFIRMED') {
        setSuccessMsg(`Order #${newOrder.id} confirmed!`)
      } else {
        setError(`Order #${newOrder.id}: ${newOrder.status}`)
      }
      setForm(EMPTY_ORDER)
      await fetchOrders()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  const confirmedCount = orders.filter((o) => o.status === 'CONFIRMED').length
  const rejectedCount = orders.filter((o) => o.status !== 'CONFIRMED').length

  return (
    <div>
      <div className="page-header">
        <h1>Orders</h1>
        <div style={{ display: 'flex', gap: '8px' }}>
          {confirmedCount > 0 && <span className="status status-confirmed">{confirmedCount} confirmed</span>}
          {rejectedCount > 0 && <span className="status status-rejected">{rejectedCount} rejected</span>}
          <span className="badge">{orders.length} total</span>
        </div>
      </div>

      {error && <div className="error">⚠ {error}</div>}
      {successMsg && (
        <div style={{ background: '#d4edda', color: '#155724', padding: '12px 16px', borderRadius: '8px', marginBottom: '16px', fontSize: '14px', border: '1px solid #c3e6cb' }}>
          ✓ {successMsg}
        </div>
      )}

      {/* Place Order Form */}
      <div className="form-card">
        <h2>+ Place New Order</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-grid">
            <div className="form-group">
              <label>Product</label>
              {products.length > 0 ? (
                <select name="productId" value={form.productId} onChange={handleProductSelect} required>
                  <option value="">Select a product...</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} (#{p.id})
                    </option>
                  ))}
                </select>
              ) : (
                <input name="productId" type="number" min="1" value={form.productId} onChange={handleChange} placeholder="Product ID" required />
              )}
            </div>
            <div className="form-group">
              <label>Product Name</label>
              <input name="productName" value={form.productName} onChange={handleChange} placeholder="Auto-filled from selection" required />
            </div>
            <div className="form-group">
              <label>Quantity</label>
              <input name="quantity" type="number" min="1" value={form.quantity} onChange={handleChange} placeholder="10" required />
            </div>
            <div className="form-group">
              <label>&nbsp;</label>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Placing...' : 'Place Order'}
              </button>
            </div>
          </div>
        </form>
      </div>

      {/* Orders List */}
      {loading ? (
        <div className="loading">Loading orders...</div>
      ) : orders.length === 0 ? (
        <div className="empty">No orders yet. Place one above!</div>
      ) : (
        <div className="card">
          <table>
            <thead>
              <tr>
                <th>Order ID</th>
                <th>Product</th>
                <th>Product ID</th>
                <th className="text-right">Qty</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {[...orders].reverse().map((o) => (
                <tr key={o.id}>
                  <td style={{ color: '#999' }}>#{o.id}</td>
                  <td><strong>{o.productName}</strong></td>
                  <td style={{ color: '#999' }}>#{o.productId}</td>
                  <td className="text-right">{o.quantity}</td>
                  <td>
                    {o.status === 'CONFIRMED' ? (
                      <span className="status status-confirmed">✓ Confirmed</span>
                    ) : (
                      <span className="status status-rejected">✗ {o.status}</span>
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

export default OrdersPage
