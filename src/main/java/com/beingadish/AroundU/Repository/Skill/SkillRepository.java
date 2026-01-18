package com.beingadish.AroundU.Repository.Skill;

import com.beingadish.AroundU.Entities.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
}
