package com.example.requesthandling.service;

import com.example.requesthandling.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserService のユニットテスト
 */
@DisplayName("ユーザーサービスのテスト")
class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService();
    }

    @Test
    @DisplayName("初期データが正しく登録されている")
    void testInitialData() {
        // When
        List<User> users = userService.getAllUsers();

        // Then
        assertEquals(3, users.size());
        assertEquals(3, userService.getUserCount());
    }

    @Test
    @DisplayName("全ユーザーを取得できる")
    void testGetAllUsers() {
        // When
        List<User> users = userService.getAllUsers();

        // Then
        assertNotNull(users);
        assertFalse(users.isEmpty());
        assertTrue(users.size() >= 3);
    }

    @Test
    @DisplayName("IDでユーザーを取得できる")
    void testGetUserById() {
        // When
        Optional<User> user = userService.getUserById("1");

        // Then
        assertTrue(user.isPresent());
        assertEquals("1", user.get().getId());
        assertEquals("user1", user.get().getName());
        assertEquals("user1@example.com", user.get().getEmail());
    }

    @Test
    @DisplayName("存在しないIDの場合は空のOptionalが返る")
    void testGetUserByIdNotFound() {
        // When
        Optional<User> user = userService.getUserById("999");

        // Then
        assertFalse(user.isPresent());
    }

    @Test
    @DisplayName("ユーザーを作成できる")
    void testCreateUser() {
        // Given
        User newUser = User.builder()
            .name("testuser")
            .email("testuser@example.com")
            .build();
        int initialCount = userService.getUserCount();

        // When
        User createdUser = userService.createUser(newUser);

        // Then
        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals("testuser", createdUser.getName());
        assertEquals("testuser@example.com", createdUser.getEmail());
        assertEquals(initialCount + 1, userService.getUserCount());
    }

    @Test
    @DisplayName("ユーザーを更新できる")
    void testUpdateUser() {
        // Given
        User updatedUser = User.builder()
            .name("updated")
            .email("updated@example.com")
            .build();

        // When
        Optional<User> result = userService.updateUser("1", updatedUser);

        // Then
        assertTrue(result.isPresent());
        assertEquals("1", result.get().getId());
        assertEquals("updated", result.get().getName());
        assertEquals("updated@example.com", result.get().getEmail());

        // 確認
        Optional<User> confirmedUser = userService.getUserById("1");
        assertTrue(confirmedUser.isPresent());
        assertEquals("updated", confirmedUser.get().getName());
    }

    @Test
    @DisplayName("存在しないユーザーの更新は失敗する")
    void testUpdateUserNotFound() {
        // Given
        User updatedUser = User.builder()
            .name("updated")
            .email("updated@example.com")
            .build();

        // When
        Optional<User> result = userService.updateUser("999", updatedUser);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("ユーザーを削除できる")
    void testDeleteUser() {
        // Given
        int initialCount = userService.getUserCount();

        // When
        boolean deleted = userService.deleteUser("1");

        // Then
        assertTrue(deleted);
        assertEquals(initialCount - 1, userService.getUserCount());
        assertFalse(userService.getUserById("1").isPresent());
    }

    @Test
    @DisplayName("存在しないユーザーの削除は失敗する")
    void testDeleteUserNotFound() {
        // Given
        int initialCount = userService.getUserCount();

        // When
        boolean deleted = userService.deleteUser("999");

        // Then
        assertFalse(deleted);
        assertEquals(initialCount, userService.getUserCount());
    }

    @Test
    @DisplayName("複数のユーザーを連続して作成できる")
    void testCreateMultipleUsers() {
        // Given
        int initialCount = userService.getUserCount();

        // When
        User user1 = userService.createUser(User.builder().name("test1").email("test1@example.com").build());
        User user2 = userService.createUser(User.builder().name("test2").email("test2@example.com").build());
        User user3 = userService.createUser(User.builder().name("test3").email("test3@example.com").build());

        // Then
        assertNotEquals(user1.getId(), user2.getId());
        assertNotEquals(user2.getId(), user3.getId());
        assertEquals(initialCount + 3, userService.getUserCount());
    }
}

