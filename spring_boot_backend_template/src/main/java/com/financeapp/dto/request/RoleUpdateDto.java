package com.financeapp.dto.request;

import com.financeapp.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleUpdateDto {

    @NotNull(message = "Role is required")
    private Role role;
}
