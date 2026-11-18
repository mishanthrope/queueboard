package com.mishanthrope.queueboard.order.controller;

import com.mishanthrope.queueboard.common.web.PageResponse;
import com.mishanthrope.queueboard.order.dto.OrderResponse;
import com.mishanthrope.queueboard.order.dto.ShiftPrepTimeRequest;
import com.mishanthrope.queueboard.order.model.OrderStatus;
import com.mishanthrope.queueboard.order.service.OrderService;
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

    private final OrderService orderService;

    @GetMapping("/kitchen")
    public List<OrderResponse> getKitchenQueue() {
        return orderService.getKitchenQueue();
    }

    @GetMapping
    public PageResponse<OrderResponse> getHistory(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return orderService.getHistory(date, status, page, size);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    @PatchMapping("/prep-time")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void shiftPrepTime(@Valid @RequestBody ShiftPrepTimeRequest request) {
        orderService.shiftPrepTime(request.deltaMinutes());
    }

    @PostMapping("/{id}/start")
    public OrderResponse startCooking(@PathVariable Long id) {
        return orderService.startCooking(id);
    }

    @PostMapping("/{id}/ready")
    public OrderResponse markReady(@PathVariable Long id) {
        return orderService.markReady(id);
    }

    @PostMapping("/{id}/issue")
    public OrderResponse issue(@PathVariable Long id) {
        return orderService.issue(id);
    }

    @PostMapping("/{id}/restore")
    public OrderResponse restore(@PathVariable Long id) {
        return orderService.restore(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id) {
        orderService.cancel(id);
    }
}
