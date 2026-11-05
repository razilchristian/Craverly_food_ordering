package com.cravely.backend.config;

import com.cravely.backend.entity.*;
import com.cravely.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final FoodCategoryRepository categoryRepository;
    private final FoodItemRepository foodItemRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           RestaurantRepository restaurantRepository,
                           FoodCategoryRepository categoryRepository,
                           FoodItemRepository foodItemRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
        this.foodItemRepository = foodItemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAdminUser();
        seedAllCatalog();
    }

    @Transactional
    public void seedAdminUser() {
        if (userRepository.findByEmail("admin@cravely.com").isEmpty()) {
            User admin = new User(
                    "Cravely Administrator",
                    "admin@cravely.com",
                    "9876543200",
                    passwordEncoder.encode("Password123!"),
                    Role.ADMIN,
                    true
            );
            userRepository.save(admin);
        }
    }

    @Transactional
    public void seedAllCatalog() {
        seedPizzaTheory();
        seedBurgerDistrict();
        seedRoyalBiryani();
        seedUrbanTadka();
        seedSpiceRoute();
        seedGreenBowl();
        seedWokExpress();
        seedSweetTooth();
    }

    @Transactional
    public Restaurant seedPizzaTheory() {
        return seedOrUpdateRestaurant(
                "owner@pizzaparadise.com", "Marco Rossi", "9876543210",
                "Pizza Theory", "Hand-tossed artisan pizzas, pastas, and Italian delights", "Pizza, Italian",
                "123 Pizza Street, Italian Quarter", "Ahmedabad", "Gujarat", "380006", "9876543210", "contact@pizzatheory.com",
                "https://images.unsplash.com/photo-1513104890138-7c749659a591", LocalTime.of(11, 30), LocalTime.of(23, 59),
                List.of(
                        new CategorySeed("Recommended", "Chef recommended pizzas"),
                        new CategorySeed("Main Course", "Classic Italian mains"),
                        new CategorySeed("Starters", "Appetizers and sides")
                ),
                List.of(
                        new ItemSeed("Paneer Tikka Pizza", "Tandoori paneer, onions, capsicum and mozzarella on a hand-tossed crust.", "Recommended", new BigDecimal("279.00"), "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38", FoodType.VEG, 20),
                        new ItemSeed("Margherita Classica", "San Marzano tomato, fresh basil and fior di latte.", "Main Course", new BigDecimal("229.00"), "https://images.unsplash.com/photo-1574071318508-1cdbab80d002", FoodType.VEG, 15),
                        new ItemSeed("Chicken Pepperoni", "Loaded chicken pepperoni with oregano and chilli oil.", "Main Course", new BigDecimal("349.00"), "https://images.unsplash.com/photo-1628840042765-356cda07504e", FoodType.NON_VEG, 25)
                )
        );
    }

    @Transactional
    public Restaurant seedBurgerDistrict() {
        return seedOrUpdateRestaurant(
                "owner@burgerdistrict.com", "Dave Burger", "9876543203",
                "The Burger District", "Gourmet smashed burgers, loaded fries, and craft milkshakes", "Burgers, American",
                "55 CG Road, Navrangpura", "Ahmedabad", "Gujarat", "380009", "9876543203", "contact@burgerdistrict.com",
                "https://images.unsplash.com/photo-1550547660-d9450f859349", LocalTime.of(12, 0), LocalTime.of(23, 59),
                List.of(
                        new CategorySeed("Recommended", "Best-selling gourmet burgers"),
                        new CategorySeed("Main Course", "Signature burgers and subs"),
                        new CategorySeed("Starters", "Loaded sides and fries")
                ),
                List.of(
                        new ItemSeed("Classic Veg Burger", "Crispy veg patty, lettuce, tomato and house sauce in a toasted brioche bun.", "Recommended", new BigDecimal("149.00"), "https://images.unsplash.com/photo-1571091718767-18b5b1457add", FoodType.VEG, 15),
                        new ItemSeed("Smoky Chicken Burger", "Grilled chicken, smoked cheddar, caramelised onions and chipotle mayo.", "Main Course", new BigDecimal("219.00"), "https://images.unsplash.com/photo-1568901346375-23c9450c58cd", FoodType.NON_VEG, 20),
                        new ItemSeed("Peri Peri Fries", "Golden fries tossed in fiery peri peri seasoning.", "Starters", new BigDecimal("99.00"), "https://images.unsplash.com/photo-1573080496219-bb080dd4f877", FoodType.VEG, 10)
                )
        );
    }

    @Transactional
    public Restaurant seedRoyalBiryani() {
        return seedOrUpdateRestaurant(
                "owner@royalbiryani.com", "Nawab Ali", "9876543205",
                "Royal Biryani House", "Authentic dum biryanis, slow-cooked kebabs, and Mughlai curries", "Indian, Biryani",
                "77 Relief Road", "Ahmedabad", "Gujarat", "380001", "9876543205", "contact@royalbiryani.com",
                "https://images.unsplash.com/photo-1697155406055-2db32d47ca07", LocalTime.of(12, 0), LocalTime.of(23, 30),
                List.of(
                        new CategorySeed("Recommended", "Royal chef recommended biryani"),
                        new CategorySeed("Rice and Biryani", "Fragrant basmati biryanis"),
                        new CategorySeed("Starters", "Tandoori and fried starters"),
                        new CategorySeed("Breads", "Fresh tandoori breads")
                ),
                List.of(
                        new ItemSeed("Hyderabadi Chicken Biryani", "Slow-cooked dum biryani with saffron rice and tender chicken.", "Recommended", new BigDecimal("249.00"), "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8", FoodType.NON_VEG, 30),
                        new ItemSeed("Veg Dum Biryani", "Fragrant basmati layered with seasonal vegetables and fried onions.", "Rice and Biryani", new BigDecimal("199.00"), "https://images.unsplash.com/photo-1642821373181-696a54913e93", FoodType.VEG, 25),
                        new ItemSeed("Chicken 65", "Spicy, crispy fried chicken with curry leaves.", "Starters", new BigDecimal("229.00"), "https://images.unsplash.com/photo-1610057099443-fde8c4d50f91", FoodType.NON_VEG, 15),
                        new ItemSeed("Butter Naan", "Soft tandoor naan brushed with butter.", "Breads", new BigDecimal("49.00"), "https://images.unsplash.com/photo-1601050690597-df0568f70950", FoodType.VEG, 10)
                )
        );
    }

    @Transactional
    public Restaurant seedUrbanTadka() {
        return seedOrUpdateRestaurant(
                "owner@urbantadka.com", "Mehul Patel", "9876543201",
                "Urban Tadka", "Traditional Gujarati thalis and North Indian homestyle delicacies", "North Indian, Gujarati",
                "101 SG Highway, Bodakdev", "Ahmedabad", "Gujarat", "380054", "9876543201", "contact@urbantadka.com",
                "https://images.unsplash.com/photo-1585937421612-70a008356fbe", LocalTime.of(11, 0), LocalTime.of(23, 0),
                List.of(
                        new CategorySeed("Recommended", "Chef special Gujarati delicacies"),
                        new CategorySeed("Main Course", "Traditional Indian gravies"),
                        new CategorySeed("Starters", "Authentic Gujarati farsan"),
                        new CategorySeed("Beverages", "Refreshing local drinks")
                ),
                List.of(
                        new ItemSeed("Gujarati Mini Thali", "Dal, kadhi, shaak, rotli, rice, farsan and a sweet.", "Recommended", new BigDecimal("199.00"), "https://images.unsplash.com/photo-1546833999-b9f581a1996d", FoodType.VEG, 20),
                        new ItemSeed("Paneer Butter Masala", "Cottage cheese in a rich, buttery tomato gravy.", "Main Course", new BigDecimal("239.00"), "https://images.unsplash.com/photo-1631452180519-c014fe946bc7", FoodType.VEG, 20),
                        new ItemSeed("Khaman Dhokla", "Steamed gram-flour sponge with mustard tempering.", "Starters", new BigDecimal("89.00"), "https://images.unsplash.com/photo-1606491956689-2ea866880c84", FoodType.VEG, 10),
                        new ItemSeed("Masala Chaas", "Spiced buttermilk with roasted cumin and mint.", "Beverages", new BigDecimal("49.00"), "https://images.unsplash.com/photo-1626132647523-66f5bf380027", FoodType.VEG, 5)
                )
        );
    }

    @Transactional
    public Restaurant seedSpiceRoute() {
        return seedOrUpdateRestaurant(
                "owner@spiceroute.com", "Venkat Raman", "9876543202",
                "Spice Route Kitchen", "Crispy dosas, fluffy idlis, and authentic South Indian filter coffee", "South Indian",
                "42 Ashram Road", "Ahmedabad", "Gujarat", "380009", "9876543202", "contact@spiceroute.com",
                "https://images.unsplash.com/photo-1567188040759-fb8a883dc6d8", LocalTime.of(7, 0), LocalTime.of(22, 30),
                List.of(
                        new CategorySeed("Recommended", "South Indian breakfast favorites"),
                        new CategorySeed("Main Course", "Authentic idli and dosa specialties"),
                        new CategorySeed("Beverages", "Traditional hot drinks")
                ),
                List.of(
                        new ItemSeed("Masala Dosa", "Crisp rice crepe with spiced potato, sambar and chutneys.", "Recommended", new BigDecimal("129.00"), "https://images.unsplash.com/photo-1589301760014-d929f3979dbc", FoodType.VEG, 15),
                        new ItemSeed("Idli Sambar", "Three fluffy idlis with sambar and coconut chutney.", "Main Course", new BigDecimal("89.00"), "https://images.unsplash.com/photo-1630383249896-424e482df921", FoodType.VEG, 10),
                        new ItemSeed("Filter Coffee", "Strong South Indian decoction coffee with frothy milk.", "Beverages", new BigDecimal("59.00"), "https://images.unsplash.com/photo-1509042239860-f550ce710b93", FoodType.VEG, 5)
                )
        );
    }

    @Transactional
    public Restaurant seedGreenBowl() {
        return seedOrUpdateRestaurant(
                "owner@greenbowl.com", "Ananya Sharma", "9876543204",
                "Green Bowl Cafe", "Fresh superfood bowls, wholesome salads, and cold-pressed juices", "Healthy, Salads",
                "8 Sindhu Bhavan Marg", "Ahmedabad", "Gujarat", "380054", "9876543204", "contact@greenbowl.com",
                "https://images.unsplash.com/photo-1490645935967-10de6ba17061", LocalTime.of(8, 0), LocalTime.of(22, 0),
                List.of(
                        new CategorySeed("Recommended", "Chef crafted power bowls"),
                        new CategorySeed("Main Course", "Nutrient rich salads and entrees"),
                        new CategorySeed("Beverages", "Fresh smoothies and juices")
                ),
                List.of(
                        new ItemSeed("Quinoa Power Bowl", "Quinoa, roasted veggies, chickpeas and tahini dressing.", "Recommended", new BigDecimal("269.00"), "https://images.unsplash.com/photo-1512621776951-a57141f2eefd", FoodType.VEG, 15),
                        new ItemSeed("Grilled Chicken Salad", "Herb chicken, greens, avocado and citrus vinaigrette.", "Main Course", new BigDecimal("289.00"), "https://images.unsplash.com/photo-1546069901-ba9599a7e63c", FoodType.NON_VEG, 20),
                        new ItemSeed("Berry Smoothie", "Mixed berries, banana and yogurt.", "Beverages", new BigDecimal("149.00"), "https://images.unsplash.com/photo-1553530666-ba11a7da3888", FoodType.VEG, 8)
                )
        );
    }

    @Transactional
    public Restaurant seedWokExpress() {
        return seedOrUpdateRestaurant(
                "owner@wokexpress.com", "Chen Wei", "9876543206",
                "Wok Express", "Sizzling wok bowls, Hakka noodles, and zesty Indo-Chinese appetisers", "Chinese, Asian",
                "22 Vastrapur Lake Road", "Ahmedabad", "Gujarat", "380015", "9876543206", "contact@wokexpress.com",
                "https://images.unsplash.com/photo-1525755662778-989d0524087e", LocalTime.of(12, 0), LocalTime.of(23, 0),
                List.of(
                        new CategorySeed("Recommended", "Wok-tossed street style noodles"),
                        new CategorySeed("Starters", "Crispy Indo-Chinese appetisers")
                ),
                List.of(
                        new ItemSeed("Veg Hakka Noodles", "Wok-tossed noodles with crunchy vegetables.", "Recommended", new BigDecimal("179.00"), "https://images.unsplash.com/photo-1585032226651-759b368d7246", FoodType.VEG, 15),
                        new ItemSeed("Chilli Chicken", "Indo-Chinese chilli chicken with peppers and spring onion.", "Starters", new BigDecimal("239.00"), "https://images.unsplash.com/photo-1525755662778-989d0524087e", FoodType.NON_VEG, 20)
                )
        );
    }

    @Transactional
    public Restaurant seedSweetTooth() {
        return seedOrUpdateRestaurant(
                "owner@sweettooth.com", "Priya Desai", "9876543207",
                "Sweet Tooth Studio", "Artisanal desserts, gooey warm brownies, and gourmet cheesecakes", "Desserts, Beverages",
                "14 Law Garden Road", "Ahmedabad", "Gujarat", "380006", "9876543207", "contact@sweettooth.com",
                "https://images.unsplash.com/photo-1551024601-bec78aea704b", LocalTime.of(10, 0), LocalTime.of(23, 59),
                List.of(
                        new CategorySeed("Recommended", "Warm artisanal bakes"),
                        new CategorySeed("Desserts", "Signature chilled cheesecakes")
                ),
                List.of(
                        new ItemSeed("Chocolate Brownie", "Fudgy dark-chocolate brownie, served warm.", "Recommended", new BigDecimal("99.00"), "https://images.unsplash.com/photo-1606313564200-e75d5e30476c", FoodType.VEG, 10),
                        new ItemSeed("Mango Cheesecake", "Creamy baked cheesecake with Alphonso mango.", "Desserts", new BigDecimal("169.00"), "https://images.unsplash.com/photo-1533134242443-d4fd215305ad", FoodType.VEG, 10)
                )
        );
    }

    @Transactional
    public Restaurant seedOrUpdateRestaurant(
            String ownerEmail, String ownerName, String ownerPhone,
            String restaurantName, String description, String cuisineType,
            String address, String city, String state, String pincode, String phone, String email,
            String imageUrl, LocalTime openTime, LocalTime closeTime,
            List<CategorySeed> categories, List<ItemSeed> items) {

        // 1. Owner
        User owner = userRepository.findByEmail(ownerEmail).orElseGet(() -> {
            User u = new User(
                    ownerName,
                    ownerEmail,
                    ownerPhone,
                    passwordEncoder.encode("Password123!"),
                    Role.RESTAURANT_OWNER,
                    true
            );
            return userRepository.save(u);
        });

        // 2. Restaurant
        Restaurant restaurant = restaurantRepository.findFirstByNameIgnoreCase(restaurantName).orElseGet(() -> {
            Restaurant r = new Restaurant();
            r.setName(restaurantName);
            r.setDescription(description);
            r.setOwner(owner);
            r.setAddress(address);
            r.setCity(city);
            r.setState(state);
            r.setPincode(pincode);
            r.setPhone(phone);
            r.setEmail(email);
            r.setCuisineType(cuisineType);
            r.setImageUrl(imageUrl);
            r.setOpeningTime(openTime);
            r.setClosingTime(closeTime);
            r.setStatus(RestaurantStatus.APPROVED);
            r.setActive(true);
            return restaurantRepository.save(r);
        });

        boolean restaurantModified = false;
        if (restaurant.getStatus() != RestaurantStatus.APPROVED) {
            restaurant.setStatus(RestaurantStatus.APPROVED);
            restaurantModified = true;
        }
        if (!Boolean.TRUE.equals(restaurant.getActive())) {
            restaurant.setActive(true);
            restaurantModified = true;
        }
        if (restaurantModified) {
            restaurant = restaurantRepository.save(restaurant);
        }

        // 3. Categories
        Map<String, FoodCategory> catMap = new HashMap<>();
        for (CategorySeed cs : categories) {
            final Restaurant targetRestaurant = restaurant;
            FoodCategory cat = categoryRepository.findFirstByRestaurantIdAndNameIgnoreCase(targetRestaurant.getId(), cs.name())
                    .orElseGet(() -> categoryRepository.save(new FoodCategory(cs.name(), cs.description(), targetRestaurant)));
            if (!Boolean.TRUE.equals(cat.getActive())) {
                cat.setActive(true);
                cat = categoryRepository.save(cat);
            }
            catMap.put(cs.name().toLowerCase(), cat);
        }

        // 4. Food Items
        for (ItemSeed is : items) {
            final Restaurant targetRestaurant = restaurant;
            FoodCategory cat = catMap.get(is.categoryName().toLowerCase());
            if (cat == null) {
                cat = categoryRepository.findFirstByRestaurantIdAndNameIgnoreCase(targetRestaurant.getId(), is.categoryName())
                        .orElseGet(() -> categoryRepository.save(new FoodCategory(is.categoryName(), is.categoryName(), targetRestaurant)));
                catMap.put(is.categoryName().toLowerCase(), cat);
            }

            Optional<FoodItem> existingOpt = foodItemRepository.findFirstByRestaurantIdAndNameIgnoreCase(targetRestaurant.getId(), is.name());
            if (existingOpt.isEmpty()) {
                existingOpt = foodItemRepository.findFirstByNameIgnoreCase(is.name());
            }

            if (existingOpt.isEmpty()) {
                FoodItem fi = new FoodItem(
                        is.name(),
                        is.description(),
                        targetRestaurant,
                        cat,
                        is.price(),
                        is.imageUrl(),
                        is.foodType(),
                        true,
                        is.prepTime()
                );
                foodItemRepository.save(fi);
            } else {
                FoodItem fi = existingOpt.get();
                boolean fiModified = false;
                if (!Boolean.TRUE.equals(fi.getAvailable())) {
                    fi.setAvailable(true);
                    fiModified = true;
                }
                if (fi.getRestaurant() == null || !fi.getRestaurant().getId().equals(targetRestaurant.getId())) {
                    fi.setRestaurant(targetRestaurant);
                    fiModified = true;
                }
                if (fi.getCategory() == null) {
                    fi.setCategory(cat);
                    fiModified = true;
                }
                if (fiModified) {
                    foodItemRepository.save(fi);
                }
            }
        }

        return restaurant;
    }

    public record CategorySeed(String name, String description) {}
    public record ItemSeed(String name, String description, String categoryName, BigDecimal price, String imageUrl, FoodType foodType, int prepTime) {}
}
