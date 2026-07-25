package com.intellimail.mail.mapper;

import com.intellimail.mail.dto.template.PromptTemplateRequest;
import com.intellimail.mail.dto.template.PromptTemplateResponse;
import com.intellimail.mail.entity.PromptTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PromptTemplateMapper {

    // Reading the entity via its Lombok-generated getter isPublic()/setPublic() resolves,
    // by JavaBeans Introspector convention, to bean property "public" (not "isPublic") -
    // bridge that explicitly here.
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "isPublic", source = "public")
    PromptTemplateResponse toResponse(PromptTemplate entity);

    // PromptTemplate has @SuperBuilder, so MapStruct builds it through
    // PromptTemplate.builder() rather than a setter-based constructor. Lombok's builder
    // method for this field is named after the field itself - isPublic(boolean) - not
    // through the Introspector's "public" bean-property renaming, so no @Mapping override
    // is needed here (unlike toResponse/updateEntityFromRequest, which go through plain
    // getters/setters and do need one).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "owner", ignore = true)
    PromptTemplate toEntity(PromptTemplateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "public", source = "isPublic")
    void updateEntityFromRequest(PromptTemplateRequest request, @MappingTarget PromptTemplate entity);
}
