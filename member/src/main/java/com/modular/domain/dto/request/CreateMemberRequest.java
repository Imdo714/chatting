package com.modular.domain.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateMemberRequest {

    private String email;
    private String password;
    private String name;

}
