package com.inspien_project.batch;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.inspien_project.common.EnvSettings;
import com.inspien_project.repository.OrderRepository;
import com.inspien_project.repository.ShipmentRepository;
import com.inspien_project.common.IdGenerator;

@Component
@RequiredArgsConstructor
public class ShipmentBatch {

    private static final Logger log = LoggerFactory.getLogger(ShipmentBatch.class);
    private final EnvSettings envSettings;
    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;

    @Scheduled(cron = "${batchCron:0 */5 * * * *}", zone = "Asia/Seoul")
    public void scheduleBatch() {
        if (envSettings.isBatchEnabled()) runBatch();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        private int total;
        private int success;
        private int failure;
    }

    public synchronized Result runBatch() {
        String requestId = "BATCH-" + UUID.randomUUID();
        int success = 0;
        int failure = 0;
        try {
            var orderList = orderRepository.findPendingOrdersByApplicantKey(envSettings.getApplicantKey());
            var usedIds = shipmentRepository.findShipmentIdsByApplicantKey(envSettings.getApplicantKey());
            for (var orderData : orderList) {
                try {
                    if (!shipmentRepository.matchesOrder(orderData)) shipmentRepository.save(IdGenerator.createId(usedIds), orderData);
                    orderRepository.updateStatus(orderData);
                    success++;
                    log.info("requestId={} stage=SHIPMENT orderId={} result=SUCCESS", requestId, orderData.getOrderId());
                } catch (Exception error) {
                    failure++;
                    log.error("requestId={} stage=SHIPMENT orderId={} result=FAILURE errorType={}",
                            requestId, orderData.getOrderId(), error.getClass().getSimpleName());
                }
            }
            log.info("requestId={} stage=BATCH_END total={} success={} failure={}",
                    requestId, orderList.size(), success, failure);
            return new Result(orderList.size(), success, failure);
        } catch (Exception error) {
            log.error("requestId={} stage=BATCH_READ result=FAILURE errorType={}",
                    requestId, error.getClass().getSimpleName());
            return new Result(0, 0, 1);
        }
    }
}
