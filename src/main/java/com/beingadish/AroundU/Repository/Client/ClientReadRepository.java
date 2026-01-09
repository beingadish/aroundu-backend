package com.beingadish.AroundU.Repository.Client;

import com.beingadish.AroundU.Entities.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClientReadRepository {
    // Finding a client by email
    @Query("SELECT c FROM Client c WHERE c.clientEmail = :email")
    Optional<Client> findByEmail(@Param("email") String email);

    @Query("SELECT c FROM Client c WHERE c.clientEmail = :email AND c.hashedPassword = :hashedPassword")
    Optional<Client> findByEmailAndPassword(@Param("email") String email, @Param("hashedPassword") String hashedPassword);

    List<Client> findAllByCreatedAtAfter(LocalDateTime date);

    List<Client> findAllByCreatedAtBefore(LocalDateTime date);

    List<Client> findAllByPincode(Integer pincode);

    boolean existsByClientEmail(String email);

    List<Client> findByClientNameContainingIgnoreCase(String namePart);

    @Query("SELECT COUNT(c) FROM Client c WHERE c.state.stateName = :stateName")
    long countByStateName(@Param("stateName") String stateName);

    @Query("SELECT c FROM Client c")
    Page<Client> findAllWithPagination(Pageable pageable);

    @Query("SELECT c FROM Client c WHERE SIZE(c.postedJobs) = :jobCount")
    List<Client> findClientsByJobCount(@Param("jobCount") int jobCount);

    @Query("SELECT c FROM Client c JOIN c.postedJobs j WHERE j.jobPriority = :priority")
    List<Client> findClientsByJobPriority(@Param("priority") String priority);

    @Query("SELECT c FROM Client c ORDER BY c.createdAt DESC")
    Optional<Client> findMostRecentlyCreatedClient();

    @Query("SELECT c FROM Client c JOIN c.postedJobs j JOIN j.skillRequiredList s WHERE s.skillName = :skillName")
    List<Client> findClientsBySkillRequired(@Param("skillName") String skillName);

    @Query("SELECT c FROM Client c JOIN c.postedJobs j WHERE j.createdAt > :date")
    List<Client> findClientsByJobCreationDate(@Param("date") LocalDateTime date);

    @Query("SELECT c FROM Client c WHERE c.district.districtName = :district AND c.state.stateName = :state")
    List<Client> findByDistrictAndState(@Param("district") String districtName, @Param("state") String stateName);
}
