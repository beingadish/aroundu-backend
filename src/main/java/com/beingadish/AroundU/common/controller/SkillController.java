package com.beingadish.AroundU.common.controller;

import com.beingadish.AroundU.common.dto.ApiResponse;
import com.beingadish.AroundU.common.dto.SkillDTO;
import com.beingadish.AroundU.common.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.beingadish.AroundU.common.constants.URIConstants.SKILL_BASE;

/**
 * Exposes skill auto-suggest and lookup endpoints. All queries run against
 * PostgreSQL only (no Redis).
 */
@RestController
@RequestMapping(SKILL_BASE)
@RequiredArgsConstructor
@Tag(name = "Skills", description = "Skill lookup and auto-suggest")
public class SkillController {

    private final SkillService skillService;

    /**
     * Auto-suggest skills by partial name match.
     *
     * @param query partial skill name (minimum 1 character)
     * @param limit maximum results to return (default 10, max 20)
     * @return alphabetically sorted list of matching skills
     */
    @GetMapping("/suggest")
    @Operation(summary = "Suggest skills", description = "Case-insensitive partial match on skill names, sorted alphabetically. Postgres-only query.")
    public ResponseEntity<ApiResponse<List<SkillDTO>>> suggest(
            @Parameter(description = "Partial skill name to search for") @RequestParam String query,
            @Parameter(description = "Max results (default 10, max 20)") @RequestParam(defaultValue = "10") int limit) {
        List<SkillDTO> suggestions = skillService.suggestSkills(query, limit);
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }
}
