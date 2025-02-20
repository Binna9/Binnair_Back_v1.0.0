package com.bb.ballBin.user.address.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @Schema(description = "주소1")
    private String address1;
    @Schema(description = "주소2")
    private String address2;

    private Boolean isDefault;
}
