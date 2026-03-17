package com.rev.app.dto;

import java.io.Serializable;

public class JwtResponseDTO implements Serializable {
    private static final long serialVersionUID = -8091879091924046844L;
    private String jwttoken;

    public JwtResponseDTO() {
    }

    public JwtResponseDTO(String jwttoken) {
        this.jwttoken = jwttoken;
    }

    public String getJwttoken() {
        return jwttoken;
    }

    public void setJwttoken(String jwttoken) {
        this.jwttoken = jwttoken;
    }
}
