package com.modular.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modular.domain.dto.request.SendMessageRequest;
import com.modular.event.ChatMessageReceivedEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisMessageBroker implements MessageListener {
    // 다른 서버에 있는 사람들에게 메시지를 보낼 때 같은 서버는 증복으로 보내지 않게 하기 위해
    @Getter
    private final String serverId = System.getenv("HOSTNAME") != null ? System.getenv("HOSTNAME") : "server-" + System.currentTimeMillis();
    private final Set<Long> subscribedRooms = ConcurrentHashMap.newKeySet();  // 구독 중인 방 ID 목록
    private final RedisMessageListenerContainer messageListenerContainer; // Redis 리스너 컨테이너
    private final ObjectMapper objectMapper;

    private final ApplicationEventPublisher eventPublisher;


    // 채팅 방 구독 메서드
    public void subscribeToRoom(Long roomId) {
        if (subscribedRooms.add(roomId)) {
            ChannelTopic topic = new ChannelTopic("chat:room:" + roomId);

            // 'this' (RedisMessageBroker 객체 자신)를 리스너로 등록합니다.
            //    이제 방마다 새 리스너를 만들지 않습니다.
            messageListenerContainer.addMessageListener(this, topic);

            log.info("Subscribed to room {}", roomId);
        } else {
            log.warn("Already subscribed to room {}", roomId);
        }
    }

    @Override // 구독중인 방에서 메시지를 받을 때
    public void onMessage(Message message, byte[] pattern) {
        log.info("=================== onMessage ======================");
        String channel = new String(message.getChannel()); // ex) "chat.room.123"
        String payload = new String(message.getBody());
        log.info("Received message from channel {}: {}", channel, payload);

        // TODO: 실제 메시지 처리 로직 구현
        try {
            SendMessageRequest sendMessage = objectMapper.readValue(payload, SendMessageRequest.class);

            // 자기 서버에서 발행한 메시지면 무시
            if (sendMessage.getServerId().equals(serverId)) {
                log.debug("Ignoring self-published message from {}", serverId);
                return;
            }

            Long roomId = sendMessage.getRoomId();
            log.info("Received message for room {} from {}", roomId, sendMessage.getServerId());

            eventPublisher.publishEvent(new ChatMessageReceivedEvent(sendMessage));
            log.info("Published ChatMessageReceivedEvent for room {}", sendMessage.getRoomId());

        } catch (Exception e) {
            log.error("Error processing received message from channel {}", channel, e);
        }
    }
}
