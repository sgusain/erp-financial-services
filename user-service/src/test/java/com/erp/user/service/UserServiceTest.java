package com.erp.user.service;

import com.erp.common.exception.ResourceNotFoundException;
import com.erp.user.entity.Role;
import com.erp.user.entity.User;
import com.erp.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void createUser_alwaysForcesRoleToUser_evenWhenAdminRequested() {
        // Regression test: registration is a public endpoint. If this ever silently
        // trusted a client-supplied role again, anyone could self-register as ADMIN.
        User maliciousRequest = new User();
        maliciousRequest.setName("Eve");
        maliciousRequest.setEmail("eve@example.com");
        maliciousRequest.setPassword("plaintext-password");
        maliciousRequest.setRole(Role.ADMIN);

        when(passwordEncoder.encode("plaintext-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.createUser(maliciousRequest);

        assertThat(saved.getRole()).isEqualTo(Role.USER);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void createUser_encodesPasswordBeforeSaving() {
        User request = new User();
        request.setName("Bob");
        request.setEmail("bob@example.com");
        request.setPassword("plaintext-password");
        request.setRole(Role.USER);

        when(passwordEncoder.encode("plaintext-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.createUser(request);

        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        verify(passwordEncoder).encode("plaintext-password");
    }

    @Test
    void getUserById_returnsUser_whenFound() {
        User user = new User();
        user.setId(1L);
        user.setEmail("found@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertThat(result.getEmail()).isEqualTo("found@example.com");
    }

    @Test
    void getUserById_throwsResourceNotFoundException_whenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getUserByEmail_returnsUser_whenFound() {
        User user = new User();
        user.setEmail("someone@example.com");
        when(userRepository.findByEmail("someone@example.com")).thenReturn(Optional.of(user));

        User result = userService.getUserByEmail("someone@example.com");

        assertThat(result.getEmail()).isEqualTo("someone@example.com");
    }

    @Test
    void getUserByEmail_throwsResourceNotFoundException_whenMissing() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("ghost@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllUsers_delegatesToRepository() {
        Pageable pageable = mock(Pageable.class);
        Page<User> page = new PageImpl<>(List.of(new User()));
        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<User> result = userService.getAllUsers(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void updateUser_updatesFieldsAndReEncodesPassword_whenPasswordProvided() {
        User existing = new User();
        existing.setId(5L);
        existing.setName("Old Name");
        existing.setEmail("old@example.com");
        existing.setPassword("old-encoded");
        existing.setRole(Role.USER);

        User updateRequest = new User();
        updateRequest.setName("New Name");
        updateRequest.setEmail("new@example.com");
        updateRequest.setPassword("new-plaintext");
        updateRequest.setRole(Role.ADMIN);

        when(userRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("new-plaintext")).thenReturn("new-encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUser(5L, updateRequest);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getPassword()).isEqualTo("new-encoded");
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void updateUser_keepsExistingPassword_whenPasswordNotProvided() {
        User existing = new User();
        existing.setId(5L);
        existing.setName("Old Name");
        existing.setEmail("old@example.com");
        existing.setPassword("old-encoded");
        existing.setRole(Role.USER);

        User updateRequest = new User();
        updateRequest.setName("New Name");
        updateRequest.setEmail("new@example.com");
        updateRequest.setPassword(null);
        updateRequest.setRole(Role.USER);

        when(userRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUser(5L, updateRequest);

        assertThat(result.getPassword()).isEqualTo("old-encoded");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void updateUser_throwsResourceNotFoundException_whenMissing() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(404L, new User()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteUser_deletesUser_whenFound() {
        User existing = new User();
        existing.setId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(existing));

        userService.deleteUser(7L);

        verify(userRepository, times(1)).delete(existing);
    }

    @Test
    void deleteUser_throwsResourceNotFoundException_whenMissing() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
