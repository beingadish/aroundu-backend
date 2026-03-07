package com.beingadish.AroundU.fx.controller;

import com.beingadish.AroundU.common.dto.ApiResponse;
import com.beingadish.AroundU.fx.dto.ExchangeRateDTO;
import com.beingadish.AroundU.fx.service.impl.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fx")
@RequiredArgsConstructor
@Tag(name = "FX", description = "Live currency exchange rates (Frankfurter/ECB data)")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/rate")
    @Operation(
            summary = "Get exchange rate",
            description = "Returns the live exchange rate between two currency codes. "
            + "E.g. /api/v1/fx/rate?from=USD&to=INR returns the USD→INR rate. "
            + "Rates are cached for 1 hour. Data sourced from Frankfurter (ECB)."
    )
    public ResponseEntity<ApiResponse<ExchangeRateDTO>> getRate(
            @RequestParam(defaultValue = "USD") String from,
            @RequestParam(defaultValue = "INR") String to) {
        ExchangeRateDTO rate = exchangeRateService.getRate(from, to);
        return ResponseEntity.ok(ApiResponse.success(rate));
    }
}
