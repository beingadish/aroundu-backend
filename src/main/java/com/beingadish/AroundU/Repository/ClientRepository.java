package com.beingadish.AroundU.Repository;

import com.beingadish.AroundU.Entities.ClientEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends CrudRepository<ClientEntity, Long> {

    @Query("SELECT c FROM ClientEntity c WHERE c.clientEmail = :email")
    Optional<ClientEntity> findByEmail(@Param("email") String email);

    @Query("SELECT c FROM ClientEntity c WHERE c.clientEmail = :email AND c.password = :password")
    Optional<ClientEntity> findByEmailAndPassword(@Param("email") String email, @Param("password") String password);

    List<ClientEntity> findAllByCreatedAtAfter(LocalDateTime date);

    List<ClientEntity> findAllByCreatedAtBefore(LocalDateTime date);

    List<ClientEntity> findAllByPincode(Integer pincode);

    boolean existsByClientEmail(String email);

    void deleteByClientEmail(String email);

    List<ClientEntity> findByClientNameContainingIgnoreCase(String namePart);

    @Query("SELECT COUNT(c) FROM ClientEntity c WHERE c.state.stateName = :stateName")
    long countByStateName(@Param("stateName") String stateName);

    @Query("SELECT c FROM ClientEntity c")
    Page<ClientEntity> findAllWithPagination(Pageable pageable);

    @Query("SELECT c FROM ClientEntity c WHERE SIZE(c.postedJobs) = :jobCount")
    List<ClientEntity> findClientsByJobCount(@Param("jobCount") int jobCount);

    @Query("SELECT c FROM ClientEntity c JOIN c.postedJobs j WHERE j.jobPriority = :priority")
    List<ClientEntity> findClientsByJobPriority(@Param("priority") String priority);

    @Query("SELECT c FROM ClientEntity c ORDER BY c.createdAt DESC")
    Optional<ClientEntity> findMostRecentlyCreatedClient();

    @Query(value = "SELECT * FROM clients c JOIN jobs j ON c.client_id = j.created_by WHERE j.job_title = :jobTitle", nativeQuery = true)
    List<ClientEntity> findClientsByPostedJobTitle(@Param("jobTitle") String jobTitle);

    @Query(value = "SELECT * FROM clients WHERE pincode BETWEEN :start AND :end", nativeQuery = true)
    List<ClientEntity> findClientsByPincodeRange(@Param("start") int start, @Param("end") int end);

    @Query("SELECT c FROM ClientEntity c JOIN c.postedJobs j JOIN j.skillRequiredList s WHERE s.skillName = :skillName")
    List<ClientEntity> findClientsBySkillRequired(@Param("skillName") String skillName);

    @Query("SELECT c FROM ClientEntity c JOIN c.postedJobs j WHERE j.createdAt > :date")
    List<ClientEntity> findClientsByJobCreationDate(@Param("date") LocalDateTime date);

    @Query("SELECT c FROM ClientEntity c WHERE c.district.districtName = :district AND c.state.stateName = :state")
    List<ClientEntity> findByDistrictAndState(@Param("district") String districtName, @Param("state") String stateName);

//    @Modifying
//    @Transactional
//    @Query("UPDATE ClientEntity c SET c.verified = true WHERE c.state.stateName = :stateName")
//    List<ClientEntity> verifyClientsInState(@Param("stateName") String stateName);
//
//    @Modifying
//    @Transactional
//    @Query("DELETE FROM ClientEntity c WHERE c.postedJobs IS EMPTY")
//    void deleteClientsWithoutJobs();
}