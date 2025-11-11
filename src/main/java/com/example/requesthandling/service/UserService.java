package com.example.requesthandling.service;

import com.example.requesthandling.model.User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ユーザー管理サービス
 * インメモリでユーザーデータを管理
 */
@Service
public class UserService {
    
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public UserService() {
        // 初期データを追加
        User user1 = User.builder().id("1").name("user1").email("user1@example.com").build();
        User user2 = User.builder().id("2").name("user2").email("user2@example.com").build();
        User user3 = User.builder().id("3").name("user3").email("user3@example.com").build();
        
        users.put(user1.getId(), user1);
        users.put(user2.getId(), user2);
        users.put(user3.getId(), user3);
        
        idGenerator.set(4); // 次のIDは4から
    }
    
    /**
     * 全ユーザーを取得
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    
    /**
     * IDでユーザーを取得
     */
    public Optional<User> getUserById(String id) {
        return Optional.ofNullable(users.get(id));
    }
    
    /**
     * ユーザーを作成
     */
    public User createUser(User user) {
        String id = String.valueOf(idGenerator.getAndIncrement());
        user.setId(id);
        users.put(id, user);
        return user;
    }
    
    /**
     * ユーザーを更新
     */
    public Optional<User> updateUser(String id, User updatedUser) {
        if (!users.containsKey(id)) {
            return Optional.empty();
        }
        updatedUser.setId(id);
        users.put(id, updatedUser);
        return Optional.of(updatedUser);
    }
    
    /**
     * ユーザーを削除
     */
    public boolean deleteUser(String id) {
        return users.remove(id) != null;
    }
    
    /**
     * ユーザー数を取得
     */
    public int getUserCount() {
        return users.size();
    }
}

