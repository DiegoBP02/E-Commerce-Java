package com.example.demo.services;

import com.example.demo.dtos.ReviewDTO;
import com.example.demo.dtos.UpdateReviewDTO;
import com.example.demo.entities.Product;
import com.example.demo.entities.Review;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.User;
import com.example.demo.enums.Role;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.services.exceptions.DatabaseException;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.example.demo.services.utils.CheckOwnership.checkOwnership;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductService productService;

    public Review create(ReviewDTO reviewDTO) {
        Product product = productService.findById(reviewDTO.getProductId());
        Customer user = (Customer) getCurrentUser();
        Review review = Review.builder()
                .comment(reviewDTO.getComment())
                .rating(reviewDTO.getRating())
                .product(product)
                .customer(user)
                .build();
        return reviewRepository.save(review);
    }

    @Transactional
    public Page<Review> findAll(Integer pageNo, Integer pageSize, String sortBy) {
        Pageable paging = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));

        return reviewRepository.findAll(paging);
    }

    public Review findById(UUID id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Review update(UUID id, UpdateReviewDTO obj) {
        try {
            Review entity = reviewRepository.getReferenceById(id);
            User user = getCurrentUser();
            checkOwnership(user, entity.getCustomer().getId());
            updateData(entity, obj);

            return reviewRepository.save(entity);
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException(id);
        }
    }

    private void updateData(Review entity, UpdateReviewDTO obj) {
        entity.setComment(obj.getComment());
        entity.setRating(obj.getRating());
    }

    public void delete(UUID id) {
        try {
            Review entity = reviewRepository.getReferenceById(id);

            User user = getCurrentUser();
            String role = user.getAuthorities().stream().toList().get(0).getAuthority();

            if (!role.equals(Role.Admin.toString())) {
                checkOwnership(user, entity.getCustomer().getId());
            }

            reviewRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

}
