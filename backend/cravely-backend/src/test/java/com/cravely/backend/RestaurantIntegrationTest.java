package com.cravely.backend;

import com.cravely.backend.dto.RegisterRequest;
import com.cravely.backend.dto.RestaurantCreateRequest;
import com.cravely.backend.dto.RestaurantResponse;
import com.cravely.backend.dto.RestaurantUpdateRequest;
import com.cravely.backend.entity.RestaurantStatus;
import com.cravely.backend.entity.Role;
import com.cravely.backend.repository.RestaurantRepository;
import com.cravely.backend.service.AuthService;
import com.cravely.backend.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RestaurantIntegrationTest {

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private AuthService authService;

    private String ownerEmail;
    private String secondOwnerEmail;
    private String customerEmail;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        ownerEmail = "owner_" + ts + "@example.com";
        secondOwnerEmail = "owner2_" + ts + "@example.com";
        customerEmail = "cust_" + ts + "@example.com";

        authService.register(new RegisterRequest("Owner One", ownerEmail, "1111111111", "Pass123!", Role.RESTAURANT_OWNER));
        authService.register(new RegisterRequest("Owner Two", secondOwnerEmail, "2222222222", "Pass123!", Role.RESTAURANT_OWNER));
        authService.register(new RegisterRequest("Customer One", customerEmail, "3333333333", "Pass123!", Role.CUSTOMER));
    }

    private RestaurantCreateRequest createSampleRequest(String name, String city, String cuisine) {
        return new RestaurantCreateRequest(
                name,
                "A fine dining experience",
                "123 Main Street",
                city,
                "State",
                "123456",
                "9876543210",
                "contact@" + name.toLowerCase().replaceAll("\\s+", "") + ".com",
                cuisine,
                "https://images.example.com/photo.jpg",
                LocalTime.of(10, 0),
                LocalTime.of(22, 0)
        );
    }

    @Test
    void testOwnerCreatesRestaurantWithPendingStatus() {
        RestaurantCreateRequest request = createSampleRequest("Spice Villa", "Mumbai", "Indian");
        RestaurantResponse response = restaurantService.createRestaurant(ownerEmail, request);

        assertNotNull(response.getId());
        assertEquals("Spice Villa", response.getName());
        assertEquals(RestaurantStatus.PENDING, response.getStatus());
        assertTrue(response.getActive());
        assertNotNull(response.getOwner());
        assertEquals(ownerEmail, response.getOwner().getEmail());
    }

    @Test
    void testCustomerCannotCreateRestaurant() {
        RestaurantCreateRequest request = createSampleRequest("Customer Cafe", "Delhi", "Cafe");
        assertThrows(AccessDeniedException.class, () -> restaurantService.createRestaurant(customerEmail, request));
    }

    @Test
    void testOwnerCanViewOnlyOwnRestaurants() {
        restaurantService.createRestaurant(ownerEmail, createSampleRequest("Owner 1 Cafe", "Mumbai", "Cafe"));
        restaurantService.createRestaurant(secondOwnerEmail, createSampleRequest("Owner 2 Bistro", "Pune", "Italian"));

        List<RestaurantResponse> owner1List = restaurantService.getOwnerRestaurants(ownerEmail);
        assertEquals(1, owner1List.size());
        assertEquals("Owner 1 Cafe", owner1List.get(0).getName());

        List<RestaurantResponse> owner2List = restaurantService.getOwnerRestaurants(secondOwnerEmail);
        assertEquals(1, owner2List.size());
        assertEquals("Owner 2 Bistro", owner2List.get(0).getName());
    }

    @Test
    void testOwnerCannotModifyAnotherOwnersRestaurant() {
        RestaurantResponse r1 = restaurantService.createRestaurant(ownerEmail, createSampleRequest("R1", "City", "Cuisine"));

        RestaurantUpdateRequest updateReq = new RestaurantUpdateRequest(
                "Hacked Name", "Desc", "Addr", "City", "State", "123456", "9999999999", "hack@ex.com", "Cuisine", null,
                LocalTime.of(9, 0), LocalTime.of(21, 0)
        );

        assertThrows(AccessDeniedException.class, () -> restaurantService.updateRestaurant(secondOwnerEmail, r1.getId(), updateReq));
        assertThrows(AccessDeniedException.class, () -> restaurantService.deactivateRestaurantByOwner(secondOwnerEmail, r1.getId()));
    }

    @Test
    void testAdminApprovalAndCustomerVisibilityLifecycle() {
        RestaurantResponse r1 = restaurantService.createRestaurant(ownerEmail, createSampleRequest("Golden Wok", "Bangalore", "Chinese"));
        RestaurantResponse r2 = restaurantService.createRestaurant(ownerEmail, createSampleRequest("Tandoor Nights", "Delhi", "Indian"));
        RestaurantResponse r3 = restaurantService.createRestaurant(secondOwnerEmail, createSampleRequest("Pasta Fresca", "Bangalore", "Italian"));

        // Step 1: Newly created restaurants are PENDING and must NOT appear in public customer listings
        var publicListBeforeApproval = restaurantService.getApprovedActiveRestaurants(PageRequest.of(0, 100));
        assertTrue(publicListBeforeApproval.getContent().stream().noneMatch(r -> r.getId().equals(r1.getId())));

        // Step 2: Admin views pending restaurants
        var pendingList = restaurantService.getPendingRestaurants();
        assertTrue(pendingList.stream().anyMatch(r -> r.getId().equals(r1.getId())));

        // Step 3: Admin approves r1 and rejects r2
        restaurantService.approveRestaurant(r1.getId());
        restaurantService.rejectRestaurant(r2.getId());

        // Step 4: Approved r1 appears in public listings; Rejected r2 does not
        var publicListAfterApproval = restaurantService.getApprovedActiveRestaurants(PageRequest.of(0, 100));
        assertTrue(publicListAfterApproval.getContent().stream().anyMatch(r -> r.getId().equals(r1.getId())));
        assertTrue(publicListAfterApproval.getContent().stream().noneMatch(r -> r.getId().equals(r2.getId())));

        // Verify public listing does not expose owner private details
        RestaurantResponse publicR1 = publicListAfterApproval.getContent().stream()
                .filter(r -> r.getId().equals(r1.getId()))
                .findFirst().orElseThrow();
        assertNull(publicR1.getOwner());

        // Step 5: Deactivated approved restaurant does not appear in public listings
        restaurantService.deactivateRestaurantByAdmin(r1.getId());
        var publicListAfterDeactivation = restaurantService.getApprovedActiveRestaurants(PageRequest.of(0, 100));
        assertTrue(publicListAfterDeactivation.getContent().stream().noneMatch(r -> r.getId().equals(r1.getId())));

        // Step 6: Re-activate r1 and verify search and filter
        restaurantService.activateRestaurantByAdmin(r1.getId());

        // Search by name
        var searchRes = restaurantService.searchRestaurantsByName("Golden", PageRequest.of(0, 10));
        assertTrue(searchRes.getContent().stream().anyMatch(r -> r.getName().equals("Golden Wok")));

        // Filter by city
        var cityRes = restaurantService.filterRestaurantsByCity("Bangalore", PageRequest.of(0, 10));
        assertTrue(cityRes.getContent().stream().anyMatch(r -> r.getName().equals("Golden Wok")));

        // Filter by cuisine
        var cuisineRes = restaurantService.filterRestaurantsByCuisine("Chinese", PageRequest.of(0, 10));
        assertTrue(cuisineRes.getContent().stream().anyMatch(r -> r.getName().equals("Golden Wok")));
    }
}

