package com.modular.service;

import com.modular.chat.ChatRoom;
import com.modular.chat.ChatRoomMember;
import com.modular.chat.Message;
import com.modular.domain.dto.request.ChatMessage;
import com.modular.redis.RedisMessageBroker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomBroadcastService {

    private final WebSocketSessionManager webSocketSessionManager;
    private final RedisMessageBroker redisMessageBroker;

    public void joinOnlineMembersToRoom(ChatRoom room) {
        Long roomId = room.getRoomId();

        for (ChatRoomMember member : room.getChatRoomMembers()) {
            Long memberId = member.getMember().getMemberId();
            if (webSocketSessionManager.isUserOnlineLocally(memberId)) {
                webSocketSessionManager.joinRoom(roomId);
            }
        }
    }

    public void broadcastMessage(Message message) {
        Long roomId = message.getChatRoom().getRoomId();
        ChatMessage chatMessage = ChatMessage.of(message, redisMessageBroker.getServerId());

        // 로컬 세션 전송 (같은 서버는 바로 전송)
        webSocketSessionManager.sendMessageToLocalRoom(roomId, chatMessage);

        // Redis 브로드캐스트 (같은 서버는 제외)
        redisMessageBroker.broadcastToRoom(roomId, chatMessage);
    }

}
