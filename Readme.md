# Recipe Based Maven Dependency Upgrade Automation Tool

Bu araç; her çeyrek sonunda yayınlanan güvenlik açığı giderme talimatlarına (Recipe YAML) göre, kurumsal mikroservislerin `pom.xml` dosyalarındaki bağımlılıkları otomatik ve formatı bozmadan güncelleyen kurşungeçirmez bir otomasyon motorudur.

## 🎯 Temel Özellikler & Kurumsal İş Mantığı
* **Gelişmiş Tip Doğrulama (Fail-Fast):** Geçersiz servis tipleri (`--service-type CAA` vb.) sisteme girdiğinde uygulama anında hata vererek durur.
* **Akıllı Otomatik Tespit (Auto-Detect):** Sadece rastgele satırlara bakmak yerine, servisin içindeki kritik kütüphanelerin sürümlerine bakar. Eğer recipe'deki kırılımlı kütüphaneler mevcutsa ve sonunda `-ca` eki yoksa servisi güvenle `NON-CA` ilan eder. Hiçbir bağlam bulunamazsa `UNKNOWN` olarak işaretler ve hassas sürümleri ezmez.
* **Hiyerarşik Öncelik Yönetimi:** Servis tipi belirlenirken şu hiyerarşi mutlak olarak işletilir: `CLI Parametresi > Service Map Dosyası (YAML) > Otomatik Tespit`.
* **Hata İzolasyonu (Fault Isolation):** Çoklu taramalarda herhangi bir serviste dosya okuma/yazma hatası oluşursa uygulama çökmez; o servisi izole edip loglar ve bir sonraki servise güvenle geçer.
* **Format Koruyucu Motor:** `pom.xml` dosyalarını güncellerken XML girintilerini (indentation), boşlukları ve yorum satırlarını milimetrik olarak korur.

## 🛠️ Kullanılabilir CLI Parametreleri

| Parametre | Zorunlu mu? | Açıklama |
| :--- | :--- | :--- |
| `--recipe` | **Evet** | Güncelleme kurallarını içeren YAML dosyasının yolu. |
| `--service-path` | Hayır | Sadece tek bir mikroservis klasörünü hedeflemek için kullanılır. |
| `--root-path` | Hayır | Altındaki tüm mikroservisleri toplu taramak için kök dizin yolu. |
| `--service-type` | Hayır | Tüm servislere zorla `ca` veya `non-ca` tipi dayatmak için kullanılır. |
| `--service-map` | Hayır | Servis adlarını tipleriyle eşleyen harici YAML dosyası (`Services.yaml`). |
| `--dry-run` | Hayır | Aktif edildiğinde diske yazma yapmaz, sadece simülasyon raporu sunar. |

## 🚀 Çalıştırma Senaryoları

Proje `maven-shade-plugin` ile çalıştırılabilir tek bir **Fat-JAR** olarak paketlenmektedir.

### 1. Üretim Öncesi Detaylı Simülasyon (Dry-Run Modu)
Harici servis haritalama dosyası (`Services.yaml`) dahil tüm parametreleri simüle etmek için:
```bash
java -jar target/dependency-upgrader-1.0-SNAPSHOT.jar --recipe vulnerability-recipe.yaml --root-path test-environment --service-map Services.yaml --dry-run
```

### 2. Gerçek Zamanlı Güncelleme ve Otomatik Yedekleme
Değişiklikleri fiziksel olarak pom.xml dosyalarına işlemek ve orijinal dosyaları otomatik olarak .backup/{recipeVersion}/{serviceName}/pom.xml konumunda güvenli yedeğe almak için:
```bash
java -jar target/dependency-upgrader-1.0-SNAPSHOT.jar --recipe vulnerability-recipe.yaml --root-path test-environment --service-map Services.yaml
```

### 📊 Örnek Konsol Çıktısı (Öncelik Hiyerarşili Çalışma Logu)
```text
[BAŞARILI] Recipe dosyası başarıyla okundu: 2026-Q4-vulnerability-fix
[BAŞARILI] Service Map dosyası başarıyla yüklendi: Services.yaml

==================================================
         [DRY-RUN] SİMÜLASYON RAPORU
==================================================
Service Name: dnext-cost-management
Service Path: test-environment/dnext-cost-management/pom.xml
Detected Service Type: NON-CA

Updated Items:
  - access-control: 0.5.6 -> 1.0.0
  - state-flow-client: 0.3.4-ca -> 1.0.1

Skipped Items:
  - spring-boot-starter-web: present in service but not found in recipe
  - lombok: present in service but not found in recipe

Unchanged Items:
  - None

Warnings:
  - None
==================================================

[BİLGİ] Kullanılan Servis Map Konumu: Services.yaml
```