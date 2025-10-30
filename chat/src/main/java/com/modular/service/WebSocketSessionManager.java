package com.modular.service;

import com.modular.redis.RedisMessageBroker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

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
        Boolean wasAlreadySubscribed = stringRedisTemplate.opsForSet().isMember(serverRoomKey, roomId.toString());

        // 아직 구독 안 했다면, RedisMessageBroker 통해 구독
        if (Boolean.FALSE.equals(wasAlreadySubscribed)) {
            redisMessageBroker.subscribeToRoom(roomId);
        }

        // 현재 서버가 구독 중인 roomId를 Redis Set에 추가 (중복은 자동 무시)
        stringRedisTemplate.opsForSet().add(serverRoomKey, roomId.toString());
    }
}
