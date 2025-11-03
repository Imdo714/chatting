package com.modular.config.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modular.domain.dto.request.SendMessageRequest;
import com.modular.domain.dto.response.ChatRoomResponse;
import com.modular.service.ChatService;
import com.modular.service.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;
    private final ChatService chatService;

    /**
     * WebSocket 클라이언트가 서버와의 연결(handshake)을 완료했을 때 한 번 호출됩니다.
     *
     * 사용자 세션을 서버에서 관리할 때 사용
     * 채팅방 참여 등록, 초기 데이터 로드할 때
     * */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long memberId = getMemberIdSession(session);
        log.info("memberId = {}", memberId);

        if(memberId != null){ // 연결시도하는 memberId가 있으면 세션에 등록
            sessionManager.addSession(memberId, session);
            log.info("Session Add = {}", memberId);

            try{
                // 관련된 채팅방 목록 가져와서 구독하기
                getRoomsJoin(memberId, session);
            }catch (Exception e){
                log.info("error = {}", e);
            }
        }
    }

    private void getRoomsJoin(Long memberId, WebSocketSession session) {
        // 내가 참여중인 채팅방 리스트
        ChatRoomResponse chatRooms = chatService.getChatRooms(memberId, PageRequest.of(0, 10));

        chatRooms.getContent().forEach(room -> {
            sessionManager.joinRoom(room.getRoomId()); // 현재 서버가 채팅방 구독하기
            sessionManager.addRoomIdSession(room.getRoomId(), session);
        });

        log.info("Loaded Chat Rooms size = {}", chatRooms.getContent().size());
    }

    /**
     * 클라이언트가 메시지를 보낼 때 마다 호출
     *
     * 메시지 파싱 -> 처리 -> 다른 세션으로 전송
     * 채팅, 실시간 알림 등
     * */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        Long memberId = getMemberIdSession(session);

        String payload = (String) message.getPayload();
        log.info("received message : {}", payload);

        try {
            // payload를 ChatMessage DTO로 변환
            SendMessageRequest sendMessage = objectMapper.readValue(payload, SendMessageRequest.class);
            log.info("sendMessage = {}", sendMessage);

            // 메시지 전송 및 DB 저장
            chatService.sendMessage(sendMessage, memberId);


            // 메시지 발행
//            sessionManager.sendMessageToRoom(sendMessage);
            // 같은 서버의 참여자에게 즉시 전송
//            sessionManager.sendMessageToLocalSessions(sendMessage);
            // TODO : 그러면 같은 서버이면 전송하고 다른 서버에면 onMessage 를 이용해 Pub/Sub 을 통해 메시지 전달

        } catch (Exception e) {
            log.error("Error handling message: {}", payload, e);
        }
    }

    private static Long getMemberIdSession(WebSocketSession session) {
         return (Long) session.getAttributes().get("memberId");
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
        log.info("session 연결 종료 = {}", session);
        Long memberId = getMemberIdSession(session);
        sessionManager.removeSession(memberId, session);
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
