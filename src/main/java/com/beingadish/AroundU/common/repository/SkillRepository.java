package com.beingadish.AroundU.common.repository;

import com.beingadish.AroundU.common.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    /**
     * Finds a skill by its normalised (lowercased) name.
     */
    @Query("SELECT s FROM Skill s WHERE lower(s.name) = lower(:name)")
    Optional<Skill> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Batch-fetches skills whose normalised names appear in the given
     * collection.
     */
    @Query("SELECT s FROM Skill s WHERE lower(s.name) IN :names")
    List<Skill> findAllByNameIgnoreCaseIn(@Param("names") Collection<String> names);

    /**
     * Auto-suggest: case-insensitive LIKE search on skill name, ordered
     * alphabetically.
     */
    @Query("SELECT s FROM Skill s WHERE lower(s.name) LIKE lower(concat('%', :query, '%')) ORDER BY s.name ASC")
    List<Skill> suggestByName(@Param("query") String query, org.springframework.data.domain.Pageable pageable);
}
