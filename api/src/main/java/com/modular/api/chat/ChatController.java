package com.modular.api.chat;

import com.modular.domain.dto.request.CreateChatRoomRequest;
import com.modular.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public void createChatRoom(@RequestBody CreateChatRoomRequest chatRoomRequest){
        chatService.createChatRoom(chatRoomRequest);
    }

}
