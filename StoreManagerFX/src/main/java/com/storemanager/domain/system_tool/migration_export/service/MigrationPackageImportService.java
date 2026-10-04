package com.storemanager.domain.system_tool.migration_export.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.storemanager.core.storage.ImageStorageService;
import com.storemanager.domain.category.model.Category;
import com.storemanager.domain.category.service.CategoryService;
import com.storemanager.domain.inventory.model.InventoryItem;
import com.storemanager.domain.inventory.model.InventoryTransaction;
import com.storemanager.domain.inventory.model.InventoryTransactionType;
import com.storemanager.domain.inventory.service.InventoryService;
import com.storemanager.domain.offline_export.OfflineExportSpecV1;
import com.storemanager.domain.offline_export.dto.CategorySnapshot;
import com.storemanager.domain.offline_export.dto.InventorySnapshot;
import com.storemanager.domain.offline_export.dto.ProductSnapshot;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.product.service.ProductService;
import com.storemanager.domain.system_tool.migration_export.model.MigrationImportResult;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MigrationPackageImportService {

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final CategoryService categoryService =
            new CategoryService();

    private final ProductService productService =
            new ProductService();

    private final InventoryService inventoryService =
            new InventoryService();

    private final ImageStorageService imageStorageService =
            new ImageStorageService();

    public MigrationImportResult importProducts(
            File packageFile
    ) {

        if (packageFile == null || !packageFile.isFile()) {
            throw new RuntimeException("Package file is required");
        }

        try (ZipFile zipFile = new ZipFile(packageFile)) {

            List<CategorySnapshot> categorySnapshots =
                    readList(
                            zipFile,
                            OfflineExportSpecV1.CATEGORIES_FILE,
                            new TypeReference<>() {
                            }
                    );

            List<ProductSnapshot> productSnapshots =
                    readList(
                            zipFile,
                            OfflineExportSpecV1.PRODUCTS_FILE,
                            new TypeReference<>() {
                            }
                    );

            List<InventorySnapshot> inventorySnapshots =
                    readList(
                            zipFile,
                            OfflineExportSpecV1.INVENTORY_FILE,
                            new TypeReference<>() {
                            }
                    );

            Map<Long, Long> categoryIdsBySourceId =
                    importCategories(
                            zipFile,
                            categorySnapshots
                    );

            Map<Long, Long> productIdsBySourceId =
                    importProducts(
                            zipFile,
                            productSnapshots,
                            categoryIdsBySourceId
                    );

            int inventoryCount =
                    importInventory(
                            inventorySnapshots,
                            productIdsBySourceId
                    );

            return new MigrationImportResult(
                    categorySnapshots.size(),
                    productSnapshots.size(),
                    inventoryCount
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot import business package products",
                    e
            );
        }
    }

    private Map<Long, Long> importCategories(
            ZipFile zipFile,
            List<CategorySnapshot> snapshots
    ) {

        Map<Long, Long> idsBySourceId =
                new HashMap<>();

        Map<String, Category> categoriesByName =
                categoriesByName();

        for (CategorySnapshot snapshot : snapshots) {
            if (snapshot == null
                    || snapshot.name() == null
                    || snapshot.name().trim().isEmpty()) {
                continue;
            }

            String key =
                    normalize(snapshot.name());

            Category category =
                    categoriesByName.getOrDefault(
                            key,
                            new Category()
                    );

            category.setName(snapshot.name());
            category.setActive(snapshot.active());
            category.setImagePath(
                    importCategoryImage(
                            zipFile,
                            snapshot.imagePath(),
                            category.getImagePath()
                    )
            );

            boolean success =
                    category.getId() == null
                            ? categoryService.create(category)
                            : categoryService.update(category);

            if (!success) {
                throw new RuntimeException(
                        "Cannot import category " + snapshot.name()
                );
            }

            Category refreshed =
                    findCategoryByName(snapshot.name())
                            .orElse(category);

            if (snapshot.sourceId() != null && refreshed.getId() != null) {
                idsBySourceId.put(
                        snapshot.sourceId(),
                        refreshed.getId()
                );
            }

            categoriesByName.put(
                    key,
                    refreshed
            );
        }

        return idsBySourceId;
    }

    private Map<Long, Long> importProducts(
            ZipFile zipFile,
            List<ProductSnapshot> snapshots,
            Map<Long, Long> categoryIdsBySourceId
    ) {

        Map<Long, Long> idsBySourceId =
                new HashMap<>();

        Map<String, Product> productsBySku =
                productsBySku();

        for (ProductSnapshot snapshot : snapshots) {
            if (snapshot == null
                    || snapshot.sku() == null
                    || snapshot.sku().trim().isEmpty()) {
                continue;
            }

            String key =
                    normalize(snapshot.sku());

            Product product =
                    productsBySku.getOrDefault(
                            key,
                            new Product()
                    );

            product.setName(snapshot.name());
            product.setSku(snapshot.sku());
            product.setCategoryId(
                    categoryIdsBySourceId.get(
                            snapshot.sourceCategoryId()
                    )
            );
            product.setBasePrice(snapshot.basePrice());
            product.setUnit(snapshot.unit());
            product.setActive(snapshot.active());
            product.setImagePath(
                    importProductImage(
                            zipFile,
                            snapshot.imagePath(),
                            product.getImagePath()
                    )
            );

            boolean success =
                    product.getId() == null
                            ? productService.create(product)
                            : productService.update(product);

            if (!success) {
                throw new RuntimeException(
                        "Cannot import product " + snapshot.sku()
                );
            }

            Product refreshed =
                    findProductBySku(snapshot.sku())
                            .orElse(product);

            if (snapshot.sourceId() != null && refreshed.getId() != null) {
                idsBySourceId.put(
                        snapshot.sourceId(),
                        refreshed.getId()
                );
            }

            productsBySku.put(
                    key,
                    refreshed
            );
        }

        return idsBySourceId;
    }

    private int importInventory(
            List<InventorySnapshot> snapshots,
            Map<Long, Long> productIdsBySourceId
    ) {

        Map<Long, Integer> currentQuantityByProductId =
                currentQuantityByProductId();

        int importedCount = 0;

        for (InventorySnapshot snapshot : snapshots) {
            if (snapshot == null || snapshot.sourceProductId() == null) {
                continue;
            }

            Long productId =
                    productIdsBySourceId.get(
                            snapshot.sourceProductId()
                    );

            if (productId == null) {
                continue;
            }

            int currentQuantity =
                    currentQuantityByProductId.getOrDefault(
                            productId,
                            0
                    );

            int delta =
                    snapshot.quantity() - currentQuantity;

            if (delta != 0) {
                InventoryTransaction transaction =
                        new InventoryTransaction();

                transaction.setProductId(productId);
                transaction.setType(InventoryTransactionType.ADJUSTMENT);
                transaction.setQuantity(delta);
                transaction.setReason("Business package import");

                if (!inventoryService.adjustStock(transaction)) {
                    throw new RuntimeException(
                            "Cannot import inventory for product " + productId
                    );
                }
            }

            currentQuantityByProductId.put(
                    productId,
                    snapshot.quantity()
            );
            importedCount++;
        }

        return importedCount;
    }

    private <T> List<T> readList(
            ZipFile zipFile,
            String entryName,
            TypeReference<List<T>> typeReference
    ) throws Exception {

        ZipEntry entry =
                zipFile.getEntry(entryName);

        if (entry == null) {
            return List.of();
        }

        try (
                InputStream inputStream =
                        zipFile.getInputStream(entry)
        ) {

            List<T> items =
                    objectMapper.readValue(
                            inputStream,
                            typeReference
                    );

            return items == null ? List.of() : items;
        }
    }

    private String importCategoryImage(
            ZipFile zipFile,
            String imagePath,
            String existingImagePath
    ) {

        return importImage(
                zipFile,
                imagePath,
                existingImagePath,
                true
        );
    }

    private String importProductImage(
            ZipFile zipFile,
            String imagePath,
            String existingImagePath
    ) {

        return importImage(
                zipFile,
                imagePath,
                existingImagePath,
                false
        );
    }

    private String importImage(
            ZipFile zipFile,
            String imagePath,
            String existingImagePath,
            boolean category
    ) {

        if (imagePath == null || imagePath.trim().isEmpty()) {
            return existingImagePath;
        }

        ZipEntry imageEntry =
                zipFile.getEntry(
                        imagePath.trim()
                );

        if (imageEntry == null) {
            return existingImagePath;
        }

        Path tempFile = null;

        try (
                InputStream inputStream =
                        zipFile.getInputStream(imageEntry)
        ) {

            tempFile =
                    Files.createTempFile(
                            "migration-image-",
                            safeExtension(imagePath)
                    );

            Files.copy(
                    inputStream,
                    tempFile,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            return category
                    ? imageStorageService.saveCategoryImage(tempFile.toFile())
                    : imageStorageService.saveProductImage(tempFile.toFile());

        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot import image " + imagePath,
                    e
            );
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                    // Best-effort cleanup.
                }
            }
        }
    }

    private Map<String, Category> categoriesByName() {

        Map<String, Category> categoriesByName =
                new HashMap<>();

        for (Category category : categoryService.findAll()) {
            categoriesByName.put(
                    normalize(category.getName()),
                    category
            );
        }

        return categoriesByName;
    }

    private Map<String, Product> productsBySku() {

        Map<String, Product> productsBySku =
                new HashMap<>();

        for (Product product : productService.findAll()) {
            productsBySku.put(
                    normalize(product.getSku()),
                    product
            );
        }

        return productsBySku;
    }

    private Optional<Category> findCategoryByName(
            String name
    ) {

        String key =
                normalize(name);

        return categoryService.findAll()
                .stream()
                .filter(category ->
                        normalize(category.getName()).equals(key)
                )
                .findFirst();
    }

    private Optional<Product> findProductBySku(
            String sku
    ) {

        String key =
                normalize(sku);

        return productService.findAll()
                .stream()
                .filter(product ->
                        normalize(product.getSku()).equals(key)
                )
                .findFirst();
    }

    private Map<Long, Integer> currentQuantityByProductId() {

        Map<Long, Integer> quantities =
                new HashMap<>();

        for (InventoryItem item : inventoryService.findAllItems()) {
            quantities.put(
                    item.getProductId(),
                    item.getQuantity()
            );
        }

        return quantities;
    }

    private String normalize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String safeExtension(
            String fileName
    ) {

        int dotIndex =
                fileName == null ? -1 : fileName.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return ".png";
        }

        String extension =
                fileName.substring(dotIndex).toLowerCase(Locale.ROOT);

        return switch (extension) {
            case ".png", ".jpg", ".jpeg", ".gif", ".bmp" -> extension;
            default -> ".png";
        };
    }
}
