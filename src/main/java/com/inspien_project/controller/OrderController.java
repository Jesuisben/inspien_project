package com.inspien_project.controller;

import java.util.Map;
import java.util.UUID;
import com.inspien_project.service.OrderService;
import com.inspien_project.config.HttpLogFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    // XML POST 요청을 이 메서드에 연결하고 결과는 JSON으로 반환
    @PostMapping(value = "/orders", consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody byte[] xml, HttpServletRequest request) {
        String requestId = (String) request.getAttribute(HttpLogFilter.requestIdAttribute);
        if (requestId == null) requestId = UUID.randomUUID().toString();
        try {
            var result = orderService.createOrder(requestId, xml);
            return ResponseEntity.ok(Map.of("success", true, "requestId", requestId,
                    "status", result.getRejectedCount() > 0 ? "PARTIAL_SUCCESS" : "SUCCESS",
                    "totalCount", result.getTotalCount(), "acceptedCount", result.getOrderIds().size(),
                    "rejectedCount", result.getRejectedCount(), "emptyHeaderCount", result.getEmptyHeaderCount(),
                    "orderIds", result.getOrderIds(), "receiptFile", result.getReceiptFile()));
        } catch (IllegalArgumentException error) {
            log.warn("requestId={} stage=XML_VALIDATE result=FAILURE", requestId);
            return ResponseEntity.badRequest().body(Map.of("success", false,
                    "requestId", requestId, "code", "INVALID_XML", "message", error.getMessage()));
        } catch (OrderService.ProcessingFailure error) {
            return ResponseEntity.status(502).body(Map.of("success", false, "requestId", requestId,
                    "code", "RECONCILIATION_REQUIRED", "orderIds", error.getOrderIds(),
                    "receiptFile", error.getReceiptFile(), "message", error.getMessage()));
        }
    }
}
