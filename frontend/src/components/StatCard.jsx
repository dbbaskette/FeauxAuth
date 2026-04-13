export default function StatCard({ label, value }) {
  return (
    <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
      <p className="text-sm font-medium text-gray-400">{label}</p>
      <p className="mt-2 text-3xl font-bold text-white">{value}</p>
    </div>
  )
}
