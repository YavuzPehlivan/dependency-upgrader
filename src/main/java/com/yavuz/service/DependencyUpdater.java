package com.yavuz.service;

import com.yavuz.model.MicroService;
import com.yavuz.model.DependencyItem;
import com.yavuz.model.Recipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DependencyUpdater {

    public static void updateService(MicroService service, Recipe recipe, boolean dryRun) throws IOException {
        // pom.xml dosyasını satır satır belleğe okuyoruz
        List<String> lines = Files.readAllLines(Paths.get(service.path));
        List<String> updatedLines = new ArrayList<>();

        // Algoritma Durum Takip Değişkenleri (State Machine)
        boolean inDependencyBlock = false;
        int blockStartIdx = -1;

        String currentGroupId = null;
        String currentArtifactId = null;

        // Satır satır tarama başlıyor
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            updatedLines.add(line); // Varsayılan olarak satırı aynen koruyoruz

            // 1. Bağımlılık bloğunun başlangıcını yakala
            if (line.contains("<dependency>")) {
                inDependencyBlock = true;
                blockStartIdx = i; // Bloğun başladığı satır indeksini tut
                currentGroupId = null;
                currentArtifactId = null;
            }

            // 2. Blok içindeyken groupId ve artifactId değerlerini ayıkla
            if (inDependencyBlock) {
                if (line.contains("<groupId>")) {
                    currentGroupId = extractTagValue(line, "groupId");
                }
                if (line.contains("<artifactId>")) {
                    currentArtifactId = extractTagValue(line, "artifactId");
                }
            }

            // 3. Bağımlılık bloğunun bitişini yakala ve analizi yap
            if (line.contains("</dependency>")) {
                inDependencyBlock = false;

                // Recipe listesinde bu kütüphane var mı diye bakıyoruz
                DependencyItem targetDependency = findMatchingDependency(recipe, currentGroupId, currentArtifactId);

                if (targetDependency != null) {
                    // Servisin tipine göre (CA / Non-CA) alması gereken hedef versiyonu belirliyoruz
                    String targetVersion = determineTargetVersion(targetDependency, service.serviceType);

                    if (targetVersion != null) {
                        // Blok başlangıcı ile bitişi arasındaki <version> satırını bulup güncelliyoruz
                        for (int k = blockStartIdx; k <= i; k++) {
                            String blockLine = lines.get(k);
                            if (blockLine.contains("<version>")) {
                                String currentVersion = extractTagValue(blockLine, "version");

                                // Eğer zaten hedef versiyondaysa işlem yapmaya gerek yok
                                if (targetVersion.equals(currentVersion)) {
                                    System.out.println("  -> [" + currentArtifactId + "] Zaten güncel: " + targetVersion);
                                    break;
                                }

                                // Girintiyi (boşlukları) korumak için sol taraftaki boşlukları hesapla
                                String indentation = blockLine.substring(0, blockLine.indexOf("<version>"));
                                String newVersionLine = indentation + "<version>" + targetVersion + "</version>";

                                // Orijinal satırı yeni sürüm satırıyla değiştiriyoruz
                                updatedLines.set(k, newVersionLine);

                                System.out.println("  -> [" + currentArtifactId + "] Güncelleniyor: "
                                        + currentVersion + " ──> " + targetVersion);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 4. Dry-Run kontrolü: Eğer dry-run false ise değişiklikleri diske yaz
        if (!dryRun) {
            Files.write(Paths.get(service.path), updatedLines);
            System.out.println("[BAŞARILI] Değişiklikler diskteki pom.xml dosyasına yazıldı.");
        } else {
            System.out.println("[DRY-RUN] Modu aktif. Diske yazma işlemi simüle edildi, dosya değiştirilmedi.");
        }
    }

    // XML tag'inin içindeki metni güvenli bir şekilde söker
    private static String extractTagValue(String line, String tagName) {
        String openTag = "<" + tagName + ">";
        String closeTag = "</" + tagName + ">";
        if (line.contains(openTag) && line.contains(closeTag)) {
            return line.substring(line.indexOf(openTag) + openTag.length(), line.indexOf(closeTag)).trim();
        }
        return null;
    }

    // Recipe içindeki kütüphanelerle pom.xml'den okuduğumuzu eşleştirir
    private static DependencyItem findMatchingDependency(Recipe recipe, String groupId, String artifactId) {
        if (recipe.dependencies == null || groupId == null || artifactId == null) return null;
        for (DependencyItem item : recipe.dependencies) {
            if (groupId.equals(item.groupId) && artifactId.equals(item.artifactId)) {
                return item;
            }
        }
        return null;
    }

    // CA veya Non-CA durumuna göre doğru sürüm dizesini seçer
    private static String determineTargetVersion(DependencyItem item, String serviceType) {
        if (item.version != null) {
            return item.version; // Sabit tekil versiyon senaryosu
        }
        if (item.versions != null && serviceType != null) {
            if (serviceType.equalsIgnoreCase("ca")) {
                return item.versions.get("ca");
            } else {
                return item.versions.get("nonCa");
            }
        }
        return null;
    }
}