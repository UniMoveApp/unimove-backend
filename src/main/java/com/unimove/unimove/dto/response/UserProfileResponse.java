package com.unimove.unimove.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String username;
    private String fullName;
    private String email;
    private String role;
    private String avatarUrl;
    private String travelPreferences;
    private String iban;
    private String ibanHolder;
}
