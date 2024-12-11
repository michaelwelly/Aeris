package com.aeris.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RedisMenuService {

    private final HashOperations<String, String, String> hashOperations;

    @Autowired
    public RedisMenuService(RedisTemplate<String, String> redisTemplate) {
        this.hashOperations = redisTemplate.opsForHash();
    }

    public void saveMenu(String menuKey, Map<String, String> menuData) {
        hashOperations.putAll(menuKey, menuData);
    }

    public Map<String, String> getMenu(String menuKey) {
        return hashOperations.entries(menuKey);
    }
}