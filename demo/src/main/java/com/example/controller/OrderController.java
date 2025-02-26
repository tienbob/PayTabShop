// src/main/java/com/example/paytabsshop/controller/OrderController.java
package com.example.controller;

import com.example.model.*;
import com.example.repository.OrderRepository;
import com.example.repository.ProductRepository;
import com.example.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentService paymentService;

    @Value("${paytabs.base-url}")
    private String paytabsBaseUrl;

    @GetMapping("/")
    public String index(Model model) {
        List<Product> products = productRepository.findAll();
        model.addAttribute("products", products);
        return "index";
    }

    @GetMapping("/my_orders")
    public String myOrders(Model model) {
        List<Order> orders = orderRepository.findAll();
        model.addAttribute("orders", orders);
        model.addAttribute("OrderStatus", Order.OrderStatus.class); // Expose the enum
        return "my_orders";
    }

    @GetMapping("/order_details/{orderId}")
    public String orderDetails(@PathVariable Long orderId, Model model) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return "error"; // Or a more specific error page
        }
        model.addAttribute("order", order);
        return "order_details";
    }


    @PostMapping("/add_to_cart")
    public RedirectView addToCart(@RequestParam Long productId, @RequestParam Integer quantity, HttpServletRequest request) {
        // Redirect to the checkout page with product and quantity parameters
        String checkoutUrl = "/checkout?productId=" + productId + "&quantity=" + quantity;
        return new RedirectView(checkoutUrl, true);
    }

    @GetMapping("/checkout")
    public String checkout(@RequestParam Long productId, @RequestParam Integer quantity, Model model) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return "error"; // Handle product not found
        }

        model.addAttribute("product", product);
        model.addAttribute("quantity", quantity);
        return "checkout";
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> createPaymentRequest(
            @RequestParam String customerName,
            @RequestParam String customerEmail,
            @RequestParam String shippingAddress,
            @RequestParam String paymentOption,
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            Model model) {

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return new ResponseEntity<>("Product not found", HttpStatus.NOT_FOUND);
        }

        Order order = new Order();
        order.setCustomerName(customerName);
        order.setCustomerEmail(customerEmail);
        order.setShippingAddress(shippingAddress);
        //Order Status Set to Pending by Default
        //Create Order Items
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);
        orderItem.setOrder(order);

        order.setOrderItems(List.of(orderItem));
        //Calcuate the Amount
        Double amount = product.getPrice() * quantity;

        Payment payment = paymentService.createPayment(order, amount);

        orderRepository.save(order);
        // Create PayTabs payment request payload
        Map<String, Object> payment_request_payload = paymentService.generatePaymentRequestPayload(order, amount);
        payment_request_payload.put("return", "/payment_success/" + order.getId());
        payment_request_payload.put("callback", "/payment_callback/" + order.getId());
        // Store the payment request payload in the database
        payment.setRequestPayload(paymentService.convertToJson(payment_request_payload));

        order.setPayment(payment);
        orderRepository.save(order);
        // Return the payment request payload as JSON for the AJAX request
        return new ResponseEntity<>(payment_request_payload, HttpStatus.OK);
    }


    @PostMapping("/payment_request")
    public ResponseEntity<?> paymentRequest(@RequestBody Map<String, Object> payment_request_payload) {
        ResponseEntity<?> response = paymentService.processPaymentRequest(payment_request_payload);
        return response;
    }

    @GetMapping("/payment_success/{orderId}")
    public String paymentSuccess(@PathVariable Long orderId, Model model) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return "error"; // Handle order not found
        }

        order.setStatus(Order.OrderStatus.COMPLETED);
        orderRepository.save(order);

        model.addAttribute("order", order);
        return "payment_success";
    }

    @PostMapping("/payment_callback/{orderId}")
    public ResponseEntity<String> paymentCallback(@PathVariable Long orderId, @RequestBody Map<String, Object> callbackData) {
        Order order = orderRepository.findById(orderId).orElse(null);

        if (order == null) {
            return new ResponseEntity<>("Order not found", HttpStatus.NOT_FOUND);
        }
        Payment payment = order.getPayment();
        // Extract payment details from the callback data
        System.out.println("PayTabs Callback Data: " + paymentService.convertToJson(callbackData));

        // Update payment and order status in the database based on the response
        if (callbackData != null && callbackData.get("response_code").equals("100")) {
            // Payment successful
            order.setStatus(Order.OrderStatus.COMPLETED);
            payment.setTransactionId((String) callbackData.get("transaction_id"));
            payment.setResponsePayload(paymentService.convertToJson(callbackData));
            orderRepository.save(order);

            return new ResponseEntity<>("Payment callback received and processed successfully.", HttpStatus.OK);
        } else {
            // Payment failed
            order.setStatus(Order.OrderStatus.FAILED);
            payment.setResponsePayload(paymentService.convertToJson(callbackData));
            orderRepository.save(order);
            return new ResponseEntity<>("Payment callback received, but payment failed.", HttpStatus.BAD_REQUEST);
        }
    }


    @GetMapping("/payment_error")
    public String paymentError() {
        return "payment_error";
    }
}
