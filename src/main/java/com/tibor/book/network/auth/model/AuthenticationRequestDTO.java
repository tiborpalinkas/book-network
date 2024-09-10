package com.tibor.book.network.auth.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthenticationRequestDTO {

    public String email;

    public String password;
}
