package com.modular.service;

import com.modular.domain.dto.request.CreateMemberRequest;
import com.modular.domain.dto.response.MemberDto;
import com.modular.member.Member;
import com.modular.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    @Override
    public MemberDto registerUser(CreateMemberRequest memberRequest) {
        Member build = Member.builder()
                .email(memberRequest.getEmail())
                .password(memberRequest.getPassword())
                .name(memberRequest.getName())
                .build();

        Member save = memberRepository.save(build);

        return MemberDto.builder()
                .id(save.getMemberId())
                .name(save.getName())
                .build();
    }

}
