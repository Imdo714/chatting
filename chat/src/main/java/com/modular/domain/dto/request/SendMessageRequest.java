package com.modular.domain.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageRequest {

    private Long roomId;
    private Long senderId;
    private String message;
}
