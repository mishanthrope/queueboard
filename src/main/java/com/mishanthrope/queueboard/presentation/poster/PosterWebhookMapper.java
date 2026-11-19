package com.mishanthrope.queueboard.presentation.poster;

import com.mishanthrope.queueboard.application.poster.PosterWebhookNotice;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PosterWebhookMapper {

    PosterWebhookNotice toNotice(PosterWebhookRequest request);
}
