# Recipe Based Maven Dependency Upgrade Automation Tool

Bu proje, her çeyrek (quarter) sonunda yayınlanan vulnerability fix recipe'sine göre mikroservislerin mevcut `pom.xml` versiyonlarını otomatik olarak güncelleyen bir otomasyon aracıdır.

## 🎯 Amaç
Geliştiricilerin manuel olarak yaptığı bağımlılık (dependency) güncelleme sürecini hızlandırmak, insan hatasını sıfıra indirmek ve servislerin CA / Non-CA durumlarına göre doğru versiyonları güvenli bir şekilde yaygınlaştırmaktır.

## 🛠️ Proje Durumu ve Yol Haritası
- [x] **Adım 1:** CLI Komut Satırı Argümanlarının Yakalanması ve Doğrulanması (Fail-Fast Güvenlik Bariyeri)
- [x] **Adım 2:** YAML Formatındaki Recipe Dosyasının Okunması ve Modellenmesi
- [x] **Adım 3 & 4:** Mikroservislerin Klasör Hiyerarşisinden Taranması ve CA/Non-CA Tiplerinin Otomatik Tespiti
- [x] **Adım 5:** `pom.xml` Dosyalarının Formatı, Boşlukları ve Yorum Satırları Bozulmadan Güvenli Güncellenmesi (Durum Bilgili Ana Motor)
- [ ] **Adım 6 & 7:** Güvenli Yedekleme (Backup) Mekanizması ve Detaylı Konsol Raporlama Üretimi

## 🧪 Test Ortamı ve Çalıştırma Senaryoları

Proje kök dizininde yer alan `test-environment/` klasörü içerisinde simülasyon yapabileceğiniz, kurumsal senaryolara (Recipe dışı harici kütüphaneler barındıran veya Recipe'de olup serviste olmayan) birebir uygun örnek mikroservisler yer almaktadır:
* `test-environment/dnext-cost-management` -> İçindeki versiyondan ötürü otomatik **CA** algılanan servis.
* `test-environment/dnext-product-catalog` -> Otomatik **NON-CA** algılanan servis.

> ℹ️ **Not:** Geliştirme ortamı olarak Linux (Ubuntu) baz alınmıştır. Java'nın NIO Path yapısı sayesinde tüm komutlar Windows (PowerShell/Git Bash) ortamlarıyla da tam uyumludur.

### 🚀 Çalıştırma Komutları

Aşağıdaki komutları projenin kök dizininde terminalden çalıştırarak sistemi test edebilirsiniz:

#### Senaryo A: Tek Bir Servis Yolunu Test Etmek (--service-path)
```bash
java src/main/java/com/yavuz/Main.java --recipe vulnerability-recipe.yaml --service-path test-environment/dnext-cost-management --dry-run
```

#### Senaryo B: Çoklu Servis Kök Dizinini Test Etmek (--root-path)
```bash
java src/main/java/com/yavuz/Main.java --recipe vulnerability-recipe.yaml --root-path test-environment --dry-run
```

#### Senaryo C: Güvenlik Bariyeri Testi (İki parametre aynı anda girilirse)
```bash
java src/main/java/com/yavuz/Main.java --recipe vulnerability-recipe.yaml --service-path test-environment/dnext-cost-management --root-path test-environment --dry-run
```

### 📊 Örnek Çalışma Çıktısı (Senaryo B - Çoklu Tarama ve Güncelleme Analizi)

[BAŞARILI] Recipe dosyası başarıyla okundu!
Recipe Versiyonu: 2026-Q4-vulnerability-fix
Recipe içindeki toplam dependency sayısı: 3
Recipe detaylari:
Dependencie (ArtifactId): access-control
-> Grup (groupId): com.pia.dnext
-> Hedef Versiyon: 1.0.0
----------------------------------------
Dependencie (ArtifactId): state-flow-client
-> Grup (groupId): com.pia.dnext
-> Dinamik Versiyon Yapısı Belirlendi:
* CA Hedefi: 1.0.0-ca
* Non-CA Hedefi: 1.0.1
----------------------------------------
Dependencie (ArtifactId): ghost-library
-> Grup (groupId): org.external
-> Hedef Versiyon: 9.9.9
----------------------------------------

=== [ADIM 3 & 4] BULUNAN SERVİSLER VE TİPLERİ ===
Servis Adı: dnext-cost-management
-> Konum: test-environment/dnext-cost-management/pom.xml
-> Tespit Edilen Tip: CA
-> Bağımlılık Güncelleme Analizi:
-> [access-control] Güncelleniyor: 0.8.5 ──> 1.0.0
-> [state-flow-client] Güncelleniyor: 0.9.0-ca ──> 1.0.0-ca
[DRY-RUN] Modu aktif. Diske yazma işlemi simüle edildi, dosya değiştirilmedi.
----------------------------------------
Servis Adı: dnext-product-catalog
-> Konum: test-environment/dnext-product-catalog/pom.xml
-> Tespit Edilen Tip: NON-CA
-> Bağımlılık Güncelleme Analizi:
-> [state-flow-client] Güncelleniyor: 0.5.0 ──> 1.0.1
[DRY-RUN] Modu aktif. Diske yazma işlemi simüle edildi, dosya değiştirilmedi.
----------------------------------------

=== [ADIM 1] PARAMETRE KONTROLÜ ===
Recipe Path: vulnerability-recipe.yaml
Service Path: null
Root Path: test-environment
Service Type: null
Service Map: null
Dry Run Modu Aktif mi?: true