package com.yavuz.service;

import com.yavuz.model.MicroService;
import com.yavuz.model.DependencyItem;
import com.yavuz.model.Recipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class ServiceDetector {

    public static List<MicroService> discoverServices(String servicePath, String rootPath) throws IOException {
        List<MicroService> discovered = new java.util.ArrayList<>();

        if (servicePath != null) {
            Path p = Paths.get(servicePath, "pom.xml");
            if (Files.exists(p)) {
                discovered.add(new MicroService(Paths.get(servicePath).getFileName().toString(), p.toString()));
            }
        } else if (rootPath != null) {
            // ÇÖZÜM 1: try-with-resources kullanılarak işletim sistemi kaynak sızıntısı engellendi
            try (Stream<Path> pathStream = Files.walk(Paths.get(rootPath))) {
                pathStream.filter(path -> path.getFileName().toString().equals("pom.xml"))
                        .forEach(path -> {
                            String serviceName = path.getParent().getFileName().toString();
                            discovered.add(new MicroService(serviceName, path.toString()));
                        });
            }
        }
        return discovered;
    }

    public static void determineServiceType(MicroService service, String cliType, Recipe recipe) {
        // 1. Öncelik: CLI parametresi ile doğrudan tip girilmişse (Kural 7.1)[cite: 1]
        if (cliType != null) {
            service.serviceType = cliType.toLowerCase();
            return;
        }

        // 2. Öncelik: Pom.xml dosyasının içindeki kritik kütüphanelerden akıllı tespit (Kural 7.3)[cite: 1]
        try {
            List<String> lines = Files.readAllLines(Paths.get(service.path));
            String currentArtifactId = null;
            boolean hasSplitDependency = false;

            for (String line : lines) {
                if (line.contains("<artifactId>")) {
                    // ÇÖZÜM 2: Ortak metot çağrılarak kod tekrarlama (duplication) uyarısı yok edildi
                    currentArtifactId = DependencyUpdater.extractTagValue(line, "artifactId");
                }

                if (line.contains("<version>") && currentArtifactId != null) {
                    if (isCaNonCaSplitDependency(recipe, currentArtifactId)) {
                        hasSplitDependency = true;
                        String version = DependencyUpdater.extractTagValue(line, "version");

                        if (version != null && version.endsWith("-ca")) {
                            service.serviceType = "ca";
                            return;
                        }
                    }
                }
            }

            if (hasSplitDependency) {
                service.serviceType = "non-ca";
            } else {
                service.serviceType = "unknown";
            }

        } catch (IOException e) {
            service.serviceType = "unknown";
        }
    }

    private static boolean isCaNonCaSplitDependency(Recipe recipe, String artifactId) {
        if (recipe == null) return false;
        if (recipe.dependencies != null) {
            for (DependencyItem item : recipe.dependencies) {
                if (artifactId.equals(item.artifactId) && item.versions != null) return true;
            }
        }
        if (recipe.dependencyManagement != null) {
            for (DependencyItem item : recipe.dependencyManagement) {
                if (artifactId.equals(item.artifactId) && item.versions != null) return true;
            }
        }
        return false;
    }
}