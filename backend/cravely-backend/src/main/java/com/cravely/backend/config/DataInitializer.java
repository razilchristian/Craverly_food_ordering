package com.cravely.backend.config;

import com.cravely.backend.entity.*;
import com.cravely.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;

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
        seedPizzaTheory();
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
    public Restaurant seedPizzaTheory() {
        return restaurantRepository.findFirstByNameIgnoreCase("Pizza Theory").orElseGet(() -> {
            // 1. Owner
            User owner = userRepository.findByEmail("owner@pizzaparadise.com").orElseGet(() -> {
                User u = new User(
                        "Marco Rossi",
                        "owner@pizzaparadise.com",
                        "9876543210",
                        passwordEncoder.encode("Password123!"),
                        Role.RESTAURANT_OWNER,
                        true
                );
                return userRepository.save(u);
            });

            // 2. Restaurant
            Restaurant restaurant = new Restaurant();
            restaurant.setName("Pizza Theory");
            restaurant.setDescription("Hand-tossed artisan pizzas, pastas, and Italian delights");
            restaurant.setOwner(owner);
            restaurant.setAddress("123 Pizza Street, Italian Quarter");
            restaurant.setCity("Ahmedabad");
            restaurant.setState("Gujarat");
            restaurant.setPincode("380006");
            restaurant.setPhone("9876543210");
            restaurant.setEmail("contact@pizzatheory.com");
            restaurant.setCuisineType("Pizza, Italian");
            restaurant.setImageUrl("https://images.unsplash.com/photo-1513104890138-7c749659a591");
            restaurant.setOpeningTime(LocalTime.of(11, 30));
            restaurant.setClosingTime(LocalTime.of(23, 59));
            restaurant.setStatus(RestaurantStatus.APPROVED);
            restaurant.setActive(true);
            Restaurant savedRestaurant = restaurantRepository.save(restaurant);

            // 3. Categories
            FoodCategory recommendedCat = categoryRepository.save(new FoodCategory("Recommended", "Chef recommended pizzas", savedRestaurant));
            FoodCategory mainCat = categoryRepository.save(new FoodCategory("Main Course", "Classic Italian mains", savedRestaurant));
            FoodCategory startersCat = categoryRepository.save(new FoodCategory("Starters", "Appetizers and sides", savedRestaurant));

            // 4. Food Items
            FoodItem paneerPizza = new FoodItem(
                    "Paneer Tikka Pizza",
                    "Tandoori paneer, onions, capsicum and mozzarella on a hand-tossed crust.",
                    savedRestaurant,
                    recommendedCat,
                    new BigDecimal("279.00"),
                    "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38",
                    FoodType.VEG,
                    true,
                    20
            );

            FoodItem margheritaPizza = new FoodItem(
                    "Margherita Classica",
                    "San Marzano tomato, fresh basil and fior di latte.",
                    savedRestaurant,
                    mainCat,
                    new BigDecimal("229.00"),
                    "https://images.unsplash.com/photo-1574071318508-1cdbab80d002",
                    FoodType.VEG,
                    true,
                    15
            );

            FoodItem pepperoniPizza = new FoodItem(
                    "Chicken Pepperoni",
                    "Loaded chicken pepperoni with oregano and chilli oil.",
                    savedRestaurant,
                    mainCat,
                    new BigDecimal("349.00"),
                    "https://images.unsplash.com/photo-1628840042765-356cda07504e",
                    FoodType.NON_VEG,
                    true,
                    25
            );

            foodItemRepository.save(paneerPizza);
            foodItemRepository.save(margheritaPizza);
            foodItemRepository.save(pepperoniPizza);

            return savedRestaurant;
        });
    }
}

