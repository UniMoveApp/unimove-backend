package com.unimove.unimove.dto.request;

import lombok.Data;

@Data
public class UpdateIbanRequest {
    private String iban;
    private String ibanHolder;
}
