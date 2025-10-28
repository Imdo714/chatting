package com.modular.service;

import com.modular.chat.ChatRoom;
import com.modular.chat.ChatRoomMember;
import com.modular.domain.dto.request.CreateChatRoomRequest;
import com.modular.member.Member;
import com.modular.repository.MemberRepository;
import com.modular.type.MemberRole;
import com.modular.repository.ChatRoomMemberRepository;
import com.modular.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MemberRepository memberRepository;
    private final WebSocketSessionManager webSocketSessionManager;

    // 클라이언트가 브라우저에게 WebSocket 연결 시도하면 서버는 HandshakeInterceptor.beforeHandshake()에서 memberId를 세션 attributes에 넣음
    // 연결 완료시 WebSocketHandler.afterConnectionEstablished() 호출 여기서 sessionManager.addSession(userId, session) 등으로 WebSocketSession을 관리

    // 1. 채팅방 생성 API REST API로 생성만
    // 2. 채팅방 목록 조회 API REST API로 DB에 내가 참여중인 채팅방만 보여주기??
    // 3. 채팅방 입장 시 WebSocket 연결 시작

    // TODO : 채팅방 목록 조회 시 WebSocket 연결 시작 할건지 아니면 채팅방 입장시 WebSocket 연결 할껀지

    @Override
    public void createChatRoom(Long memberId, CreateChatRoomRequest chatRoomRequest) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("없는 사람이에요"));

        ChatRoom chatRoom = ChatRoom.builder()
                .roomName(chatRoomRequest.getName())
                .type(chatRoomRequest.getType())
                .maxMembers(chatRoomRequest.getMaxMembers())
                .createdBy(member)
                .build();

        ChatRoom room = chatRoomRepository.save(chatRoom);// 채팅방 생성

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .chatRoom(room)
                .member(member)
                .role(MemberRole.ADMIN)
                .build();

        chatRoomMemberRepository.save(chatRoomMember); // 멤버 채팅방에 넣어주기

        // 생성자 세션 갱신 해주기
        if(webSocketSessionManager.isUserOnlineLocally(memberId)){ // 세션에 연결되어 있는 확인
            // 채팅방 생성 알림 메시지 전송
            // 생성된 채팅방 구독 하기
        }
    }

}
