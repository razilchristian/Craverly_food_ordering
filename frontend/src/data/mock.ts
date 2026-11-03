import type { Category, Coupon, FoodItem, Restaurant } from '../types'

const u = (id: string, w = 800) => `https://images.unsplash.com/${id}?auto=format&fit=crop&w=${w}&q=75`

export const categories: Category[] = [
  { id: 'pizza', name: 'Pizza', image: u('photo-1565299624946-b28f40a0ae38', 300) },
  { id: 'burgers', name: 'Burgers', image: u('photo-1568901346375-23c9450c58cd', 300) },
  { id: 'biryani', name: 'Biryani', image: u('photo-1563379091339-03b21ab4a4f8', 300) },
  { id: 'gujarati', name: 'Gujarati', image: u('photo-1546833999-b9f581a1996d', 300) },
  { id: 'south-indian', name: 'South Indian', image: u('photo-1589301760014-d929f3979dbc', 300) },
  { id: 'chinese', name: 'Chinese', image: u('photo-1585032226651-759b368d7246', 300) },
  { id: 'desserts', name: 'Desserts', image: u('photo-1606313564200-e75d5e30476c', 300) },
  { id: 'healthy', name: 'Healthy Food', image: u('photo-1512621776951-a57141f2eefd', 300) },
]

export const restaurants: Restaurant[] = [
  { id: 'urban-tadka', name: 'Urban Tadka', image: u('photo-1585937421612-70a008356fbe'), rating: 4.5, reviews: 1240, cuisines: ['North Indian', 'Gujarati'], deliveryMins: 30, deliveryFee: 35, priceForTwo: 450, veg: true, offer: '20% OFF', category: 'gujarati', minOrder: 149, hours: '11:00 AM – 11:00 PM' },
  { id: 'spice-route', name: 'Spice Route Kitchen', image: u('photo-1567188040759-fb8a883dc6d8'), rating: 4.3, reviews: 860, cuisines: ['South Indian'], deliveryMins: 25, deliveryFee: 25, priceForTwo: 300, veg: true, category: 'south-indian', minOrder: 99, hours: '7:00 AM – 10:30 PM' },
  { id: 'burger-district', name: 'The Burger District', image: u('photo-1550547660-d9450f859349'), rating: 4.4, reviews: 2104, cuisines: ['Burgers', 'American'], deliveryMins: 22, deliveryFee: 30, priceForTwo: 400, veg: false, offer: 'Free delivery', category: 'burgers', minOrder: 149, hours: '12:00 PM – 1:00 AM' },
  { id: 'pizza-theory', name: 'Pizza Theory', image: u('photo-1513104890138-7c749659a591'), rating: 4.6, reviews: 1789, cuisines: ['Pizza', 'Italian'], deliveryMins: 35, deliveryFee: 40, priceForTwo: 600, veg: false, offer: 'Combo deals', category: 'pizza', minOrder: 199, hours: '11:30 AM – 12:00 AM' },
  { id: 'green-bowl', name: 'Green Bowl Cafe', image: u('photo-1490645935967-10de6ba17061'), rating: 4.2, reviews: 512, cuisines: ['Healthy', 'Salads'], deliveryMins: 20, deliveryFee: 20, priceForTwo: 500, veg: true, category: 'healthy', minOrder: 149, hours: '8:00 AM – 10:00 PM' },
  { id: 'royal-biryani', name: 'Royal Biryani House', image: u('photo-1697155406055-2db32d47ca07'), rating: 4.7, reviews: 3320, cuisines: ['Indian', 'Biryani'], deliveryMins: 40, deliveryFee: 45, priceForTwo: 550, veg: false, offer: '10% OFF', category: 'biryani', minOrder: 199, hours: '12:00 PM – 11:30 PM' },
  { id: 'wok-express', name: 'Wok Express', image: u('photo-1525755662778-989d0524087e'), rating: 4.1, reviews: 640, cuisines: ['Chinese', 'Asian'], deliveryMins: 28, deliveryFee: 30, priceForTwo: 420, veg: false, category: 'chinese', minOrder: 149, hours: '12:00 PM – 11:00 PM' },
  { id: 'sweet-tooth', name: 'Sweet Tooth Studio', image: u('photo-1551024601-bec78aea704b'), rating: 4.8, reviews: 980, cuisines: ['Desserts', 'Beverages'], deliveryMins: 18, deliveryFee: 20, priceForTwo: 350, veg: true, offer: 'Buy 1 Get 1', category: 'desserts', minOrder: 99, hours: '10:00 AM – 12:00 AM' },
]

const f = (id: string, restaurantId: string, name: string, price: number, img: string, veg: boolean, section: FoodItem['section'], description: string, bestseller = false, rating = 4.4): FoodItem =>
  ({ id, restaurantId, name, price, image: u(img, 500), veg, section, description, bestseller, rating, available: true })

export const foodItems: FoodItem[] = [
  f('f1', 'burger-district', 'Classic Veg Burger', 149, 'photo-1571091718767-18b5b1457add', true, 'Recommended', 'Crispy veg patty, lettuce, tomato and house sauce in a toasted brioche bun.', true),
  f('f2', 'burger-district', 'Smoky Chicken Burger', 219, 'photo-1568901346375-23c9450c58cd', false, 'Main Course', 'Grilled chicken, smoked cheddar, caramelised onions and chipotle mayo.'),
  f('f3', 'burger-district', 'Peri Peri Fries', 99, 'photo-1573080496219-bb080dd4f877', true, 'Starters', 'Golden fries tossed in fiery peri peri seasoning.'),
  f('f4', 'pizza-theory', 'Paneer Tikka Pizza', 279, 'photo-1565299624946-b28f40a0ae38', true, 'Recommended', 'Tandoori paneer, onions, capsicum and mozzarella on a hand-tossed crust.', true, 4.6),
  f('f5', 'pizza-theory', 'Margherita Classica', 229, 'photo-1574071318508-1cdbab80d002', true, 'Main Course', 'San Marzano tomato, fresh basil and fior di latte.'),
  f('f6', 'pizza-theory', 'Chicken Pepperoni', 349, 'photo-1628840042765-356cda07504e', false, 'Main Course', 'Loaded chicken pepperoni with oregano and chilli oil.'),
  f('f7', 'royal-biryani', 'Hyderabadi Chicken Biryani', 249, 'photo-1563379091339-03b21ab4a4f8', false, 'Recommended', 'Slow-cooked dum biryani with saffron rice and tender chicken.', true, 4.8),
  f('f8', 'royal-biryani', 'Veg Dum Biryani', 199, 'photo-1642821373181-696a54913e93', true, 'Rice and Biryani', 'Fragrant basmati layered with seasonal vegetables and fried onions.'),
  f('f9', 'royal-biryani', 'Chicken 65', 229, 'photo-1610057099443-fde8c4d50f91', false, 'Starters', 'Spicy, crispy fried chicken with curry leaves.'),
  f('f10', 'royal-biryani', 'Butter Naan', 49, 'photo-1601050690597-df0568f70950', true, 'Breads', 'Soft tandoor naan brushed with butter.'),
  f('f11', 'urban-tadka', 'Gujarati Mini Thali', 199, 'photo-1546833999-b9f581a1996d', true, 'Recommended', 'Dal, kadhi, shaak, rotli, rice, farsan and a sweet.', true, 4.6),
  f('f12', 'urban-tadka', 'Paneer Butter Masala', 239, 'photo-1631452180519-c014fe946bc7', true, 'Main Course', 'Cottage cheese in a rich, buttery tomato gravy.'),
  f('f13', 'urban-tadka', 'Khaman Dhokla', 89, 'photo-1606491956689-2ea866880c84', true, 'Starters', 'Steamed gram-flour sponge with mustard tempering.'),
  f('f14', 'urban-tadka', 'Masala Chaas', 49, 'photo-1626132647523-66f5bf380027', true, 'Beverages', 'Spiced buttermilk with roasted cumin and mint.'),
  f('f15', 'spice-route', 'Masala Dosa', 129, 'photo-1589301760014-d929f3979dbc', true, 'Recommended', 'Crisp rice crepe with spiced potato, sambar and chutneys.', true, 4.5),
  f('f16', 'spice-route', 'Idli Sambar', 89, 'photo-1630383249896-424e482df921', true, 'Main Course', 'Three fluffy idlis with sambar and coconut chutney.'),
  f('f17', 'spice-route', 'Filter Coffee', 59, 'photo-1509042239860-f550ce710b93', true, 'Beverages', 'Strong South Indian decoction coffee with frothy milk.'),
  f('f18', 'green-bowl', 'Quinoa Power Bowl', 269, 'photo-1512621776951-a57141f2eefd', true, 'Recommended', 'Quinoa, roasted veggies, chickpeas and tahini dressing.', true),
  f('f19', 'green-bowl', 'Grilled Chicken Salad', 289, 'photo-1546069901-ba9599a7e63c', false, 'Main Course', 'Herb chicken, greens, avocado and citrus vinaigrette.'),
  f('f20', 'green-bowl', 'Berry Smoothie', 149, 'photo-1553530666-ba11a7da3888', true, 'Beverages', 'Mixed berries, banana and yogurt.'),
  f('f21', 'wok-express', 'Veg Hakka Noodles', 179, 'photo-1585032226651-759b368d7246', true, 'Recommended', 'Wok-tossed noodles with crunchy vegetables.', true),
  f('f22', 'wok-express', 'Chilli Chicken', 239, 'photo-1525755662778-989d0524087e', false, 'Starters', 'Indo-Chinese chilli chicken with peppers and spring onion.'),
  f('f23', 'sweet-tooth', 'Chocolate Brownie', 99, 'photo-1606313564200-e75d5e30476c', true, 'Recommended', 'Fudgy dark-chocolate brownie, served warm.', true, 4.8),
  f('f24', 'sweet-tooth', 'Mango Cheesecake', 169, 'photo-1533134242443-d4fd215305ad', true, 'Desserts', 'Creamy baked cheesecake with Alphonso mango.'),
]

export const popularDishIds = ['f1', 'f4', 'f7', 'f11', 'f15', 'f23']

export const coupons: Coupon[] = [
  { code: 'WELCOME10', description: '10% off your first order', percent: 10 },
  { code: 'FREEDEL', description: 'Free delivery on selected orders', freeDelivery: true },
  { code: 'COMBO15', description: '15% off combo meals', percent: 15 },
  { code: 'WEEKEND20', description: '20% off on weekends', percent: 20 },
  { code: 'SWEET5', description: '5% off desserts', percent: 5 },
]
