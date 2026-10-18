package com.inspien_project.test.order;

import com.inspien_project.dto.OrderInputDto;
import com.inspien_project.dto.OrderDataDto;
import com.inspien_project.test.common.OrderTestHelper;
import java.nio.file.Path;
import static com.inspien_project.test.common.OrderTestHelper.*;

public class OrderRepositoryTest {

    public static void main(String[] args) {
        var testData = OrderTestHelper.createTestData(Path.of("target/checks"));
        var orderRepository = testData.getOrderRepository();
        orderRepository.save(OrderDataDto.createOrderData("A000", "myKey",
                new OrderInputDto("U", "I", "홍길동", "서울", "상품", "0010")));
        assertTrue(orderRepository.findOrderIdsByApplicantKey("myKey").contains("A000"));
        assertTrue(orderRepository.findOrderIdsByApplicantKey("otherKey").isEmpty());
        assertEquals("0010", testData.getOrderJdbc().queryForObject(
                "SELECT PRICE FROM ORDER_TB WHERE APPLICANT_KEY = ?", String.class, "myKey"));
        System.out.println("OrderRepositoryTest 확인 완료");
    }
}
