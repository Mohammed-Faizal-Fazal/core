package com.instafit.core.repository;

import com.instafit.core.entity.Carpenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarpenterRepository extends JpaRepository<Carpenter, Long> {
    Optional<Carpenter> findByCarpenterId(String carpenterId);
    Optional<Carpenter> findByMobile(String mobile);
    boolean existsByCarpenterId(String carpenterId);
    boolean existsByMobile(String mobile);
    List<Carpenter> findByActive(Boolean active);
    List<Carpenter> findByCityCode(String cityCode);
    List<Carpenter> findByBranchCode(String branchCode);
    List<Carpenter> findAllByOrderByCarpenterNameAsc();
}