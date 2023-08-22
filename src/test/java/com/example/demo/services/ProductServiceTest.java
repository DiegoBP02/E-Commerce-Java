package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.ProductDTO;
import com.example.demo.entities.Product;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.enums.Role;
import com.example.demo.repositories.ProductRepository;
import com.example.demo.services.exceptions.DatabaseException;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import com.example.demo.services.exceptions.UnauthorizedAccessException;
import com.example.demo.utils.TestDataBuilder;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ProductServiceTest extends ApplicationConfigTest {
    @Autowired
    private ProductService productService;

    @MockBean
    private ProductRepository productRepository;

    private Authentication authentication;
    private SecurityContext securityContext;

    private Seller seller = (Seller) TestDataBuilder.buildUserWithId();
    private Product product = TestDataBuilder.buildProduct(seller);
    private ProductDTO productDTO = TestDataBuilder.buildProductDTO();
    private Page<Product> productPage = new PageImpl<>
            (Collections.singletonList(product),
                    PageRequest.of(0, 5, Sort.by("name")), 1);

    @BeforeEach
    void setupSecurityContext() {
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(seller);

        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    private void verifyNoAuthentication() {
        verify(authentication, never()).getPrincipal();
        verify(securityContext, never()).getAuthentication();
    }

    private void verifyAuthentication() {
        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
    }

    @Test
    void givenValidProductDTO_whenCreate_thenReturnProduct() {
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.create(productDTO);

        assertEquals(product, result);

        verifyAuthentication();
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void givenProducts_whenFindAll_ThenReturnProductPage() {
        when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);

        Page<Product> result = productService
                .findAll(productPage.getPageable().getPageNumber(),
                        productPage.getPageable().getPageSize(),
                        productPage.getPageable().getSort().stream().toList().get(0).getProperty());

        assertEquals(productPage, result);

        verifyNoAuthentication();
        verify(productRepository, times(1)).findAll(productPage.getPageable());
    }

    @Test
    void givenProducts_whenFindByCategory_ThenReturnProducts() {
        List<Product> products = Collections.singletonList(product);
        when(productRepository.findByCategory(product.getCategory())).thenReturn(products);

        List<Product> result = productService.findByCategory(product.getCategory());

        assertEquals(products, result);

        verifyNoAuthentication();
        verify(productRepository, times(1)).findByCategory(product.getCategory());
    }

    @Test
    void givenProduct_whenFindById_thenReturnProduct() {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        Product result = productService.findById(product.getId());

        assertEquals(product, result);

        verifyNoAuthentication();
        verify(productRepository, times(1)).findById(product.getId());
    }

    @Test
    void givenNoProduct_whenFindById_thenThrowResourceNotFoundException() {
        when(productRepository.findById(product.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.findById(product.getId()));

        verifyNoAuthentication();
        verify(productRepository, times(1)).findById(product.getId());
    }

    @Test
    void givenValidIdAndProductDTO_whenUpdate_thenReturnUpdatedProduct() {
        when(productRepository.getReferenceById(product.getId())).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);

        productDTO = ProductDTO.builder().price(BigDecimal.ONE).build();

        Product result = productService.update(product.getId(), productDTO);

        assertEquals(productDTO.getPrice(), result.getPrice());

        verifyAuthentication();
        verify(productRepository, times(1)).getReferenceById(product.getId());
        verify(productRepository, times(1)).save(product);
    }

    @Test
    void givenNoProduct_whenUpdate_thenThrowResourceNotFoundException() {
        when(productRepository.getReferenceById(product.getId())).thenReturn(product);
        when(productRepository.save(product))
                .thenThrow(EntityNotFoundException.class);

        assertThrows(ResourceNotFoundException.class,
                () -> productService.update(product.getId(), productDTO));

        verifyAuthentication();
        verify(productRepository, times(1)).getReferenceById(product.getId());
        verify(productRepository, times(1)).save(product);
    }

    @Test
    void givenProductDoesNotBelongToUser_whenUpdate_thenThrowUnauthorizedAccessException() {
        User user2 = mock(User.class);
        when(user2.getId()).thenReturn(UUID.randomUUID());

        when(authentication.getPrincipal()).thenReturn(user2);
        when(productRepository.getReferenceById(product.getId())).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);

        assertThrows(UnauthorizedAccessException.class,
                () -> productService.update(product.getId(), productDTO));

        verifyAuthentication();
        verify(productRepository, times(1)).getReferenceById(product.getId());
        verify(productRepository, never()).save(product);
    }

    @Test
    void givenProduct_whenDelete_thenDeleteProduct() {
        when(productRepository.getReferenceById(any(UUID.class))).thenReturn(product);

        productService.delete(UUID.randomUUID());

        verifyAuthentication();
        verify(productRepository, times(1)).getReferenceById(any(UUID.class));
        verify(productRepository, times(1)).deleteById(any(UUID.class));
    }

    @Test
    void givenNoProduct_whenDelete_thenThrowResourceNotFoundException() {
        when(productRepository.getReferenceById(product.getId())).thenReturn(product);
        doThrow(EmptyResultDataAccessException.class)
                .when(productRepository).deleteById(product.getId());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.delete(product.getId()));

        verifyAuthentication();
        verify(productRepository, times(1)).getReferenceById(product.getId());
        verify(productRepository, times(1)).deleteById(product.getId());
    }

    @Test
    void givenProductAndDeleteCausesDataIntegrityViolationException_whenDelete_thenThrowDatabaseException() {
        when(productRepository.getReferenceById(product.getId())).thenReturn(product);
        doThrow(DataIntegrityViolationException.class)
                .when(productRepository).deleteById(product.getId());

        assertThrows(DatabaseException.class,
                () -> productService.delete(product.getId()));

        verifyAuthentication();
        verify(productRepository, times(1)).getReferenceById(product.getId());
        verify(productRepository, times(1)).deleteById(product.getId());
    }

    @Test
    void givenProductDoesNotBelongToUser_whenDelete_thenThrowUnauthorizedAccessException() {
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(Role.Seller.name());
        User user2 = mock(User.class);
        when(user2.getId()).thenReturn(UUID.randomUUID());
        doReturn(authorities).when(user2).getAuthorities();

        when(authentication.getPrincipal()).thenReturn(user2);
        when(productRepository.getReferenceById(product.getId())).thenReturn(product);

        assertThrows(UnauthorizedAccessException.class,
                () -> productService.delete(product.getId()));

        verifyAuthentication();
        verify(productRepository, times(1)).getReferenceById(product.getId());
        verify(productRepository, never()).deleteById(product.getId());
    }

    @Test
    void givenProductAndCurrentUserIsAdmin_whenDelete_thenDeleteProduct() {
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(Role.Admin.name());
        User user2 = mock(User.class);
        when(user2.getId()).thenReturn(UUID.randomUUID());
        doReturn(authorities).when(user2).getAuthorities();
        when(authentication.getPrincipal()).thenReturn(user2);

        when(productRepository.getReferenceById(product.getId())).thenReturn(product);

        productService.delete(product.getId());

        verifyAuthentication();
        verify(productRepository, times(1)).getReferenceById(product.getId());
        verify(productRepository, times(1)).deleteById(product.getId());
    }

}