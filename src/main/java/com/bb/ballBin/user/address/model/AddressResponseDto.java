package com.bb.ballBin.user.address.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AddressResponseDto {

    private String addressId;
    private String receiver;
    private String phone;
    private String postalCode;
    private String address1;
    private String address2;

    private Boolean isDefault;
}
