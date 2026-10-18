
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

    private String orderId;
    private String applicantKey;
    private String userId;
    private String itemId;
    private String name;
    private String address;
    private String itemName;
    private String price;
    private String status;

    public static OrderDataDto createOrderData(String id, String key, OrderInputDto input) {
        return new OrderDataDto(id, key, input.getUserId(), input.getItemId(), input.getName(),
                input.getAddress(), input.getItemName(), input.getPrice(), "N");
    }
}