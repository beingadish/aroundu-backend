package com.beingadish.AroundU.common.service;

import com.beingadish.AroundU.common.dto.SkillDTO;
import com.beingadish.AroundU.common.entity.Skill;
import com.beingadish.AroundU.common.mapper.SkillMapper;
import com.beingadish.AroundU.common.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages skill lifecycle: normalisation, find-or-create, and auto-suggest.
 * <p>
 * Skills are persisted in lowercase and deduplicated at the database level via
 * a UNIQUE constraint on {@code lower(name)}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SkillService {

    private final SkillRepository skillRepository;
    private final SkillMapper skillMapper;

    /**
     * Resolves a list of skill names to persisted {@link Skill} entities.
     * <ul>
     * <li>Each name is normalised (trimmed, whitespace-collapsed,
     * lowercased).</li>
     * <li>Existing skills are batch-fetched first.</li>
     * <li>Missing skills are created; concurrent duplicate inserts are handled
     * via optimistic catch-and-reload.</li>
     * </ul>
     *
     * @param names raw skill name strings provided by the client
     * @return set of persisted Skill entities (never empty if input is
     * non-empty)
     */
    @Transactional
    public Set<Skill> findOrCreateSkills(List<String> names) {
        if (names == null || names.isEmpty()) {
            return Collections.emptySet();
        }

        // 1. Normalise and deduplicate
        List<String> normalised = names.stream()
                .map(Skill::normalize)
                .filter(Objects::nonNull)
                .filter(n -> !n.isBlank())
                .distinct()
                .toList();

        if (normalised.isEmpty()) {
            return Collections.emptySet();
        }

        // 2. Batch-fetch existing
        List<Skill> existing = skillRepository.findAllByNameIgnoreCaseIn(normalised);
        Map<String, Skill> existingMap = existing.stream()
                .collect(Collectors.toMap(s -> s.getName().toLowerCase(), s -> s));

        Set<Skill> result = new HashSet<>(existing);

        // 3. Insert missing skills with optimistic concurrency handling
        for (String name : normalised) {
            if (!existingMap.containsKey(name)) {
                Skill skill = persistSkillSafely(name);
                result.add(skill);
            }
        }

        return result;
    }

    /**
     * Returns skill IDs for the given names, creating any that don't exist.
     *
     * @param names raw skill name strings
     * @return list of skill IDs
     */
    @Transactional
    public List<Long> findOrCreateSkillIds(List<String> names) {
        return findOrCreateSkills(names).stream()
                .map(Skill::getId)
                .toList();
    }

    /**
     * Auto-suggest skills matching a partial query string.
     *
     * @param query partial skill name to search for
     * @param limit maximum results to return (capped at 20)
     * @return alphabetically sorted list of matching skill DTOs
     */
    @Transactional(readOnly = true)
    public List<SkillDTO> suggestSkills(String query, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        int cappedLimit = Math.min(Math.max(limit, 1), 20);
        List<Skill> results = skillRepository.suggestByName(query.strip(), PageRequest.of(0, cappedLimit));
        return skillMapper.toDtoListFromList(results);
    }

    /**
     * Persists a single skill, handling concurrent duplicate insertion via
     * catch-and-reload.
     */
    private Skill persistSkillSafely(String normalisedName) {
        try {
            Skill skill = Skill.builder().name(normalisedName).build();
            return skillRepository.saveAndFlush(skill);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent insert won the race — reload the existing skill
            log.debug("Concurrent skill creation detected for '{}', reloading", normalisedName);
            return skillRepository.findByNameIgnoreCase(normalisedName)
                    .orElseThrow(() -> new IllegalStateException(
                    "Skill '" + normalisedName + "' should exist after concurrent insert"));
        }
    }
}
