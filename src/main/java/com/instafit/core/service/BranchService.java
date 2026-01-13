package com.instafit.core.service;

import com.instafit.core.entity.Branch;
import com.instafit.core.repository.BranchRepository;
import com.instafit.core.repository.CityRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class BranchService {

    private static final Logger logger = LoggerFactory.getLogger(BranchService.class);

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CityRepository cityRepository;

    @Transactional(readOnly = true)
    public List<Branch> getAllBranches() {
        return branchRepository.findAllByOrderByBranchCodeAsc();
    }

    @Transactional(readOnly = true)
    public List<Branch> getActiveBranches() {
        return branchRepository.findByActive(true);
    }

    @Transactional
    public Branch createBranch(Branch branch, String username) {
        if (branchRepository.existsByBranchCode(branch.getBranchCode())) {
            throw new RuntimeException("Branch code already exists: " + branch.getBranchCode());
        }

        validateCityCodes(branch.getCityCodes());

        branch.setCreatedBy(username);
        branch.setUpdatedBy(username);
        return branchRepository.save(branch);
    }

    @Transactional
    public Branch updateBranch(Long id, Branch updatedBranch, String username) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        validateCityCodes(updatedBranch.getCityCodes());

        branch.setBranchDesc(updatedBranch.getBranchDesc());
        branch.setCityCodes(updatedBranch.getCityCodes());
        branch.setActive(updatedBranch.getActive());
        branch.setUpdatedBy(username);

        return branchRepository.save(branch);
    }

    @Transactional
    public Branch toggleActive(Long id, String username) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        branch.setActive(!branch.getActive());
        branch.setUpdatedBy(username);

        return branchRepository.save(branch);
    }

    @Transactional
    public void deleteBranch(Long id) {
        branchRepository.deleteById(id);
    }

    public List<Branch> uploadFromExcel(MultipartFile file, String username) throws IOException {
        String filename = file.getOriginalFilename();

        if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            logger.info("Detected CSV file: {}", filename);
            return uploadFromCSV(file, username);
        } else {
            logger.info("Detected Excel file: {}", filename);
            return uploadFromExcelFile(file, username);
        }
    }

    private List<Branch> uploadFromCSV(MultipartFile file, String username) throws IOException {
        List<Branch> branches = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalLines = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (lineNumber == 1) {
                    logger.debug("Skipping header: {}", line);
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                totalLines++;
                String[] parts = parseCsvLine(line);

                if (parts.length < 2) {
                    errors.add("Line " + lineNumber + ": Invalid format, expected at least 2 columns");
                    continue;
                }

                try {
                    String branchCode = parts[0].trim();
                    String branchDesc = parts[1].trim();
                    String cityCodes = parts.length > 2 ? parts[2].trim() : "";

                    if (branchCode.isEmpty()) {
                        errors.add("Line " + lineNumber + ": Branch code is empty");
                        continue;
                    }

                    if (branchDesc.isEmpty()) {
                        errors.add("Line " + lineNumber + ": Branch description is empty");
                        continue;
                    }

                    // Validate city codes
                    try {
                        validateCityCodes(cityCodes);
                    } catch (RuntimeException e) {
                        errors.add("Line " + lineNumber + ": " + e.getMessage());
                        continue;
                    }

                    Branch savedBranch = saveBranchInNewTransaction(
                            branchCode.toUpperCase(),
                            branchDesc,
                            cityCodes.isEmpty() ? null : cityCodes.toUpperCase(),
                            username
                    );

                    if (savedBranch != null) {
                        branches.add(savedBranch);
                        logger.debug("Successfully created branch: {} - {}", branchCode, branchDesc);
                    } else {
                        errors.add("Line " + lineNumber + ": Branch code already exists: " + branchCode);
                    }

                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                    logger.error("Error processing line {}: {}", lineNumber, line, e);
                }
            }
        }

        if (!errors.isEmpty()) {
            logger.warn("CSV upload completed with {} errors out of {} total lines", errors.size(), totalLines);
            if (errors.size() <= 10) {
                logger.warn("Errors: {}", String.join("; ", errors));
            } else {
                logger.warn("First 10 errors: {}", String.join("; ", errors.subList(0, 10)));
            }
        }

        logger.info("Successfully uploaded {} branches from CSV (Total lines: {}, Errors: {})",
                branches.size(), totalLines, errors.size());
        return branches;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Branch saveBranchInNewTransaction(String branchCode, String branchDesc, String cityCodes, String username) {
        try {
            if (branchRepository.existsByBranchCode(branchCode)) {
                logger.debug("Branch already exists: {}", branchCode);
                return null;
            }

            Branch branch = new Branch(branchCode, branchDesc, cityCodes);
            branch.setCreatedBy(username);
            branch.setUpdatedBy(username);

            Branch saved = branchRepository.save(branch);
            branchRepository.flush();

            logger.info("Saved branch: {} - {}", branchCode, branchDesc);
            return saved;

        } catch (Exception e) {
            logger.error("Failed to save branch {}: {}", branchCode, e.getMessage(), e);
            return null;
        }
    }

    private List<Branch> uploadFromExcelFile(MultipartFile file, String username) throws IOException {
        List<Branch> branches = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String branchCode = getCellValue(row.getCell(0));
                    String branchDesc = getCellValue(row.getCell(1));
                    String cityCodes = getCellValue(row.getCell(2));

                    if (branchCode == null || branchCode.trim().isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Branch code is empty");
                        continue;
                    }

                    if (branchDesc == null || branchDesc.trim().isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Branch description is empty");
                        continue;
                    }

                    try {
                        validateCityCodes(cityCodes);
                    } catch (RuntimeException e) {
                        errors.add("Row " + (i + 1) + ": " + e.getMessage());
                        continue;
                    }

                    Branch savedBranch = saveBranchInNewTransaction(
                            branchCode.trim().toUpperCase(),
                            branchDesc.trim(),
                            cityCodes != null && !cityCodes.trim().isEmpty() ? cityCodes.trim().toUpperCase() : null,
                            username
                    );

                    if (savedBranch != null) {
                        branches.add(savedBranch);
                    } else {
                        errors.add("Row " + (i + 1) + ": Branch code already exists: " + branchCode);
                    }

                } catch (Exception e) {
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        if (!errors.isEmpty()) {
            logger.warn("Excel upload completed with {} errors", errors.size());
            if (errors.size() <= 10) {
                logger.warn("Errors: {}", String.join("; ", errors));
            } else {
                logger.warn("First 10 errors: {}", String.join("; ", errors.subList(0, 10)));
            }
        }

        logger.info("Successfully uploaded {} branches from Excel", branches.size());
        return branches;
    }

    private void validateCityCodes(String cityCodes) {
        if (cityCodes == null || cityCodes.trim().isEmpty()) {
            return; // Optional field
        }

        String[] codes = cityCodes.split(",");
        List<String> invalidCities = new ArrayList<>();

        for (String code : codes) {
            String trimmedCode = code.trim().toUpperCase();
            if (!trimmedCode.isEmpty() && !cityRepository.existsByCityCode(trimmedCode)) {
                invalidCities.add(trimmedCode);
            }
        }

        if (!invalidCities.isEmpty()) {
            throw new RuntimeException("Invalid city codes: " + String.join(", ", invalidCities));
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());

        return result.toArray(new String[0]);
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue)) {
                    return String.valueOf((long) numValue);
                }
                return String.valueOf(numValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
}