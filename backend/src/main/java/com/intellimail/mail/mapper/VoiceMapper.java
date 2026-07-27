package com.intellimail.mail.mapper;

import com.intellimail.mail.dto.voice.VoiceResponse;
import com.intellimail.mail.entity.VoiceInteraction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VoiceMapper {

    VoiceResponse toResponse(VoiceInteraction interaction);
}
