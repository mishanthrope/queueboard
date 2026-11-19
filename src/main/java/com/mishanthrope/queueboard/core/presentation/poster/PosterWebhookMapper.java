package com.mishanthrope.queueboard.core.presentation.poster;

import com.mishanthrope.queueboard.core.application.poster.command.ProcessPosterWebhook;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PosterWebhookMapper {

    ProcessPosterWebhook toCommand(PosterWebhookRequest request);
}
