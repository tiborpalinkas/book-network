package com.tibor.book.network.auth.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
public class AuthenticationResponseDTO {

    private String token;
}
