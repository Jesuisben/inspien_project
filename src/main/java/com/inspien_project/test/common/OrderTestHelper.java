
package com.inspien_project.test.common;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.inspien_project.service.OrderService;
import com.inspien_project.service.ReceiptService;
import com.inspien_project.service.FtpService;
import com.inspien_project.mapper.OrderXmlMapper;

import lombok.Getter;
import lombok.Setter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.inspien_project.common.EnvSettings;
import com.inspien_project.repository.OrderRepository;
import com.inspien_project.repository.ShipmentRepository;

public final class OrderTestHelper {

    private OrderTestHelper() { }

    public static void assertEquals(Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) throw new IllegalStateException("기대값과 실제값 불일치");
    }

    public static void assertTrue(boolean value) {
        if (!value) throw new IllegalStateException("검증 조건 불일치");
    }

    public static void assertArrayEquals(byte[] expected, byte[] actual) {
        assertTrue(java.util.Arrays.equals(expected, actual));
    }

    public static void assertNotNull(Object value) { assertTrue(value != null); }

    public interface CheckAction { void run() throws Exception; }

    public static void assertThrows(Class<? extends Throwable> expected, CheckAction action) {
        try { action.run(); }
        catch (Throwable error) {
            if (expected.isInstance(error)) return;
            throw new IllegalStateException("예상하지 않은 예외 종류", error);
        }
        throw new IllegalStateException("예상한 예외가 발생하지 않음");
    }

    private static DriverManagerDataSource createTestDataSource(String database) {
        if (!database.equals("inspien_order_test") && !database.equals("inspien_shipment_test")) {
            throw new IllegalArgumentException("검증 스키마 이름을 확인하세요.");
        }
        EnvSettings settings = EnvSettings.load();
        if (settings.getTestDbUsername() == null || settings.getTestDbPassword() == null) {
            throw new IllegalArgumentException("로컬 MySQL 검증 계정을 .env에 설정하세요.");
        }
        var source = new DriverManagerDataSource("jdbc:mysql://127.0.0.1:3306/" + database,
                settings.getTestDbUsername(), settings.getTestDbPassword());
        try (var connection = source.getConnection()) {
            if (!database.equals(connection.getCatalog())) throw new IllegalArgumentException("검증 DB 불일치");
        } catch (java.sql.SQLException error) { throw new IllegalStateException("로컬 검증 DB 연결 실패", error); }
        return source;
    }

    @Getter
    @Setter
    public static class TestData {
        private EnvSettings settings;
        private JdbcTemplate orderJdbc;
        private OrderRepository orderRepository;
        private OrderService orderService;
        private TestFtpService ftpService;
        private JdbcTemplate shipmentJdbc;
        private ShipmentRepository shipmentRepository;
        public int countOrders(String applicantKey) {
            return orderJdbc.queryForObject("SELECT COUNT(*) FROM ORDER_TB WHERE APPLICANT_KEY = ?", Integer.class, applicantKey);
        }
    }

    public static class TestFtpService extends FtpService {
        public final Map<String, byte[]> files = new LinkedHashMap<>();
        public boolean fail;
        public TestFtpService(EnvSettings settings) { super(settings); }
        @Override public void uploadFile(Path file, String name) throws IOException {
            if (fail) throw new IOException("SIMULATED_FTP_FAILURE");
            files.put(name, Files.readAllBytes(file));
        }
    }

    public static TestData createTestData(Path folder) {
        var settings = new EnvSettings();
        settings.setApplicantKey("TEST_KEY");
        settings.setName("테스트");
        settings.setReceiptDirectory(folder.toString());
        settings.setReceiptCharset("EUC-KR");
        settings.setFtpHost("127.0.0.1");
        settings.setFtpPort(1);
        settings.setFtpUsername("test");
        settings.setFtpPassword("test-only");
        settings.setFtpDirectory("/");
        settings.setFtpControlEncoding("UTF-8");
        var orderSource = createTestDataSource("inspien_order_test");
        var orderJdbc = new JdbcTemplate(orderSource);
        orderJdbc.execute("DROP TABLE IF EXISTS ORDER_TB");
        orderJdbc.execute("""
                CREATE TABLE ORDER_TB (ORDER_ID VARCHAR(4), APPLICANT_KEY VARCHAR(128),
                USER_ID VARCHAR(128), ITEM_ID VARCHAR(128), NAME VARCHAR(256), ADDRESS VARCHAR(1024),
                ITEM_NAME VARCHAR(256), PRICE VARCHAR(64), STATUS VARCHAR(1),
                PRIMARY KEY (ORDER_ID, APPLICANT_KEY))
                """);
        var orderRepository = new OrderRepository(orderJdbc);
        var testData = new TestData();
        testData.setSettings(settings);
        testData.setOrderJdbc(orderJdbc);
        testData.setOrderRepository(orderRepository);
        var ftpService = new TestFtpService(settings);
        var transactionTemplate = new TransactionTemplate(new JdbcTransactionManager(orderSource));
        testData.setFtpService(ftpService);
        testData.setOrderService(new OrderService(settings, new OrderXmlMapper(), orderRepository,
                new ReceiptService(settings), ftpService, transactionTemplate));
        var shipmentJdbc = new JdbcTemplate(createTestDataSource("inspien_shipment_test"));
        shipmentJdbc.execute("DROP TABLE IF EXISTS SHIPMENT_TB");
        shipmentJdbc.execute("""
                CREATE TABLE SHIPMENT_TB (SHIPMENT_ID VARCHAR(4), APPLICANT_KEY VARCHAR(128),
                ORDER_ID VARCHAR(4), ITEM_ID VARCHAR(128), ADDRESS VARCHAR(1024),
                PRIMARY KEY (SHIPMENT_ID, APPLICANT_KEY))
                """);
        testData.setShipmentJdbc(shipmentJdbc);
        testData.setShipmentRepository(new ShipmentRepository(shipmentJdbc));
        return testData;
    }
}