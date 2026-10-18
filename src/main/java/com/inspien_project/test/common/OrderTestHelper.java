// 검증 공통 파일

package com.inspien_project.test.common;

import java.nio.file.Path;
import lombok.Getter;
import lombok.Setter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.inspien_project.common.EnvSettings;
import com.inspien_project.repository.OrderRepository;

public final class OrderTestHelper {

    private OrderTestHelper() { }

    // 검증 파일의 재사용 메소드
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

    // 예외처리 검증 중첩 인터페이스임
    public interface CheckAction { void run() throws Exception; }

    public static void assertThrows(Class<? extends Throwable> expected, CheckAction action) {
        try { action.run(); }
        catch (Throwable error) {
            if (expected.isInstance(error)) return;
            throw new IllegalStateException("예상하지 않은 예외 종류", error);
        }
        throw new IllegalStateException("예상한 예외가 발생하지 않음");
    }

    // 회사 연결 필드는 읽지 않음. 고정된 loopback URL과 검증 전용 스키마만 사용
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
        public int countOrders(String applicantKey) {
            return orderJdbc.queryForObject("SELECT COUNT(*) FROM ORDER_TB WHERE APPLICANT_KEY = ?", Integer.class, applicantKey);
        }
    }

    // 호출할 때 로컬 검증 스키마의 테이블을 새로 만듦. 해당 두 스키마에는 개인 자료를 넣지 않음
    public static TestData createTestData(Path folder) {
        var settings = new EnvSettings();
        settings.setApplicantKey("TEST_KEY");
        settings.setName("테스트");
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
        return testData;
    }
}