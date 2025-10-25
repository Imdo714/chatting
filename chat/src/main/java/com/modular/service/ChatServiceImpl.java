package com.modular.service;

import com.modular.domain.dto.request.CreateChatRoomRequest;
import com.modular.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;

    @Override
    public void createChatRoom(CreateChatRoomRequest chatRoomRequest) {

    }
}
