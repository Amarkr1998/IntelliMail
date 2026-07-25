package com.intellimail.mail.mapper;

import com.intellimail.mail.dto.email.EmailHistoryResponse;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.entity.EmailRequest;
import com.intellimail.mail.entity.GeneratedReply;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmailMapper {

    @Mapping(target = "emailRequestId", source = "emailRequest.id")
    @Mapping(target = "requestType", source = "emailRequest.requestType")
    EmailReplyResponse toReplyResponse(GeneratedReply reply);

    List<EmailReplyResponse> toReplyResponses(List<GeneratedReply> replies);

    @Mapping(target = "replies", source = "replies")
    EmailHistoryResponse toHistoryResponse(EmailRequest emailRequest, List<GeneratedReply> replies);
}
