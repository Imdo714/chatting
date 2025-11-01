package com.modular.repository;

import com.modular.chat.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        JOIN ChatRoomMember crm ON cr.roomId = crm.chatRoom.roomId
        WHERE crm.member.memberId = :memberId AND cr.isActive = true
    """)
    Page<ChatRoom> findUserChatRooms(Long memberId, Pageable pageable);

    @Query("SELECT r FROM ChatRoom r LEFT JOIN FETCH r.createdBy m WHERE r.roomId = :roomId")
    Optional<ChatRoom> findRoomWithMembersById(Long roomId);
}
