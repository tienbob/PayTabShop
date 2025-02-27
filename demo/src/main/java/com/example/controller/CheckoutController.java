package com.example.controller;

import java.util.Map;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.example.model.Product;
import com.example.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class CheckoutController {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);

    @Value("${paytabs.profile-id}")
    private String profileId;

    @Value("${paytabs.integration-key}")
    private String integrationKey;

    @Value("${paytabs.base-url}")
    private String paytabsBaseUrl;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private Environment environment;

    @PostMapping("/checkout")
    public ResponseEntity<?> createPaymentRequest(
            @RequestParam String customerName,
            @RequestParam String customerEmail,
            @RequestParam String shippingAddress,
            @RequestParam String paymentOption,
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String cardNumber,
            @RequestParam(required = false) String expiryDate,
            @RequestParam(required = false) String cvv) {

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return new ResponseEntity<>("Product not found", org.springframework.http.HttpStatus.NOT_FOUND);
        }

        // 1. Construct PayTabs payment request payload
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("profile_id", profileId);
        paymentRequest.put("tran_type", "sale");
        paymentRequest.put("tran_class", "ecom");

        // *** Add the missing required parameters ***
        Long orderId = System.currentTimeMillis();
        Double cartAmount = product.getPrice() * quantity; // Calculate the cart amount
        paymentRequest.put("cart_id", String.valueOf(orderId)); // Use a unique cart ID, ensure the id is unique
        paymentRequest.put("cart_amount", cartAmount); // The cart amount
        paymentRequest.put("cart_currency", "EGP"); // Change if needed
        paymentRequest.put("cart_description", "Payment for product");
        paymentRequest.put("customer_email", customerEmail);
        paymentRequest.put("framed", true);

        // Determine if we are in the test environment
        String activeProfile = System.getenv("SPRING_PROFILES_ACTIVE");
        boolean isDevProfile = activeProfile != null && activeProfile.equalsIgnoreCase("dev");

        // Add card details to request if available or in test env
        if (isDevProfile) {
            paymentRequest.put("card_number", environment.getProperty("paytabs.testcard.number"));
            paymentRequest.put("card_expiry", environment.getProperty("paytabs.testcard.expiry"));
            paymentRequest.put("card_cvv", environment.getProperty("paytabs.testcard.cvv"));
        }
        else if(cardNumber != null && expiryDate != null && cvv != null){
            paymentRequest.put("card_number", cardNumber);
            paymentRequest.put("card_expiry", expiryDate);
            paymentRequest.put("card_cvv", cvv);
        } else {
            return new ResponseEntity<>("Card Details Required", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        // 2. Make the API call to PayTabs
        RestTemplate restTemplate = new RestTemplate();

        // *** Add the HttpHeaders code here ***
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", integrationKey); // integrationKey should be your PayTabs integration key
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(paymentRequest, headers);

        ResponseEntity<Map> paytabsResponse = restTemplate.postForEntity(
                paytabsBaseUrl, // PayTabs API endpoint URL
                requestEntity,
                Map.class);

        // 3. Extract the tran_ref from PayTabs response and paymentUrl
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) paytabsResponse.getBody();
        if (responseBody == null) {
            return ResponseEntity.status(500).body("Error: PayTabs response body is null");
        }

        //3. Log the entire response
        logger.info("PayTabs Response: {}", responseBody);

        String redirect_url = (String) responseBody.get("redirect_url");
        String tranRef = (String) responseBody.get("tran_ref");

        // 4. Construct the response to send back to the JavaScript code
        Map<String, Object> response = new HashMap<>();
        response.put("redirect_url", redirect_url);
        response.put("tran_ref", tranRef);

        return ResponseEntity.ok(response);
    }
}