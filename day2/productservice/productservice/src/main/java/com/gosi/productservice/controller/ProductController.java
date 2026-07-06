package com.gosi.productservice.controller;

import com.gosi.productservice.model.Product;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {



    // hardcoded data - no database, keep it simple
    private final List<Product> products = List.of(
            new Product(1, "Laptop", 55000.0),
            new Product(2, "Mobile", 20000.0),
            new Product(3, "Headphones", 2000.0)
    );

    // GET http://localhost:8080/products/1
    @GetMapping("/{id}")
    public Product getProduct(@PathVariable int id) {
        return products.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }
}
