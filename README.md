<div align="center">
  <img src="logo.svg" width="120" alt="FortNet Logo" />
  <h1>🛡️ FortNet - Local VPN Firewall</h1>
  <p><b>A modern, privacy-first Android application designed to enhance digital wellbeing by providing robust, on-device app blocking capabilities.</b></p>
  
  [![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
  [![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4CAF50.svg?style=flat&logo=android)](https://developer.android.com/jetpack/compose)
  [![Room DB](https://img.shields.io/badge/Database-Room-blue?style=flat&logo=sqlite)](https://developer.android.com/training/data-storage/room)
  [![Platform](https://img.shields.io/badge/Platform-Android-green.svg?style=flat&logo=android)](https://www.android.com/)
  <br/>
</div>

## 📌 النظرة العامة (Overview)
في عصر تزايد المشتتات الرقمية، تم بناء **FortNet** ليكون حلاً تقنياً متقدماً لمساعدة المستخدمين على استعادة تركيزهم وإدارة وقت شاشتهم بفعالية. بدلاً من الاعتماد على خوادم خارجية قد تنتهك الخصوصية، يبتكر FortNet بيئة **Local VPN (شبكة افتراضية محلية)** داخل جهاز المستخدم نفسه. يتيح ذلك حجب الاتصال بالإنترنت عن تطبيقات محددة (مثل وسائل التواصل الاجتماعي أو الألعاب) على مستوى النظام، مع ضمان عدم مغادرة بيانات المستخدم للجهاز.

تم تصميم هذا المشروع ليس فقط كأداة مفيدة، بل كنموذج تطبيقي يعكس اتباع **أفضل الممارسات البرمجية الحديثة (Best Practices)** في بيئة تطوير أندرويد.

---

## ✨ المميزات الأساسية (Core Features)

- **🚫 حظر دقيق على مستوى التطبيق (App-Level Firewall):** القدرة على عزل تطبيقات محددة عن شبكة الإنترنت بضغطة زر دون التأثير على عمل باقِ تطبيقات النظام.
- **⏳ نظام الإدارة الزمنية المتقدم (Smart Timers):** إمكانية تعيين أوقات حظر مؤقتة لتطبيق معين (مثلاً: حظر فيسبوك لمدة 45 دقيقة للتركيز في العمل). يتولى النظام إلغاء الحظر أوتوماتيكيًا فور انتهاء الوقت.
- **📅 جدولة ذكية روتينية (Advanced Scheduling):** محرك جدولة يتيح للمستخدم ضبط فترات منقطعة خلال أيام الأسبوع (كمثال: حظر الألعاب أيام العمل من 9 صباحاً لـ 5 مساءً).
- **🗂️ التصنيف التلقائي الذكي:** يعتمد على الـ `PackageManager` لتحليل وتصنيف التطبيقات المثبتة تلقائياً (تواصل اجتماعي، أدوات، ألعاب) لتسهيل تسلسل الاستخدام (UX).
- **🔄 التحديث الذاتي المدمج (In-App Updater):** نظام تحديث داخلي مدمج يعتمد على `OkHttp` و `DownloadManager` لمراقبة مستودع خارجي (GitHub) وتنبيه المستخدم، مع دعم التثبيت التلقائي عبر `FileProvider`.

---

## 🛠️ البنية التحتية والتقنيات (Tech Stack & Architecture)

تمت هندسة الكود ليكون قابلاً للتوسع (Scalable) ومبنياً على أحدث التقنيات الموصى بها من Google:

*   **لغة البرمجة:** 100% Kotlin.
*   **واجهة المستخدم (UI):** مبنية بالكامل باستخدام **Jetpack Compose** باعتمادات `StateFlow` لتوفير واجهة تفاعلية وردود فعل لحظية بمنهجية الـ *Declarative UI*.
*   **بنية التطبيق (Architecture):** اعتماد نمط **MVVM (Model-View-ViewModel)** لفصل منطق العرض عن البيانات، مما يعزز قابلية الصيانة والاختبار.
*   **إدارة المهام الخلفية (Background Processing):**
    *   `Kotlin Coroutines` لمعالجة البيانات بشكل متزامن وبكفاءة.
    *   `WorkManager` للمهام المجدولة المضمونة (إلغاء حظر التطبيقات حسب الجداول الزمنية بشكل خفيف على البطارية).
*   **الشبكات الافتراضية (Networking & VPN):** استخدام `VpnService` الخاص بنظام الأندرويد لإنشاء نفق محلي (Blackhole Routing) عبر الـ IPv4 و IPv6 مع خاصية الـ Kill Switch.
*   **تخزين البيانات (Persistence):** استخدام **Room Database** كـ ORM للتعامل مع قاعدة بيانات SQLite باحترافية، مع استخدام الـ Flows للـ Reactive Streams.

---

## 🔒 الأمان والخصوصية (Security & Privacy)
الخصوصية في FortNet ليست مجرد ميزة، بل هي المعمارية الأساسية:
- **تحكم محلي كامل (On-Device Processing):** جميع عمليات حظر الـ Traffic تتم على مستوى جهاز المستخدم دون رفع أي Metadata أو Logs لأي سيرفر.
- **استدامة الموارد:** النظام مصمم لاستهلاك طاقة يقترب من الصفر رغم عمل الـ VPN في الخلفية بشكل دائم، وذلك لاعتماده على فلترة الـ Packets داخلياً (Local Routing).

---

## 🚀 طريقة التثبيت وتجربة التطبيق

لتحميل وتجربة النسخة الجاهزة للاستخدام:
1. قم بالانتقال إلى قسم الـ **Releases** أو حمل أحدث نسخة مباشرة من هنا: [app-release.apk](https://raw.githubusercontent.com/Ahmed122223-g/FortNet/main/FortNet.apk)
2. اسمح بالتثبيت من مصادر خارجية (Unknown Sources) عبر إعدادات هاتفك.
3. عند أول تشغيل، سيطلب التطبيق إذن إعداد الـ VPN كي يتمكن من حماية وإدارة اتصال تطبيقاتك، يرجى الموافقة.

---

## 💻 للمطورين (Build Instructions)

لنسخ المشروع وتشغيله في بيئة عملك (Android Studio):

```bash
# استنساخ المشروع
git clone https://github.com/Ahmed122223-g/FortNet.git

# الدخول إلى المجلد
cd FortNet

#ناء نسخة التطوير التجريبية
./gradlew assembleDebug
```