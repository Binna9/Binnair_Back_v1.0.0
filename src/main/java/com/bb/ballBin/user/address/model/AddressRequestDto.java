package com.bb.ballBin.user.address.model;

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
    @Schema(description = "우편번호1")
    private String postalCode1;
    @Schema(description = "우편번호2")
    private String postalCode2;
    @Schema(description = "우편번호3")
    private String postalCode3;
    @Schema(description = "주소1")
    private String address1;
    @Schema(description = "주소2")
    private String address2;
    @Schema(description = "주소2")
    private String address3;

    @Schema(description = "기본 주소 값")
    private Boolean isDefaultAddress1;
}
