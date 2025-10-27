package com.modular.domain.dto.request;

import com.modular.type.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateChatRoomRequest {

    private String name;
    private ChatRoomType type;
    private int maxMembers;

}
