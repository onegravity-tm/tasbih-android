# 📿 Tasbih - Zamonaviy Android Ilovasi

Ushbu loyiha zamonaviy **Kotlin** va **Jetpack Compose (Material 3)** texnologiyalarida yaratilgan raqamli Tasbih ilovasidir.

Loyiha arxitekturasi shaxsiy kompyuterda og'ir Android Studio va gigabaytlab SDK o'rnatmasdan, bevosita **GitHub Actions** orqali bulutda avtomatik APK yig'ishga to'liq moslashtirilgan.

---

## 🚀 Texnologiyalar va Versiyalar
* **Til:** Kotlin 2.0.20
* **Build tizimi:** Gradle 8.9 + Android Gradle Plugin (AGP) 8.5.2
* **Java:** JDK 17
* **SDK:** compileSdk 34, minSdk 26 (Android 8.0+), targetSdk 34
* **UI:** Jetpack Compose (BOM 2024.09.00), Material 3

---

## 📦 GitHub'ga yuklash va APK olish bo'yicha qo'llanma

### 1-qadam: Loyihani GitHub repozitoriyingizga yuklash
GitHub saytida yangi bo'sh repozitoriya yarating (masalan: `tasbih-android`). So'ngra buyruqlar satrida quyidagilarni bajaring:

```bash
cd "C:\Users\user\.gemini\antigravity\scratch\tasbih-android"
git init
git add .
git commit -m "feat: initial setup with Jetpack Compose and GitHub Actions"
git branch -M main
git remote add origin https://github.com/SIZNING_LOGIN/tasbih-android.git
git push -u origin main
```

*(Eslatma: Agar kompyuteringizda Git terminali bo'lmasa, **GitHub Desktop** dasturi orqali ushbu papkani tanlab, bitta tugma bilan GitHub'ga yuklashingiz mumkin).*

---

### 2-qadam: Bulutda avtomatik APK yig'ilishi
Kodni `main` tarmog'iga push qilishingiz bilan:
1. GitHub Actions avtomatik ravishda `.github/workflows/build-apk.yml` skriptini ishga tushiradi.
2. `ubuntu-latest` bulut serverida barcha kerakli qaramliklar yuklanadi va keshlanadi.
3. Taxminan 1–2 daqiqada `app-debug.apk` tayyor bo'ladi.

---

### 3-qadam: APK faylni yuklab olish va telefonga o'rnatish
1. GitHub repozitoriyangizga kiring va yuqoridagi menyudan **Actions** bo'limini bosing.
2. Ro'yxatdagi eng oxirgi yashil belgi bilan tugagan **Build Android APK** jarayonini oching.
3. Sahifaning eng pastki qismida **Artifacts** bo'limida **`app-debug`** faylini ko'rasiz.
4. Ushbu faylni bosing va zip arxivni yuklab oling.
5. Arxiv ichidagi `app-debug.apk` faylini telefoningizga o'tkazing va o'rnating.
