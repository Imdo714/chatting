package com.modular.domain.dto.request;

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

}
