// 주문 입력(저장)값 DTO - 운송 상태

package com.inspien_project.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDataDto {

    // 주문 ID
    private String orderId;
    // 지원자 키
    private String applicantKey;
    // 고객 ID
    private String userId;
    // 상품 ID
    private String itemId;
    // 고객 이름
    private String name;
    // 고객 주소
    private String address;
    // 상품 이름
    private String itemName;
    // 상품 가격
    private String price;
    // 운송 적재 상태 (N, Y)
    private String status;

    // 입력 값에 주문 ID·지원자 키· 운송 적재 N 상태를 넣어 저장할 주문 행 구성
    public static OrderDataDto createOrderData(String id, String key, OrderInputDto input) {
        return new OrderDataDto(id, key, input.getUserId(), input.getItemId(), input.getName(),
                input.getAddress(), input.getItemName(), input.getPrice(), "N");
    }
}