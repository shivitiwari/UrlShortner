package com.systemdesign.url_shortener.service;

import com.systemdesign.url_shortener.constant.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    public boolean isAllowed(String clientIp) {
        String key = "rate_limit:" + clientIp;
        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount == 1) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }

        return currentCount <= Constant.MAX_REQUESTS_PER_MINUTE;
    }
}