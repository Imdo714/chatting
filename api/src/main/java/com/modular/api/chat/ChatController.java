package com.modular.api.chat;

import com.modular.domain.dto.request.CreateChatRoomRequest;
import com.modular.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/{memberId}")
    public void createChatRoom(
            @PathVariable Long memberId,
            @RequestBody CreateChatRoomRequest chatRoomRequest
    ) {
        chatService.createChatRoom(memberId, chatRoomRequest);
    }

}
