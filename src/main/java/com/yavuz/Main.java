package com.yavuz;

import com.yavuz.model.Recipe;
import com.yavuz.service.RecipeParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        // === CONFIGURATION ===
        final String recipeFolder = "Recipe";
        final String rootPath = "test-environment";
        String recipePath;
        boolean dryRun = false;

        System.out.println("==================================================");
        System.out.println("               Dependency Upgrader                ");
        System.out.println("==================================================");
        System.out.println("[BİLGİ] Sistem bileşenleri kontrol ediliyor...");

        // 1. Klasör Varlık Kontrolleri
        File folder = new File(recipeFolder);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("[KRİTİK HATA] '" + recipeFolder + "' klasörü bulunamadı!");
            System.out.println("Lütfen proje kökünde 'Recipe' adında bir klasör açın.");
            return;
        }
        if (!Files.exists(Paths.get(rootPath))) {
            System.out.println("[KRİTİK HATA] '" + rootPath + "' dizini bulunamadı!");
            System.out.println("Lütfen proje kökünde 'test-environment' klasörünün varlığından emin olun.");
            return;
        }

        // 2. Klasör İçindeki YAML Dosyalarını Dinamik Tarama
        File[] yamlFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yaml") || name.toLowerCase().endsWith(".yml"));

        if (yamlFiles == null || yamlFiles.length == 0) {
            System.out.println("[KRİTİK HATA] '" + recipeFolder + "' klasörü içinde hiç .yaml veya .yml dosyası bulunamadı!");
            return;
        }

        Scanner scanner = new Scanner(System.in);

        // 3. Dosya Sayısına Göre Akış Belirleme
        if (yamlFiles.length == 1) {
            // Sadece 1 dosya varsa otomatik seçer
            recipePath = yamlFiles[0].getPath();
            System.out.println("[BAŞARILI] Klasörde tek tarif dosyası bulundu, otomatik seçildi: " + yamlFiles[0].getName());
        } else {
            // Birden fazla dosya varsa kullanıcıya sorar
            System.out.println("\n[SİSTEM] '" + recipeFolder + "' klasöründe birden fazla tarif dosyası bulundu.");
            for (int i = 0; i < yamlFiles.length; i++) {
                System.out.println(" [" + (i + 1) + "] -> " + yamlFiles[i].getName());
            }

            while (true) {
                System.out.print("Lütfen kullanmak istediğiniz tarifin numarasını seçin: ");
                String input = scanner.nextLine().trim();
                try {
                    int index = Integer.parseInt(input) - 1;
                    if (index >= 0 && index < yamlFiles.length) {
                        recipePath = yamlFiles[index].getPath();
                        System.out.println("[OK] Seçilen dosya: " + yamlFiles[index].getName());
                        break;
                    } else {
                        System.out.println("[UYARI] Geçersiz numara! Lütfen listedeki sayılardan birini girin.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("[UYARI] Geçersiz girdi! Lütfen sadece bir sayı girin.");
                }
            }
        }

        System.out.println("--------------------------------------------------");
        System.out.println("    Çalıştırılacak Tarif : " + recipePath);
        System.out.println("    Kök Tarama Dizini    : " + rootPath);
        System.out.println("--------------------------------------------------");

        // === ETKİLEŞİMLİ KULLANICI ÇALIŞMA MODU SEÇİMİ ===
        System.out.println("Lütfen devam etmek için bir çalışma modu seçin:");
        System.out.println(" [y] -> Gerçek Güncelleme Modu (Dosyaları değiştirir, otomatik backup alır)");
        System.out.println(" [d] -> Simülasyon (Dry-Run) Modu (Dosyalara dokunmaz, sadece rapor sunar)");
        System.out.println(" [Herhangi Başka Bir Tuş] -> İşlemi İptal Et ve Çık");
        System.out.print("Seçiminiz: ");

        String choice = scanner.nextLine().trim().toLowerCase();

        if (choice.equals("y")) {
            System.out.println("\n[ONAYLANDI] Gerçek güncelleme modu başlatılıyor... Değişiklikler kalıcı olacaktır.\n");
        } else if (choice.equals("d")) {
            dryRun = true;
            System.out.println("\n[ONAYLANDI] Simülasyon (Dry-Run) modu başlatılıyor... Güvenli analiz.\n");
        } else {
            System.out.println("\n[UYARI] İşlem kullanıcı kararıyla iptal edildi. Program sonlandırılıyor.");
            return;
        }

        // === ADIM 2: RECIPE DOSYASINI OKUMA ===
        Recipe recipe;
        try {
            recipe = RecipeParser.parseRecipe(recipePath);
            System.out.println("[SİSTEM] Recipe dosyası başarıyla hafızaya yüklendi: " + recipe.recipeVersion);
        } catch (IOException e) {
            System.out.println("\n[KRİTİK HATA] Recipe dosyası okunurken hata: " + e.getMessage());
            return;
        }

        // === ADIM 3 & 4: SERVİS TARAMA VE DÖNGÜ ===
        try {
            List<com.yavuz.model.MicroService> services = com.yavuz.service.ServiceDetector.discoverServices(null, rootPath);

            if (services.isEmpty()) {
                System.out.println("Belirtilen konumlarda geçerli bir pom.xml bulunamadı!");
            } else {
                for (com.yavuz.model.MicroService service : services) {
                    List<String> errors = new ArrayList<>();

                    try {
                        com.yavuz.service.ServiceDetector.determineServiceType(service, null, recipe);
                        com.yavuz.service.DependencyUpdater.updateService(service, recipe, dryRun, errors);

                    } catch (Exception e) {
                        errors.add("Servis işlenirken kritik hata oluştu: " + e.getMessage());

                        com.yavuz.service.DependencyUpdater.printServiceReport(
                                service,
                                new ArrayList<>(),
                                new ArrayList<>(),
                                new ArrayList<>(),
                                new ArrayList<>(),
                                errors,
                                dryRun
                        );
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[KRİTİK HATA] Kök dizin taranırken sistem hatası oluştu: " + e.getMessage());
        }
    }
}