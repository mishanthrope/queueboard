package com.mishanthrope.queueboard.application.poster;

import java.util.Optional;

public interface PosterClient {

    Optional<PosterCheck> findClosedAndPaidCheck(long checkId);
}
