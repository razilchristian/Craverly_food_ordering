import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { AppProvider } from './context/AppContext'
import AdminLayout from './layouts/AdminLayout'
import CustomerLayout from './layouts/CustomerLayout'
import OwnerLayout from './layouts/OwnerLayout'
import AdminAnalyticsPage from './pages/admin/AdminAnalyticsPage'
import AdminDashboardPage from './pages/admin/AdminDashboardPage'
import AdminFoodItemsPage from './pages/admin/AdminFoodItemsPage'
import AdminOrdersPage from './pages/admin/AdminOrdersPage'
import AdminRestaurantsPage from './pages/admin/AdminRestaurantsPage'
import AdminUsersPage from './pages/admin/AdminUsersPage'
import CartPage from './pages/customer/CartPage'
import HomePage from './pages/customer/HomePage'
import OrderTrackingPage from './pages/customer/OrderTrackingPage'
import OrdersPage from './pages/customer/OrdersPage'
import OwnerDashboardPage from './pages/owner/OwnerDashboardPage'
import RestaurantDetailPage from './pages/customer/RestaurantDetailPage'
import RestaurantsPage from './pages/customer/RestaurantsPage'

export default function App() {
  return (
    <AppProvider>
      <BrowserRouter>
        <Routes>
          {/* Customer Portal */}
          <Route element={<CustomerLayout />}>
            <Route index element={<HomePage />} />
            <Route path="restaurants" element={<RestaurantsPage />} />
            <Route path="restaurants/:id" element={<RestaurantDetailPage />} />
            <Route path="cart" element={<CartPage />} />
            <Route path="orders" element={<OrdersPage />} />
            <Route path="orders/:orderId/tracking" element={<OrderTrackingPage />} />
          </Route>

          {/* Restaurant Owner / Partner Portal */}
          <Route path="owner" element={<OwnerLayout />}>
            <Route index element={<Navigate to="/owner/dashboard" replace />} />
            <Route path="dashboard" element={<OwnerDashboardPage />} />
          </Route>

          {/* Admin Management Portal */}
          <Route path="admin" element={<AdminLayout />}>
            <Route index element={<Navigate to="/admin/dashboard" replace />} />
            <Route path="dashboard" element={<AdminDashboardPage />} />
            <Route path="analytics" element={<AdminAnalyticsPage />} />
            <Route path="orders" element={<AdminOrdersPage />} />
            <Route path="restaurants" element={<AdminRestaurantsPage />} />
            <Route path="food-items" element={<AdminFoodItemsPage />} />
            <Route path="users" element={<AdminUsersPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AppProvider>
  )
}
