package com.example.service;

import com.example.model.Order;
import com.example.model.Payment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    @Value("${paytabs.profile-id}")
    private String profileId;

    @Value("${paytabs.integration-key}")
    private String integrationKey;

    @Value("${paytabs.base-url}")
    private String paytabsBaseUrl;

    // Helper function to format floats
    public static String formatFloat(double value) {
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(value);
    }

    public Payment createPayment(Order order, Double amount) {
        Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setPaymentMethod("PayTabs iFrame");
        return payment;
    }

    public Map<String, Object> generatePaymentRequestPayload(Order order, Double amount) {

        Map<String, Object> payment_request_payload = new HashMap<>();
        payment_request_payload.put("profile_id", profileId);
        payment_request_payload.put("tran_type", "sale");
        payment_request_payload.put("tran_class", "ecom");
        payment_request_payload.put("cart_id", String.valueOf(order.getId())); // Unique order ID
        payment_request_payload.put("cart_amount", formatFloat(amount)); // Format to two decimal places
        payment_request_payload.put("cart_currency", "EGP"); // Or your currency
        payment_request_payload.put("cart_description", "Order #" + order.getId() + " - " + order.getOrderItems().get(0).getProduct().getName() + " x " + order.getOrderItems().get(0).getQuantity());
        Map<String, Object> customer_details = new HashMap<>();
        customer_details.put("name", order.getCustomerName());
        customer_details.put("email", order.getCustomerEmail());
        customer_details.put("phone", ""); // Optional
        customer_details.put("street1", order.getShippingAddress());
        customer_details.put("city", ""); // Optional
        customer_details.put("state", ""); // Optional
        customer_details.put("country", "EG"); // Or your country code
        customer_details.put("zip", ""); // Optional
        payment_request_payload.put("customer_details", customer_details);

        Map<String, Object> shipping_details = new HashMap<>();
        shipping_details.put("name", order.getCustomerName());
        shipping_details.put("email", order.getCustomerEmail());
        shipping_details.put("phone", ""); // Optional
        shipping_details.put("street1", order.getShippingAddress());
        shipping_details.put("city", ""); // Optional
        shipping_details.put("state", ""); // Optional
        shipping_details.put("country", "EG"); // Or your country code
        shipping_details.put("zip", ""); // Optional
        payment_request_payload.put("shipping_details", shipping_details);

        payment_request_payload.put("hide_shipping", true); // Hide unnecessary shipping fields
        payment_request_payload.put("framed", true); // Use iFrame mode

        return payment_request_payload;
    }
    public ResponseEntity<?> processPaymentRequest(Map<String, Object> payment_request_payload) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", integrationKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payment_request_payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(paytabsBaseUrl, requestEntity, Map.class);
            return new ResponseEntity<>(response.getBody(), response.getStatusCode());

        } catch (Exception e) {
            System.err.println("Error during PayTabs API call: " + e.getMessage());
            return new ResponseEntity<>("Error during PayTabs API call: " + e.getMessage(), org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String convertToJson(Object obj) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            System.err.println("Error converting object to JSON: " + e.getMessage());
            return "{}"; // Return an empty JSON object in case of error
        }
    }
}