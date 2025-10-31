package com.modular.api.chat;

import com.modular.domain.dto.request.CreateChatRoomRequest;
import com.modular.domain.dto.response.ChatRoomResponse;
import com.modular.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public ResponseEntity<ChatRoomResponse> getChatRooms(
            @RequestParam Long memberId,
            @PageableDefault(size = 5) Pageable pageable
    ){
        return ResponseEntity.ok(chatService.getChatRooms(memberId, pageable));
    }
}
