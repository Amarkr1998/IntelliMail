package com.intellimail.mail.mapper;

import com.intellimail.mail.dto.email.EmailHistoryResponse;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.entity.EmailRequest;
import com.intellimail.mail.entity.GeneratedReply;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.RequestType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmailMapperTest {

    private final EmailMapper emailMapper = Mappers.getMapper(EmailMapper.class);

    private EmailRequest sampleRequest() {
        User user = User.builder().fullName("User").email("user@intellimail.com").password("hashed").build();
        return EmailRequest.builder()
                .user(user)
                .requestType(RequestType.FOLLOWUP)
                .originalContent("Following up on last week's proposal")
                .build();
    }

    @Test
    void toReplyResponse_pullsRequestIdAndTypeFromNestedEmailRequest() {
        EmailRequest request = sampleRequest();
        GeneratedReply reply = GeneratedReply.builder()
                .emailRequest(request)
                .content("Hi, just checking in on the proposal...")
                .aiModel("gpt-4o")
                .attemptNumber(1)
                .build();

        EmailReplyResponse response = emailMapper.toReplyResponse(reply);

        assertThat(response.emailRequestId()).isEqualTo(request.getId());
        assertThat(response.requestType()).isEqualTo(RequestType.FOLLOWUP);
        assertThat(response.content()).contains("checking in");
    }

    @Test
    void toHistoryResponse_assemblesHeaderAndReplyListFromTwoSources() {
        EmailRequest request = sampleRequest();
        GeneratedReply reply1 = GeneratedReply.builder().emailRequest(request).content("v1").attemptNumber(1).build();
        GeneratedReply reply2 = GeneratedReply.builder().emailRequest(request).content("v2").attemptNumber(2).build();

        EmailHistoryResponse history = emailMapper.toHistoryResponse(request, List.of(reply1, reply2));

        assertThat(history.id()).isEqualTo(request.getId());
        assertThat(history.requestType()).isEqualTo(RequestType.FOLLOWUP);
        assertThat(history.replies()).hasSize(2);
        assertThat(history.replies().get(1).content()).isEqualTo("v2");
    }
}
