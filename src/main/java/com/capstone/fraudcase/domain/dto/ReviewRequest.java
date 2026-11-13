package com.capstone.fraudcase.domain.dto;

import com.capstone.fraudcase.domain.Disposition;
import com.capstone.fraudcase.domain.RecommendedRoute;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotNull Disposition disposition,
        @NotBlank @Size(max = 1000) String reason,
        RecommendedRoute rerouteTo) {
}
