# Recipe Based Maven Dependency Upgrade Automation Tool

Bu proje, her çeyrek (quarter) sonunda yayınlanan vulnerability fix recipe'sine göre mikroservislerin mevcut `pom.xml` versiyonlarını otomatik olarak güncelleyen bir otomasyon aracıdır.

## 🎯 Amaç
Geliştiricilerin manuel olarak yaptığı bağımlılık (dependency) güncelleme sürecini hızlandırmak, insan hatasını sıfıra indirmek ve servislerin CA / Non-CA durumlarına göre doğru versiyonları güvenli bir şekilde yaygınlaştırmaktır.

## 🛠️ Proje Durumu ve Yol Haritası
- [x] **Adım 1:** CLI Komut Satırı Argümanlarının Yakalanması ve Doğrulanması (Fail-Fast Güvenlik Bariyeri)
- [x] **Adım 2:** YAML Formatındaki Recipe Dosyasının Okunması ve Modellenmesi
- [x] **Adım 3 & 4:** Mikroservislerin Klasör Hiyerarşisinden Taranması ve CA/Non-CA Tiplerinin Otomatik Tespiti
- [x] **Adım 5:** `pom.xml` Dosyalarının Formatı, Boşlukları ve Yorum Satırları Bozulmadan Güvenli Güncellenmesi (Durum Bilgili Ana Motor)
- [x] **Adım 6:** Kurumsal Standartta Klasör Bazlı Güvenli Yedekleme (`.backup/{recipeVersion}/{serviceName}/pom.xml`)
- [x] **Adım 7:** Hata İzolasyonlu Gelişmiş Konsol Raporlama Mimarisi (Updated, Skipped, Unchanged, Warnings)

## 🛡️ Güvenlik ve Hata İzolasyonu Mimarisi
* **Hata İzolasyonu (Fault Isolation):** Toplu taramalarda herhangi bir servisin `pom.xml` dosyasında okuma/yazma hatası oluşması durumunda uygulama çökmez. Hatayı ilgili servis bazında izole ederek loglar ve bir sonraki servisle işleme güvenle devam eder.
* **Güvenli Yedekleme:** Gerçek güncelleme modunda diske yazım yapılmadan hemen önce orijinal dosya koruma altına alınır.

## 🧪 Test Ortamı ve Çalıştırma Senaryoları

Proje kök dizininde yer alan `test-environment/` klasörü içerisinde simülasyon yapabileceğiniz, kurumsal senaryolara birebir uygun örnek mikroservisler yer almaktadır:
* `test-environment/dnext-cost-management` -> İçindeki versiyondan ötürü otomatik **CA** algılanan servis.
* `test-environment/dnext-product-catalog` -> Otomatik **NON-CA** algılanan servis.

### 🚀 Çalıştırma Komutları

Aşağıdaki komutları projenin kök dizininde terminalden çalıştırarak sistemi test edebilirsiniz:

#### Senaryo A: Simülasyon / Dry-Run Modu (Diske Yazmaz)
```bash
java src/main/java/com/yavuz/Main.java --recipe vulnerability-recipe.yaml --root-path test-environment --dry-run
```

#### Senaryo B: Gerçek Güncelleme ve Otomatik Yedekleme Modu (Diske Yazar)
```bash
java src/main/java/com/yavuz/Main.java --recipe vulnerability-recipe.yaml --root-path test-environment
```

### 📊 Örnek Çalışma Çıktısı (Gerçek Güncelleme Modu)
```text
[BAŞARILI] Recipe dosyası başarıyla okundu: 2026-Q4-vulnerability-fix

==================================================
         GERÇEK GÜNCELLEME RAPORU
==================================================
Service Name: dnext-cost-management
Service Path: test-environment/dnext-cost-management/pom.xml
Detected Service Type: CA

Updated Items:
  - access-control: 0.5.6 -> 1.0.0
  - state-flow-client: 0.8.5-ca -> 1.0.0-ca

Skipped Items:
  - spring-boot-starter-web: present in service but not found in recipe
  - lombok: present in service but not found in recipe

Unchanged Items:
  - None

Warnings:
  - None
==================================================


==================================================
         GERÇEK GÜNCELLEME RAPORU
==================================================
Service Name: dnext-product-catalog
Service Path: test-environment/dnext-product-catalog/pom.xml
Detected Service Type: NON-CA

Updated Items:
  - state-flow-client: 0.4.3 -> 1.0.1

Skipped Items:
  - guava: present in service but not found in recipe

Unchanged Items:
  - None

Warnings:
  - None
==================================================

=== [ADIM 1] PARAMETRE KONTROLÜ ===
Recipe Path: vulnerability-recipe.yaml
Service Path: null
Root Path: test-environment
Service Type: null
Service Map: null
Dry Run Modu Aktif mi?: false
```