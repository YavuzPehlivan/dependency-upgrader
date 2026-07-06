package com.yavuz;

import com.yavuz.model.Recipe;
import com.yavuz.service.RecipeParser;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {

        String recipePath = null;
        String servicePath = null;
        String rootPath = null;
        String serviceType = null;
        String serviceMap = null;
        boolean dryRun = false;

        // === ADIM 1: PARAMETRE AYIKLAMA ===
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--recipe":
                    if (i + 1 < args.length) recipePath = args[++i];
                    break;
                case "--service-path":
                    if (i + 1 < args.length) servicePath = args[++i];
                    break;
                case "--root-path":
                    if (i + 1 < args.length) rootPath = args[++i];
                    break;
                case "--service-type":
                    if (i + 1 < args.length) serviceType = args[++i];
                    break;
                case "--service-map":
                    if (i + 1 < args.length) serviceMap = args[++i];
                    break;
                case "--dry-run":
                    dryRun = true;
                    break;
                default:
                    System.out.println("Bilinmeyen parametre geçildi: " + args[i]);
                    break;
            }
        }

        // Güvenlik Doğrulaması (Fail-Fast)
        if (servicePath != null && rootPath != null) {
            System.out.println("\n[KRİTİK HATA]!!! Aynı anda hem --service-path hem de --root-path parametreleri verilemez!");
            return;
        }

        Recipe recipe;

        // === ADIM 2: RECIPE DOSYASINI OKUMA ===
        if (recipePath != null) {
            try {
                recipe = RecipeParser.parseRecipe(recipePath);
                System.out.println("\n[BAŞARILI] Recipe dosyası başarıyla okundu: " + recipe.recipeVersion);
            } catch (IOException e) {
                System.out.println("\n[KRİTİK HATA] Recipe dosyası okunurken hata: " + e.getMessage());
                return;
            }
        } else {
            System.out.println("\n[UYARI] --recipe parametresi verilmediği için işlem başlatılamıyor.");
            return;
        }

        // === ADIM 3 & 4: SERVİS TARAMA VE DÖNGÜ ===
        try {
            java.util.List<com.yavuz.model.MicroService> services = com.yavuz.service.ServiceDetector.discoverServices(servicePath, rootPath);

            if (services.isEmpty()) {
                System.out.println("Belirtilen konumlarda geçerli bir pom.xml bulunamadı!");
            } else {
                for (com.yavuz.model.MicroService service : services) {

                    // NFR-3 / FR-11: HATA İZOLASYON SİSTEMİ (Bir servis çökerse diğeri devam eder)
                    try {
                        com.yavuz.service.ServiceDetector.determineServiceType(service, serviceType);

                        // === ADIM 5, 6 & 7: ÇALIŞTIRMA, YEDEKLEME VE RAPORLAMA ===
                        com.yavuz.service.DependencyUpdater.updateService(service, recipe, dryRun);

                    } catch (Exception e) {
                        System.out.println("\n==================================================");
                        System.out.println("[HATA - İZOLE EDİLDİ] Servis İşlenemedi: " + service.name);
                        System.out.println("Hata Detayı: " + e.getMessage());
                        System.out.println("Sistem kuralı gereği bir sonraki servise geçiliyor.");
                        System.out.println("==================================================\n");
                    }
                }
            }
        } catch (java.io.IOException e) {
            System.out.println("[KRİTİK HATA] Kök dizin taranırken sistem hatası oluştu: " + e.getMessage());
        }

        // === ADIM 1: PARAMETRE KONTROLÜ (RAPOR) ===
        System.out.println("\n=== [ADIM 1] PARAMETRE KONTROLÜ ===");
        System.out.println("Recipe Path: " + recipePath);
        System.out.println("Service Path: " + servicePath);
        System.out.println("Root Path: " + rootPath);
        System.out.println("Service Type: " + serviceType);
        System.out.println("Service Map: " + serviceMap);
        System.out.println("Dry Run Modu Aktif mi?: " + dryRun);
    }
}