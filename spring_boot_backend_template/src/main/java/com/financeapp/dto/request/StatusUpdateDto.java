package com.financeapp.dto.request;

import com.financeapp.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusUpdateDto {

    @NotNull(message = "Status is required")
    private UserStatus status;
}
