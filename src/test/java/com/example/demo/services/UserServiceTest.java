package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest extends ApplicationConfigTest {

    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    private User user = TestDataBuilder.buildUserWithId();
    private Seller seller = (Seller) TestDataBuilder.buildUserWithId();
    private Customer customer = (Customer) TestDataBuilder.buildCustomerWithId();

    @Test
    void givenSeller_whenFindByIdAndEnsureType_thenReturnSeller(){
        when(userRepository.findById(seller.getId())).thenReturn(Optional.of(seller));

        User result = userService.findByIdAndEnsureType(seller.getId(), Seller.class);

        assertThat(result).isInstanceOf(Seller.class);

        verify(userRepository,times(1)).findById(seller.getId());
    }

    @Test
    void givenNotSeller_whenFindByIdAndEnsureType_thenThrowClassCastException(){
        when(userRepository.findById(seller.getId())).thenReturn(Optional.of(customer));

        ClassCastException classCastException = assertThrows(ClassCastException.class,
                () -> userService.findByIdAndEnsureType(seller.getId(), Seller.class));

        assertEquals("Invalid user type. Cannot cast user to the specified type."
                ,classCastException.getMessage());

        verify(userRepository,times(1)).findById(seller.getId());
    }

    @Test
    void givenCustomer_whenFindByIdAndEnsureType_thenReturnCustomer(){
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        User result = userService.findByIdAndEnsureType(customer.getId(), Customer.class);

        assertThat(result).isInstanceOf(Customer.class);

        verify(userRepository,times(1)).findById(customer.getId());
    }

    @Test
    void givenNotCustomer_whenFindByIdAndEnsureType_thenThrowClassCastException(){
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(seller));

        ClassCastException classCastException = assertThrows(ClassCastException.class,
                () -> userService.findByIdAndEnsureType(customer.getId(), Customer.class));

        assertEquals("Invalid user type. Cannot cast user to the specified type."
                ,classCastException.getMessage());

        verify(userRepository,times(1)).findById(customer.getId());
    }

    @Test
    void givenUserDoesNotExists_whenFindByIdAndEnsureType_thenThrowResourceNotFoundException(){
        when(userRepository.findById(seller.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.findByIdAndEnsureType(seller.getId(), Seller.class));

        verify(userRepository,times(1)).findById(seller.getId());
    }

}