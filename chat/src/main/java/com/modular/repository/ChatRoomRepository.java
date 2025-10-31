package com.modular.repository;

import com.modular.chat.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        JOIN ChatRoomMember crm ON cr.roomId = crm.chatRoom.roomId
        WHERE crm.member.memberId = :memberId AND cr.isActive = true
    """)
    Page<ChatRoom> findUserChatRooms(Long memberId, Pageable pageable);

}
