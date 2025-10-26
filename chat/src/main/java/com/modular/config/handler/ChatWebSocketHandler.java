package com.modular.config.handler;

import com.modular.service.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final WebSocketSessionManager sessionManager;

    /**
     * WebSocket 클라이언트가 서버와의 연결(handshake)을 완료했을 때 한 번 호출됩니다.
     *
     * 사용자 세션을 서버에서 관리할 때 사용
     * 채팅방 참여 등록, 초기 데이터 로드할 때
     * */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long memberId = (Long) session.getAttributes().get("memberId");

        if(memberId != null){ // 연결시도하는 memberId가 있으면 세션에 등록
            sessionManager.addSession(memberId, session);
            log.info("Session Add {}", memberId);
        }
    }

    /**
     * 클라이언트가 메시지를 보낼 때 마다 호출
     *
     * 메시지 파싱 -> 처리 -> 다른 세션으로 전송
     * 채팅, 실시간 알림 등
     * */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {

    }

    /**
     * WebSocket 연결 중 에러 발생 시 호출
     *
     * 로그 기록, 세션 정리, 재연결 처리
     * */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

    }

    /**
     * WebSocket 연결이 종료될 때 호출
     *
     * 세션 해제, 사용자 상태 업데이트
     * */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {

    }

    /**
     * WebSocket 메시지가 분할되어 올 수 있는 여부를 알려줌
     *
     * true -> 메시지가 여러 조각으로 올 수 있음
     * false -> 한 번에 온 메시지만 처리
     * */
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
