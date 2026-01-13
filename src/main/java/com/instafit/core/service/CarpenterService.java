package com.instafit.core.service;

import com.instafit.core.entity.Branch;
import com.instafit.core.entity.Carpenter;
import com.instafit.core.entity.City;
import com.instafit.core.entity.User;
import com.instafit.core.repository.BranchRepository;
import com.instafit.core.repository.CarpenterRepository;
import com.instafit.core.repository.CityRepository;
import com.instafit.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CarpenterService {

    private static final Logger logger = LoggerFactory.getLogger(CarpenterService.class);

    @Autowired
    private CarpenterRepository carpenterRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<Carpenter> getAllCarpenters() {
        return carpenterRepository.findAllByOrderByCarpenterNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Carpenter> getActiveCarpenters() {
        return carpenterRepository.findByActive(true);
    }

    @Transactional(readOnly = true)
    public List<Carpenter> getCarpentersByCity(String cityCode) {
        return carpenterRepository.findByCityCode(cityCode);
    }

    @Transactional(readOnly = true)
    public List<Carpenter> getCarpentersByBranch(String branchCode) {
        return carpenterRepository.findByBranchCode(branchCode);
    }

    @Transactional
    public Carpenter registerCarpenter(Carpenter carpenter, String username) {
        if (carpenterRepository.existsByCarpenterId(carpenter.getCarpenterId())) {
            throw new RuntimeException("Carpenter ID already exists: " + carpenter.getCarpenterId());
        }

        if (carpenterRepository.existsByMobile(carpenter.getMobile())) {
            throw new RuntimeException("Mobile number already exists: " + carpenter.getMobile());
        }

        if (userRepository.existsByPhoneNumber(carpenter.getMobile())) {
            throw new RuntimeException("Mobile number already registered as user");
        }

        if (carpenter.getCityCode() != null && !carpenter.getCityCode().isEmpty()) {
            City city = cityRepository.findByCityCode(carpenter.getCityCode())
                    .orElseThrow(() -> new RuntimeException("Invalid city code: " + carpenter.getCityCode()));
            carpenter.setCityDesc(city.getCityDesc());
        }

        if (carpenter.getBranchCode() != null && !carpenter.getBranchCode().isEmpty()) {
            Branch branch = branchRepository.findByBranchCode(carpenter.getBranchCode())
                    .orElseThrow(() -> new RuntimeException("Invalid branch code: " + carpenter.getBranchCode()));
            carpenter.setBranchCode(branch.getBranchDesc());
        }

        carpenter.setCreatedBy(username);
        carpenter.setUpdatedBy(username);

        Carpenter savedCarpenter = carpenterRepository.save(carpenter);
        createCarpenterUserAccount(savedCarpenter);

        logger.info("Registered carpenter: {} - {}", savedCarpenter.getCarpenterId(), savedCarpenter.getCarpenterName());

        return savedCarpenter;
    }

    @Transactional
    public Carpenter registerCarpenterFromRegistration(Carpenter carpenter, String username) {
        if (carpenterRepository.existsByCarpenterId(carpenter.getCarpenterId())) {
            throw new RuntimeException("Carpenter ID already exists: " + carpenter.getCarpenterId());
        }

        if (carpenterRepository.existsByMobile(carpenter.getMobile())) {
            throw new RuntimeException("Mobile number already registered as carpenter");
        }

        if (carpenter.getCityCode() != null && !carpenter.getCityCode().isEmpty()) {
            City city = cityRepository.findByCityCode(carpenter.getCityCode())
                    .orElseThrow(() -> new RuntimeException("Invalid city code: " + carpenter.getCityCode()));
            carpenter.setCityDesc(city.getCityDesc());
        }

        if (carpenter.getBranchCode() != null && !carpenter.getBranchCode().isEmpty()) {
            Branch branch = branchRepository.findByBranchCode(carpenter.getBranchCode())
                    .orElseThrow(() -> new RuntimeException("Invalid branch code: " + carpenter.getBranchCode()));
            carpenter.setBranchCode(branch.getBranchDesc());
        }

        carpenter.setCreatedBy(username);
        carpenter.setUpdatedBy(username);

        Carpenter savedCarpenter = carpenterRepository.save(carpenter);
        logger.info("Registered carpenter from registration page: {} - {}", savedCarpenter.getCarpenterId(), savedCarpenter.getCarpenterName());

        return savedCarpenter;
    }

    private void createCarpenterUserAccount(Carpenter carpenter) {
        User user = new User();
        user.setPhoneNumber(carpenter.getMobile());
        user.setFullName(carpenter.getCarpenterName());
        user.setEmail(carpenter.getEmail());
        user.setRole(User.Role.CARPENTER);
        user.setActive(carpenter.getActive());
        user.setPassword(passwordEncoder.encode(carpenter.getMobile()));
        user.setCreatedBy(carpenter.getCreatedBy());
        user.setUpdatedBy(carpenter.getUpdatedBy());

        userRepository.save(user);
        logger.info("Created user account for carpenter: {}", carpenter.getMobile());
    }

    @Transactional
    public Carpenter updateCarpenter(Long id, Carpenter updatedCarpenter, String username) {
        Carpenter carpenter = carpenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carpenter not found"));

        if (updatedCarpenter.getCityCode() != null && !updatedCarpenter.getCityCode().isEmpty()) {
            City city = cityRepository.findByCityCode(updatedCarpenter.getCityCode())
                    .orElseThrow(() -> new RuntimeException("Invalid city code: " + updatedCarpenter.getCityCode()));
            carpenter.setCityCode(updatedCarpenter.getCityCode());
            carpenter.setCityDesc(city.getCityDesc());
        }

        if (updatedCarpenter.getBranchCode() != null && !updatedCarpenter.getBranchCode().isEmpty()) {
            Branch branch = branchRepository.findByBranchCode(updatedCarpenter.getBranchCode())
                    .orElseThrow(() -> new RuntimeException("Invalid branch code: " + updatedCarpenter.getBranchCode()));
            carpenter.setBranchCode(updatedCarpenter.getBranchCode());
            carpenter.setBranchCode(branch.getBranchDesc());
        }

        carpenter.setCarpenterName(updatedCarpenter.getCarpenterName());
        carpenter.setEmail(updatedCarpenter.getEmail());
        carpenter.setJobType(updatedCarpenter.getJobType());
        carpenter.setActive(updatedCarpenter.getActive());
        carpenter.setUpdatedBy(username);

        userRepository.findByPhoneNumber(carpenter.getMobile()).ifPresent(user -> {
            user.setActive(carpenter.getActive());
            user.setFullName(carpenter.getCarpenterName());
            user.setEmail(carpenter.getEmail());
            userRepository.save(user);
        });

        return carpenterRepository.save(carpenter);
    }

    @Transactional
    public Carpenter toggleActive(Long id, String username) {
        Carpenter carpenter = carpenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carpenter not found"));

        carpenter.setActive(!carpenter.getActive());
        carpenter.setUpdatedBy(username);

        userRepository.findByPhoneNumber(carpenter.getMobile()).ifPresent(user -> {
            user.setActive(carpenter.getActive());
            userRepository.save(user);
        });

        return carpenterRepository.save(carpenter);
    }

    @Transactional
    public void deleteCarpenter(Long id) {
        Carpenter carpenter = carpenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carpenter not found"));

        userRepository.findByPhoneNumber(carpenter.getMobile()).ifPresent(user -> {
            userRepository.delete(user);
        });

        carpenterRepository.deleteById(id);
    }
    @Transactional(readOnly = true)
    public Carpenter getCarpenterById(String carpenterId) {
        return carpenterRepository.findByCarpenterId(carpenterId)
                .orElseThrow(() -> new RuntimeException("Carpenter not found: " + carpenterId));
    }

    @Transactional(readOnly = true)
    public Carpenter getCarpenterByMobile(String mobile) {
        return carpenterRepository.findByMobile(mobile)
                .orElseThrow(() -> new RuntimeException("Carpenter not found for mobile: " + mobile));
    }
}