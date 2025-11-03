package com.modular.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageSequenceService {

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final String prefix = "chat:sequence:";

    public Long getNextSequence(Long roomId){
        String key = prefix + roomId;
        Long sequence = stringRedisTemplate.opsForValue().increment(key);
        
        // 메시지를 처음 보내면 NPE 발생해서 Null이면 1 반환
        return (sequence != null) ? sequence : 1L;
    }

}
