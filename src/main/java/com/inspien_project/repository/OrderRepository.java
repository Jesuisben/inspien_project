package com.inspien_project.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.inspien_project.dto.OrderDataDto;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(@Qualifier("orderJdbc") JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public Set<String> findOrderIdsByApplicantKey(String key) {
        return new HashSet<>(jdbcTemplate.queryForList(
                "SELECT ORDER_ID FROM ORDER_TB WHERE APPLICANT_KEY = ?", String.class, key));
    }

    public void save(OrderDataDto orderData) {
        int count = jdbcTemplate.update("""
                INSERT INTO ORDER_TB
                (ORDER_ID, APPLICANT_KEY, USER_ID, ITEM_ID, NAME, ADDRESS, ITEM_NAME, PRICE, STATUS)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, orderData.getOrderId(), orderData.getApplicantKey(), orderData.getUserId(), orderData.getItemId(), orderData.getName(),
                orderData.getAddress(), orderData.getItemName(), orderData.getPrice(), orderData.getStatus());
        if (count != 1) throw new IllegalStateException("주문 저장 건수가 올바르지 않음");
    }

    public List<OrderDataDto> findPendingOrdersByApplicantKey(String key) {
        return jdbcTemplate.query("""
                SELECT ORDER_ID, APPLICANT_KEY, USER_ID, ITEM_ID, NAME, ADDRESS, ITEM_NAME, PRICE, STATUS
                FROM ORDER_TB WHERE APPLICANT_KEY = ? AND STATUS = 'N' ORDER BY ORDER_ID
                """, (result, index) -> new OrderDataDto(result.getString("ORDER_ID"),
                result.getString("APPLICANT_KEY"), result.getString("USER_ID"), result.getString("ITEM_ID"),
                result.getString("NAME"), result.getString("ADDRESS"), result.getString("ITEM_NAME"),
                result.getString("PRICE"), result.getString("STATUS")), key);
    }

    public void updateStatus(OrderDataDto orderData) {
        int count = jdbcTemplate.update("""
                UPDATE ORDER_TB SET STATUS = 'Y'
                WHERE APPLICANT_KEY = ? AND ORDER_ID = ? AND STATUS = 'N'
                """, orderData.getApplicantKey(), orderData.getOrderId());
        if (count != 1) throw new IllegalStateException("주문 상태 변경 건수를 확인하세요.");
    }
}
