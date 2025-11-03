package com.modular.service;

import com.modular.domain.dto.request.CreateChatRoomRequest;
import com.modular.domain.dto.request.SendMessageRequest;
import com.modular.domain.dto.response.ChatRoomResponse;
import org.springframework.data.domain.Pageable;

public interface ChatService {
    void createChatRoom(Long memberId, CreateChatRoomRequest chatRoomRequest);

    ChatRoomResponse getChatRooms(Long memberId, Pageable pageable);

    void sendMessage(SendMessageRequest sendMessage, Long memberId);
}
