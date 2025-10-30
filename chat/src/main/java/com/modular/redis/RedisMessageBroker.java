package com.modular.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // 채팅 방 구독 메서드
    public void subscribeToRoom(Long roomId) {
        if (subscribedRooms.add(roomId)) {
            ChannelTopic topic = new ChannelTopic("chat.room." + roomId);

            // 2. 'this' (RedisMessageBroker 객체 자신)를 리스너로 등록합니다.
            //    이제 방마다 새 리스너를 만들지 않습니다.
            messageListenerContainer.addMessageListener(this, topic);

            log.info("Subscribed to room {}", roomId);
        } else {
            // (코틀린 코드의 오류를 수정 -> 자바 코드의 올바른 로직 사용)
            log.warn("⚠️ Already subscribed to room {}", roomId);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        // message.getChannel()을 통해 어떤 채널(방)에서 온 메시지인지 구분해야 함!!!
        String channel = new String(message.getChannel()); // ex) "chat.room.123"
        String received = new String(message.getBody());

        log.info("Received message from channel {}: {}", channel, received);

        // TODO: 실제 메시지 처리 로직 구현
    }
}
