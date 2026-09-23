import { useState, useEffect } from 'react'

const EMPTY_PRODUCT = { name: '', sku: '', price: '', category: '', quantity: '' }

function ProductsPage() {
  const [products, setProducts] = useState([])
  const [form, setForm] = useState(EMPTY_PRODUCT)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const fetchProducts = async () => {
    try {
      setError(null)
      const res = await fetch('/products')
      if (!res.ok) throw new Error(`Failed to fetch products (${res.status})`)
      const data = await res.json()
      setProducts(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchProducts()
  }, [])

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const body = {
        ...form,
        price: parseFloat(form.price),
        quantity: parseInt(form.quantity, 10),
      }
      const res = await fetch('/products', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      })
      if (!res.ok) throw new Error(`Failed to create product (${res.status})`)
      setForm(EMPTY_PRODUCT)
      await fetchProducts()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this product?')) return
    try {
      const res = await fetch(`/products/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error(`Failed to delete product (${res.status})`)
      await fetchProducts()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Products</h1>
        <span className="badge">{products.length} items</span>
      </div>

      {error && <div className="error">⚠ {error}</div>}

      {/* Add Product Form */}
      <div className="form-card">
        <h2>+ Add New Product</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-grid">
            <div className="form-group">
              <label>Name</label>
              <input name="name" value={form.name} onChange={handleChange} placeholder="Laptop Pro" required />
            </div>
            <div className="form-group">
              <label>SKU</label>
              <input name="sku" value={form.sku} onChange={handleChange} placeholder="LAP-002" required />
            </div>
            <div className="form-group">
              <label>Price</label>
              <input name="price" type="number" step="0.01" min="0" value={form.price} onChange={handleChange} placeholder="999.99" required />
            </div>
            <div className="form-group">
              <label>Category</label>
              <input name="category" value={form.category} onChange={handleChange} placeholder="Electronics" required />
            </div>
            <div className="form-group">
              <label>Quantity</label>
              <input name="quantity" type="number" min="0" value={form.quantity} onChange={handleChange} placeholder="100" required />
            </div>
            <div className="form-group">
              <label>&nbsp;</label>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Adding...' : 'Add Product'}
              </button>
            </div>
          </div>
        </form>
      </div>

      {/* Product List */}
      {loading ? (
        <div className="loading">Loading products...</div>
      ) : products.length === 0 ? (
        <div className="empty">No products found. Add one above!</div>
      ) : (
        <div className="card">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>SKU</th>
                <th>Category</th>
                <th className="text-right">Price</th>
                <th className="text-right">Qty</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {products.map((p) => (
                <tr key={p.id}>
                  <td style={{ color: '#999' }}>#{p.id}</td>
                  <td><strong>{p.name}</strong></td>
                  <td><code style={{ background: '#f0f0f5', padding: '2px 6px', borderRadius: '3px', fontSize: '12px' }}>{p.sku}</code></td>
                  <td>{p.category}</td>
                  <td className="text-right">${p.price?.toFixed(2)}</td>
                  <td className="text-right">{p.quantity}</td>
                  <td className="text-right">
                    <button className="btn btn-danger" onClick={() => handleDelete(p.id)}>Delete</button>
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

export default ProductsPage
