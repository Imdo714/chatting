package com.modular.api.member;

import com.modular.domain.dto.request.CreateMemberRequest;
import com.modular.domain.dto.response.MemberDto;
import com.modular.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/user")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/register")
    public ResponseEntity<MemberDto> register(@RequestBody CreateMemberRequest userRequest){
        log.info("userRequest = {}", userRequest);
        MemberDto memberDto = memberService.registerUser(userRequest);

        return ResponseEntity.ok(memberDto);
    }

}
