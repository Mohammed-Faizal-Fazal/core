package com.instafit.core.service;

import com.instafit.core.entity.City;
import com.instafit.core.entity.Pincode;
import com.instafit.core.repository.CityRepository;
import com.instafit.core.repository.PincodeRepository;
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
public class PincodeService {

    private static final Logger logger = LoggerFactory.getLogger(PincodeService.class);

    @Autowired
    private PincodeRepository pincodeRepository;

    @Autowired
    private CityRepository cityRepository;

    @Transactional(readOnly = true)
    public List<Pincode> getAllPincodes() {
        return pincodeRepository.findAllByOrderByPincodeAsc();
    }

    @Transactional(readOnly = true)
    public List<Pincode> getActivePincodes() {
        return pincodeRepository.findByActive(true);
    }

    @Transactional(readOnly = true)
    public List<Pincode> getPincodesByCity(String cityCode) {
        return pincodeRepository.findByCityCode(cityCode);
    }

    @Transactional
    public Pincode createPincode(Pincode pincode, String username) {
        if (pincodeRepository.existsByPincode(pincode.getPincode())) {
            throw new RuntimeException("Pincode already exists: " + pincode.getPincode());
        }

        City city = cityRepository.findByCityCode(pincode.getCityCode())
                .orElseThrow(() -> new RuntimeException("Invalid city code: " + pincode.getCityCode()));

        pincode.setCityDesc(city.getCityDesc());
        pincode.setCreatedBy(username);
        pincode.setUpdatedBy(username);

        return pincodeRepository.save(pincode);
    }

    @Transactional
    public Pincode updatePincode(Long id, Pincode updatedPincode, String username) {
        Pincode pincode = pincodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pincode not found"));

        City city = cityRepository.findByCityCode(updatedPincode.getCityCode())
                .orElseThrow(() -> new RuntimeException("Invalid city code: " + updatedPincode.getCityCode()));

        pincode.setCityCode(updatedPincode.getCityCode());
        pincode.setCityDesc(city.getCityDesc());
        pincode.setArea(updatedPincode.getArea());
        pincode.setActive(updatedPincode.getActive());
        pincode.setUpdatedBy(username);

        return pincodeRepository.save(pincode);
    }

    @Transactional
    public Pincode toggleActive(Long id, String username) {
        Pincode pincode = pincodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pincode not found"));

        pincode.setActive(!pincode.getActive());
        pincode.setUpdatedBy(username);

        return pincodeRepository.save(pincode);
    }

    @Transactional
    public void deletePincode(Long id) {
        pincodeRepository.deleteById(id);
    }

    public List<Pincode> uploadFromExcel(MultipartFile file, String username) throws IOException {
        String filename = file.getOriginalFilename();

        if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            logger.info("Detected CSV file: {}", filename);
            return uploadFromCSV(file, username);
        } else {
            logger.info("Detected Excel file: {}", filename);
            return uploadFromExcelFile(file, username);
        }
    }

    private List<Pincode> uploadFromCSV(MultipartFile file, String username) throws IOException {
        List<Pincode> pincodes = new ArrayList<>();
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
                    String pincode = parts[0].trim();
                    String cityCode = parts[1].trim();
                    String area = parts.length > 2 ? parts[2].trim() : "";

                    if (pincode.isEmpty()) {
                        errors.add("Line " + lineNumber + ": Pincode is empty");
                        continue;
                    }

                    if (cityCode.isEmpty()) {
                        errors.add("Line " + lineNumber + ": City code is empty");
                        continue;
                    }

                    // Validate city code
                    City city = cityRepository.findByCityCode(cityCode.toUpperCase()).orElse(null);
                    if (city == null) {
                        errors.add("Line " + lineNumber + ": Invalid city code: " + cityCode);
                        continue;
                    }

                    Pincode savedPincode = savePincodeInNewTransaction(
                            pincode,
                            cityCode.toUpperCase(),
                            city.getCityDesc(),
                            area.isEmpty() ? null : area,
                            username
                    );

                    if (savedPincode != null) {
                        pincodes.add(savedPincode);
                        logger.debug("Successfully created pincode: {} - {} ({})", pincode, cityCode, area);
                    } else {
                        errors.add("Line " + lineNumber + ": Pincode already exists: " + pincode);
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

        logger.info("Successfully uploaded {} pincodes from CSV (Total lines: {}, Errors: {})",
                pincodes.size(), totalLines, errors.size());
        return pincodes;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pincode savePincodeInNewTransaction(String pincode, String cityCode, String cityDesc, String area, String username) {
        try {
            if (pincodeRepository.existsByPincode(pincode)) {
                logger.debug("Pincode already exists: {}", pincode);
                return null;
            }

            Pincode pincodeEntity = new Pincode(pincode, cityCode);
            pincodeEntity.setCityDesc(cityDesc);
            pincodeEntity.setArea(area);
            pincodeEntity.setCreatedBy(username);
            pincodeEntity.setUpdatedBy(username);

            Pincode saved = pincodeRepository.save(pincodeEntity);
            pincodeRepository.flush();

            logger.info("Saved pincode: {} - {}", pincode, cityCode);
            return saved;

        } catch (Exception e) {
            logger.error("Failed to save pincode {}: {}", pincode, e.getMessage(), e);
            return null;
        }
    }

    private List<Pincode> uploadFromExcelFile(MultipartFile file, String username) throws IOException {
        List<Pincode> pincodes = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String pincode = getCellValue(row.getCell(0));
                    String cityCode = getCellValue(row.getCell(1));
                    String area = getCellValue(row.getCell(2));

                    if (pincode == null || pincode.trim().isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Pincode is empty");
                        continue;
                    }

                    if (cityCode == null || cityCode.trim().isEmpty()) {
                        errors.add("Row " + (i + 1) + ": City code is empty");
                        continue;
                    }

                    City city = cityRepository.findByCityCode(cityCode.trim().toUpperCase()).orElse(null);
                    if (city == null) {
                        errors.add("Row " + (i + 1) + ": Invalid city code: " + cityCode);
                        continue;
                    }

                    Pincode savedPincode = savePincodeInNewTransaction(
                            pincode.trim(),
                            cityCode.trim().toUpperCase(),
                            city.getCityDesc(),
                            area != null && !area.trim().isEmpty() ? area.trim() : null,
                            username
                    );

                    if (savedPincode != null) {
                        pincodes.add(savedPincode);
                    } else {
                        errors.add("Row " + (i + 1) + ": Pincode already exists: " + pincode);
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

        logger.info("Successfully uploaded {} pincodes from Excel", pincodes.size());
        return pincodes;
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