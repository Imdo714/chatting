package com.modular.service;

import com.modular.domain.dto.request.CreateChatRoomRequest;

public interface ChatService {
    void createChatRoom(Long memberId, CreateChatRoomRequest chatRoomRequest);
}
