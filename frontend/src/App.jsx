import { Routes, Route, Navigate } from 'react-router-dom'
import { api } from './api/client'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Clients from './pages/Clients'
import ClientForm from './pages/ClientForm'
import Users from './pages/Users'
import UserForm from './pages/UserForm'
import Tokens from './pages/Tokens'
import Inspector from './pages/Inspector'

function ProtectedRoute({ children }) {
  if (!api.isAuthenticated()) {
    return <Navigate to="/login" replace />
  }
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route index element={<Dashboard />} />
        <Route path="clients" element={<Clients />} />
        <Route path="clients/new" element={<ClientForm />} />
        <Route path="clients/:id/edit" element={<ClientForm />} />
        <Route path="users" element={<Users />} />
        <Route path="users/new" element={<UserForm />} />
        <Route path="users/:id/edit" element={<UserForm />} />
        <Route path="tokens" element={<Tokens />} />
        <Route path="inspector" element={<Inspector />} />
      </Route>
    </Routes>
  )
}
