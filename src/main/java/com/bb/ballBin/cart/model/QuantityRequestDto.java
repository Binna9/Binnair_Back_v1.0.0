package com.bb.ballBin.cart.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class QuantityRequestDto {

    @Schema(description = "수량")
    private Integer quantity;
}
