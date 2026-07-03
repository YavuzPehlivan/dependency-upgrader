package com.yavuz.service;

import com.yavuz.model.MicroService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ServiceDetector {

    // FR-2 ve FR-11: Belirtilen yollardaki pom.xml dosyalarını tarar ve listeler
    public static List<MicroService> discoverServices(String servicePath, String rootPath) throws IOException {
        List<MicroService> discovered = new ArrayList<>();

        // Senaryo A: Kullanıcı tek bir servis yolu verdiyse
        if (servicePath != null) {
            Path pomPath = Paths.get(servicePath, "pom.xml");
            if (Files.exists(pomPath)) {
                File folder = new File(servicePath);
                discovered.add(new MicroService(folder.getName(), pomPath.toString()));
            }
        }
        // Senaryo B: Kullanıcı çoklu servis taranacak bir kök dizin verdiyse
        else if (rootPath != null) {
            Path root = Paths.get(rootPath);
            if (Files.exists(root)) {
                // Kök dizinin altındaki ilk seviye klasörleri listeliyoruz
                Files.list(root).forEach(path -> {
                    Path pomPath = path.resolve("pom.xml");
                    if (Files.exists(pomPath)) {
                        discovered.add(new MicroService(path.getFileName().toString(), pomPath.toString()));
                    }
                });
            }
        }
        return discovered;
    }

    // FR-9 ve FR-10: Servis tipinin CA mi yoksa Non-CA mi olduğunu belirler
    public static void determineServiceType(MicroService service, String cliType) {
        // 1. Öncelik: CLI parametresi ile doğrudan tip girilmişse (Kural 7.1)
        if (cliType != null) {
            service.serviceType = cliType.toLowerCase();
            return;
        }

        // 2. Öncelik: Pom.xml dosyasının içindeki versiyondan otomatik tespit (Kural 7.3)
        try {
            List<String> lines = Files.readAllLines(Paths.get(service.path));
            for (String line : lines) {
                // Eğer pom.xml satırlarında -ca ekiyle biten bir sürüm tag'i görürsek
                if (line.contains("<version>") && line.contains("-ca</version>")) {
                    service.serviceType = "ca";
                    return;
                }
            }
            // Dosya okundu ama -ca suffix'i bulunamadıysa non-ca kabul ediyoruz
            service.serviceType = "non-ca";
        } catch (IOException e) {
            service.serviceType = "unknown";
        }
    }
}
