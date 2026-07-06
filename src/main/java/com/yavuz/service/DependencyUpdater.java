package com.yavuz.service;

import com.yavuz.model.MicroService;
import com.yavuz.model.DependencyItem;
import com.yavuz.model.Recipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class DependencyUpdater {

    public static void updateService(MicroService service, Recipe recipe, boolean dryRun) throws IOException {
        Path originalPath = Paths.get(service.path);
        List<String> lines = Files.readAllLines(originalPath);
        List<String> updatedLines = new ArrayList<>(lines);

        // Doküman Madde 20 Raporlama Kategorileri
        List<String> updatedItems = new ArrayList<>();
        List<String> skippedItems = new ArrayList<>();
        List<String> unchangedItems = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Map<String, String> propertiesToUpdate = new HashMap<>();

        boolean inParent = false;
        boolean inDependency = false;
        boolean inPlugin = false;

        int versionLineIdx = -1;
        String currentGroupId = null;
        String currentArtifactId = null;
        String currentVersion = null;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (line.contains("<parent>")) {
                inParent = true;
                currentGroupId = currentArtifactId = currentVersion = null;
                versionLineIdx = -1;
            } else if (line.contains("<dependency>")) {
                inDependency = true;
                currentGroupId = currentArtifactId = currentVersion = null;
                versionLineIdx = -1;
            } else if (line.contains("<plugin>") && !line.contains("</plugin>")) {
                inPlugin = true;
                currentGroupId = currentArtifactId = currentVersion = null;
                versionLineIdx = -1;
            }

            if (inParent || inDependency || inPlugin) {
                if (line.contains("<groupId>")) currentGroupId = extractTagValue(line, "groupId");
                if (line.contains("<artifactId>")) currentArtifactId = extractTagValue(line, "artifactId");
                if (line.contains("<version>")) {
                    currentVersion = extractTagValue(line, "version");
                    versionLineIdx = i;
                }
            }

            if (line.contains("</parent>") && inParent) {
                inParent = false;
                DependencyItem match = findMatchingItem(recipe.parentVersions, currentGroupId, currentArtifactId);
                if (match != null) {
                    processMatch(match, currentVersion, versionLineIdx, service.serviceType, updatedLines, propertiesToUpdate, updatedItems, unchangedItems, currentArtifactId);
                } else if (currentArtifactId != null) {
                    skippedItems.add(currentArtifactId + ": present in service but not found in recipe");
                }
            }
            else if (line.contains("</dependency>") && inDependency) {
                inDependency = false;
                DependencyItem match = findMatchingItem(recipe.dependencies, currentGroupId, currentArtifactId);
                if (match == null) {
                    match = findMatchingItem(recipe.dependencyManagement, currentGroupId, currentArtifactId);
                }

                if (match != null) {
                    if (currentVersion == null) {
                        skippedItems.add(currentArtifactId + ": skipped direct version update because version is managed by BOM or parent.");
                        warnings.add(currentArtifactId + " version is managed by BOM, direct version was not added");
                    } else {
                        processMatch(match, currentVersion, versionLineIdx, service.serviceType, updatedLines, propertiesToUpdate, updatedItems, unchangedItems, currentArtifactId);
                    }
                } else if (currentArtifactId != null) {
                    // Doküman Örnek Rapor şablonu eşleşmesi
                    skippedItems.add(currentArtifactId + ": present in service but not found in recipe");
                }
            }
            else if (line.contains("</plugin>") && inPlugin) {
                inPlugin = false;
                DependencyItem match = findMatchingItem(recipe.plugins, currentGroupId, currentArtifactId);
                if (match != null) {
                    processMatch(match, currentVersion, versionLineIdx, service.serviceType, updatedLines, propertiesToUpdate, updatedItems, unchangedItems, currentArtifactId);
                } else if (currentArtifactId != null) {
                    skippedItems.add(currentArtifactId + ": present in service but not found in recipe");
                }
            }
        }

        // Property güncellemeleri
        if (!propertiesToUpdate.isEmpty()) {
            for (int i = 0; i < updatedLines.size(); i++) {
                String line = updatedLines.get(i);
                for (Map.Entry<String, String> entry : propertiesToUpdate.entrySet()) {
                    String propOpenTag = "<" + entry.getKey() + ">";
                    if (line.contains(propOpenTag)) {
                        String currentPropVal = extractTagValue(line, entry.getKey());
                        if (currentPropVal != null && !entry.getValue().equals(currentPropVal)) {
                            String indentation = line.substring(0, line.indexOf(propOpenTag));
                            updatedLines.set(i, indentation + propOpenTag + entry.getValue() + "</" + entry.getKey() + ">");
                            updatedItems.add(entry.getKey() + " (Property): " + currentPropVal + " -> " + entry.getValue());
                        } else if (currentPropVal != null) {
                            unchangedItems.add(entry.getKey() + " (Property): zaten güncel (" + currentPropVal + ")");
                        }
                    }
                }
            }
        }

        // Fiziksel yedekleme ve yazma (Yalnızca gerçek modda)
        if (!dryRun) {
            String recipeVer = (recipe.recipeVersion != null) ? recipe.recipeVersion : "unknown-recipe";
            Path backupDir = Paths.get(".backup", recipeVer, service.name);
            Files.createDirectories(backupDir);
            Path backupFile = backupDir.resolve("pom.xml");

            Files.copy(originalPath, backupFile, StandardCopyOption.REPLACE_EXISTING);
            Files.write(originalPath, updatedLines);
        }

        // Tam uyumlu konsol rapor çıktısı
        printServiceReport(service, updatedItems, skippedItems, unchangedItems, warnings, dryRun);
    }

    private static void processMatch(DependencyItem match, String currentVersion, int versionLineIdx, String serviceType,
                                     List<String> updatedLines, Map<String, String> propertiesToUpdate,
                                     List<String> updatedItems, List<String> unchangedItems, String artifactId) {
        if (match == null || currentVersion == null) return;

        String targetVersion = match.version;
        if (targetVersion == null && match.versions != null) {
            if ("ca".equalsIgnoreCase(serviceType)) {
                targetVersion = match.versions.get("ca");
            } else {
                targetVersion = match.versions.get("nonCa");
            }
        }

        if (targetVersion == null) return;

        if (currentVersion.startsWith("${") && currentVersion.endsWith("}")) {
            String propName = currentVersion.substring(2, currentVersion.length() - 1);
            propertiesToUpdate.put(propName, targetVersion);
        } else {
            if (!targetVersion.equals(currentVersion)) {
                String blockLine = updatedLines.get(versionLineIdx);
                String indentation = blockLine.substring(0, blockLine.indexOf("<version>"));
                updatedLines.set(versionLineIdx, indentation + "<version>" + targetVersion + "</version>");
                updatedItems.add(artifactId + ": " + currentVersion + " -> " + targetVersion);
            } else {
                // Sürüm zaten hedef sürümle aynıysa Değişmeyenlere ekle[cite: 1]
                unchangedItems.add(artifactId + ": zaten güncel (" + currentVersion + ")");
            }
        }
    }

    private static String extractTagValue(String line, String tagName) {
        String openTag = "<" + tagName + ">";
        String closeTag = "</" + tagName + ">";
        if (line.contains(openTag) && line.contains(closeTag)) {
            return line.substring(line.indexOf(openTag) + openTag.length(), line.indexOf(closeTag)).trim();
        }
        return null;
    }

    private static DependencyItem findMatchingItem(List<DependencyItem> list, String groupId, String artifactId) {
        if (list == null || groupId == null || artifactId == null) return null;
        for (DependencyItem item : list) {
            if (groupId.equals(item.groupId) && artifactId.equals(item.artifactId)) {
                return item;
            }
        }
        return null;
    }

    private static void printServiceReport(MicroService service, List<String> updated, List<String> skipped, List<String> unchanged, List<String> warnings, boolean dryRun) {
        System.out.println("\n==================================================");
        System.out.println(dryRun ? "         [DRY-RUN] SİMÜLASYON RAPORU" : "         GERÇEK GÜNCELLEME RAPORU");
        System.out.println("==================================================");
        System.out.println("Service Name: " + service.name);
        System.out.println("Service Path: " + service.path);
        System.out.println("Detected Service Type: " + (service.serviceType != null ? service.serviceType.toUpperCase() : "UNKNOWN"));

        System.out.println("\nUpdated Items:");
        if (updated.isEmpty()) System.out.println("  - None");
        else { for (String s : updated) System.out.println("  - " + s); }

        System.out.println("\nSkipped Items:");
        if (skipped.isEmpty()) System.out.println("  - None");
        else { for (String s : skipped) System.out.println("  - " + s); }

        System.out.println("\nUnchanged Items:");
        if (unchanged.isEmpty()) System.out.println("  - None");
        else { for (String s : unchanged) System.out.println("  - " + s); }

        System.out.println("\nWarnings:");
        if (warnings.isEmpty()) System.out.println("  - None");
        else { for (String s : warnings) System.out.println("  - " + s); }
        System.out.println("==================================================\n");
    }
}