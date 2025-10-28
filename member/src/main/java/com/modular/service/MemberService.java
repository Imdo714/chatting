package com.modular.service;


import com.modular.domain.dto.request.CreateMemberRequest;
import com.modular.domain.dto.request.LoginRequest;
import com.modular.domain.dto.response.MemberDto;

public interface MemberService {
    MemberDto registerUser(CreateMemberRequest userRequest);

    MemberDto login(LoginRequest loginRequest);
}
