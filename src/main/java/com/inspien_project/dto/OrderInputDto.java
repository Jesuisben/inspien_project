// 상품 입력값 DTO (XML로 정보 받음)

package com.inspien_project.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderInputDto {

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
}