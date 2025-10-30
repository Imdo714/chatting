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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MemberRepository memberRepository;
    private final WebSocketSessionManager webSocketSessionManager;

    // 클라이언트가 브라우저에게 WebSocket 연결 시도하면 서버는 HandshakeInterceptor.beforeHandshake()에서 memberId를 세션 attributes에 넣음
    // 연결 완료시 WebSocketHandler.afterConnectionEstablished() 호출 여기서 sessionManager.addSession(userId, session) 등으로 WebSocketSession을 관리

    @Transactional
    @Override
    public void createChatRoom(Long memberId, CreateChatRoomRequest chatRoomRequest) {
        // 방장 정보 조회
        Member creator = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("없는 사람이에요"));

        List<Long> invitedMemberIds = chatRoomRequest.getInvitedMemberIds();

        ChatRoom chatRoom = ChatRoom.builder()
                .roomName(chatRoomRequest.getName())
                .type(chatRoomRequest.getType())
                .maxMembers(chatRoomRequest.getMaxMembers())
                .createdBy(creator)
                .build();

        ChatRoom room = chatRoomRepository.save(chatRoom);// 채팅방 생성

        // 초대된 멤버 엔티티 리스트 한 번에 조회
        List<Member> invitedMembers = memberRepository.findAllById(invitedMemberIds);

        // 요청된 ID 수와 실제 조회된 멤버 수가 다를 경우
        if (invitedMembers.size() != invitedMemberIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 멤버가 포함되어 있습니다.");
        }

        List<ChatRoomMember> chatMembers = new ArrayList<>();

        ChatRoomMember adminMember = ChatRoomMember.builder()
                .chatRoom(room)
                .member(creator)
                .role(MemberRole.ADMIN)
                .build();
        chatMembers.add(adminMember);

        // 초대된 멤버 들을 리스트에 추가
        for (Member invitedMember : invitedMembers) {
            ChatRoomMember userMember = ChatRoomMember.builder()
                    .chatRoom(room)
                    .member(invitedMember)
                    .role(MemberRole.MEMBER) // 일반 참여자
                    .build();
            chatMembers.add(userMember);
        }

        chatRoomMemberRepository.saveAll(chatMembers); // 멤버 채팅방에 넣어주기

        // 생성자 세션 갱신 해주기
        if(webSocketSessionManager.isUserOnlineLocally(memberId)){ // 세션에 연결되어 있는 확인
            webSocketSessionManager.joinRoom(room.getRoomId()); // 생성된 채팅방 구독 하기
        }

        // 초대된 멤버중 온라이라면 토픽 구독
        for (Member invitedMember : invitedMembers) {
            if (webSocketSessionManager.isUserOnlineLocally(invitedMember.getMemberId())) {
                webSocketSessionManager.joinRoom(room.getRoomId());
            }
        }
    }


}
