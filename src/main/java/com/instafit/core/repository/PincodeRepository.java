package com.instafit.core.repository;

import com.instafit.core.entity.Pincode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PincodeRepository extends JpaRepository<Pincode, Long> {

    Optional<Pincode> findByPincode(String pincode);

    boolean existsByPincode(String pincode);

    List<Pincode> findByCityCode(String cityCode);

    List<Pincode> findByActive(Boolean active);

    List<Pincode> findAllByOrderByPincodeAsc();
}