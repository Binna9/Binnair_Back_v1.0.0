package com.bin.web.address.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponseDto {

    @Schema(description = "주소 ID")
    private String addressId;

    @Schema(description = "수령인")
    private String receiver;
    @Schema(description = "전화번호")
    private String phoneNumber;
    @Schema(description = "우편번호")
    private String postalCode;
    @Schema(description = "주소")
    private String address;

    @Schema(description = "기본 주소 값")
    private boolean isDefault;
}
