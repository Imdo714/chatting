package com.modular.chat;

import com.modular.member.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member sendMember; // 보낸 사람

    @Column(name = "content", nullable = false)
    private String content; // 메시지 내용

    @Column(name = "sequence_number")
    private Long sequenceNumber; // 메시지 순서 보장을 위한 번호 (채팅방별로 관리)
}
