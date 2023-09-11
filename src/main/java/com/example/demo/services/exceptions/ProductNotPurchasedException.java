package com.example.demo.services.exceptions;

public class ProductNotPurchasedException extends RuntimeException {
    public ProductNotPurchasedException() {
        super("You cannot review a product that you haven't purchased");
    }
}
