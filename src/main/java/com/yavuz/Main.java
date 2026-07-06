package com.yavuz;

import com.yavuz.model.Recipe;
import com.yavuz.service.RecipeParser;
import com.yavuz.service.ServiceMapParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {

        String recipePath = null;
        String servicePath = null;
        String rootPath = null;
        String serviceType = null;
        String serviceMapPath = null;
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
                    if (i + 1 < args.length) serviceMapPath = args[++i];
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
            System.out.println("\n[KRİTİK HATA]!!! Aynı onda hem --service-path hem de --root-path parametreleri verilemez!");
            return;
        }

        if (serviceType != null
                && !serviceType.equalsIgnoreCase("ca")
                && !serviceType.equalsIgnoreCase("non-ca")) {
            System.out.println("\n[KRİTİK HATA]!!! --service-type sadece 'ca' veya 'non-ca' olabilir. Girilen: " + serviceType);
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

        // === ADIM 5.1: SERVICE MAP DOSYASINI PARSE ETME ===
        Map<String, Map<String, String>> parsedServiceMap = null;
        if (serviceMapPath != null) {
            try {
                parsedServiceMap = ServiceMapParser.parseServiceMap(serviceMapPath);
                System.out.println("[BAŞARILI] Service Map dosyası başarıyla yüklendi: " + serviceMapPath);
            } catch (IOException e) {
                System.out.println("[UYARI] Service Map dosyası okunurken hata oluştu, otomatik tespite devam edilecek: " + e.getMessage());
            }
        }

        // === ADIM 3 & 4: SERVİS TARAMA VE DÖNGÜ ===
        try {
            java.util.List<com.yavuz.model.MicroService> services = com.yavuz.service.ServiceDetector.discoverServices(servicePath, rootPath);

            if (services.isEmpty()) {
                System.out.println("Belirtilen konumlarda geçerli bir pom.xml bulunamadı!");
            } else {
                for (com.yavuz.model.MicroService service : services) {
                    // DÜZENLEME 1: Her servis için izole çalışan errors listesi döngünün başında ayağa kaldırıldı
                    List<String> errors = new ArrayList<>();

                    try {
                        // Öncelik Hiyerarşisi Sürücüsü
                        String finalType = serviceType;
                        if (finalType == null && parsedServiceMap != null && parsedServiceMap.containsKey(service.name)) {
                            finalType = parsedServiceMap.get(service.name).get("serviceType");
                        }

                        com.yavuz.service.ServiceDetector.determineServiceType(service, finalType, recipe);

                        // DÜZENLEME 2: updateService metoduna dördüncü parametre olarak errors listesi enjekte edildi
                        com.yavuz.service.DependencyUpdater.updateService(service, recipe, dryRun, errors);

                    } catch (Exception e) {
                        // DÜZENLEME 3: Ham konsol çıktısı yerine, hata yakalandığında listeye alıp kurumsal rapora yönlendiriyoruz
                        errors.add("Servis islenirken kritik hata olustu: " + e.getMessage());

                        // Hata durumunda bile şablon bütünlüğünü koruyarak estetik Errors alanını ekrana basıyoruz
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
        } catch (java.io.IOException e) {
            System.out.println("[KRİTİK HATA] Kök dizin taranırken sistem hatası oluştu: " + e.getMessage());
        }

        if (serviceMapPath != null) {
            System.out.println("[BİLGİ] Kullanılan Servis Map Konumu: " + serviceMapPath);
        }
    }
}