package com.bb.ballBin.register.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RegisterDto {

    private String userName;
    private String password;
    private String email;
    private String nickName;

}
