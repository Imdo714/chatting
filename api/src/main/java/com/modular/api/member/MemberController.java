package com.modular.api.member;

import com.modular.domain.dto.request.CreateMemberRequest;
import com.modular.domain.dto.request.LoginRequest;
import com.modular.domain.dto.response.MemberDto;
import com.modular.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/register")
    public ResponseEntity<MemberDto> register(@RequestBody CreateMemberRequest userRequest){
        log.info("userRequest = {}", userRequest);
        MemberDto memberDto = memberService.registerUser(userRequest);

        return ResponseEntity.ok(memberDto);
    }

    @PostMapping("/login")
    public ResponseEntity<MemberDto> login(@RequestBody LoginRequest loginRequest){
        MemberDto memberDto = memberService.login(loginRequest);
        return ResponseEntity.ok(memberDto);
    }

}
