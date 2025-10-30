package com.modular.domain.dto.request;

import com.modular.type.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CreateChatRoomRequest {

    private String name;
    private ChatRoomType type;
    private int maxMembers;
    private List<Long> invitedMemberIds;
}
