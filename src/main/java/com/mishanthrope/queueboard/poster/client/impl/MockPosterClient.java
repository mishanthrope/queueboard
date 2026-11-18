package com.mishanthrope.queueboard.poster.client.impl;

import com.mishanthrope.queueboard.poster.client.PosterClient;
import com.mishanthrope.queueboard.poster.dto.PosterTransaction;
import com.mishanthrope.queueboard.poster.dto.PosterTransactionProduct;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MockPosterClient implements PosterClient {

    private static final String STATUS_CLOSED = "2";
    private static final String PAY_TYPE_CARD = "1";

    private static final List<String> MENU = List.of(
            "Шаурма класична",
            "Шаурма сирна",
            "Шаурма з куркою гриль",
            "Шаурма вегетаріанська",
            "Фалафель у піті",
            "Картопля фрі",
            "Кола 0.5",
            "Айран");

    private static final List<String> MODIFIERS = List.of(
            "без цибулі",
            "гострий соус",
            "часниковий соус",
            "подвійне м'ясо",
            "без капусти",
            "сир",
            "халапеньйо");

    private static final List<String> COMMENTS = List.of(
            "з собою",
            "на місці",
            "окремо соус",
            "розрізати навпіл");

    private final Clock clock;

    @Override
    public PosterTransaction getTransaction(long transactionId) {
        var random = new Random(transactionId);
        long closedSecondsAgo = 3 + random.nextInt(5);
        String dateClose = String.valueOf(
                clock.instant().minusSeconds(closedSecondsAgo).toEpochMilli());
        boolean hasComment = random.nextInt(3) == 0;
        String comment = COMMENTS.get(random.nextInt(COMMENTS.size()));
        return new PosterTransaction(
                String.valueOf(transactionId),
                STATUS_CLOSED,
                PAY_TYPE_CARD,
                dateClose,
                hasComment ? comment : null);
    }

    @Override
    public List<PosterTransactionProduct> getTransactionProducts(long transactionId) {
        var random = new Random(transactionId);
        int count = 1 + random.nextInt(3);
        var products = new ArrayList<PosterTransactionProduct>();
        for (int i = 0; i < count; i++) {
            products.add(new PosterTransactionProduct(
                    MENU.get(random.nextInt(MENU.size())),
                    String.valueOf(1 + random.nextInt(3)),
                    randomModificators(random)));
        }
        return products;
    }

    private static String randomModificators(Random random) {
        int count = random.nextInt(3);
        if (count == 0) {
            return null;
        }
        List<String> shuffled = new ArrayList<>(MODIFIERS);
        Collections.shuffle(shuffled, random);
        return String.join(", ", shuffled.subList(0, count));
    }
}
