# Recipe Based Maven Dependency Upgrade Automation Tool

Bu araç; kurumsal mikroservis mimarilerinde (`pom.xml`) çeyrek dönemlik güvenlik açığı giderme talimatlarını (Recipe YAML) baz alarak bağımlılıkları, parent versiyonlarını ve Maven eklentilerini (plugin) otomatik, güvenli ve dosya formatını bozmadan güncelleyen gelişmiş bir otomasyon motorudur.

## 🎯 Öne Çıkan Gelişmiş Özellikler

* **Mimaride BOM ve Bağımlılık Ayrımı (Dependency Management):** `<dependencyManagement>` bloğu ile normal `<dependencies>` alanlarını durum takibiyle (`inDependencyManagement`) birbirinden izole eder; yanlış çapraz eşleşmeleri tamamen önler.
* **Gelişmiş Hata İzolasyonu (Error Isolation):** Çoklu taramalarda herhangi bir serviste bozuk XML yapısı veya dosya hatası oluşursa uygulama çökmez. Hatayı ilgili servisin raporundaki `Errors:` alanına yazar ve bir sonraki servise güvenle geçer.
* **Akıllı Servis Tipi Tespiti (Auto-Detect):** Servis tipini belirlerken kurumsal hiyerarşiyi işletir: `CLI Parametresi > Service Map Dosyası (YAML) > Otomatik Tespit`. Bağlam bulunamazsa `UNKNOWN` olarak işaretler ve hassas sürümleri ezmez.
* **Sağlamlaştırılmış Satır Kontrolü:** Tek satırda yazılmış kırılgan bağımlılık bloklarını sessizce yutmak yerine `Warnings:` listesine ekleyerek raporlar.
* **Otomatik Yedekleme Mekanizması:** Değişiklik yapılan her başarılı operasyonda, orijinal `pom.xml` dosyasını reçete sürümüne göre `.backup/` dizini altında güvenli bir şekilde arşivler.
* ** JUnit 5 Birim Testleri:** `@TempDir` mimarisi kullanılarak diske kalıcı yazma hasarı vermeden tüm kritik iş kuralları (AC-1, AC-6, AC-9) otomatik olarak test edilir.

## 🛠️ CLI Parametreleri

| Parametre | Zorunlu mu? | Açıklama |
| :--- | :--- | :--- |
| `--recipe` | **Evet** | Güncelleme kurallarını içeren YAML dosyasının yolu. |
| `--service-path` | Hayır | Sadece tek bir mikroservis klasörünü hedeflemek için kullanılır. |
| `--root-path` | Hayır | Altındaki tüm mikroservisleri toplu taramak için kök dizin yolu. |
| `--service-type` | Hayır | Tüm servislere zorla `ca` veya `non-ca` tipi dayatmak için kullanılır. |
| `--service-map` | Hayır | Servis adlarını tipleriyle eşleyen harici YAML dosyası (`Services.yaml`). |
| `--dry-run` | Hayır| Aktif edildiğinde diske yazma yapmaz, sadece simülasyon raporu sunar. |

## 🚀 Çalıştırma ve Test Talimatları

### 1. Birim Testleri Çalıştırma (JUnit 5)
```bash
mvn test
```
### 2. Projeyi Bağımlılıklarıyla Birlikte Paketleme (Fat-JAR)
```bash
mvn clean package
```

### 3. Simülasyon Modunda Çalıştırma (Dry-Run)
```bash
java -jar target/dependency-upgrader-1.0-SNAPSHOT.jar --recipe vulnerability-recipe.yaml --root-path test-environment --dry-run
```

### 4. Gerçek Modda Güncelleme ve Yedekleme Başlatma
```bash
java -jar target/dependency-upgrader-1.0-SNAPSHOT.jar --recipe vulnerability-recipe.yaml --root-path test-environment
```

## 📊 Örnek Konsol Rapor Formatı
```text
==================================================
         GERÇEK GÜNCELLEME RAPORU
==================================================
Service Name: dnext-parent-test
Service Path: test-environment/dnext-parent-test/pom.xml
Detected Service Type: NON-CA

Updated Items:
  - spring-boot-starter-parent: 3.3.0 -> 3.5.0
  - dnext-common-dependencies: 0.9.0 -> 1.0.0
  - state-flow-client: 0.4.3 -> 1.0.1
  - jacoco-maven-plugin: 0.8.11 -> 0.8.12

Skipped Items:
  - None

Unchanged Items:
  - None

Warnings:
  - None

Errors:
  - None
==================================================
```