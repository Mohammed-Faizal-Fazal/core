package com.instafit.core.service;

import com.instafit.core.entity.Item;
import com.instafit.core.repository.ItemRepository;
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
public class ItemService {

    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

    @Autowired
    private ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public List<Item> getAllItems() {
        return itemRepository.findAllByOrderByItemCodeAsc();
    }

    @Transactional(readOnly = true)
    public List<Item> getActiveItems() {
        return itemRepository.findByActive(true);
    }

    @Transactional
    public Item createItem(Item item, String username) {
        if (itemRepository.existsByItemCode(item.getItemCode())) {
            throw new RuntimeException("Item code already exists: " + item.getItemCode());
        }
        item.setCreatedBy(username);
        item.setUpdatedBy(username);
        return itemRepository.save(item);
    }

    @Transactional
    public Item updateItem(Long id, Item updatedItem, String username) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        item.setItemDesc(updatedItem.getItemDesc());
        item.setActive(updatedItem.getActive());
        item.setUpdatedBy(username);

        return itemRepository.save(item);
    }

    @Transactional
    public Item toggleActive(Long id, String username) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        item.setActive(!item.getActive());
        item.setUpdatedBy(username);

        return itemRepository.save(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        itemRepository.deleteById(id);
    }

    public List<Item> uploadFromExcel(MultipartFile file, String username) throws IOException {
        String filename = file.getOriginalFilename();

        if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            logger.info("Detected CSV file: {}", filename);
            return uploadFromCSV(file, username);
        } else {
            logger.info("Detected Excel file: {}", filename);
            return uploadFromExcelFile(file, username);
        }
    }

    private List<Item> uploadFromCSV(MultipartFile file, String username) throws IOException {
        List<Item> items = new ArrayList<>();
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
                    String itemCode = parts[0].trim();
                    String itemDesc = parts[1].trim();

                    if (itemCode.isEmpty()) {
                        errors.add("Line " + lineNumber + ": Item code is empty");
                        continue;
                    }

                    if (itemDesc.isEmpty()) {
                        errors.add("Line " + lineNumber + ": Item description is empty");
                        continue;
                    }

                    Item savedItem = saveItemInNewTransaction(itemCode.toUpperCase(), itemDesc, username);

                    if (savedItem != null) {
                        items.add(savedItem);
                        logger.debug("Successfully created item: {} - {}", itemCode, itemDesc);
                    } else {
                        errors.add("Line " + lineNumber + ": Item code already exists: " + itemCode);
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

        logger.info("Successfully uploaded {} items from CSV (Total lines: {}, Errors: {})",
                items.size(), totalLines, errors.size());
        return items;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Item saveItemInNewTransaction(String itemCode, String itemDesc, String username) {
        try {
            if (itemRepository.existsByItemCode(itemCode)) {
                logger.debug("Item already exists: {}", itemCode);
                return null;
            }

            Item item = new Item(itemCode, itemDesc);
            item.setCreatedBy(username);
            item.setUpdatedBy(username);

            Item saved = itemRepository.save(item);
            itemRepository.flush();

            logger.info("Saved item: {} - {}", itemCode, itemDesc);
            return saved;

        } catch (Exception e) {
            logger.error("Failed to save item {}: {}", itemCode, e.getMessage(), e);
            return null;
        }
    }

    private List<Item> uploadFromExcelFile(MultipartFile file, String username) throws IOException {
        List<Item> items = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int totalRows = sheet.getLastRowNum();

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String itemCode = getCellValue(row.getCell(0));
                    String itemDesc = getCellValue(row.getCell(1));

                    if (itemCode == null || itemCode.trim().isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Item code is empty");
                        continue;
                    }

                    if (itemDesc == null || itemDesc.trim().isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Item description is empty");
                        continue;
                    }

                    Item savedItem = saveItemInNewTransaction(
                            itemCode.trim().toUpperCase(),
                            itemDesc.trim(),
                            username
                    );

                    if (savedItem != null) {
                        items.add(savedItem);
                    } else {
                        errors.add("Row " + (i + 1) + ": Item code already exists: " + itemCode);
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

        logger.info("Successfully uploaded {} items from Excel", items.size());
        return items;
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