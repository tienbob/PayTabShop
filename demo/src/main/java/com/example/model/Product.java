package com.example.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "products")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255) //Added length for name
    private String name;

    @Column(columnDefinition = "TEXT") //Specifying the data type
    private String description;

    @Column(nullable = false)
    private Double price;
}