package com.socialpublish.auth.dto;

import com.socialpublish.common.validation.FieldsMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@FieldsMatch(first = "newPassword", second = "confirmPassword", message = "Passwords do not match")
@Getter
@Setter
public class SetPasswordRequest {

    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}
