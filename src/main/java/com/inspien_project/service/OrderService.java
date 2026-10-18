package com.inspien_project.service;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import com.inspien_project.common.EnvSettings;
import com.inspien_project.repository.OrderRepository;
import com.inspien_project.common.IdGenerator;
import com.inspien_project.mapper.OrderXmlMapper;
import com.inspien_project.dto.OrderDataDto;
import com.inspien_project.service.ReceiptService;
import com.inspien_project.service.FtpService;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final EnvSettings envSettings;
    private final OrderXmlMapper orderXmlMapper;
    private final OrderRepository orderRepository;
    private final ReceiptService receiptService;
    private final FtpService ftpService;
    private final TransactionTemplate transactionTemplate;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        private List<String> orderIds;
        private String receiptFile;
        private int totalCount;
        private int rejectedCount;
        private int emptyHeaderCount;
    }

    public static class ProcessingFailure extends RuntimeException {
        private final List<String> orderIds;
        private final String receiptFile;

        public ProcessingFailure(List<String> ids, String file, Exception cause) {
            super("DB와 FTP 상태를 요청 식별자로 대조", cause);
            this.orderIds = List.copyOf(ids);
            this.receiptFile = file;
        }

        public List<String> getOrderIds() { return orderIds; }
        public String getReceiptFile() { return receiptFile; }
    }

    private List<String> extractOrderIds(List<OrderDataDto> orderList) {
        List<String> ids = new ArrayList<>();
        for (OrderDataDto orderData : orderList) {
            ids.add(orderData.getOrderId());
        }
        return ids;
    }

    public synchronized Result createOrder(String requestId, byte[] xml) {
        var parsed = orderXmlMapper.parse(xml);
        var orderInputList = parsed.getOrderList();
        if (parsed.getRejectedCount() > 0 || parsed.getEmptyHeaderCount() > 0) {
            log.warn("requestId={} stage=XML_MAP total={} accepted={} rejected={} emptyHeaders={} reason=HEADER_ITEM_MISMATCH",
                    requestId, parsed.getTotalCount(), orderInputList.size(), parsed.getRejectedCount(), parsed.getEmptyHeaderCount());
        } else {
            log.info("requestId={} stage=XML_MAP total={} accepted={} rejected=0 emptyHeaders=0",
                    requestId, parsed.getTotalCount(), orderInputList.size());
        }
        List<OrderDataDto> orderList = new ArrayList<>();
        String receiptName = "";
        try {
            var usedIds = orderRepository.findOrderIdsByApplicantKey(envSettings.getApplicantKey());
            for (var input : orderInputList) {
                orderList.add(OrderDataDto.createOrderData(IdGenerator.createId(usedIds), envSettings.getApplicantKey(), input));
            }
            var receipt = receiptService.createReceipt(orderList);
            receiptName = receipt.getName();
            log.info("requestId={} stage=ORDER_BEGIN count={} receipt={}", requestId, orderList.size(), receiptName);
            transactionTemplate.executeWithoutResult(status -> {
                for (OrderDataDto orderData : orderList) orderRepository.save(orderData);
                try { ftpService.uploadFile(receipt.getPath(), receipt.getName()); }
                catch (Exception error) { throw new IllegalStateException("FTP_TRANSFER", error); }
            });
            String result = parsed.getRejectedCount() > 0 ? "PARTIAL_SUCCESS" : "SUCCESS";
            log.info("requestId={} stage=ORDER_END result={} total={} accepted={} rejected={} emptyHeaders={} orderIds={} receipt={}",
                    requestId, result, parsed.getTotalCount(), orderList.size(), parsed.getRejectedCount(),
                    parsed.getEmptyHeaderCount(), extractOrderIds(orderList), receiptName);
            return new Result(extractOrderIds(orderList), receiptName, parsed.getTotalCount(),
                    parsed.getRejectedCount(), parsed.getEmptyHeaderCount());
        } catch (Exception error) {
            var ids = extractOrderIds(orderList);
            log.error("requestId={} stage=ORDER_END result=FAILURE orderIds={} receipt={} errorType={}",
                    requestId, ids, receiptName, error.getClass().getSimpleName());
            throw new ProcessingFailure(ids, receiptName, error);
        }
    }
}
