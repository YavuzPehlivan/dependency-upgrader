package com.yavuz.service;

import com.yavuz.model.DependencyItem;
import com.yavuz.model.MicroService;
import com.yavuz.model.Recipe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DependencyUpdaterTest {

    @TempDir
    Path tempDir;

    // UYARI 1 ÇÖZÜLDÜ: 'test' ön eki kaldırıldı, modern JUnit 5 isimlendirmesine geçildi
    @Test
    public void extractTagValue_ShouldReturnCorrectValue() {
        String xmlLine = "    <artifactId>state-flow-client</artifactId>";
        String value = DependencyUpdater.extractTagValue(xmlLine, "artifactId");
        assertEquals("state-flow-client", value);
    }

    @Test
    public void updateService_ShouldUpdateExistingDependency_AC1() throws IOException {
        Path pomPath = tempDir.resolve("pom.xml");

        // UYARI 2 ÇÖZÜLDÜ: Eski string birleştirme yerine Java 17 Text Block (""") yapısı entegre edildi
        String originalPom = """
                <?xml version="1.0"?>
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>com.pia.dnext</groupId>
                            <artifactId>access-control</artifactId>
                            <version>0.5.6</version>
                        </dependency>
                    </dependencies>
                </project>
                """;
        Files.writeString(pomPath, originalPom);

        MicroService service = new MicroService("test-service", pomPath.toString());
        service.serviceType = "non-ca";

        Recipe recipe = new Recipe();
        recipe.recipeVersion = "2026-Test";
        recipe.dependencies = new ArrayList<>();

        DependencyItem item = new DependencyItem();
        item.groupId = "com.pia.dnext";
        item.artifactId = "access-control";
        item.version = "1.0.0";
        recipe.dependencies.add(item);

        List<String> errors = new ArrayList<>();

        DependencyUpdater.updateService(service, recipe, false, errors);

        String updatedContent = Files.readString(pomPath);
        assertTrue(updatedContent.contains("<version>1.0.0</version>"));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void updateService_ShouldUpdatePropertyCorrectly_AC4() throws IOException {
        Path pomPath = tempDir.resolve("pom.xml");
        String originalPom = """
                <?xml version="1.0"?>
                <project>
                    <properties>
                        <access-control.version>0.5.6</access-control.version>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>com.pia.dnext</groupId>
                            <artifactId>access-control</artifactId>
                            <version>${access-control.version}</version>
                        </dependency>
                    </dependencies>
                </project>
                """;
        Files.writeString(pomPath, originalPom);

        MicroService service = new MicroService("property-service", pomPath.toString());
        service.serviceType = "non-ca";

        Recipe recipe = new Recipe();
        recipe.dependencies = new ArrayList<>();
        DependencyItem item = new DependencyItem();
        item.groupId = "com.pia.dnext";
        item.artifactId = "access-control";
        item.version = "1.0.0";
        recipe.dependencies.add(item);

        List<String> errors = new ArrayList<>();
        DependencyUpdater.updateService(service, recipe, false, errors);

        String updatedContent = Files.readString(pomPath);
        // Doğrulama: Doğrudan versiyon değil, yukarıdaki property değeri güncellenmeli!
        assertTrue(updatedContent.contains("<access-control.version>1.0.0</access-control.version>"));
    }

    @Test
    public void updateService_ShouldSkipBomManagedDependencies_AC5() throws IOException {
        Path pomPath = tempDir.resolve("pom.xml");
        String originalPom = """
                <?xml version="1.0"?>
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>com.pia.dnext</groupId>
                            <artifactId>access-control</artifactId>
                            <!-- Versiyon yok, parent veya BOM'dan miras alıyor -->
                        </dependency>
                    </dependencies>
                </project>
                """;
        Files.writeString(pomPath, originalPom);

        MicroService service = new MicroService("bom-service", pomPath.toString());
        service.serviceType = "non-ca";

        Recipe recipe = new Recipe();
        recipe.dependencies = new ArrayList<>();
        DependencyItem item = new DependencyItem();
        item.groupId = "com.pia.dnext";
        item.artifactId = "access-control";
        item.version = "1.0.0";
        recipe.dependencies.add(item);

        List<String> errors = new ArrayList<>();
        DependencyUpdater.updateService(service, recipe, false, errors);

        String updatedContent = Files.readString(pomPath);
        // Doğrulama: Dosyaya zorla versiyon eklenmemeli, eski hali korunmalı
        assertFalse(updatedContent.contains("<version>"));
    }

    @Test
    public void updateService_ShouldHandleSingleLineDependencyWithoutCorruption_BugFix() throws IOException {
        Path pomPath = tempDir.resolve("pom.xml");
        // Claude'un patlar dediği o efsane tek satırlık dependency bloğu
        String originalPom = "<dependency><groupId>com.pia.dnext</groupId><artifactId>state-flow-client</artifactId><version>0.1.0</version></dependency>";
        Files.writeString(pomPath, originalPom);

        MicroService service = new MicroService("single-line-service", pomPath.toString());
        service.serviceType = "non-ca";

        Recipe recipe = new Recipe();
        recipe.dependencies = new ArrayList<>();
        DependencyItem item = new DependencyItem();
        item.groupId = "com.pia.dnext";
        item.artifactId = "state-flow-client";
        item.version = "1.0.0";
        recipe.dependencies.add(item);

        List<String> errors = new ArrayList<>();
        DependencyUpdater.updateService(service, recipe, false, errors);

        String updatedContent = Files.readString(pomPath);
        // Doğrulama: Hem versiyon güncellenmeli hem de arkasındaki kapanış etiketi ASLA kaybolmamalı!
        assertTrue(updatedContent.contains("<version>1.0.0</version></dependency>"));
    }

    @Test
    public void updateService_ShouldSelectCaVersion_AC6() throws IOException {
        Path pomPath = tempDir.resolve("pom.xml");

        String originalPom = """
                <?xml version="1.0"?>
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>com.pia.dnext</groupId>
                            <artifactId>state-flow-client</artifactId>
                            <version>0.1.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;
        Files.writeString(pomPath, originalPom);

        MicroService service = new MicroService("ca-service", pomPath.toString());
        service.serviceType = "ca";

        Recipe recipe = new Recipe();
        recipe.dependencies = new ArrayList<>();

        DependencyItem item = new DependencyItem();
        item.groupId = "com.pia.dnext";
        item.artifactId = "state-flow-client";
        item.versions = new HashMap<>();
        item.versions.put("ca", "1.0.0-ca");
        item.versions.put("nonCa", "1.0.1");
        recipe.dependencies.add(item);

        List<String> errors = new ArrayList<>();

        DependencyUpdater.updateService(service, recipe, false, errors);

        String updatedContent = Files.readString(pomPath);
        assertTrue(updatedContent.contains("<version>1.0.0-ca</version>"));
    }

    @Test
    public void updateService_ShouldSkipUpgradeWhenTypeIsUnknown_AC9() throws IOException {
        Path pomPath = tempDir.resolve("pom.xml");

        String originalPom = """
                <?xml version="1.0"?>
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>com.pia.dnext</groupId>
                            <artifactId>state-flow-client</artifactId>
                            <version>0.8.5</version>
                        </dependency>
                    </dependencies>
                </project>
                """;
        Files.writeString(pomPath, originalPom);

        MicroService service = new MicroService("unknown-service", pomPath.toString());
        service.serviceType = "unknown";

        Recipe recipe = new Recipe();
        recipe.dependencies = new ArrayList<>();

        DependencyItem item = new DependencyItem();
        item.groupId = "com.pia.dnext";
        item.artifactId = "state-flow-client";
        item.versions = new HashMap<>();
        item.versions.put("ca", "1.0.0-ca");
        item.versions.put("nonCa", "1.0.1");
        recipe.dependencies.add(item);

        List<String> errors = new ArrayList<>();

        DependencyUpdater.updateService(service, recipe, false, errors);

        String updatedContent = Files.readString(pomPath);
        assertTrue(updatedContent.contains("<version>0.8.5</version>"));
    }
}