package com.modular.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MemberRole {

    ADMIN("관리자"),
    MEMBER("회원")
    ;

    private final String text;
}
