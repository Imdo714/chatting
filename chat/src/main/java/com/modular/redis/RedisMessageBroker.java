package com.modular.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modular.domain.dto.request.ChatMessage;
import com.modular.event.ChatMessageReceivedEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisMessageBroker implements MessageListener {

    @Getter // 다른 서버에 있는 사람들에게 메시지를 보낼 때 같은 서버는 증복으로 보내지 않게 하기 위해
    private final String serverId = System.getenv("HOSTNAME") != null ? System.getenv("HOSTNAME") : "server-" + System.currentTimeMillis();
    private final ConcurrentHashMap<Long, Long> processedMessages = new ConcurrentHashMap<>(); // 메시지 증복 처리를 위해 처리한 메시지 ID 저장
    private final Set<Long> subscribedRooms = ConcurrentHashMap.newKeySet();  // 구독 중인 방 ID 목록
    private final RedisMessageListenerContainer messageListenerContainer; // Redis 리스너 컨테이너
    private final RedisTemplate<String, String> stringRedisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private static final int MAX_PROCESSED_MESSAGES_SIZE = 100; // processedMessages 를 100씩만 담아주기 위해

    // 채팅 방 구독 메서드
    public void subscribeToRoom(Long roomId) {
        if (subscribedRooms.add(roomId)) {
            ChannelTopic topic = new ChannelTopic("chat:room:" + roomId);

            // 'this' (RedisMessageBroker 객체 자신)를 리스너로 등록합니다.
            messageListenerContainer.addMessageListener(this, topic);
            log.info("Subscribed to room {}", roomId);
        } else {
            log.warn("Already subscribed to room {}", roomId);
        }
    }
    @Override // 구독중인 방에서 메시지를 받을 때
    public void onMessage(Message message, byte[] pattern) {
        log.info("=================== onMessage ======================");
        try {
            String payload = new String(message.getBody());

            ChatMessage sendMessage = objectMapper.readValue(payload, ChatMessage.class);

            if(sendMessage.getServerId().equals(this.serverId)){
                log.info("같은 서버여서 패스하겠습니다. = {}", sendMessage.getServerId());
                return;
            }

            // ID가 Map 에 없으면 현재 시간을 저장 그리고 Null 반환, 만약 ID가 있으면 원래있던 Value 값 반환
            Long previousValue = processedMessages.putIfAbsent(sendMessage.getMessageId(), System.currentTimeMillis());
            if (previousValue != null) {
                log.info("이미 처리한 메시지입니다. (putIfAbsent) = {}", sendMessage.getMessageId());
                return;
            }

            eventPublisher.publishEvent( // 이벤트를 발행 해 웹소켓으로 메시지 보내기
                    new ChatMessageReceivedEvent(this, sendMessage.getRoomId(), sendMessage)
            );

            evictOldMessages(); // processedMessages 정리 로직
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void evictOldMessages() { // 임계값 초과 시, 가장 오래된 메시지 ID부터 제거하여 최신 100개만 유지
        if (processedMessages.size() > MAX_PROCESSED_MESSAGES_SIZE) {
            int removalCount = processedMessages.size() - MAX_PROCESSED_MESSAGES_SIZE;

            List<Long> keysToRemove = processedMessages.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue()) // 타임스탬프 기준 정렬 (오래된 순)
                    .map(Map.Entry::getKey)
                    .limit(removalCount)
                    .toList();

            keysToRemove.forEach(processedMessages::remove);
            log.info("오래된 메시지 ID {}개를 정리합니다. 대상 ID: {}", removalCount, keysToRemove);
        }
    }

    public void broadcastToRoom(Long roomId, ChatMessage chatMessage) { // 구독중인 채널에 메시지 전달
        String topic = "chat:room:" + roomId;

        try {
            String messagePayload = objectMapper.writeValueAsString(chatMessage);
            log.info("messagePayload = {}", messagePayload);

            // Redis 로 발행!
            stringRedisTemplate.convertAndSend(topic, messagePayload);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
