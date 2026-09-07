# VpnAuto

**[English](#english) · [Русский](#русский)**

<p align="center"><img src="docs/screenshot-main.png" width="280"> <img src="docs/screenshot-pick.png" width="280"></p>

---

## Русский

VpnAuto включает VPN, когда ты открываешь выбранное приложение, и выключает, когда выходишь. Для каждого приложения можно назначить свой сервер из **v2rayNG** — так одни приложения ходят через один сервер, другие через второй, а всё остальное — напрямую.

Никакого Tasker, никаких макросов: поставил, отметил приложения, забыл.

### Как это работает
- Приложение регистрирует службу спец. возможностей и следит, какое окно на переднем плане.
- При входе в отмеченное приложение шлёт v2rayNG команду «подключиться к серверу X» (через его штатный Tasker-интерфейс).
- При выходе — ждёт указанное время (по умолчанию 3 с) и шлёт «отключиться».
- При переходе между приложениями с разными серверами — переключает туннель.

### Установка
1. Установи [v2rayNG](https://github.com/2dust/v2rayNG/releases), импортируй подписки и **подключись вручную один раз** (чтобы Android выдал разрешение на VPN).
2. Скачай `VpnAuto-x.y.apk` со страницы [Releases](../../releases/latest) и установи.
3. Открой VpnAuto и пройди три карточки сверху:
   - **Служба** → включи VpnAuto в спец. возможностях.
   - **Не усыплять приложение** → Разрешить.
   - Задержка выключения — по вкусу.
4. Напротив нужного приложения нажми **+** → откроется список серверов v2rayNG → отметь сервер → галочка сверху.

### Если что-то не так
| Симптом | Что делать |
|---|---|
| Пункт VpnAuto в спец. возможностях серый / не включается | Настройки → Приложения → VpnAuto → **⋮** → **Разрешить ограниченные настройки**. Это защита Android от APK не из магазина. |
| После обновления APK служба «включена», но не работает | Спец. возможности → VpnAuto → **выключить и включить заново**. Android отвязывает службу при каждом обновлении. |
| Служба отваливается через несколько часов (Samsung) | Карточка «Не усыплять приложение» → Разрешить. Дополнительно: Настройки → Батарея → Спящие приложения → убедись, что VpnAuto нет в списке. |
| VPN не поднимается, всплывашка «VpnAuto → сервер» есть | v2rayNG: убери из оптимизации батареи, проверь, что ручное подключение работает. |
| Кнопка «+» пишет «v2rayNG не установлен» | Нужен v2rayNG с пакетом `com.v2ray.ang` (официальный). Форки с другим пакетом не поддерживаются. |

### Ограничения
- Android допускает только один активный VPN, поэтому при смене сервера туннель на секунду рвётся.
- Поддерживается только v2rayNG. **Happ** — закрытый форк, внешнего API у него нет. NekoBox / Hiddify — в планах.
- «Выход из приложения» на Android — это уход в фон, поэтому есть задержка перед выключением.

### Сборка
Проект собирается GitHub Actions при каждом пуше и публикует APK в Releases. Для подписи своим ключом добавь в Secrets репозитория: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Без них соберётся с debug-ключом.

Локально: `gradle assembleRelease` (Android SDK 34, JDK 17).

---

## English

VpnAuto turns your VPN on when you open a selected app and off when you leave it. Each app can be bound to its own **v2rayNG** server, so some apps route through one server, others through another, and everything else goes direct.

No Tasker, no macros: install, tick the apps, forget.

### How it works
- An accessibility service watches which window is in the foreground.
- When a selected app appears, VpnAuto tells v2rayNG to connect to server X (via v2rayNG's built-in Tasker interface).
- When you leave, it waits a configurable delay (default 3 s) and sends "disconnect".
- Switching between apps bound to different servers restarts the tunnel with the new server.

### Setup
1. Install [v2rayNG](https://github.com/2dust/v2rayNG/releases), import your subscriptions and **connect manually once** so Android grants the VPN permission.
2. Download `VpnAuto-x.y.apk` from [Releases](../../releases/latest) and install it.
3. Open VpnAuto and go through the three cards at the top:
   - **Service** → enable VpnAuto in Accessibility.
   - **Keep app awake** → Allow.
   - Turn-off delay — to taste.
4. Tap **+** next to an app → v2rayNG's server list opens → select a server → check mark.

### Troubleshooting
| Symptom | Fix |
|---|---|
| VpnAuto is greyed out in Accessibility | Settings → Apps → VpnAuto → **⋮** → **Allow restricted settings** (Android's sideload protection). |
| After updating the APK the service shows "on" but does nothing | Accessibility → VpnAuto → **toggle off and on**. Android unbinds the service on every update. |
| Service dies after a few hours (Samsung) | "Keep app awake" card → Allow. Also check Settings → Battery → Sleeping apps. |
| Toast "VpnAuto → server" appears but no VPN | Exclude v2rayNG from battery optimisation; make sure manual connect works. |
| "+" says "v2rayNG is not installed" | Only the official `com.v2ray.ang` package is supported. |

### Limitations
- Android allows a single active VPN, so switching servers briefly drops the tunnel.
- v2rayNG only. **Happ** is a closed-source fork without an external API. NekoBox / Hiddify are planned.
- "Leaving an app" on Android means going to background, hence the delay.

### Building
GitHub Actions builds on every push and publishes the APK to Releases. To sign with your own key, add repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Without them the build uses the debug key.

Locally: `gradle assembleRelease` (Android SDK 34, JDK 17).

## License
MIT — see [LICENSE](LICENSE).
