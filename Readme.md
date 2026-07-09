# Maven Dependency Upgrader

Bu araç; bir reçete (`Recipe`) dosyasının içindeki (`vulnerability-recipe.yaml`) kurallara göre, hedef klasördeki (`test-environment`) tüm mikroservislerin `pom.xml` dosyalarını otomatik olarak günceller.

Satır bazlı çalıştığı için `pom.xml` dosyalarının orijinal formatını, girintilerini ve yorum satırlarını asla bozmaz.

## Özellikler

* **Etkileşimli Panel:** Parametre girmekle uğraştırmaz. Başlayınca dosyaları kontrol eder, onay (`y/d`) ve çalışma modu sorar.
* **Property Desteği:** Sürümler doğrudan koda yazılmadıysa, yukarıdaki `<properties>` bloğundaki değişken değerini bulup günceller.
* **Strict Matching:** Sadece `artifactId` değil, `groupId` kontrolü de yapar; yanlış kütüphaneleri ezmez.
* **BOM ve Güvenlik Ayrımı:** `<dependencyManagement>` ile normal bağımlılıkları ayırır. Sürümü olmayan (BOM'dan gelen) kütüphanelere zorla sürüm eklemez.
* **Tek Satır Koruması:** Yan yana yazılmış `<dependency>` bloklarında XML yapısını korur, dosyayı bozmaz.
* **Yedekleme & Hata İzolasyonu:** Değişiklik yapmadan önce orijinal dosyanın yedeğini alır. Bir serviste hata çıkarsa durmaz, diğerine geçer.

## Çalıştırma (IDE veya Terminal)
Proje dizininde Recipe ve test-environment klasörünün bulunması yeterlidir. IDE'den doğrudan oynat (Run) butonuna basabilir veya şu komutu kullanabilirsin:
```bash
mvn clean package
java -jar target/dependency-upgrader-1.0-SNAPSHOT.jar
```

### Konsol Çıktısı Örneği
```text
==================================================
               Dependency Upgrader                
==================================================
[BİLGİ] Sistem bileşenleri kontrol ediliyor...
[BAŞARILI] Klasörde tek tarif dosyası bulundu, otomatik seçildi: vulnerability-recipe.yaml
--------------------------------------------------
    Çalıştırılacak Tarif : Recipe/vulnerability-recipe.yaml
    Kök Tarama Dizini    : test-environment
--------------------------------------------------
Lütfen devam etmek için bir çalışma modu seçin:
 [y] -> Gerçek Güncelleme Modu (Dosyaları değiştirir, otomatik backup alır)
 [d] -> Simülasyon (Dry-Run) Modu (Dosyalara dokunmaz, sadece rapor sunar)
 [Herhangi Başka Bir Tuş] -> İşlemi İptal Et ve Çık
Seçiminiz: d

[ONAYLANDI] Simülasyon (Dry-Run) modu başlatılıyor... Güvenli analiz.

[SİSTEM] Recipe dosyası başarıyla hafızaya yüklendi: 2026-Q4-vulnerability-fix

==================================================
1.         [DRY-RUN] SİMÜLASYON RAPORU
==================================================
Service Name: dnext-cost-management
Service Path: test-environment/dnext-cost-management/pom.xml
Detected Service Type: CA

Updated Items: 0
  - None

Skipped Items: 4
  - spring-boot-starter-web: present in service but not found in recipe
  - lombok: present in service but not found in recipe
  - guava: present in service but not found in recipe
  - hsqldb: present in service but not found in recipe

Unchanged Items: 5
  - spring-boot-starter-parent: zaten güncel (1.0.0)
  - dnext-common-dependencies: zaten güncel (1.0.0-ca)
  - state-flow-client: zaten güncel (1.0.0-ca)
  - jacoco-maven-plugin: zaten güncel (1.0.0)
  - dnext.access-control.version (Property): zaten güncel (1.0.0)

Warnings: 1
  - Tek satirlik dependency tespit edildi, atlanmis olabilir: <dependency><groupId>org.hsqldb</groupId><artifactId>hsqldb</artifactId><version>2.7.2</version></dependency>

Errors: 0
  - None
==================================================
```