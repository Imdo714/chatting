package com.modular.domain.dto.request;

import com.modular.chat.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ChatMessage {

    private Long messageId;
    private Long roomId;
    private String content;
    private Long senderId;
    private String senderName;
    private Long sequenceNumber;
    private String serverId;

    public static ChatMessage of(Message message, String serverId){
        return ChatMessage.builder()
                .messageId(message.getMessageId())
                .roomId(message.getChatRoom().getRoomId())
                .content(message.getContent())
                .senderId(message.getSendMember().getMemberId())
                .senderName(message.getSendMember().getName())
                .sequenceNumber(message.getSequenceNumber())
                .serverId(serverId)
                .build();
    }


}
