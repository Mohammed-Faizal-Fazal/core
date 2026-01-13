package com.instafit.core.repository;

import com.instafit.core.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByBranchCode(String branchCode);

    boolean existsByBranchCode(String branchCode);

    List<Branch> findByActive(Boolean active);

    List<Branch> findAllByOrderByBranchCodeAsc();

    // Find branches serving a specific city
    @Query("SELECT b FROM Branch b WHERE b.cityCodes LIKE %:cityCode%")
    List<Branch> findByCityCode(@Param("cityCode") String cityCode);
}