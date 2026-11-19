package com.mishanthrope.queueboard.core.infrastructure.poster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mishanthrope.queueboard.core.application.poster.PosterCheck;
import com.mishanthrope.queueboard.core.application.poster.PosterCheckLine;
import com.mishanthrope.queueboard.core.application.poster.PosterDataError;
import com.mishanthrope.queueboard.core.infrastructure.poster.api.PosterApi;
import com.mishanthrope.queueboard.core.infrastructure.poster.api.dto.PosterTransaction;
import com.mishanthrope.queueboard.core.infrastructure.poster.api.dto.PosterTransactionProduct;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PosterClientAdapterTest {

    private static final long CHECK_ID = 5L;
    private static final long CLOSED_AT_MILLIS = 1_794_999_038_000L;

    @Mock
    private PosterApi posterApi;

    @InjectMocks
    private PosterClientAdapter adapter;

    private static PosterTransaction transaction(
            String status, String payType, String dateClose, String comment) {
        return new PosterTransaction("5", status, payType, dateClose, comment);
    }

    private static PosterTransaction paidTransaction() {
        return transaction("2", "1", String.valueOf(CLOSED_AT_MILLIS), null);
    }

    private static PosterTransactionProduct product(String name, String num, String modifiers) {
        return new PosterTransactionProduct(name, num, modifiers);
    }

    private void givenTransaction(PosterTransaction transaction, PosterTransactionProduct... p) {
        when(posterApi.getTransaction(CHECK_ID)).thenReturn(transaction);
        when(posterApi.getTransactionProducts(CHECK_ID)).thenReturn(List.of(p));
    }

    static Stream<Arguments> notPaidTransactions() {
        return Stream.of(
                Arguments.of("not closed", transaction("1", "1", "1", null)),
                Arguments.of("closed without payment", transaction("2", "0", "1", null)),
                Arguments.of("no pay type", transaction("2", null, "1", null)),
                Arguments.of("no transaction at all", null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("notPaidTransactions")
    void findClosedAndPaidCheck_whenNotClosedAndPaid_returnsEmptyWithoutFetchingProducts(
            String description, PosterTransaction transaction) {
        when(posterApi.getTransaction(CHECK_ID)).thenReturn(transaction);

        assertThat(adapter.findClosedAndPaidCheck(CHECK_ID)).isEmpty();

        verify(posterApi, never()).getTransactionProducts(anyLong());
    }

    @Test
    void findClosedAndPaidCheck_parsesPaidAtCommentAndLines() {
        givenTransaction(
                transaction("2", "1", String.valueOf(CLOSED_AT_MILLIS), "з собою"),
                product("Шаурма", "2", "без цибулі, гострий соус ,"),
                product("Кола", "3.000", null));

        PosterCheck check = adapter.findClosedAndPaidCheck(CHECK_ID).orElseThrow();

        assertThat(check.paidAt()).isEqualTo(Instant.ofEpochMilli(CLOSED_AT_MILLIS));
        assertThat(check.comment()).isEqualTo("з собою");
        assertThat(check.lines()).containsExactly(
                new PosterCheckLine("Шаурма", 2, List.of("без цибулі", "гострий соус")),
                new PosterCheckLine("Кола", 3, List.of()));
    }

    @Test
    void findClosedAndPaidCheck_withBlankComment_returnsNullComment() {
        givenTransaction(
                transaction("2", "1", String.valueOf(CLOSED_AT_MILLIS), "   "),
                product("Шаурма", "1", null));

        assertThat(adapter.findClosedAndPaidCheck(CHECK_ID).orElseThrow().comment()).isNull();
    }

    @Test
    void findClosedAndPaidCheck_withBlankModifierList_returnsNoModifiers() {
        givenTransaction(paidTransaction(), product("Шаурма", "1", " , ,"));

        assertThat(adapter.findClosedAndPaidCheck(CHECK_ID).orElseThrow().lines().get(0)
                .modifiers()).isEmpty();
    }

    @Test
    void findClosedAndPaidCheck_withoutProducts_returnsCheckWithoutLines() {
        when(posterApi.getTransaction(CHECK_ID)).thenReturn(paidTransaction());
        when(posterApi.getTransactionProducts(CHECK_ID)).thenReturn(null);

        assertThat(adapter.findClosedAndPaidCheck(CHECK_ID).orElseThrow().lines()).isEmpty();
    }

    @Test
    void findClosedAndPaidCheck_withoutCloseDate_throwsPosterDataError() {
        givenTransaction(transaction("2", "1", null, null), product("Шаурма", "1", null));

        assertThatThrownBy(() -> adapter.findClosedAndPaidCheck(CHECK_ID))
                .isInstanceOf(PosterDataError.class)
                .hasMessage("Poster check has no date_close");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-5", "12.5", ""})
    void findClosedAndPaidCheck_withInvalidCloseDate_throwsPosterDataError(String dateClose) {
        givenTransaction(transaction("2", "1", dateClose, null), product("Шаурма", "1", null));

        assertThatThrownBy(() -> adapter.findClosedAndPaidCheck(CHECK_ID))
                .isInstanceOf(PosterDataError.class)
                .hasMessage("Invalid date_close: " + dateClose);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", "", "99999999999"})
    void findClosedAndPaidCheck_withInvalidQuantity_throwsPosterDataError(String num) {
        givenTransaction(paidTransaction(), product("Шаурма", num, null));

        assertThatThrownBy(() -> adapter.findClosedAndPaidCheck(CHECK_ID))
                .isInstanceOf(PosterDataError.class)
                .hasMessage("Invalid product quantity: " + num);
    }

    @Test
    void findClosedAndPaidCheck_withoutQuantity_throwsPosterDataError() {
        givenTransaction(paidTransaction(), product("Шаурма", null, null));

        assertThatThrownBy(() -> adapter.findClosedAndPaidCheck(CHECK_ID))
                .isInstanceOf(PosterDataError.class)
                .hasMessage("Poster product has no num");
    }
}
