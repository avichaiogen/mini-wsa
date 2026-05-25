package com.akamai.miniwsa.samples;

import com.akamai.miniwsa.domain.Action;
import com.akamai.miniwsa.domain.RuleCategory;
import com.akamai.miniwsa.samples.dto.SamplesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

// Invalid enum values for category or action resolve to MethodArgumentTypeMismatchException → 400
// via the existing GlobalExceptionHandler.
@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
public class SamplesController {

    private final SamplesService samplesService;

    @GetMapping("/samples")
    public SamplesResponse getSamples(
            @RequestParam(required = false) Long configId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) RuleCategory category,
            @RequestParam(required = false) Action action,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return samplesService.getSamples(configId, from, to, category, action, limit, offset);
    }
}
