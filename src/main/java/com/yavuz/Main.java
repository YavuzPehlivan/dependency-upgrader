package com.yavuz;

import com.yavuz.model.DependencyItem;
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

        // === GÜVENLİK VE DOĞRULAMA KONTROLÜ ===
        if (servicePath != null && rootPath != null) {
            System.out.println("\n[KRİTİK HATA]!!! Aynı anda hem --service-path hem de --root-path parametreleri verilemez!");
            System.out.println("Service Path: " + servicePath);
            System.out.println("Root Path: " + rootPath);
            System.out.println("Lütfen tek bir servis güncellemesi için --service-path\n" +
                    "Toplu tarama için ise sadece --root-path parametresini kullanın.");
            return;
        }

        // Geniş kapsamlı recipe nesnemiz (Aşağıdaki adımlar da buna erişebilecek)
        Recipe recipe = null;

        // === 2. ADIM: RECIPE DOSYASINI OKUMA VE DETAYLARI YAZDIRMA ===
        if (recipePath != null) {
            try {
                // ÇÖZÜM: Baştaki 'Recipe' tip kelimesini sildik, yukarıdaki ana değişkeni dolduruyoruz
                recipe = RecipeParser.parseRecipe(recipePath);

                System.out.println("\n[BAŞARILI] Recipe dosyası başarıyla okundu!");
                System.out.println("Recipe Versiyonu: " + recipe.recipeVersion);

                if (recipe.dependencies != null) {
                    System.out.println("Recipe içindeki toplam dependency sayısı: " + recipe.dependencies.size());
                    System.out.println("Recipe detaylari:");

                    for (DependencyItem item : recipe.dependencies){
                        System.out.println("Dependencie (ArtifactId): " + item.artifactId);
                        System.out.println(" -> Grup (groupId): " + item.groupId);

                        // Senaryo 1: Eğer doğrudan tek bir versiyon varsa
                        if (item.version != null) {
                            System.out.println(" -> Hedef Versiyon: " + item.version);
                        }

                        // Senaryo 2: Eğer CA / Non-CA ayrımı varsa
                        if (item.versions != null) {
                            System.out.println(" -> Dinamik Versiyon Yapısı Belirlendi:");
                            System.out.println("    * CA Hedefi: " + item.versions.get("ca"));
                            System.out.println("    * Non-CA Hedefi: " + item.versions.get("nonCa"));
                        }
                        System.out.println("----------------------------------------");
                    }
                }

            } catch (IOException e) {
                System.out.println("\n[HATA] Recipe dosyası okunurken bir hata oluştu: " + e.getMessage());
            }
        } else {
            System.out.println("\n[UYARI] --recipe parametresi verilmediği için dosya okuma adımı atlandı.");
        }

        // === ADIM 3 & 4: SERVİS TARAMA VE TİP TESPİTİ KONTROLÜ ===
        try {
            // Servisleri diskten tarayıp listeliyoruz
            java.util.List<com.yavuz.model.MicroService> services = com.yavuz.service.ServiceDetector.discoverServices(servicePath, rootPath);

            System.out.println("\n=== [ADIM 3 & 4] BULUNAN SERVİSLER VE TİPLERİ ===");
            if (services.isEmpty()) {
                System.out.println("Belirtilen konumlarda geçerli bir pom.xml bulunamadı!");
            } else {
                for (com.yavuz.model.MicroService service : services) {
                    // Her bir servisin tipini kurallara göre analiz ediyoruz
                    com.yavuz.service.ServiceDetector.determineServiceType(service, serviceType);

                    System.out.println("Servis Adı: " + service.name);
                    System.out.println(" -> Konum: " + service.path);
                    System.out.println(" -> Tespit Edilen Tip: " + service.serviceType.toUpperCase());

                    // === ADIM 5: MOTORU TETİKLE ===
                    System.out.println(" -> Bağımlılık Güncelleme Analizi:");
                    com.yavuz.service.DependencyUpdater.updateService(service, recipe, dryRun);
                    System.out.println("----------------------------------------");
                }
            }
        } catch (java.io.IOException e) {
            System.out.println("[HATA] Servisler taranırken hata oldu: " + e.getMessage());
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