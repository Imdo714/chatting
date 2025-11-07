package com.modular.chat;

import com.modular.type.ChatRoomType;
import com.modular.member.Member;
import com.modular.type.MemberRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_room")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long roomId; // 채팅방 아이디

    @Column(name = "chat_room_name")
    private String roomName; // 채팅방 이름

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ChatRoomType type; // 채팅방 타입 (1:1, Group)

    @Column(name = "is_active")
    private Boolean isActive; // 채팅방 활성화 여부 (비활성화 시 사용 불가)

    @Column(name = "max_members")
    private int maxMembers; // 최대  입장 인원수

    @Column(name = "created_at")
    private LocalDateTime createdAt; // 채팅방 생성일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Member createdBy; // 채팅 방 생성자

    @Builder.Default
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatRoomMember> chatRoomMembers = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messageList = new ArrayList<>();

    public static ChatRoom create(Member creator, String roomName, ChatRoomType type, int maxMembers) {
        ChatRoom room = ChatRoom.builder()
                .roomName(roomName)
                .type(type)
                .isActive(true)
                .maxMembers(maxMembers)
                .createdAt(LocalDateTime.now())
                .createdBy(creator)
                .build();

        room.addMember(creator, MemberRole.ADMIN);
        return room;
    }

    public void addMembers(List<Member> members) {
        for (Member member : members) {
            addMember(member, MemberRole.MEMBER);
        }
    }

    private void addMember(Member member, MemberRole role) {
        ChatRoomMember crm = ChatRoomMember.builder()
                .chatRoom(this)
                .member(member)
                .role(role)
                .build();
        this.chatRoomMembers.add(crm);
    }
}
