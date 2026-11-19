package com.mishanthrope.queueboard.core.application.poster;

import java.util.Optional;

public interface PosterClient {

    Optional<PosterCheck> findClosedAndPaidCheck(long checkId);
}
