package com.modular.domain.dto.response;

import com.modular.chat.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ChatRoomResponse {
    private List<ChatRoomInfo> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ChatRoomInfo {
        private Long roomId;
        private String roomName;
        private String type;
        private Boolean isActive;
        private int maxMembers;
        private LocalDateTime createdAt;
        private String createdByName;
        private int memberCount;

        public static ChatRoomInfo from(ChatRoom chatRoom) {
            return ChatRoomInfo.builder()
                    .roomId(chatRoom.getRoomId())
                    .roomName(chatRoom.getRoomName())
                    .type(chatRoom.getType().name())
                    .isActive(chatRoom.getIsActive())
                    .maxMembers(chatRoom.getMaxMembers())
                    .createdAt(chatRoom.getCreatedAt())
                    .createdByName(chatRoom.getCreatedBy().getName())
                    .memberCount(chatRoom.getChatRoomMembers().size())
                    .build();
        }
    }

    public static ChatRoomResponse from(Page<ChatRoom> chatRoomPage) {
        return ChatRoomResponse.builder()
                .content(chatRoomPage.getContent().stream()
                        .map(ChatRoomInfo::from)
                        .toList())
                .page(chatRoomPage.getNumber())
                .size(chatRoomPage.getSize())
                .totalElements(chatRoomPage.getTotalElements())
                .totalPages(chatRoomPage.getTotalPages())
                .last(chatRoomPage.isLast())
                .build();
    }

}
