package com.inspien_project.test.order;

import static com.inspien_project.test.common.OrderTestHelper.*;
import java.nio.file.Files;

import com.inspien_project.test.common.OrderTestHelper;
import com.inspien_project.test.common.OrderXmlData;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import com.inspien_project.dto.OrderInputDto;
import com.inspien_project.dto.OrderDataDto;
import com.inspien_project.service.OrderService;
import com.inspien_project.service.ReceiptService;

public class OrderServiceTest {

    Path folder;

    void testOrderAndReceipt() {
        var testData = OrderTestHelper.createTestData(folder);
        var result = testData.getOrderService().createOrder("test-request", OrderXmlData.xml.getBytes(StandardCharsets.UTF_8));
        assertEquals(2, testData.countOrders("TEST_KEY"));
        assertTrue(result.getReceiptFile().matches("INSPIEN_테스트_[0-9]{14}\\.txt"));
        String text = new String(testData.getFtpService().files.get(result.getReceiptFile()), Charset.forName("EUC-KR"));
        assertEquals("A000^USER1^ITEM1^TEST_KEY^홍길동^서울 금천구^청바지^21000\n"
                + "A001^USER1^ITEM2^TEST_KEY^홍길동^서울 금천구^티셔츠^15800\n", text);
    }

    void testPartialOrder() throws Exception {
        var testData = OrderTestHelper.createTestData(folder);
        String header = "<HEADER><USER_ID>USER2</USER_ID><NAME>김</NAME><ADDRESS>서울</ADDRESS><STATUS>N</STATUS></HEADER>";
        String xml = OrderXmlData.xml.replaceFirst("<ITEM><USER_ID>USER1", "<ITEM><USER_ID>UNKNOWN")
                .replace("</ORDERS>", header + "</ORDERS>");
        var result = testData.getOrderService().createOrder("partial-request", xml.getBytes(StandardCharsets.UTF_8));
        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getOrderIds().size());
        assertEquals(1, result.getRejectedCount());
        assertEquals(1, result.getEmptyHeaderCount());
        assertEquals(1, testData.countOrders("TEST_KEY"));
        String receipt = new String(testData.getFtpService().files.get(result.getReceiptFile()), Charset.forName("EUC-KR"));
        assertEquals(1L, receipt.lines().count());
        assertTrue(receipt.contains("^ITEM2^"));
        var failedData = OrderTestHelper.createTestData(Files.createTempDirectory(folder, "failure-"));
        failedData.getFtpService().fail = true;
        assertThrows(OrderService.ProcessingFailure.class,
                () -> failedData.getOrderService().createOrder("partial-failure", xml.getBytes(StandardCharsets.UTF_8)));
        assertEquals(0, failedData.countOrders("TEST_KEY"));
    }

    void testFtpFailure() {
        var testData = OrderTestHelper.createTestData(folder);
        testData.getFtpService().fail = true;
        assertThrows(OrderService.ProcessingFailure.class,
                () -> testData.getOrderService().createOrder("ftp-failure", OrderXmlData.xml.getBytes(StandardCharsets.UTF_8)));
        assertTrue(testData.countOrders("TEST_KEY") == 0);
        assertTrue(testData.getFtpService().files.isEmpty());
    }

    void testDbFailure() {
        var testData = OrderTestHelper.createTestData(folder);
        testData.getOrderJdbc().execute("ALTER TABLE ORDER_TB ADD CONSTRAINT TEST_PRICE CHECK (PRICE <> '15800')");
        assertThrows(OrderService.ProcessingFailure.class,
                () -> testData.getOrderService().createOrder("db-failure", OrderXmlData.xml.getBytes(StandardCharsets.UTF_8)));
        assertTrue(testData.countOrders("TEST_KEY") == 0);
        assertTrue(testData.getFtpService().files.isEmpty());
    }

    void testReceiptEncoding() {
        var orderData = OrderDataDto.createOrderData("A001", "TEST_KEY", new OrderInputDto("U", "I", "홍길동", "서울", "상품😀", "10"));
        assertThrows(java.nio.charset.CharacterCodingException.class,
                () -> ReceiptService.toReceiptBytes(List.of(orderData), Charset.forName("EUC-KR")));
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("target/checks"));
        {
            var test = new OrderServiceTest();
            test.folder = Files.createTempDirectory(Path.of("target/checks"), "OrderServiceTest-");
            test.testOrderAndReceipt();
        }
        {
            var test = new OrderServiceTest();
            test.folder = Files.createTempDirectory(Path.of("target/checks"), "OrderServiceTest-");
            test.testPartialOrder();
        }
        {
            var test = new OrderServiceTest();
            test.folder = Files.createTempDirectory(Path.of("target/checks"), "OrderServiceTest-");
            test.testFtpFailure();
        }
        {
            var test = new OrderServiceTest();
            test.folder = Files.createTempDirectory(Path.of("target/checks"), "OrderServiceTest-");
            test.testDbFailure();
        }
        {
            var test = new OrderServiceTest();
            test.folder = Files.createTempDirectory(Path.of("target/checks"), "OrderServiceTest-");
            test.testReceiptEncoding();
        }
        System.out.println("OrderServiceTest 확인 완료");
    }
}
