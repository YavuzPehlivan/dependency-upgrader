# Recipe Based Maven Dependency Upgrade Automation Tool

Bu proje, her çeyrek (quarter) sonunda yayınlanan vulnerability fix recipe'sine göre mikroservislerin mevcut `pom.xml` versiyonlarını otomatik olarak güncelleyen bir otomasyon aracıdır.

## 🎯 Amaç
Geliştiricilerin manuel olarak yaptığı bağımlılık (dependency) güncelleme sürecini hızlandırmak, insan hatasını sıfıra indirmek ve servislerin CA / Non-CA durumlarına göre doğru versiyonları güvenli bir şekilde yaygınlaştırmaktır.

## 🛠️ Proje Durumu ve Yol Haritası
- [x] **Adım 1:** CLI Komut Satırı Argümanlarının Yakalanması
- [x] **Adım 2:** YAML Formatındaki Recipe Dosyasının Okunması ve Modellenmesi
- [x] **Adım 3 & 4:** Mikroservislerin Taranması ve CA/Non-CA Tiplerinin Otomatik Tespiti
- [ ] **Adım 5:** `pom.xml` Dosyalarının Formatı Bozulmadan Güvenli Güncellenmesi (Ana Motor)
- [ ] **Adım 6 & 7:** Yedekleme (Backup), Dry-Run Modu ve Detaylı Raporlama Üretimi

## 🧪 Test Ortamı ve Çalıştırma Senaryoları

Proje kök dizininde yer alan `test-environment/` klasörü içerisinde simülasyon yapabileceğiniz örnek mikroservisler yer almaktadır:
* `test-environment/dnext-cost-management` -> İçindeki versiyondan ötürü otomatik **CA** algılanan servis.
* `test-environment/dnext-product-catalog` -> Otomatik **NON-CA** algılanan servis.

### 🚀 Çalıştırma Komutları

Aşağıdaki komutları projenin kök dizininde terminalden çalıştırarak sistemi test edebilirsiniz:

### Ben Linux kullanıyorum bu kodlar da Linux'a göre

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

### Olması gereken çıktılar da şu şekildedir (A)
[BAŞARILI] Recipe dosyası başarıyla okundu!
Recipe Versiyonu: 2026-Q4-vulnerability-fix
Recipe içindeki toplam dependency sayısı: 2
Recipe detaylari:
Dependencie (ArtifactId): access-control
-> Grup (groupId): com.pia.dnext
-> Hedef Versiyon: 1.0.0
Dependencie (ArtifactId): state-flow-client
-> Grup (groupId): com.pia.dnext
-> Dinamik Versiyon Yapısı Belirlendi:
* CA Hedefi: 1.0.0-ca
* Non-CA Hedefi: 1.0.0

=== [ADIM 3 & 4] BULUNAN SERVİSLER VE TİPLERİ ===
Servis Adı: dnext-cost-management
-> Konum: test-environment/dnext-cost-management/pom.xml
-> Tespit Edilen Tip: CA
----------------------------------------

=== [ADIM 1] PARAMETRE KONTROLÜ ===
Recipe Path: vulnerability-recipe.yaml
Service Path: test-environment/dnext-cost-management
Root Path: null
Service Type: null
Service Map: null
Dry Run Modu Aktif mi?: true
