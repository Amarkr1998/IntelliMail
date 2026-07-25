package com.intellimail.mail.mapper;

import com.intellimail.mail.dto.template.PromptTemplateRequest;
import com.intellimail.mail.dto.template.PromptTemplateResponse;
import com.intellimail.mail.entity.PromptTemplate;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.RequestType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplateMapperTest {

    private final PromptTemplateMapper mapper = Mappers.getMapper(PromptTemplateMapper.class);

    @Test
    void toEntity_bridgesIsPublicToPublicBeanProperty() {
        PromptTemplateRequest request = new PromptTemplateRequest(
                "Cold Outreach v1", "Starter template", RequestType.COLD_OUTREACH,
                "Write a cold outreach email about {{topic}}", "Be concise and persuasive.", true);

        PromptTemplate entity = mapper.toEntity(request);

        assertThat(entity.isPublic()).isTrue();
        assertThat(entity.getName()).isEqualTo("Cold Outreach v1");
        assertThat(entity.getCategory()).isEqualTo(RequestType.COLD_OUTREACH);
        assertThat(entity.getId()).isNull();
    }

    @Test
    void toResponse_exposesOwnerIdAndIsPublicFlag() {
        User owner = User.builder().fullName("Owner").email("owner@intellimail.com").password("hashed").build();
        PromptTemplate entity = PromptTemplate.builder()
                .name("Thank You Note")
                .category(RequestType.THANK_YOU)
                .promptText("Write a thank-you email for {{context}}")
                .owner(owner)
                .isPublic(false)
                .build();

        PromptTemplateResponse response = mapper.toResponse(entity);

        assertThat(response.isPublic()).isFalse();
        assertThat(response.ownerId()).isEqualTo(owner.getId());
    }
}
