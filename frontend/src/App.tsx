import { BrowserRouter, Route, Routes } from 'react-router'
import { AppProvider } from './context/AppContext'
import CustomerLayout from './layouts/CustomerLayout'
import CartPage from './pages/customer/CartPage'
import HomePage from './pages/customer/HomePage'
import OrderTrackingPage from './pages/customer/OrderTrackingPage'
import OrdersPage from './pages/customer/OrdersPage'
import RestaurantDetailPage from './pages/customer/RestaurantDetailPage'
import RestaurantsPage from './pages/customer/RestaurantsPage'

export default function App() {
  return (
    <AppProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<CustomerLayout />}>
            <Route index element={<HomePage />} />
            <Route path="restaurants" element={<RestaurantsPage />} />
            <Route path="restaurants/:id" element={<RestaurantDetailPage />} />
            <Route path="cart" element={<CartPage />} />
            <Route path="orders" element={<OrdersPage />} />
            <Route path="orders/:orderId/tracking" element={<OrderTrackingPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AppProvider>
  )
}
