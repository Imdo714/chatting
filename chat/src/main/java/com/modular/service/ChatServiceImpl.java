package com.modular.service;

import com.modular.chat.ChatRoom;
import com.modular.chat.ChatRoomMember;
import com.modular.chat.Message;
import com.modular.domain.dto.request.ChatMessage;
import com.modular.domain.dto.request.CreateChatRoomRequest;
import com.modular.domain.dto.request.SendMessageRequest;
import com.modular.domain.dto.response.ChatRoomResponse;
import com.modular.member.Member;
import com.modular.redis.RedisMessageBroker;
import com.modular.repository.ChatRoomMemberRepository;
import com.modular.repository.ChatRoomRepository;
import com.modular.repository.MemberRepository;
import com.modular.repository.MessageRepository;
import com.modular.type.MemberRole;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final MessageRepository messageRepository;
    private final WebSocketSessionManager webSocketSessionManager;
    private final MessageSequenceService messageSequenceService;
    private final RedisMessageBroker redisMessageBroker;

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
                .isActive(true)
                .maxMembers(chatRoomRequest.getMaxMembers())
                .createdAt(LocalDateTime.now())
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

    @Override
    public ChatRoomResponse getChatRooms(Long memberId, Pageable pageable) {
        Page<ChatRoom> chatRoomPage = chatRoomRepository.findUserChatRooms(memberId, pageable);
        return ChatRoomResponse.from(chatRoomPage);
    }

    @Override
    @Transactional
    public void sendMessage(SendMessageRequest sendMessage, Long memberId) {
        // 채팅방, 회원, 검증, 현재 유저가 해당 방의 활성 멤버인지 확인
        Long sequenceNumber = messageSequenceService.getNextSequence(sendMessage.getRoomId());

        Message message = Message.builder()
                .chatRoom(getReferenceChatRoomById(sendMessage.getRoomId()))
                .sendMember(getReferenceMemberById(sendMessage.getSenderId()))
                .content(sendMessage.getMessage())
                .sequenceNumber(sequenceNumber)
                .createdAt(LocalDateTime.now())
                .build();
        Message savedMessage = messageRepository.save(message);// 메시지 저장
        log.info("저장된 메시지 ID = {}", savedMessage.getMessageId());

        ChatMessage chatMessage = ChatMessage.builder()
                .messageId(savedMessage.getMessageId())
                .roomId(savedMessage.getChatRoom().getRoomId())
                .content(savedMessage.getContent())
                .senderId(savedMessage.getSendMember().getMemberId())
                .senderName(savedMessage.getSendMember().getName())
                .sequenceNumber(savedMessage.getSequenceNumber())
                .build();

        // 로컬 세션에 즉시 전송 (실시간 응답성 보장)
        webSocketSessionManager.sendMessageToLocalRoom(sendMessage.getRoomId(), chatMessage);

        // 다른 서버에 Redis 을 이용해 브로드캐스팅 전달 (같은 서버는 제외)
        redisMessageBroker.broadcastToRoom(sendMessage.getRoomId(), chatMessage, redisMessageBroker.getServerId());
    }

    private ChatRoom getReferenceChatRoomById(Long roomId) {
        try {
            return chatRoomRepository.getReferenceById(roomId);
        } catch (EntityNotFoundException e) {
            throw new IllegalArgumentException("채팅방 없음");
        }
    }

    private Member getReferenceMemberById(Long memberId) {
        try {
            return memberRepository.getReferenceById(memberId);
        } catch (EntityNotFoundException e) {
            throw new IllegalArgumentException("멤버 없음");
        }
    }


}
