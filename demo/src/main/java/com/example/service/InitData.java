package com.example.service;

import com.example.model.Order;
import com.example.model.OrderItem;
import com.example.model.Product;
import com.example.repository.OrderItemRepository;
import com.example.repository.OrderRepository;
import com.example.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class InitData {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @PostConstruct
    public void initializeData() {
        createProducts();
        createOrders();
    }

    private void createProducts() {
        // Check if products already exist
        if (productRepository.count() > 0) {
            return; // Products already exist, don't create them again
        }

        for (int i = 1; i <= 10; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setDescription("Description for Product " + i);
            product.setPrice(Math.round(Math.random() * 10000.0) / 100.0); // Random price with 2 decimal places
            productRepository.save(product);
        }
    }

    private void createOrders() {
        // Check if orders already exist
        if (orderRepository.count() > 0) {
            return; // Orders already exist, don't create them again
        }
    
        Random random = new Random();
        List<Product> allProducts = productRepository.findAll();
    
        for (int i = 1; i <= 5; i++) { // Create 5 sample orders
            Order order = new Order();
            order.setCustomerName("Customer " + i);
            order.setCustomerEmail("customer" + i + "@example.com");
            order.setShippingAddress("Address " + i);
            //Set to Pending by Default
            //SAVE ORDER HERE
            orderRepository.save(order);
    
            // Create Order Items
            List<OrderItem> orderItems = new ArrayList<>();
            int numItems = random.nextInt(3) + 1; // Each order will have 1-3 items
            double totalAmount = 0;
    
            for (int j = 0; j < numItems; j++) {
                Product product = allProducts.get(random.nextInt(allProducts.size()));
                int quantity = random.nextInt(5) + 1; // Quantity of 1-5
    
                OrderItem orderItem = new OrderItem();
                orderItem.setProduct(product);
                orderItem.setQuantity(quantity);
                orderItem.setOrder(order);
                // Save Order Item
                orderItemRepository.save(orderItem);
                orderItems.add(orderItem);
                totalAmount += product.getPrice() * quantity;
            }
    
            order.setOrderItems(orderItems);
    
            // Create a Payment object (optional, for demonstration)
            // Payment payment = new Payment();
            // payment.setAmount(totalAmount);
            // payment.setPaymentMethod("Credit Card");
    
            // associate Payment with the Order before saving (if applicable)
            // order.setPayment(payment);
    
            //orderRepository.save(order); Remove it as the orderItems should already persist it
    
        }
    }

}