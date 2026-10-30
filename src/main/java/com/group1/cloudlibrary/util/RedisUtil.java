package com.group1.cloudlibrary.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 操作工具类
 * <p>所有操作均做异常兜底：当 Redis 未启动时不影响业务主流程（自动降级）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.warn("Redis 写入失败(已降级): key={}, err={}", key, e.getMessage());
        }
    }

    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.warn("Redis 写入失败(已降级): key={}, err={}", key, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis 读取失败(已降级): key={}, err={}", key, e.getMessage());
            return null;
        }
    }

    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            return false;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis 删除失败(已降级): key={}, err={}", key, e.getMessage());
        }
    }

    public void expire(String key, long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(key, timeout, unit);
        } catch (Exception ignored) {
        }
    }

    /** 自增计数，用于统计搜索次数等 */
    public Long increment(String key, long delta, long timeout, TimeUnit unit) {
        try {
            Long value = redisTemplate.opsForValue().increment(key, delta);
            if (value != null && value == delta) {
                redisTemplate.expire(key, timeout, unit);
            }
            return value;
        } catch (Exception e) {
            return null;
        }
    }
}
