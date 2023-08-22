package com.example.demo.controller;

import com.example.demo.dtos.ReviewDTO;
import com.example.demo.dtos.UpdateReviewDTO;
import com.example.demo.entities.Review;
import com.example.demo.services.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Controller
@RequestMapping(value = "/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PreAuthorize("hasAuthority('Customer')")
    @PostMapping
    public ResponseEntity<Review> createReview(@Valid @RequestBody ReviewDTO reviewDTO) {
        Review review = reviewService.create(reviewDTO);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(review.getId()).toUri();

        return ResponseEntity.created(uri).body(review);
    }

    @GetMapping
    public ResponseEntity<Page<Review>> findAll(
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "5") Integer pageSize,
            @RequestParam(defaultValue = "rating") String sortBy
    ) {
        Page<Review> reviews = reviewService.findAll(pageNo, pageSize, sortBy);
        return ResponseEntity.ok().body(reviews);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<Review> findById(@PathVariable UUID id) {
        return ResponseEntity.ok().body(reviewService.findById(id));
    }

    @PreAuthorize("hasAuthority('Customer')")
    @PatchMapping(value = "/{id}")
    public ResponseEntity<Review> update(@PathVariable UUID id,
                                         @Valid @RequestBody UpdateReviewDTO obj) {
        return ResponseEntity.ok().body(reviewService.update(id, obj));
    }

    @PreAuthorize("hasAnyAuthority('Customer', 'Admin')")
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Review> delete(@PathVariable UUID id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
