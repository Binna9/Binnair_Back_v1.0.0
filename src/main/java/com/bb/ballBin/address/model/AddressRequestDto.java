package com.bb.ballBin.address.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AddressRequestDto {

    @Schema(description = "수령인")
    private String receiver;
    @Schema(description = "전화번호")
    private String phone;
    @Schema(description = "우편번호")
    private String postalCode;
    @Schema(description = "주소")
    private String address;

    @Schema(description = "기본 주소 값")
    private String isDefault;
}
