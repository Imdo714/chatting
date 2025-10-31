package com.modular.service;

import com.modular.domain.dto.request.CreateChatRoomRequest;
import com.modular.domain.dto.response.ChatRoomResponse;
import org.springframework.data.domain.Pageable;

public interface ChatService {
    void createChatRoom(Long memberId, CreateChatRoomRequest chatRoomRequest);

    ChatRoomResponse getChatRooms(Long memberId, Pageable pageable);
}
