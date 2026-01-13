package com.instafit.core.service;

import com.instafit.core.entity.City;
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
public class CityService {

    private static final Logger logger = LoggerFactory.getLogger(CityService.class);

    @Autowired
    private CityRepository cityRepository;

    @Transactional(readOnly = true)
    public List<City> getAllCities() {
        return cityRepository.findAllByOrderByCityCodeAsc();
    }

    @Transactional(readOnly = true)
    public List<City> getActiveCities() {
        return cityRepository.findByActive(true);
    }

    @Transactional(readOnly = true)
    public City getCityByCode(String cityCode) {
        return cityRepository.findByCityCode(cityCode)
                .orElseThrow(() -> new RuntimeException("City not found: " + cityCode));
    }

    @Transactional
    public City createCity(City city, String username) {
        if (cityRepository.existsByCityCode(city.getCityCode())) {
            throw new RuntimeException("City code already exists: " + city.getCityCode());
        }
        city.setCreatedBy(username);
        city.setUpdatedBy(username);
        return cityRepository.save(city);
    }

    @Transactional
    public City updateCity(Long id, City updatedCity, String username) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("City not found"));

        city.setCityDesc(updatedCity.getCityDesc());
        city.setActive(updatedCity.getActive());
        city.setUpdatedBy(username);

        return cityRepository.save(city);
    }

    @Transactional
    public City toggleActive(Long id, String username) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("City not found"));

        city.setActive(!city.getActive());
        city.setUpdatedBy(username);

        return cityRepository.save(city);
    }

    @Transactional
    public void deleteCity(Long id) {
        cityRepository.deleteById(id);
    }

    public List<City> uploadFromExcel(MultipartFile file, String username) throws IOException {
        String filename = file.getOriginalFilename();

        if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            logger.info("Detected CSV file: {}", filename);
            return uploadFromCSV(file, username);
        } else {
            logger.info("Detected Excel file: {}", filename);
            return uploadFromExcelFile(file, username);
        }
    }

    private List<City> uploadFromCSV(MultipartFile file, String username) throws IOException {
        List<City> cities = new ArrayList<>();
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
                    String cityCode = parts[0].trim();
                    String cityDesc = parts[1].trim();

                    if (cityCode.isEmpty()) {
                        errors.add("Line " + lineNumber + ": City code is empty");
                        continue;
                    }

                    if (cityDesc.isEmpty()) {
                        errors.add("Line " + lineNumber + ": City description is empty");
                        continue;
                    }

                    City savedCity = saveCityInNewTransaction(cityCode.toUpperCase(), cityDesc, username);

                    if (savedCity != null) {
                        cities.add(savedCity);
                        logger.debug("Successfully created city: {} - {}", cityCode, cityDesc);
                    } else {
                        errors.add("Line " + lineNumber + ": City code already exists: " + cityCode);
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

        logger.info("Successfully uploaded {} cities from CSV (Total lines: {}, Errors: {})",
                cities.size(), totalLines, errors.size());
        return cities;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public City saveCityInNewTransaction(String cityCode, String cityDesc, String username) {
        try {
            if (cityRepository.existsByCityCode(cityCode)) {
                logger.debug("City already exists: {}", cityCode);
                return null;
            }

            City city = new City(cityCode, cityDesc);
            city.setCreatedBy(username);
            city.setUpdatedBy(username);

            City saved = cityRepository.save(city);
            cityRepository.flush();

            logger.info("Saved city: {} - {}", cityCode, cityDesc);
            return saved;

        } catch (Exception e) {
            logger.error("Failed to save city {}: {}", cityCode, e.getMessage(), e);
            return null;
        }
    }

    private List<City> uploadFromExcelFile(MultipartFile file, String username) throws IOException {
        List<City> cities = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String cityCode = getCellValue(row.getCell(0));
                    String cityDesc = getCellValue(row.getCell(1));

                    if (cityCode == null || cityCode.trim().isEmpty()) {
                        errors.add("Row " + (i + 1) + ": City code is empty");
                        continue;
                    }

                    if (cityDesc == null || cityDesc.trim().isEmpty()) {
                        errors.add("Row " + (i + 1) + ": City description is empty");
                        continue;
                    }

                    City savedCity = saveCityInNewTransaction(
                            cityCode.trim().toUpperCase(),
                            cityDesc.trim(),
                            username
                    );

                    if (savedCity != null) {
                        cities.add(savedCity);
                    } else {
                        errors.add("Row " + (i + 1) + ": City code already exists: " + cityCode);
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

        logger.info("Successfully uploaded {} cities from Excel", cities.size());
        return cities;
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