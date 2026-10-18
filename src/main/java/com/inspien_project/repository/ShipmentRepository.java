package com.inspien_project.repository;

import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.inspien_project.dto.OrderDataDto;

@Repository
public class ShipmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public ShipmentRepository(@Qualifier("shipmentJdbc") JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public Set<String> findShipmentIdsByApplicantKey(String key) {
        return new HashSet<>(jdbcTemplate.queryForList(
                "SELECT SHIPMENT_ID FROM SHIPMENT_TB WHERE APPLICANT_KEY = ?", String.class, key));
    }

    public boolean matchesOrder(OrderDataDto orderData) {
        var matches = jdbcTemplate.query("""
                SELECT ITEM_ID, ADDRESS FROM SHIPMENT_TB WHERE APPLICANT_KEY = ? AND ORDER_ID = ?
                """, (result, index) -> orderData.getItemId().equals(result.getString("ITEM_ID"))
                        && orderData.getAddress().equals(result.getString("ADDRESS")),
                orderData.getApplicantKey(), orderData.getOrderId());
        if (matches.isEmpty()) return false;
        if (matches.size() != 1 || !matches.getFirst()) {
            throw new IllegalStateException("기존 운송 데이터의 중복 또는 불일치를 확인하세요.");
        }
        return true;
    }

    public void save(String shipmentId, OrderDataDto orderData) {
        int count = jdbcTemplate.update("""
                INSERT INTO SHIPMENT_TB (SHIPMENT_ID, APPLICANT_KEY, ORDER_ID, ITEM_ID, ADDRESS)
                VALUES (?, ?, ?, ?, ?)
                """, shipmentId, orderData.getApplicantKey(), orderData.getOrderId(), orderData.getItemId(), orderData.getAddress());
        if (count != 1) throw new IllegalStateException("운송 저장 건수가 올바르지 않습니다.");
    }
}
