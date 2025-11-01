package com.modular.event;

import com.modular.domain.dto.request.SendMessageRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatMessageReceivedEvent {
    private final SendMessageRequest message;
}
