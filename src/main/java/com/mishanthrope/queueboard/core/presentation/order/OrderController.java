package com.mishanthrope.queueboard.core.presentation.order;

import com.mishanthrope.queueboard.core.application.common.PageView;
import com.mishanthrope.queueboard.core.application.order.command.CancelOrder;
import com.mishanthrope.queueboard.core.application.order.command.CancelOrderHandler;
import com.mishanthrope.queueboard.core.application.order.command.IssueOrder;
import com.mishanthrope.queueboard.core.application.order.command.IssueOrderHandler;
import com.mishanthrope.queueboard.core.application.order.command.MarkReady;
import com.mishanthrope.queueboard.core.application.order.command.MarkReadyHandler;
import com.mishanthrope.queueboard.core.application.order.command.RestoreOrder;
import com.mishanthrope.queueboard.core.application.order.command.RestoreOrderHandler;
import com.mishanthrope.queueboard.core.application.order.command.ShiftPrepTime;
import com.mishanthrope.queueboard.core.application.order.command.ShiftPrepTimeHandler;
import com.mishanthrope.queueboard.core.application.order.command.StartCooking;
import com.mishanthrope.queueboard.core.application.order.command.StartCookingHandler;
import com.mishanthrope.queueboard.core.application.order.query.GetKitchenQueue;
import com.mishanthrope.queueboard.core.application.order.query.GetKitchenQueueHandler;
import com.mishanthrope.queueboard.core.application.order.query.GetOrderDetails;
import com.mishanthrope.queueboard.core.application.order.query.GetOrderDetailsHandler;
import com.mishanthrope.queueboard.core.application.order.query.GetOrderHistory;
import com.mishanthrope.queueboard.core.application.order.query.GetOrderHistoryHandler;
import com.mishanthrope.queueboard.core.application.order.query.OrderView;
import com.mishanthrope.queueboard.core.domain.order.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final GetKitchenQueueHandler getKitchenQueue;
    private final GetOrderDetailsHandler getOrderDetails;
    private final GetOrderHistoryHandler getOrderHistory;
    private final StartCookingHandler startCooking;
    private final MarkReadyHandler markReady;
    private final IssueOrderHandler issueOrder;
    private final RestoreOrderHandler restoreOrder;
    private final CancelOrderHandler cancelOrder;
    private final ShiftPrepTimeHandler shiftPrepTime;

    @GetMapping("/kitchen")
    public List<OrderView> getKitchenQueue() {
        return getKitchenQueue.handle(new GetKitchenQueue());
    }

    @GetMapping
    public PageView<OrderView> getHistory(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return getOrderHistory.handle(new GetOrderHistory(date, status, page, size));
    }

    @GetMapping("/{id}")
    public OrderView getOrder(@PathVariable long id) {
        return getOrderDetails.handle(new GetOrderDetails(id));
    }

    @PatchMapping("/prep-time")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void shiftPrepTime(@Valid @RequestBody ShiftPrepTimeRequest request) {
        shiftPrepTime.handle(new ShiftPrepTime(request.deltaMinutes()));
    }

    @PostMapping("/{id}/start")
    public OrderView startCooking(@PathVariable long id) {
        startCooking.handle(new StartCooking(id));
        return getOrderDetails.handle(new GetOrderDetails(id));
    }

    @PostMapping("/{id}/ready")
    public OrderView markReady(@PathVariable long id) {
        markReady.handle(new MarkReady(id));
        return getOrderDetails.handle(new GetOrderDetails(id));
    }

    @PostMapping("/{id}/issue")
    public OrderView issue(@PathVariable long id) {
        issueOrder.handle(new IssueOrder(id));
        return getOrderDetails.handle(new GetOrderDetails(id));
    }

    @PostMapping("/{id}/restore")
    public OrderView restore(@PathVariable long id) {
        restoreOrder.handle(new RestoreOrder(id));
        return getOrderDetails.handle(new GetOrderDetails(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable long id) {
        cancelOrder.handle(new CancelOrder(id));
    }
}
