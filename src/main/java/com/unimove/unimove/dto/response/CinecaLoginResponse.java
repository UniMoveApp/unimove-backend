package com.unimove.unimove.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinecaLoginResponse {

    private UserInfo user;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserInfo {
        private String firstName;
        private String lastName;
        private String userId;
        private String grpDes;
        private Long persId;
    }
}