package com.beingadish.AroundU.Repository.Job;

import com.beingadish.AroundU.Entities.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

	@Query("select j from Job j where (:city is null or lower(j.jobLocation.city) = lower(:city)) and (:area is null or lower(j.jobLocation.area) = lower(:area))")
	List<Job> searchByLocation(@Param("city") String city, @Param("area") String area);

	@Query("select distinct j from Job j left join j.skillSet s where (:city is null or lower(j.jobLocation.city) = lower(:city)) and (:area is null or lower(j.jobLocation.area) = lower(:area)) and (:skillIds is null or s.id in :skillIds)")
	List<Job> searchByLocationAndSkills(@Param("city") String city, @Param("area") String area, @Param("skillIds") List<Long> skillIds);
}
