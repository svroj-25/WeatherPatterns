# Practice 6: Architectural Design Patterns (MVC, MVP, MVVM, MVI)
**Fan:** Android Development / Software Modeling and Design (SMD)  
**Loyiha nomi:** `WeatherPatterns`  
**Paket (Package):** `uz.ttpu.weatherpatterns`  
**Texnologiyalar:** Kotlin, Jetpack Compose, Material 3, Coroutines, StateFlow, JUnit4  

---

## Part 1 — Why architecture (Questions 1–2)

### Question 1 — What a proper architecture gives you (THEORETICAL)

**Kontekst:** Jamoadoshingiz sizga 600 qatordan iborat monolit `MainActivity.kt` faylini topshirdi. Unda yagona klass bir vaqtning o‘zida: serverga so‘rov yuboradi, JSON parse qiladi, SharedPreferences bilan ishlaydi, formani tekshiradi (validation) va UI-ni yangilaydi.

Ma’ruzada aytilishicha, to‘g‘ri arxitektura ilovaga 3 ta asosiy narsani beradi:
1. **Oddiylik (KISS — Keep It Simple, Stupid)**
2. **Testlanuvchanlik (Testability)**
3. **Arzon texnik xizmat ko‘rsatish (Low-cost maintenance)**

#### Ushbu klass har uchala xususiyatni qanday yo‘qotganiga aniq misollar:

1. **Oddiylik (KISS) yo‘qolishi:**
   * *Misol:* 600 qatorli klassda tarmoq xatolari (masalan, `IOException`), UI animatsiyalari va SharedPreferences kalitlari bir joyda aralashib ketgan. Dasturchi tugma bosilganda nima sodir bo‘lishini tushunish uchun butun 600 qatorlik faylni boshidan oxirigacha tahlil qilishga majbur bo‘ladi. Klassda «Single Responsibility Principle» (yagona mas’uliyat) printsipi butunlay buzilgan.
2. **Testlanuvchanlik (Testability) yo‘qolishi:**
   * *Misol:* JSON parsing yoki forma validatsiyasi to‘g‘ri ishlayotganini tekshirish uchun oddiy va tezkor JVM Unit-test yoza olmaysiz. Chunki biznes-mantiq bevosita Android `Context`, `SharedPreferences` va `View` vidjetlariga qattiq bog‘langan (tight coupling). Uni faqat sekin ishlaydigan Android Emulator / Instrumented test orqali yoki murakkab mocklar bilan tekshirish mumkin bo‘lib qoladi.
3. **Arzon texnik xizmat ko‘rsatish (Low-cost maintenance) yo‘qolishi:**
   * *Misol:* Agar serverdagi API formati (JSON kalitlari) o‘zgarsa yoki ma’lumotlarni SharedPreferences o‘rniga Room ma’lumotlar bazasida saqlashga qaror qilinsa, to‘g‘ridan-to‘g‘ri `MainActivity` ichidagi kod tahrirlanadi. Bu esa kutilmaganda UI chizilishi yoki hodisalar boshqaruvida yangi xatolar (regression bugs) paydo bo‘lishiga olib keladi. Bitta o‘zgarish butun Activity'ni xavf ostiga qo‘yadi.

#### Klassdan birinchi bo‘lib qaysi qismni ajratib chiqarish kerak va nima uchun?
* **Javob:** Birinchi navbatda **Tarmoq bilan ishlash va ma’lumotlarni saqlash (Data/Network Layer — Repository va API)** qismini alohida klassga ajratib chiqarish kerak.
* **Sababi:** Ma’lumotlar qatlami (Data Layer) Android UI va Lifecycle (Activity hayot sikli)dan butunlay mustaqil bo‘lishi lozim. U ajratilgach, uni alohida tezkor unit-testlar bilan tekshirish, kesh qo‘shish yoki soxta (mock/fake) ma’lumotlar bilan almashtirish mumkin bo‘ladi va UI klassi ancha yengillashadi.

---

### Question 2 — Starting from nothing (THEORETICAL)

**Kontekst:** Kursdoshingiz: «Men Android ilovalarimda hech qachon arxitekturadan foydalanmaganman. Nimadan boshlashim kerak?» deb so‘radi.

#### Mavjud ilovada arxitekturaga o‘tish bo‘yicha 4–5 bosqichli reja:
1. **1-qadam (Eng birinchi qadam — UI arxitekturasi tanlashdan oldin):**
   * **Ma’lumotlar qatlamini (Data Layer / Repository) ajratish.** Barcha tarmoq so‘rovlari, ma’lumotlar bazasi yoki fayllar bilan ishlash kodlarini Activity/Fragment'dan chiqarib, alohida `Repository` klasslariga o‘tkazish.
2. **2-qadam:** Har bir ekran uchun **ma’lumotlar holatini (UI State)** aniqlab olish (masalan, `Loading`, `Success`, `Error`).
3. **3-qadam:** Ekran murakkabligiga qarab mos namuna (pattern) tanlash: oddiy ekranlar uchun oddiy holat boshqaruvi, murakkab dinamik ekranlar uchun esa **MVVM** yoki **MVI**.
4. **4-qadam:** UI qatlamini reaktiv qilish — Activity yoki Composable'lar ma’lumotlarni to‘g‘ridan-to‘g‘ri yuklamay, faqat holatni (StateFlow) kuzatadigan (observe) va hodisalarni yuboradigan passiv ko‘rinishga keltirish.
5. **5-qadam:** Biznes mantiq va Repository uchun mustaqil Unit-testlar yozish.

#### Nima uchun «Hamma joyda faqat MVVM ishlat» degan maslahat to‘g‘ri emas?
* Ma’ruzada ta’kidlanganidek:
  1. **Har qanday loyihaga yoki ekranga mos tushadigan yagona arxitektura yo‘q.** 
  2. Masalan, 2 ta statik matndan iborat «Biz haqimizda» (About) ekrani uchun `ViewModel`, `StateFlow` va `Factory` yaratish — bu ortiqcha murakkablik (over-engineering) va keraksiz kod ko‘payishidir.
  3. Boshqa tomondan, bir vaqtda o‘nlab parallel asinxron hodisalar keladigan murakkab chat yoki oqimli tizimlarda oddiy MVVM holatlarning chalkashishiga (race conditions) sabab bo‘lishi mumkin; bunday joyda qat’iy bir tomonlama oqimga ega bo‘lgan **MVI** ancha ishonchliroqdir.

---

## Part 2 — One Model layer for all four patterns (Questions 3–4)

### Question 3 — Project and the shared data layer (HANDS-ON)

* **Loyiha:** `WeatherPatterns` (`uz.ttpu.weatherpatterns`, Min SDK 24).
* **Fayllar:**
  * `data/model/Weather.kt`: `data class Weather(val city: String, val temperatureC: Int, val condition: String)`
  * `data/api/WeatherApi.kt`: `suspend fun fetchWeather(city: String): Weather`
  * `data/api/FakeWeatherApi.kt`: 1500 ms kechikish bilan shahar ma’lumotlarini qaytaruvchi yoki `IllegalArgumentException` tashlovchi sinf.
  * `data/repository/WeatherRepository.kt`:
    ```kotlin
    class WeatherRepository(private val api: WeatherApi) {
        suspend fun getWeather(city: String): Result<Weather> {
            return try {
                val weather = api.fetchWeather(city)
                Result.success(weather)
            } catch (e: IllegalArgumentException) {
                Result.failure(e)
            }
        }
    }
    ```
    *(Izoh: `runCatching` ishlatilmadi, chunki u Kotlin Coroutines'ning `CancellationException`ini ham yutib yuboradi).*

* **Unit Testlar (`WeatherRepositoryTest.kt`):**
  * `known_city_returns_success()`: "Tashkent" so‘ralganda `isSuccess == true` va `temperatureC == 24` ekanligi tasdiqlandi.
  * `unknown_city_returns_failure()`: "Atlantis" so‘ralganda `isFailure == true` ekanligi tasdiqlandi.
  * **Natija:** 100% muvaffaqiyatli o‘tdi.

---

### Question 4 — Shared screen, module and launcher (HANDS-ON)

* `di/WeatherModule.kt`: Barcha arxitekturalar uchun 3 soniyalik (`latencyMs = 3_000`) kechikishga ega yagona `repository` yaratildi.
* `ui/WeatherScreen.kt`: Barcha patternlar tomonidan ishlatiladigan holat matni (`status`) va 4 ta shahar ("Tashkent", "Samarkand", "Bukhara", "Atlantis") tugmalarini o‘z ichiga olgan Jetpack Compose ekrani.
* `MainActivity.kt`: "MVC", "MVP", "MVVM", "MVI" tugmalari orqali mos Activity'larni ochuvchi Launcher.
* `AndroidManifest.xml`: Barcha 4 ta Activity (`MvcActivity`, `MvpActivity`, `MvvmActivity`, `MviActivity`) ro‘yxatga olindi.

---

## Part 3 — MVC (Questions 5–6)

### Question 5 — MVC roles and the missing Controller (THEORETICAL)

#### 6 ta bo‘lakning rollarga taqsimlanishi:
1. **The Weather data class** — **Model**
2. **WeatherRepository** — **Model**
3. **The composable WeatherScreen** — **View**
4. **An onClick lambda in MainActivity that calls the repository and then writes the result into a TextView** — **View va Controller (ikkala rolni bir vaqtda bajaradi)**
5. **FakeWeatherApi** — **Model**
6. **A new class WeatherController that you are about to write** — **Controller**

#### 4-bo‘lakning muammosi:
* **Muammo:** Ushbu lambda bir vaqtning o‘zida foydalanuvchi bosishini qayta ishlab, Repository'ni chaqiradi (Controller vazifasi) va natijani bevosita `TextView`ga yozadi (View vazifasi).
* **Natijasi:** Bu klassik «Massive View Controller / God Activity» muammosini keltirib chiqaradi. Activity haddan tashqari kattalashadi va biznes-mantiq UI vidjetlariga yopishib qolgani uchun uni emulatorsiz mustaqil JVM Unit-test qilishning iloji qolmaydi.

#### Ma’ruza jumlasini to‘ldirish:
> "The Model has no knowledge of the **View**."  
> *(Model View haqida hech narsa bilmaydi va unga bog‘liq emas).*

---

### Question 6 — MVC with a separate Controller (HANDS-ON)

* `WeatherModel`: `isLoading`, `weather` va `error` holatlarini saqlaydi va `load(city)` orqali yangilaydi.
* `WeatherView`: Yagona `fun render()` funksiyasiga ega interfeys.
* `WeatherController`: `onCitySelected(city)` chaqirilganda Model'ni yuklash holatiga o‘tkazadi, View'ga `render()` buyrug‘ini beradi, ma’lumot kelgach yana `render()` buyrug‘ini yuboradi.
* `MvcActivity`: `render()` ichida to‘g‘ridan-to‘g‘ri `model`dan qiymatlarni o‘qib, `status`ni yangilaydi.

#### `render()` ustidagi savolga javob:
> **Nega `render()` hech qanday parametr qabul qilmaydi va agar Controller View'ga `Weather` obyektini uzatganida nima o‘zgarar edi?**  
> *Klassik MVC'da View bevosita Model'ga qaray oladi va undagi ma’lumotlarni mustaqil o‘qiydi. Shuning uchun `render()` parametrsiz chaqiriladi — bu «Model o‘zgardi, borib o‘zing o‘qib ol va chizil» degani. Agar Controller ma’lumotlarni o‘zi olib, View'ga parametr sifatida uzatsa, View Model'ga bog‘lanishdan butunlay qutuladi va Controller endi Presenter'ga aylanadi (ya’ni bu MVP namunasiga aylanadi).*

---

## Part 4 — MVP (Questions 7–9)

### Question 7 — MVP rules: find the violations (THEORETICAL)

**MVP ning 3 ta oltin qoidasi:**
1. View Model'ga to‘g‘ridan-to‘g‘ri murojaat qila olmaydi.
2. Presenter yagona View bilan 1-ga-1 munosabatda bog‘langan.
3. View mutlaqo «soqov» (dumb) va passiv bo‘lib, u faqat foydalanuvchi harakatini xabar qiladi, mantiqni Presenter'ga qoldiradi.

#### Qoidalar tahlili:
* **(A) qator:** `val result = repository.getWeather("Tashkent")`  
  * **Buzilgan qoida:** **Rule 1 (View cannot access the Model).** View (Activity) to‘g‘ridan-to‘g‘ri Model qatlami (`repository`) bilan muloqot qilmoqda.
* **(B) qator:** `statusLabel.text = if (result.isSuccess) "Sunny" else "Oops"`  
  * **Buzilgan qoida:** **Rule 3 (The View is completely dumb and passive).** View o‘z ichida qanday matn chiqarishni tanlash bo‘yicha taqdimot mantiqini (presentation logic) mustaqil bajarmoqda.
* **(C) va (D) qatorlar:**
  * `(C)`: `presenter = WeatherPresenter(this)`
  * `(D)`: `activity.statusLabel.text = "Loading…"`
  * **Tahlil:** Ushbu qatorlar MVP'ning **2-qoidasiga (Presenter 1-to-1 View bilan bog‘langanligi)** mos keladi. **Lekin testlash nuqtai nazaridan jiddiy xato:** `WeatherPresenter` o‘z konstruktorida to‘g‘ridan-to‘g‘ri Android'ning aniq `WeatherActivity` sinfini va uning `statusLabel: TextView` vidjetini ushlab turibdi. Natijada Presenter'ni emulatorsiz kompyuterda oddiy JVM test bilan sinab bo‘lmaydi.
* **Konstruktor yechimi:** Presenter Activity'ni emas, balki interfeysni bilishi kerak:
  ```kotlin
  class WeatherPresenter(private val view: WeatherContract.View)
  ```

---

### Question 8 — MVP with a testable Presenter (HANDS-ON)

* `WeatherContract.View`: `showLoading()`, `showWeather(weather)`, `showError(message)`.
* `WeatherPresenter`: `onCitySelected(city)` orqali View'ga buyruqlar beradi.
* `WeatherPresenterTest`: Android emulyatorisiz `FakeView` orqali 3 ta test yozildi:
  1. `known_city_shows_loading_then_weather`: `["loading", "weather:Tashkent"]` ketma-ketligi tekshirildi.
  2. `unknown_city_shows_loading_then_error`: `["loading", "error:No forecast for Atlantis"]` tekshirildi.
  3. `two_requests_in_a_row_show_two_loading_events`: `["loading", "weather:Tashkent", "loading", "weather:Samarkand"]` tekshirildi.
  * **Natija:** 100% muvaffaqiyatli o‘tdi.

---

### Question 9 — The Presenter that outlives its View (HANDS-ON)

#### Eksperiment tahlili:
1. `MvpActivity`da "Bukhara" bosilgach, 3 soniyalik so‘rov ketadi. Ekran shu 3 soniya ichida burilganda (rotation):
   * Eski `MvpActivity` (masalan, `activity=10892218`) `onDestroy` bo‘ladi.
   * Yangi `MvpActivity` (masalan, `activity=4892837`) noldan yaratiladi va yangi Presenter ochiladi.
   * 3 soniyadan keyin eski Presenter'dagi so‘rov javob oladi va `showWeather` chaqiruvini **eski, o‘lgan (destroyed) Activity'ga** yuboradi (`showWeather called on activity=10892218`).
2. **Ekranda nima bo‘ladi?**
   * Yangi Activity boshlang‘ich holatda ochilgani uchun ekranda yana **"Pick a city"** turadi. Tarmoqdan kelgan natija foydalanuvchiga ko‘rinmaydi — **ma’lumot yo‘qotildi** va eskirgan View xotirada qolib **Memory Leak** paydo bo‘ldi.
3. **Tuzatish (Fix):**
   * Presenter'ga `fun destroy() { scope.cancel() }` funksiyasi qo‘shildi va Activity'ning `onDestroy()` metodidan chaqirildi. Natijada eski Activity o‘lganda korutina bekor qilinadi va o‘lik View chaqirilmaydi.
4. **Holat rotation'dan omon qolishi uchun qayerda yashashi kerak?**
   * Holat konfiguratsiya o‘zgarishlaridan (rotation) omon qoluvchi tuzilmada, ya’ni Android Architecture **`ViewModel`** (yoki doimiy ma’lumotlar bazasi / `SavedStateHandle`) ichida yashashi shart.

---

## Part 5 — MVVM (Questions 10–11)

### Question 10 — MVVM: where does each class live? (THEORETICAL)

#### 9 ta klassning 6 ta paketga taqsimlanishi:
* `data/api`: `WeatherApi`, `FakeWeatherApi`
* `data/model`: `Weather`
* `data/repository`: `WeatherRepository`
* `presentation/viewmodel`: `WeatherViewModel`, `WeatherUiState`
* `presentation/view`: `WeatherScreen`, `MvvmActivity`
* `di`: `WeatherModule`

#### Savollarga javoblar:
* **(a) Nima uchun ViewModel Activity yoki Composable'ga havola (reference) ushlab turmasligi kerak va uning o‘rniga nimani taqdim etadi?**
  * *Javob:* `ViewModel` konfiguratsiya o‘zgarishlaridan (masalan, ekran aylanishi) omon qoladi, lekin `Activity` va `Composable` butunlay yo‘q qilinadi va qayta tuziladi. Agar `ViewModel` ularga havola ushlasa, yo‘q qilingan Activity xotiradan tozalanmaydi va bu og‘ir **Memory Leak**ka sabab bo‘ladi.
  * Uning o‘rniga ViewModel o‘z holatini reaktiv oqim — **`StateFlow<WeatherUiState>`** ko‘rinishida taqdim etadi. View ushbu oqimni passiv tarzda kuzatadi (observe qiladi).
* **(b) ViewModel View va Model'ni bo‘sh bog‘lagani (loosely coupled) tufayli yozish mumkin bo‘lgan aniq test:**
  * *Misol:* `WeatherViewModelTest` — hech qanday Android konteksti, Activity yoki Composable'larsiz toza JVM'da `viewModel.onCitySelected("Tashkent")` chaqirilib, `viewModel.uiState.value` holati `Loading`dan `Success(Weather("Tashkent", 24, "Sunny"))`ga to‘g‘ri o‘tganligini tekshirish mumkin.

---

### Question 11 — MVVM with a ViewModel and StateFlow (HANDS-ON)

* `WeatherUiState`: `Idle`, `Loading`, `Success(weather)`, `Error(message)` holatlari.
* `WeatherViewModel`: `_uiState` (`MutableStateFlow`) va tashqi `uiState` (`StateFlow`).
* `MvvmActivity`: `viewModel.uiState.collectAsStateWithLifecycle()` orqali UI-ni yangilaydi.

#### Eksperiment natijasi:
* "Bukhara" tugmasi bosilib, 3 soniya ichida ekran aylantirilganda:
  * **Ekranda nima bo‘ladi?** Ekran aylangandan keyin ham ekranda **"Loading…"** holati saqlanib turadi va 3 soniya to‘lgach avtomatik tarzda **"Bukhara: 27°C, Windy"** yozuvi chiqadi.
  * **So‘rov yo‘qoldimi?** **Yo‘q, so‘rov yo‘qolmadi!** Chunki `WeatherViewModel` va uning `viewModelScope` korutinasi Activity aylanishida o‘lmaydi. Yangi yaratilgan Activity esa o‘sha tirik `ViewModel`ning `uiState` oqimiga qayta ulanadi.

---

## Part 6 — MVI (Questions 12–13)

### Question 12 — MVI principles: spot the violations (THEORETICAL)

**MVI ning 3 ta asosiy printsipi:**
1. **Unidirectional Data Flow (UDF):** Ma’lumotlar faqat bir tomonga (Model -> View -> Intent -> Model) harakatlanadi.
2. **Separation of Concerns:** Model holatni boshqaradi, View uni chizadi, Intent foydalanuvchi maqsadini bildiradi.
3. **Immutability (O‘zgarmaslik):** Holat mavjud obyekt ichida o‘zgartirilmaydi, har doim yangi nusxa (`copy()`) yaratiladi.

#### Kod buzilishlari tahlili:
* **(1) `var city = "Tashkent"`, `var isLoading = false`:**
  * *Buzilgan printsip:* **Immutability va Single State Container.** Holat tarqoq o‘zgaruvchan `var`larda saqlanmoqda, bu esa holatlarning chalkashishiga olib keladi.
* **(2) `fun onSpinnerChanged(name: String)`, `fun onRefreshButtonClicked()`:**
  * *Buzilgan printsip:* **Unidirectional Data Flow / Intent Abstraction.** ViewModel aniq UI vidjetlariga moslashtirilgan alohida imperativ funksiyalar ochmoqda. MVI'da barcha hodisalar yagona `Intent` oqimi orqali kelishi shart.
* **(3) `data class WeatherState(var isLoading: Boolean, var temperatureC: Int)`:**
  * *Buzilgan printsip:* **Immutability.** State xususiyatlari `var` qilib e’lon qilingan. Bu tashqaridan holatni ichki o‘zgartirib yuborish imkonini beradi.
* **(4) `viewModel.isLoading = true` (Activity ichida):**
  * *Buzilgan printsip:* **Separation of Concerns va Unidirectional Data Flow.** View to‘g‘ridan-to‘g‘ri ViewModel'ning ichki holatini o‘zgartirmoqda. View holatni faqat o‘qishi va o‘zgarish uchun faqat Intent yuborishi mumkin.

#### MVI uchun zarur bo‘lgan 3 ta deklaratsiya:
```kotlin
// 1. Yagona o'zgarmas holat
data class WeatherState(val isLoading: Boolean = false, val weather: Weather? = null, val error: String? = null)

// 2. Yagona Intent interfeysi
sealed interface WeatherIntent {
    data class SelectCity(val city: String) : WeatherIntent
    data object DismissError : WeatherIntent
}

// 3. ViewModel'ning yagona ommaviy funksiyasi
fun onIntent(intent: WeatherIntent)
```

---

### Question 13 — MVI: one state, immutable updates, intents (HANDS-ON)

* `WeatherMviViewModel`: `onIntent(intent)` orqali barcha buyruqlarni qabul qiladi va `_state.update { it.copy(...) }` orqali yangi o‘zgarmas holatlarni yaratadi.
* `toStatusText()` mantiqida: `isLoading` va eski `weather` bir vaqtda mavjud bo‘lsa, **birinchi bo‘lib `isLoading` tekshiriladi**, shunda foydalanuvchi yangi shahar bosilganda darhol "Loading…" yozuvini ko‘radi.
* **Unit Testlar (`WeatherMviViewModelTest.kt`):**
  1. `selecting_a_known_city_ends_with_weather_and_no_loading()` — muvaffaqiyatli ob-havo kelishi.
  2. `selecting_an_unknown_city_ends_with_error()` — noma’lum shahar xatolik berishi.
  3. `dismissing_the_error_clears_it_and_keeps_the_old_weather()` — xatolik bekor qilinganda eski ob-havo holatda saqlanib qolishi.
  * **Natija:** 100% muvaffaqiyatli o‘tdi.

---

## Part 7 — Compare and choose (Questions 14–15)

### Question 14 — What does each pattern depend on? (HANDS-ON)

#### Arxitekturalarni taqqoslash jadvali:

| Pattern | Mantiqni ushlab turuvchi sinf (Logic Class) | `import android.*` bormi? | `import androidx.*` bormi? | View nimaga havola tutadi? | Ekran aylanganda (Rotation) nima saqlanib qoladi? |
| :--- | :--- | :---: | :---: | :--- | :--- |
| **MVC** | `WeatherController` | **Yo‘q** | **Yo‘q** | Controller (`WeatherController`) | **Hech narsa saqlanmaydi** (Controller va Model yangidan ochiladi) |
| **MVP** | `WeatherPresenter` | **Yo‘q** | **Yo‘q** | Presenter (`WeatherPresenter`) | **Hech narsa saqlanmaydi** (Presenter bekor qilinadi, yangisi ochiladi) |
| **MVVM** | `WeatherViewModel` | **Yo‘q** | **Ha** (`androidx.lifecycle.*`) | ViewModel (`WeatherViewModel`) | **`WeatherViewModel` va uning `uiState`i to‘liq saqlanadi** |
| **MVI** | `WeatherMviViewModel` | **Yo‘q** | **Ha** (`androidx.lifecycle.*`) | ViewModel (`WeatherMviViewModel`) | **`WeatherMviViewModel` va uning `state`i to‘liq saqlanadi** |

#### Testlash kutubxonalari bo‘yicha tushuntirish:
* **MVC va MVP:** Hech qanday qo‘shimcha test kutubxonasisiz (faqat standart JUnit) oddiy JVM'da ishlaydi, chunki ular toza Kotlin klasslari bo‘lib, `Dispatchers.Unconfined` orqali to‘g‘ridan-to‘g‘ri sinovdan o‘tadi.
* **MVVM va MVI:** Ularning mantiqi `viewModelScope` (`Dispatchers.Main`) ga tayangani sababli, oddiy kompyuterda (emulyatorsiz) Unit-test qilish uchun **`kotlinx-coroutines-test`** kutubxonasi (`Dispatchers.setMain(UnconfinedTestDispatcher())`) talab etiladi.

---

### Question 15 — Choose a pattern (THEORETICAL)

#### 3 ta real vaziyat uchun arxitektura tanlovi:

1. **Vaziyat 1: Ikkita statik matndan iborat «Biz haqimizda» (About) ekrani:**
   * **Tanlov:** **Hech qanday pattern kerak emas, faqat View / Composable (No pattern, only a data layer / plain View).**
   * **Ma’ruza asosi:** **Oddiylik (KISS)** va «Har qanday loyihaga yagona pattern to‘g‘ri kelmaydi». Statik matn uchun ViewModel, Factory, reducers va oqimlar yaratish keraksiz ortiqcha yukdir.
   * **Narxi (Cost):** Agar kelajakda ekranga dinamik ma’lumotlar qo‘shilsa, uni arxitekturaga o‘tkazish (refactor qilish) talab etiladi.

2. **Vaziyat 2: Xabarlar ro‘yxati, yozish indikatori («typing...») va tarmoqqa ulanish holati bir vaqtda o‘zgaruvchi Chat ekrani:**
   * **Tanlov:** **MVI (Model-View-Intent).**
   * **Ma’ruza asosi:** **Single Source of Truth, Unidirectional Data Flow va Immutability.** Bir vaqtning o‘zida turli manbalardan (web-socket xabarlari, internet uzilishi, foydalanuvchi kiritishi) ko‘plab asinxron hodisalar kelganda, MVI barcha o‘zgarishlarni yagona atomar `ChatState`ga aylantiradi va holatlar chalkashishini (race conditions) to‘liq bartaraf etadi.
   * **Narxi (Cost):** Ko‘proq qolipli kod (boilerplate) — har bir harakat uchun alohida Intent va reductor mantiqini yozish kerak bo‘ladi.

3. **Vaziyat 3: 40 ta Activity to‘g‘ridan-to‘g‘ri ma’lumotlar bazasini o‘qiydigan 5 yillik eski ilova, 2 ta dasturchi va 1 oydan keyin reliz:**
   * **Tanlov:** **Hech qanday UI patternini o‘zgartirmasdan, faqat ma’lumotlar qatlamini (Repository pattern) ajratish.**
   * **Ma’ruza asosi:** **Bosqichma-bosqich refaktoring (Incremental refactoring).** 1 oy ichida 2 kishi bilan 40 ta Activity'ni MVVM yoki MVI ga o‘tkazish imkonsiz va juda ko‘p yangi xatolarni keltirib chiqaradi. Birinchi bosqichda faqat bazaga kirishni Repository'ga yig‘ish relizni xavf ostiga qo‘ymasdan kodni tartibga soladi.
   * **Narxi (Cost):** Activity'lar ichidagi taqdimot mantiqi bu relizda ham eski holatida qoladi.

#### (1) va (2) vaziyatlar bitta ilova ichida turli tuzilmalar bilan birga yashay oladimi?
* **Ha, albatta yashay oladi.**
* **Ma’ruzadan keltirilgan tasdiqlovchi fikr:**
  > *"Even inside one complex project each module can have its own structure. A design pattern is abstract and its implementation depends on the requirements."*  
  > *(Hatto bitta murakkab loyiha ichida ham har bir modul o‘z tuzilmasiga ega bo‘lishi mumkin. Dizayn namunasi mavhum bo‘lib, uni qo‘llash aniq talablarga bog‘liq).*
