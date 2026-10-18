package com.inspien_project.test.shipment;

import static com.inspien_project.test.common.OrderTestHelper.*;
import java.nio.file.Files;

import com.inspien_project.test.common.OrderTestHelper;
import java.nio.file.Path;
import com.inspien_project.batch.ShipmentBatch;
import com.inspien_project.repository.OrderRepository;
import com.inspien_project.repository.ShipmentRepository;
import com.inspien_project.dto.OrderInputDto;
import com.inspien_project.dto.OrderDataDto;

public class ShipmentBatchTest {

    Path folder;

    private OrderDataDto createOrderData(String id, String key) {
        return OrderDataDto.createOrderData(id, key, new OrderInputDto("USER1", "ITEM1", "홍길동", "서울", "상품", "10"));
    }

    void testPendingOrders() {
        var testData = OrderTestHelper.createTestData(folder);
        testData.getOrderRepository().save(createOrderData("A000", "TEST_KEY"));
        testData.getOrderRepository().save(createOrderData("A000", "OTHER_KEY"));
        assertEquals(1, new ShipmentBatch(testData.getSettings(), testData.getOrderRepository(), testData.getShipmentRepository()).runBatch().getSuccess());
        assertTrue(testData.getOrderRepository().findPendingOrdersByApplicantKey("TEST_KEY").isEmpty());
        assertEquals(1, testData.getOrderRepository().findPendingOrdersByApplicantKey("OTHER_KEY").size());
        assertEquals(0, new ShipmentBatch(testData.getSettings(), testData.getOrderRepository(), testData.getShipmentRepository()).runBatch().getTotal());
        assertEquals(1, testData.getShipmentJdbc().queryForObject("SELECT COUNT(*) FROM SHIPMENT_TB", Integer.class));
    }

    void testUpdateRetry() {
        var testData = OrderTestHelper.createTestData(folder);
        testData.getOrderRepository().save(createOrderData("A000", "TEST_KEY"));
        var failOrderRepository = new OrderRepository(testData.getOrderJdbc()) {
            @Override public void updateStatus(OrderDataDto orderData) { throw new IllegalStateException("SIMULATED_UPDATE_FAILURE"); }
        };
        var firstBatch = new ShipmentBatch(testData.getSettings(), failOrderRepository, testData.getShipmentRepository());
        assertEquals(1, firstBatch.runBatch().getFailure());
        assertEquals(1, testData.getOrderRepository().findPendingOrdersByApplicantKey("TEST_KEY").size());
        assertEquals(1, new ShipmentBatch(testData.getSettings(), testData.getOrderRepository(), testData.getShipmentRepository()).runBatch().getSuccess());
        assertEquals(1, testData.getShipmentJdbc().queryForObject("SELECT COUNT(*) FROM SHIPMENT_TB", Integer.class));
    }

    void testShipmentMismatch() {
        var testData = OrderTestHelper.createTestData(folder);
        testData.getOrderRepository().save(createOrderData("A000", "TEST_KEY"));
        testData.getShipmentJdbc().update("INSERT INTO SHIPMENT_TB VALUES (?, ?, ?, ?, ?)",
                "B000", "TEST_KEY", "A000", "WRONG_ITEM", "서울");
        assertEquals(1, new ShipmentBatch(testData.getSettings(), testData.getOrderRepository(), testData.getShipmentRepository()).runBatch().getFailure());
        assertEquals(1, testData.getOrderRepository().findPendingOrdersByApplicantKey("TEST_KEY").size());
    }

    void testInsertFailure() {
        var testData = OrderTestHelper.createTestData(folder);
        testData.getOrderRepository().save(createOrderData("A000", "TEST_KEY"));
        var failShipmentRepository = new ShipmentRepository(testData.getShipmentJdbc()) {
            @Override
            public void save(String shipmentId, OrderDataDto orderData) {
                throw new IllegalStateException("SIMULATED_SHIPMENT_INSERT_FAILURE");
            }
        };
        var result = new ShipmentBatch(testData.getSettings(), testData.getOrderRepository(), failShipmentRepository).runBatch();
        assertEquals(1, result.getTotal());
        assertEquals(0, result.getSuccess());
        assertEquals(1, result.getFailure());
        assertEquals(1, testData.getOrderRepository().findPendingOrdersByApplicantKey("TEST_KEY").size());
        assertEquals(0, testData.getShipmentJdbc().queryForObject("SELECT COUNT(*) FROM SHIPMENT_TB", Integer.class));
    }

    void testDisabledSchedule() {
        var testData = OrderTestHelper.createTestData(folder);
        testData.getOrderRepository().save(createOrderData("A000", "TEST_KEY"));
        new ShipmentBatch(testData.getSettings(), testData.getOrderRepository(), testData.getShipmentRepository()).scheduleBatch();
        assertEquals(1, testData.getOrderRepository().findPendingOrdersByApplicantKey("TEST_KEY").size());
        assertEquals(0, testData.getShipmentJdbc().queryForObject("SELECT COUNT(*) FROM SHIPMENT_TB", Integer.class));
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("target/checks"));
        {
            var test = new ShipmentBatchTest();
            test.folder = Files.createTempDirectory(Path.of("target/checks"), "ShipmentBatchTest-");
            test.testPendingOrders();
        }
        {
            var test = new ShipmentBatchTest();
            test.folder = Files.createTempDirectory(Path.of("target/checks"), "ShipmentBatchTest-");
            test.testUpdateRetry();
        }
        {
            var test = new ShipmentBatchTest();
            test.folder = Files.createTempDirectory(Path.of("target/checks"), "ShipmentBatchTest-");
            test.testShipmentMismatch();
        }
        {
            var test = new ShipmentBatchTest();
            test.folder = Files.createTempDirectory(Path.of("target/checks"), "ShipmentBatchTest-");
            test.testInsertFailure();
        }
        {
            var test = new ShipmentBatchTest();
            test.folder = Files.createTempDirectory(Path.of("target/checks"), "ShipmentBatchTest-");
            test.testDisabledSchedule();
        }
        System.out.println("ShipmentBatchTest 확인 완료");
    }
}
