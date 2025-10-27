package com.modular.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatRoomType {

    DIRECT("1:1 채팅"),
    GROUP("그룹 채팅"),
    CHANNEL("공개 채팅")
    ;

    private final String text;
}
