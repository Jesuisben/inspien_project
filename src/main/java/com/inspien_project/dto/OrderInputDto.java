
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

    private String userId;
    private String itemId;
    private String name;
    private String address;
    private String itemName;
    private String price;
}