package com.modular.service;

import com.modular.chat.ChatRoom;
import com.modular.chat.ChatRoomMember;
import com.modular.chat.Message;
import com.modular.domain.dto.request.CreateChatRoomRequest;
import com.modular.domain.dto.request.SendMessageRequest;
import com.modular.domain.dto.response.ChatRoomResponse;
import com.modular.member.Member;
import com.modular.repository.ChatRoomMemberRepository;
import com.modular.repository.ChatRoomRepository;
import com.modular.repository.MemberRepository;
import com.modular.repository.MessageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final ChatRoomBroadcastService chatRoomBroadcastService;
    private final MessageSequenceService messageSequenceService;

    @Transactional
    @Override
    public void createChatRoom(Long memberId, CreateChatRoomRequest request) {
        // 방장 조회
        Member creator = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("없는 사람이에요"));

        // 초대 멤버 조회 및 검증
        List<Member> invitedMembers = memberRepository.findAllById(request.getInvitedMemberIds());
        if (invitedMembers.size() != request.getInvitedMemberIds().size()) {
            throw new IllegalArgumentException("존재하지 않는 멤버가 포함되어 있습니다.");
        }

        ChatRoom chatRoom = ChatRoom.create(creator, request.getName(), request.getType(), request.getMaxMembers());
        chatRoom.addMembers(invitedMembers);
        chatRoomRepository.save(chatRoom);

        // 세션 관련 로직은 별도 서비스로 이동 (또는 이벤트 발행)
        chatRoomBroadcastService.joinOnlineMembersToRoom(chatRoom);
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

        ChatRoom room = getReferenceChatRoomById(sendMessage.getRoomId());
        Member sender = getReferenceMemberById(sendMessage.getSenderId());

        Message message = Message.create(room, sender, sendMessage.getMessage(), sequenceNumber);
        Message savedMessage = messageRepository.save(message);// 메시지 저장
        log.info("저장된 메시지 ID = {}", savedMessage.getMessageId());

        chatRoomBroadcastService.broadcastMessage(savedMessage);
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
