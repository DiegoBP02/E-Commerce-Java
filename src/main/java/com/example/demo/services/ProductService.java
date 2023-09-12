package com.example.demo.services;

import com.example.demo.dtos.ProductDTO;
import com.example.demo.entities.Product;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.enums.ProductCategory;
import com.example.demo.enums.Role;
import com.example.demo.repositories.ProductRepository;
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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.example.demo.config.utils.GetCurrentUser.getCurrentUser;
import static com.example.demo.services.utils.CheckOwnership.checkOwnership;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserService userService;

    public Product create(ProductDTO productDTO) {
        Seller user = (Seller) getCurrentUser();
        Product product = Product.builder()
                .name(productDTO.getName())
                .description(productDTO.getDescription())
                .price(productDTO.getPrice())
                .seller(user)
                .category(productDTO.getCategory())
                .build();
        return productRepository.save(product);
    }

    @Transactional
    public Page<Product> findAll(Integer pageNo, Integer pageSize, Sort.Direction sortOrder, String sortBy) {
        Pageable paging = PageRequest.of(pageNo, pageSize, sortOrder, sortBy);

        return productRepository.findAll(paging);
    }

    public List<Product> findByCategory(ProductCategory productCategory) {
        return productRepository.findByCategory(productCategory);
    }

    public Product findById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Page<Product> findByCurrentUser(Integer pageNo, Integer pageSize, Sort.Direction sortOrder, String sortBy) {
        Seller seller = (Seller) getCurrentUser();
        Pageable paging = PageRequest.of(pageNo, pageSize, sortOrder, sortBy);

        return productRepository.findAllBySeller(seller, paging);
    }

    public Page<Product> findAllBySeller(UUID sellerId, Integer pageNo, Integer pageSize, Sort.Direction sortOrder, String sortBy) {
        Seller seller = userService.findByIdAndEnsureType(sellerId, Seller.class);
        Pageable paging = PageRequest.of(pageNo, pageSize, sortOrder, sortBy);

        return productRepository.findAllBySeller(seller, paging);
    }

    public Product update(UUID id, ProductDTO obj) {
        try {
            Product entity = productRepository.getReferenceById(id);
            User user = getCurrentUser();
            checkOwnership(user, entity.getSeller().getId());
            updateData(entity, obj);

            return productRepository.save(entity);
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException(id);
        }
    }

    private void updateData(Product entity, ProductDTO obj) {
        entity.setDescription(obj.getDescription());
        entity.setName(obj.getName());
        entity.setPrice(obj.getPrice());
    }

    public void delete(UUID id) {
        try {
            Product entity = productRepository.getReferenceById(id);

            User user = getCurrentUser();
            String role = user.getAuthorities().stream().toList().get(0).getAuthority();

            if (!role.equals(Role.Admin.toString())) {
                checkOwnership(user, entity.getSeller().getId());
            }

            productRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

}
