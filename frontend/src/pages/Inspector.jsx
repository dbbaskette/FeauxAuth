import { useState } from 'react'
import { api } from '../api/client'

export default function Inspector() {
  const [token, setToken] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')

  const handleInspect = async (e) => {
    e.preventDefault()
    setError('')
    setResult(null)

    try {
      const data = await api.post('/api/admin/inspector', { token })
      if (data.error) {
        setError(data.error)
      } else {
        setResult(data)
      }
    } catch (err) {
      setError('Failed to inspect token')
    }
  }

  return (
    <div className="max-w-4xl">
      <h1 className="text-2xl font-bold text-white mb-6">Token Inspector</h1>

      <form onSubmit={handleInspect} className="mb-8">
        <textarea
          value={token}
          onChange={e => setToken(e.target.value)}
          rows={4}
          placeholder="Paste a JWT here..."
          className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white font-mono text-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
        <button type="submit"
                className="mt-3 px-6 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg transition-colors">
          Inspect
        </button>
      </form>

      {error && (
        <div className="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg mb-6">{error}</div>
      )}

      {result && (
        <div className="space-y-6">
          <div>
            <h2 className="text-lg font-semibold text-white mb-2">Header</h2>
            <pre className="bg-gray-800 border border-gray-700 rounded-lg p-4 text-sm text-green-400 overflow-x-auto">
              {JSON.stringify(result.header, null, 2)}
            </pre>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-white mb-2">Payload</h2>
            <pre className="bg-gray-800 border border-gray-700 rounded-lg p-4 text-sm text-blue-400 overflow-x-auto">
              {JSON.stringify(result.payload, null, 2)}
            </pre>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div className={`rounded-lg p-4 border ${result.signatureValid ? 'bg-green-900/30 border-green-700' : 'bg-red-900/30 border-red-700'}`}>
              <p className="text-sm text-gray-400">Signature</p>
              <p className={`text-lg font-bold ${result.signatureValid ? 'text-green-400' : 'text-red-400'}`}>
                {result.signatureValid ? 'Valid' : 'Invalid'}
              </p>
            </div>
            <div className={`rounded-lg p-4 border ${result.expired ? 'bg-red-900/30 border-red-700' : 'bg-green-900/30 border-green-700'}`}>
              <p className="text-sm text-gray-400">Expiry</p>
              <p className={`text-lg font-bold ${result.expired ? 'text-red-400' : 'text-green-400'}`}>
                {result.expired ? 'Expired' : 'Valid'}
              </p>
            </div>
            <div className={`rounded-lg p-4 border ${
              result.revocationStatus === 'revoked' ? 'bg-red-900/30 border-red-700' :
              result.revocationStatus === 'active' ? 'bg-green-900/30 border-green-700' :
              'bg-gray-800 border-gray-700'
            }`}>
              <p className="text-sm text-gray-400">Revocation</p>
              <p className={`text-lg font-bold ${
                result.revocationStatus === 'revoked' ? 'text-red-400' :
                result.revocationStatus === 'active' ? 'text-green-400' :
                'text-gray-400'
              }`}>
                {result.revocationStatus === 'revoked' ? 'Revoked' :
                 result.revocationStatus === 'active' ? 'Active' : 'Unknown'}
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
