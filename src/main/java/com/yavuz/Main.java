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

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--recipe":
                    // Eğer kelime '--recipe' ise, bir sonraki kelime dosya yoludur
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

        // === 2. ADIM: RECIPE DOSYASINI OKUMA KONTROLÜ ===
        if (recipePath != null) {
            try {
                Recipe recipe = RecipeParser.parseRecipe(recipePath);
                System.out.println("\n[BAŞARILI] Recipe dosyası başarıyla okundu!");
                System.out.println("Recipe Versiyonu: " + recipe.recipeVersion);

                if (recipe.dependencies != null) {
                    System.out.println("Recipe içindeki toplam dependency sayısı: " + recipe.dependencies.size());
                }
            } catch (IOException e) {
                System.out.println("\n[HATA] Recipe dosyası okunurken bir hata oluştu: " + e.getMessage());
            }
        } else {
            System.out.println("\n[UYARI] --recipe parametresi verilmediği için dosya okuma adımı atlandı.");
        }

        // 3. Kodumuzun parametreleri doğru yakalayıp yakalamadığını ekranda görüyoruz
        System.out.println("\n=== [ADIM 1] PARAMETRE KONTROLÜ ===");
        System.out.println("Recipe Path: " + recipePath);
        System.out.println("Service Path: " + servicePath);
        System.out.println("Root Path: " + rootPath);
        System.out.println("Service Type: " + serviceType);
        System.out.println("Service Map: " + serviceMap);
        System.out.println("Dry Run Modu Aktif mi?: " + dryRun);
    }
}
