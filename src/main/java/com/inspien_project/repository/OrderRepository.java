package com.inspien_project.repository;

import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.inspien_project.dto.OrderDataDto;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(@Qualifier("orderJdbc") JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    // 본인 지원자 키에 속한 기존 ID만 읽어 다른 지원자 데이터와 채번 범위를 구분
    public Set<String> findOrderIdsByApplicantKey(String key) {
        return new HashSet<>(jdbcTemplate.queryForList(
                "SELECT ORDER_ID FROM ORDER_TB WHERE APPLICANT_KEY = ?", String.class, key));
    }

    // 컬럼 순서에 맞춰 값을 바인딩하고 정확히 한 행을 저장했는지 확인
    public void save(OrderDataDto orderData) {
        int count = jdbcTemplate.update("""
                INSERT INTO ORDER_TB
                (ORDER_ID, APPLICANT_KEY, USER_ID, ITEM_ID, NAME, ADDRESS, ITEM_NAME, PRICE, STATUS)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, orderData.getOrderId(), orderData.getApplicantKey(), orderData.getUserId(), orderData.getItemId(), orderData.getName(),
                orderData.getAddress(), orderData.getItemName(), orderData.getPrice(), orderData.getStatus());
        if (count != 1) throw new IllegalStateException("주문 저장 건수가 올바르지 않음");
    }
}
