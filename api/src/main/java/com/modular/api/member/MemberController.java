package com.modular.api.member;

import com.modular.domain.dto.request.CreateMemberRequest;
import com.modular.domain.dto.response.MemberDto;
import com.modular.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/register")
    public ResponseEntity<MemberDto> register(@RequestBody CreateMemberRequest userRequest){
        MemberDto memberDto = memberService.registerUser(userRequest);

        return ResponseEntity.ok(memberDto);
    }

}
