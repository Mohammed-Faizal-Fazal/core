package com.instafit.core.service;

import com.instafit.core.entity.FitType;
import com.instafit.core.repository.FitTypeRepository;
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
public class FitTypeService {

    private static final Logger logger = LoggerFactory.getLogger(FitTypeService.class);

    @Autowired
    private FitTypeRepository fitTypeRepository;

    @Transactional(readOnly = true)
    public List<FitType> getAllFitTypes() {
        return fitTypeRepository.findAllByOrderByFitTypeCodeAsc();
    }

    @Transactional(readOnly = true)
    public List<FitType> getActiveFitTypes() {
        return fitTypeRepository.findByActive(true);
    }

    @Transactional
    public FitType createFitType(FitType fitType, String username) {
        if (fitTypeRepository.existsByFitTypeCode(fitType.getFitTypeCode())) {
            throw new RuntimeException("Fit type code already exists: " + fitType.getFitTypeCode());
        }
        fitType.setCreatedBy(username);
        fitType.setUpdatedBy(username);
        return fitTypeRepository.save(fitType);
    }

    @Transactional
    public FitType updateFitType(Long id, FitType updatedFitType, String username) {
        FitType fitType = fitTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fit type not found"));

        fitType.setFitTypeDesc(updatedFitType.getFitTypeDesc());
        fitType.setActive(updatedFitType.getActive());
        fitType.setUpdatedBy(username);

        return fitTypeRepository.save(fitType);
    }

    @Transactional
    public FitType toggleActive(Long id, String username) {
        FitType fitType = fitTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fit type not found"));

        fitType.setActive(!fitType.getActive());
        fitType.setUpdatedBy(username);

        return fitTypeRepository.save(fitType);
    }

    @Transactional
    public void deleteFitType(Long id) {
        fitTypeRepository.deleteById(id);
    }

    public List<FitType> uploadFromExcel(MultipartFile file, String username) throws IOException {
        String filename = file.getOriginalFilename();

        if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            logger.info("Detected CSV file: {}", filename);
            return uploadFromCSV(file, username);
        } else {
            logger.info("Detected Excel file: {}", filename);
            return uploadFromExcelFile(file, username);
        }
    }

    private List<FitType> uploadFromCSV(MultipartFile file, String username) throws IOException {
        List<FitType> fitTypes = new ArrayList<>();
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
                    errors.add("Line " + lineNumber + ": Invalid format, expected 2 columns");
                    continue;
                }

                try {
                    String fitTypeCode = parts[0].trim();
                    String fitTypeDesc = parts[1].trim();

                    if (fitTypeCode.isEmpty()) {
                        errors.add("Line " + lineNumber + ": Fit type code is empty");
                        continue;
                    }

                    if (fitTypeDesc.isEmpty()) {
                        errors.add("Line " + lineNumber + ": Fit type description is empty");
                        continue;
                    }

                    FitType savedFitType = saveFitTypeInNewTransaction(fitTypeCode.toUpperCase(), fitTypeDesc, username);

                    if (savedFitType != null) {
                        fitTypes.add(savedFitType);
                        logger.debug("Successfully created fit type: {} - {}", fitTypeCode, fitTypeDesc);
                    } else {
                        errors.add("Line " + lineNumber + ": Fit type code already exists: " + fitTypeCode);
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

        logger.info("Successfully uploaded {} fit types from CSV (Total lines: {}, Errors: {})",
                fitTypes.size(), totalLines, errors.size());
        return fitTypes;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FitType saveFitTypeInNewTransaction(String fitTypeCode, String fitTypeDesc, String username) {
        try {
            if (fitTypeRepository.existsByFitTypeCode(fitTypeCode)) {
                logger.debug("Fit type already exists: {}", fitTypeCode);
                return null;
            }

            FitType fitType = new FitType(fitTypeCode, fitTypeDesc);
            fitType.setCreatedBy(username);
            fitType.setUpdatedBy(username);

            FitType saved = fitTypeRepository.save(fitType);
            fitTypeRepository.flush();

            logger.info("Saved fit type: {} - {}", fitTypeCode, fitTypeDesc);
            return saved;

        } catch (Exception e) {
            logger.error("Failed to save fit type {}: {}", fitTypeCode, e.getMessage(), e);
            return null;
        }
    }

    private List<FitType> uploadFromExcelFile(MultipartFile file, String username) throws IOException {
        List<FitType> fitTypes = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int totalRows = sheet.getLastRowNum();

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String fitTypeCode = getCellValue(row.getCell(0));
                    String fitTypeDesc = getCellValue(row.getCell(1));

                    if (fitTypeCode == null || fitTypeCode.trim().isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Fit type code is empty");
                        continue;
                    }

                    if (fitTypeDesc == null || fitTypeDesc.trim().isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Fit type description is empty");
                        continue;
                    }

                    FitType savedFitType = saveFitTypeInNewTransaction(
                            fitTypeCode.trim().toUpperCase(),
                            fitTypeDesc.trim(),
                            username
                    );

                    if (savedFitType != null) {
                        fitTypes.add(savedFitType);
                    } else {
                        errors.add("Row " + (i + 1) + ": Fit type code already exists: " + fitTypeCode);
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

        logger.info("Successfully uploaded {} fit types from Excel", fitTypes.size());
        return fitTypes;
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