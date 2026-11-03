package com.cravely.backend;

import com.cravely.backend.dto.*;
import com.cravely.backend.entity.FoodType;
import com.cravely.backend.entity.Role;
import com.cravely.backend.service.AuthService;
import com.cravely.backend.service.FoodCategoryService;
import com.cravely.backend.service.FoodItemService;
import com.cravely.backend.service.MenuService;
import com.cravely.backend.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MenuIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private FoodCategoryService categoryService;

    @Autowired
    private FoodItemService foodItemService;

    @Autowired
    private MenuService menuService;

    private String owner1Email;
    private String owner2Email;
    private Long approvedRestaurantId;
    private Long pendingRestaurantId;

    @BeforeEach
    void setup() {
        long ts = System.currentTimeMillis();
        owner1Email = "menu_owner1_" + ts + "@example.com";
        owner2Email = "menu_owner2_" + ts + "@example.com";

        authService.register(new RegisterRequest("Menu Owner 1", owner1Email, "1112223334", "Pass123!", Role.RESTAURANT_OWNER));
        authService.register(new RegisterRequest("Menu Owner 2", owner2Email, "1112223335", "Pass123!", Role.RESTAURANT_OWNER));

        // Create approved restaurant for owner1
        RestaurantCreateRequest r1 = new RestaurantCreateRequest(
                "Royal Dine", "Fine Dining", "Address 1", "Mumbai", "Maharashtra", "400001",
                "9876543210", "royal@dine.com", "Indian", "https://img.com/r1.jpg",
                LocalTime.of(10, 0), LocalTime.of(23, 0)
        );
        var createdR1 = restaurantService.createRestaurant(owner1Email, r1);
        restaurantService.approveRestaurant(createdR1.getId());
        approvedRestaurantId = createdR1.getId();

        // Create pending restaurant for owner1
        RestaurantCreateRequest r2 = new RestaurantCreateRequest(
                "Pending Cafe", "Cafe", "Address 2", "Mumbai", "Maharashtra", "400002",
                "9876543211", "pending@cafe.com", "Cafe", "https://img.com/r2.jpg",
                LocalTime.of(9, 0), LocalTime.of(21, 0)
        );
        var createdR2 = restaurantService.createRestaurant(owner1Email, r2);
        pendingRestaurantId = createdR2.getId();
    }

    @Test
    void testCreateCategoryAndDuplicateRejection() {
        FoodCategoryCreateRequest request = new FoodCategoryCreateRequest("Starters", "Appetizing starters");
        FoodCategoryResponse created = categoryService.createCategory(owner1Email, approvedRestaurantId, request);

        assertNotNull(created.getId());
        assertEquals("Starters", created.getName());
        assertEquals(approvedRestaurantId, created.getRestaurantId());

        // Duplicate name rejection in same restaurant
        assertThrows(IllegalArgumentException.class, () ->
                categoryService.createCategory(owner1Email, approvedRestaurantId, new FoodCategoryCreateRequest("Starters", "Duplicate"))
        );
    }

    @Test
    void testOwnerCannotAddMenuToUnapprovedRestaurant() {
        FoodCategoryCreateRequest catReq = new FoodCategoryCreateRequest("Beverages", "Drinks");
        assertThrows(IllegalStateException.class, () ->
                categoryService.createCategory(owner1Email, pendingRestaurantId, catReq)
        );
    }

    @Test
    void testCreateVegAndNonVegFoodItems() {
        var cat = categoryService.createCategory(owner1Email, approvedRestaurantId, new FoodCategoryCreateRequest("Mains", "Main Course"));

        FoodItemCreateRequest vegItemReq = new FoodItemCreateRequest(
                "Paneer Butter Masala", "Cottage cheese in rich tomato gravy",
                cat.getId(), new BigDecimal("250.00"), "https://img.com/paneer.jpg",
                FoodType.VEG, true, 20
        );
        FoodItemResponse vegItem = foodItemService.createFoodItem(owner1Email, approvedRestaurantId, vegItemReq);
        assertNotNull(vegItem.getId());
        assertEquals(FoodType.VEG, vegItem.getFoodType());
        assertEquals(new BigDecimal("250.00"), vegItem.getPrice());

        FoodItemCreateRequest nonVegItemReq = new FoodItemCreateRequest(
                "Butter Chicken", "Chicken in rich tomato butter gravy",
                cat.getId(), new BigDecimal("320.00"), "https://img.com/chicken.jpg",
                FoodType.NON_VEG, true, 25
        );
        FoodItemResponse nonVegItem = foodItemService.createFoodItem(owner1Email, approvedRestaurantId, nonVegItemReq);
        assertNotNull(nonVegItem.getId());
        assertEquals(FoodType.NON_VEG, nonVegItem.getFoodType());
    }

    @Test
    void testFoodItemCategoryBelongingToAnotherRestaurantRejected() {
        // Owner 2 creates restaurant & category
        var r2 = restaurantService.createRestaurant(owner2Email, new RestaurantCreateRequest(
                "Owner 2 Cafe", "Cafe", "Addr", "Pune", "State", "411001",
                "9998887776", "o2@cafe.com", "Cafe", null, LocalTime.of(8, 0), LocalTime.of(20, 0)
        ));
        restaurantService.approveRestaurant(r2.getId());
        var o2Cat = categoryService.createCategory(owner2Email, r2.getId(), new FoodCategoryCreateRequest("O2 Desserts", "Desserts"));

        // Owner 1 tries to create food item in approvedRestaurantId using Owner 2's category
        FoodItemCreateRequest rogueReq = new FoodItemCreateRequest(
                "Gulab Jamun", "Sweet", o2Cat.getId(), new BigDecimal("100.00"), null, FoodType.VEG, true, 10
        );

        assertThrows(IllegalArgumentException.class, () ->
                foodItemService.createFoodItem(owner1Email, approvedRestaurantId, rogueReq)
        );
    }

    @Test
    void testOwnerCannotModifyAnotherOwnersFoodItem() {
        var cat = categoryService.createCategory(owner1Email, approvedRestaurantId, new FoodCategoryCreateRequest("Breads", "Breads"));
        var item = foodItemService.createFoodItem(owner1Email, approvedRestaurantId, new FoodItemCreateRequest(
                "Naan", "Garlic Naan", cat.getId(), new BigDecimal("50.00"), null, FoodType.VEG, true, 5
        ));

        FoodItemUpdateRequest updateReq = new FoodItemUpdateRequest(
                "Hacked Naan", "Desc", cat.getId(), new BigDecimal("60.00"), null, FoodType.VEG, true, 5
        );

        assertThrows(AccessDeniedException.class, () ->
                foodItemService.updateFoodItem(owner2Email, item.getId(), updateReq)
        );

        assertThrows(AccessDeniedException.class, () ->
                foodItemService.deleteFoodItem(owner2Email, item.getId())
        );
    }

    @Test
    void testCustomerBrowsingMenuAndFiltering() {
        var cat1 = categoryService.createCategory(owner1Email, approvedRestaurantId, new FoodCategoryCreateRequest("Snacks", "Quick bites"));
        var item1 = foodItemService.createFoodItem(owner1Email, approvedRestaurantId, new FoodItemCreateRequest(
                "Veg Samosa", "Crispy snack", cat1.getId(), new BigDecimal("40.00"), null, FoodType.VEG, true, 10
        ));
        var item2 = foodItemService.createFoodItem(owner1Email, approvedRestaurantId, new FoodItemCreateRequest(
                "Chicken Tikka", "Grilled chicken", cat1.getId(), new BigDecimal("220.00"), null, FoodType.NON_VEG, true, 15
        ));
        var item3 = foodItemService.createFoodItem(owner1Email, approvedRestaurantId, new FoodItemCreateRequest(
                "Fish Finger", "Fried fish", cat1.getId(), new BigDecimal("260.00"), null, FoodType.NON_VEG, false, 20 // UNAVAILABLE
        ));

        // 1. Full Restaurant Menu Response
        MenuResponse menu = menuService.getRestaurantMenu(approvedRestaurantId);
        assertNotNull(menu);
        assertEquals(approvedRestaurantId, menu.getRestaurantId());
        assertEquals(1, menu.getCategories().size());
        // Only 2 available items should appear in the public menu (item3 is unavailable)
        assertEquals(2, menu.getCategories().get(0).getItems().size());

        // 2. Filter by VEG
        List<FoodItemResponse> vegList = foodItemService.filterPublicItemsByType(approvedRestaurantId, FoodType.VEG);
        assertEquals(1, vegList.size());
        assertEquals("Veg Samosa", vegList.get(0).getName());

        // 3. Filter by NON_VEG (only available items)
        List<FoodItemResponse> nonVegList = foodItemService.filterPublicItemsByType(approvedRestaurantId, FoodType.NON_VEG);
        assertEquals(1, nonVegList.size());
        assertEquals("Chicken Tikka", nonVegList.get(0).getName());

        // 4. Search by name
        List<FoodItemResponse> searchList = foodItemService.searchPublicItemsByName(approvedRestaurantId, "Samosa");
        assertEquals(1, searchList.size());
        assertEquals("Veg Samosa", searchList.get(0).getName());

        // 5. Admin can view all and deactivate item
        foodItemService.setAdminItemStatus(item1.getId(), false);
        var vegAfterDeact = foodItemService.filterPublicItemsByType(approvedRestaurantId, FoodType.VEG);
        assertEquals(0, vegAfterDeact.size());
    }
}

