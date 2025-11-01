package com.modular.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.modular.domain.dto.request.SendMessageRequest;
import com.modular.event.ChatMessageReceivedEvent;
import com.modular.redis.RedisMessageBroker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final Map<Long, Set<WebSocketSession>> memberSession = new ConcurrentHashMap<>();
    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RedisMessageBroker redisMessageBroker;
    private final ObjectMapper objectMapper;

    @EventListener
    public void handleRedisChatMessage(ChatMessageReceivedEvent event) {
        SendMessageRequest message = event.getMessage();
        log.info("Received ChatMessageReceivedEvent for room {}", message.getRoomId());
        sendMessageToLocalSessions(message);
    }

    private final String serverRoomsKeyPrefix = "chat:server:rooms:";

    public void addSession(Long memberId, WebSocketSession session){
        log.info("Adding session {} to server", memberId);

        memberSession.computeIfAbsent(memberId, k -> ConcurrentHashMap.newKeySet()) // memberId에 매핑된 Set이 없으면 새 ConcurrentHashMap.newKeySet()을 만들어서 넣어 줌
                .add(session);
    }

    public Boolean isUserOnlineLocally(Long memberId){
        // 사용자의 세션 집합을 가져옴 (없으면 null)
        Set<WebSocketSession> sessions = memberSession.get(memberId);
        if (sessions == null) {
            return false; // 세션 자체가 없으면 오프라인
        }

        // 열려 있는(open) 세션만 필터링
        Set<WebSocketSession> openSessions = sessions.stream()
                .filter(webSocketSession -> webSocketSession.isOpen())
                .collect(Collectors.toSet());

        // 닫힌(closed) 세션이 있다면 세션 집합에서 제거
        if (openSessions.size() != sessions.size()) {
            Set<WebSocketSession> closedSessions = sessions.stream()
                    .filter(session -> !session.isOpen())
                    .collect(Collectors.toSet());

            sessions.removeAll(closedSessions);

            // 세션 집합이 완전히 비었으면 memberSession에서도 제거
            if (sessions.isEmpty()) {
                memberSession.remove(memberId);
            }
        }

        // 열려 있는 세션이 하나라도 있으면 true (온라인 상태)
        return !openSessions.isEmpty();
    }

    public void joinRoom(Long roomId) {
        // ex) chat:server:rooms:server-1761803111895
        String serverRoomKey = serverRoomsKeyPrefix + redisMessageBroker.getServerId();

        // 이미 구독 중인지 Redis Set에서 확인
        // Redis Pub/Sub의 구독은 서버 전체 단위여서 같은 서버에서는 한번만 구독하면 됨
        Boolean wasAlreadySubscribed = stringRedisTemplate.opsForSet().isMember(serverRoomKey, roomId.toString());

        // 아직 구독 안 했다면, RedisMessageBroker 통해 구독
        if (Boolean.FALSE.equals(wasAlreadySubscribed)) {
            redisMessageBroker.subscribeToRoom(roomId);
        }

        // 현재 서버가 구독 중인 roomId를 Redis Set에 추가 (중복은 자동 무시)
        stringRedisTemplate.opsForSet().add(serverRoomKey, roomId.toString());

        log.info("Joined room {} for on server {}", roomId, redisMessageBroker.getServerId());
    }

    public void removeSession(Long memberId, WebSocketSession session) {
        memberSession.remove(memberId);
    }

    public void sendMessageToRoom(SendMessageRequest sendMessage) { // Redis Pub/Sub 에 메시지 발행
        String topic = "chat:room:" + sendMessage.getRoomId();
        log.info("topic = {}", topic);

        sendMessage.updateServerId(redisMessageBroker.getServerId());

        try {
            String messagePayload = objectMapper.writeValueAsString(sendMessage);
            log.info("messagePayload = {}", messagePayload);

            // Redis로 발행!
            stringRedisTemplate.convertAndSend(topic, messagePayload);
            log.info("Published to Redis topic: {}, from server: {}", topic, sendMessage.getServerId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat message", e);
        }
    }

    public void sendMessageToLocalSessions(SendMessageRequest message) {
        log.info("=================== sendMessageToLocalSessions ======================");
        // 예: roomId별로 memberId 목록 관리하고 있다면 그 세션들 찾아서 보내기
        Set<WebSocketSession> sessions = memberSession.get(message.getSenderId()); // 예시

        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                    }
                } catch (IOException e) {
                    log.error("Failed to send WebSocket message to session {}", session.getId(), e);
                }
            }
        }
    }

}
