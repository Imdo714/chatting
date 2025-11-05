package com.modular.event;

import com.modular.domain.dto.request.ChatMessage;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ChatMessageReceivedEvent extends ApplicationEvent {

    private final Long roomId;
    private final ChatMessage message;

    public ChatMessageReceivedEvent(Object source, Long roomId, ChatMessage message) {
        super(source);
        this.roomId = roomId;
        this.message = message;
    }
}
