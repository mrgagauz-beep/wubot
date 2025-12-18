# Полная документация авторизации WarUniverse

Документация цикла авторизации на основе анализа старого проекта `D:\Projects\WarUniverse-BOT`.

## Содержание

1. [Формат конфигурационного файла](#1-формат-конфигурационного-файла)
2. [Шаг 1: Загрузка конфига](#2-шаг-1-загрузка-конфига)
3. [Шаг 2: HTTP авторизация](#3-шаг-2-http-авторизация)
4. [Шаг 3: TCP подключение](#4-шаг-3-tcp-подключение)
5. [Шаг 4: Отправка пакетов авторизации](#5-шаг-4-отправка-пакетов-авторизации)
6. [Шаг 5: Получение ответов](#6-шаг-5-получение-ответов)
7. [Шаг 6: Вход на карту](#7-шаг-6-вход-на-карту)
8. [Возможные ошибки](#8-возможные-ошибки)
9. [Полный цикл авторизации (диаграмма)](#9-полный-цикл-авторизации)

---

## 1. Формат конфигурационного файла

### config.json

```json
{
  "connection": {
    "serverHost": "162.19.232.126",
    "serverPort": 43431,
    "username": "YourLogin",
    "password": "YourPassword",

    "apiHost": "eu.api.waruniverse.space",
    "apiPort": 443,

    "token": "",
    "deviceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",

    "clientVersion": [1, 233, 0],
    "platform": "Desktop",
    "systemLocale": "ru_MD",
    "preferredLocale": "ru"
  }
}
```

### Параметры подключения

| Параметр | Тип | Описание |
|----------|-----|----------|
| `serverHost` | String | IP адрес игрового сервера (TCP) |
| `serverPort` | int | Порт игрового сервера (TCP) |
| `apiHost` | String | Хост HTTPS API для получения токена |
| `apiPort` | int | Порт HTTPS API (443) |
| `username` | String | Логин пользователя |
| `password` | String | Пароль пользователя |
| `token` | String | Сохранённый токен (формат: `USER_ID:SESSION_UUID`) |
| `deviceId` | String | UUID устройства (UID) |
| `clientVersion` | int[] | Версия клиента [major, minor, patch] |
| `platform` | String | Платформа: "Desktop", "Android", "iOS" |

### Файл preferences (~/.prefs/WarUniverse)

Игра сохраняет учётные данные в XML файле `~/.prefs/WarUniverse`:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
  <entry key="login">Demonster</entry>
  <entry key="password">password123</entry>
  <entry key="ssid">30325721:20f39dbd-e6a3-49bd-9044-1982572744b3</entry>
  <entry key="uid">cdf6ccc9-d5ff-428d-8074-b22711a0f79b</entry>
  <entry key="game_server">eu</entry>
  <entry key="credentials">login1;pass1;1
login2;pass2</entry>
</properties>
```

---

## 2. Шаг 1: Загрузка конфига

### Код загрузки конфига из WuBot.java:703-740

```java
public static void main(String[] args) {
    log.info("Loading configuration...");

    Map<String, Object> config = loadConfig();
    if (config == null) {
        log.error("Failed to load config.json");
        log.error("Please create config.json from config.example.json");
        return;
    }

    log.info("Config loaded successfully");

    WuBot bot = new WuBot(config);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        log.info("Shutdown signal received");
        bot.shutdown();
    }));

    bot.start();
}

@SuppressWarnings("unchecked")
private static Map<String, Object> loadConfig() {
    Path configPath = Path.of("config.json");

    if (!Files.exists(configPath)) {
        log.warn("config.json not found in current directory");
        return null;
    }

    try (FileReader reader = new FileReader(configPath.toFile())) {
        return gson.fromJson(reader, Map.class);
    } catch (IOException e) {
        log.error("Error reading config.json: {}", e.getMessage());
        return null;
    }
}
```

### Загрузка аккаунтов из preferences (AccountManager.java:112-160)

```java
public boolean loadAccounts() {
    if (!Files.exists(prefsPath)) {
        log.warn("Preferences file not found: {}", prefsPath);
        return false;
    }

    try (FileInputStream fis = new FileInputStream(prefsPath.toFile())) {
        prefs = new Properties();
        prefs.loadFromXML(fis);

        // Parse credentials
        String credentialsStr = prefs.getProperty("credentials");
        if (credentialsStr != null && !credentialsStr.isEmpty()) {
            accounts = parseCredentials(credentialsStr);
        }

        // Add current login/password if not in list
        String currentLogin = prefs.getProperty("login");
        String currentPassword = prefs.getProperty("password");
        if (currentLogin != null && currentPassword != null) {
            boolean found = accounts.stream()
                    .anyMatch(a -> a.getLogin().equals(currentLogin));
            if (!found) {
                Account current = new Account(currentLogin, currentPassword, true);
                current.setServer(prefs.getProperty("game_server"));
                current.setNickname(prefs.getProperty("nickname"));
                accounts.add(0, current);
            }
        }

        log.info("Loaded {} accounts from preferences", accounts.size());
        return true;

    } catch (IOException e) {
        log.error("Failed to load preferences: {}", e.getMessage());
        return false;
    }
}
```

---

## 3. Шаг 2: HTTP авторизация

### Эндпоинт

```
GET https://eu.api.waruniverse.space/auth-api/v4/get-token/login?login=LOGIN&password=PASSWORD
```

### Headers

```
Accept: application/json
User-Agent: WarUniverse/1.233.0
```

### Ответ (успешный)

```json
{
  "token": "4cce0299-15af-4c6e-b6df-7239643396d4",
  "tokenId": 30337147,
  "registered": false
}
```

### Формирование итогового токена

```
TOKEN = tokenId + ":" + token
Пример: "30337147:4cce0299-15af-4c6e-b6df-7239643396d4"
```

### Код HTTP авторизации (HttpAuthClient.java:48-100)

```java
public String authenticate(String login, String password) {
    log.info("Attempting HTTPS authentication for {}...", login);

    // Use the correct endpoint discovered via mitmproxy
    String token = getTokenViaApi(login, password);
    if (token != null) {
        return token;
    }

    log.error("HTTPS auth failed");
    return null;
}

private String getTokenViaApi(String login, String password) {
    try {
        // URL encode parameters
        String encodedLogin = java.net.URLEncoder.encode(login, "UTF-8");
        String encodedPassword = java.net.URLEncoder.encode(password, "UTF-8");

        String url = "https://" + API_HOST + "/auth-api/v4/get-token/login?login="
                + encodedLogin + "&password=" + encodedPassword;

        log.debug("Auth URL: {}", url.replace(encodedPassword, "***"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "WarUniverse/1.233.0")
                .GET()
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        log.info("Auth response: status={}", response.statusCode());
        log.debug("Response body: {}", response.body());

        if (response.statusCode() == 200) {
            return parseTokenResponse(response.body());
        } else {
            log.error("Auth failed with status {}: {}", response.statusCode(), response.body());
        }

    } catch (Exception e) {
        log.error("Auth request failed: {}", e.getMessage());
    }
    return null;
}

private String parseTokenResponse(String responseBody) {
    try {
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

        if (json.has("token") && json.has("tokenId")) {
            String token = json.get("token").getAsString();
            long tokenId = json.get("tokenId").getAsLong();

            String fullToken = tokenId + ":" + token;
            log.info("Got token: {}:{}...", tokenId, token.substring(0, 8));
            return fullToken;
        }

        // Check for error
        if (json.has("error")) {
            log.error("API error: {}", json.get("error").getAsString());
        }

    } catch (Exception e) {
        log.error("Failed to parse token response: {}", e.getMessage());
    }
    return null;
}
```

---

## 4. Шаг 3: TCP подключение

### Параметры подключения

| Параметр | Значение |
|----------|----------|
| Host | 162.19.232.126 |
| Port | 43431 |
| Protocol | TCP (KryoNet) |
| Write Buffer | 16384 bytes |
| Object Buffer | 16384 bytes |
| Timeout | 15000 ms |

### Код подключения (WuClient.java:96-137)

```java
private static final int WRITE_BUFFER = 16384;
private static final int OBJECT_BUFFER = 16384;
private static final int TIMEOUT_MS = 15000;

public boolean connect(String host, int port) {
    this.serverHost = host;
    this.serverPort = port;

    log.info("Connecting to {}:{} (TCP only)...", host, port);

    try {
        client.start();
        // Connect with TCP only (no UDP - game uses TCP for all packets)
        client.connect(TIMEOUT_MS, host, port);
        waitForConnection();
        return connected;
    } catch (Exception e) {
        log.error("Failed to connect: {}", e.getMessage(), e);
        return false;
    }
}

private void waitForConnection() {
    int maxWait = 50; // 5 seconds max
    while (!connected && maxWait-- > 0) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }
    if (connected) {
        log.info("Connection established successfully");
    } else {
        log.warn("Connection flag not set after waiting");
        // Force check KryoNet internal state
        connected = client.isConnected();
        if (connected) {
            log.info("Connection confirmed via KryoNet");
        }
    }
}
```

### Регистрация пакетов Kryo (обязательно до подключения)

```java
public WuClient() {
    this.client = new Client(WRITE_BUFFER, OBJECT_BUFFER);
    this.packetLogger = new PacketLogger();
    this.packetHandlers = new CopyOnWriteArrayList<>();

    // Register packets BEFORE connecting
    PacketRegistry.register(client.getKryo());

    // Setup listener
    setupListener();
}
```

---

## 5. Шаг 4: Отправка пакетов авторизации

### Метод 1: Token-based авторизация (основной)

#### Пакет ApiRequestPacket

```java
package com.spaiowenta.commons.api;

public class ApiRequestPacket {
    private int requestId;
    private String uri;
    private String requestDataJson;
}
```

#### URI эндпоинта

```
auth/token-login
```

#### Формат requestDataJson

```json
{
  "token": "30337147:4cce0299-15af-4c6e-b6df-7239643396d4",
  "clientInfo": {
    "uid": "cdf6ccc9-d5ff-428d-8074-b22711a0f79b",
    "build": 0,
    "version": [1, 233, 0],
    "platform": "Desktop",
    "systemLocale": "ru_MD",
    "preferredLocale": "ru",
    "clientHash": "269980fe6e943c59e8ff10338f719870"
  }
}
```

#### Код отправки (AuthManager.java:208-241)

```java
public CompletableFuture<Boolean> loginWithToken(String token) {
    log.info("Authenticating with token...");

    apiResponseFuture = new CompletableFuture<>();

    // Build AuthTokenLoginRequest JSON
    AuthTokenLoginRequest tokenRequest = new AuthTokenLoginRequest(token, clientInfo);
    String requestJson = gson.toJson(tokenRequest);

    // Build ApiRequestPacket
    int requestId = requestIdCounter.getAndIncrement();
    ApiRequestPacket apiRequest = new ApiRequestPacket(requestId, AUTH_TOKEN_LOGIN_URI, requestJson);

    client.send(apiRequest);

    return apiResponseFuture
            .orTimeout(AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .thenApply(response -> {
                // Check responseData for netStatus (not responseInfo!)
                String data = response.getResponseDataJson();
                String info = response.getResponseInfoJson();
                boolean success = (data != null && data.contains("SUCCESSFUL")) ||
                                  (info != null && info.contains("SUCCESSFUL"));
                return success;
            })
            .exceptionally(e -> {
                if (e.getCause() instanceof TimeoutException) {
                    log.error("Token authentication timeout");
                } else {
                    log.error("Token authentication error: {}", e.getMessage());
                }
                return false;
            });
}

private static class AuthTokenLoginRequest {
    private final String token;
    private final ClientInfoJson clientInfo;

    public AuthTokenLoginRequest(String token, ClientInfo info) {
        this.token = token;
        this.clientInfo = new ClientInfoJson(info);
    }
}
```

### Метод 2: API Sign-in (альтернативный)

#### URI эндпоинта

```
auth/signin
```

#### Формат requestDataJson

```json
{
  "login": "username",
  "password": "password",
  "clientInfo": {
    "uid": "cdf6ccc9-d5ff-428d-8074-b22711a0f79b",
    "build": 0,
    "version": [1, 233, 0],
    "platform": "Desktop",
    "systemLocale": "ru_MD",
    "preferredLocale": "ru",
    "clientHash": "269980fe6e943c59e8ff10338f719870"
  }
}
```

#### Код (AuthManager.java:251-305)

```java
public CompletableFuture<Boolean> loginWithApi(String login, String password) {
    log.info("Authenticating via API with {}...", login);

    apiResponseFuture = new CompletableFuture<>();

    // Build SignInRequest JSON
    SignInRequest signInRequest = new SignInRequest(login, password, clientInfo);
    String requestJson = gson.toJson(signInRequest);

    log.debug("Auth request JSON: {}", requestJson);

    // Build ApiRequestPacket
    int requestId = requestIdCounter.getAndIncrement();
    ApiRequestPacket apiRequest = new ApiRequestPacket(requestId, AUTH_SIGNIN_URI, requestJson);

    client.send(apiRequest);

    return apiResponseFuture
            .orTimeout(AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .thenApply(response -> {
                String data = response.getResponseDataJson();
                String info = response.getResponseInfoJson();
                log.debug("Auth response: info={}, data={}", info, data);

                boolean success = (data != null && data.contains("SUCCESSFUL")) ||
                                  (info != null && info.contains("SUCCESSFUL"));

                if (success) {
                    // Try to extract token from response
                    if (data != null && data.contains("ssid")) {
                        // Parse ssid from response
                        try {
                            int ssidStart = data.indexOf("\"ssid\":\"") + 8;
                            int ssidEnd = data.indexOf("\"", ssidStart);
                            if (ssidStart > 7 && ssidEnd > ssidStart) {
                                this.authToken = data.substring(ssidStart, ssidEnd);
                                this.sessionId = this.authToken;
                                log.info("Received auth token: {}", authToken);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse token from response: {}", e.getMessage());
                        }
                    }
                }
                return success;
            })
            .exceptionally(e -> {
                if (e.getCause() instanceof TimeoutException) {
                    log.error("API authentication timeout");
                } else {
                    log.error("API authentication error: {}", e.getMessage());
                }
                return false;
            });
}

private static class SignInRequest {
    private final String login;
    private final String password;
    private final ClientInfoJson clientInfo;

    public SignInRequest(String login, String password, ClientInfo info) {
        this.login = login;
        this.password = password;
        this.clientInfo = new ClientInfoJson(info);
    }
}
```

### Метод 3: SignUpRequestPacket (устаревший)

```java
package com.spaiowenta.commons.packets.auth;

public class SignUpRequestPacket {
    public String login;
    public String password;
}
```

### Метод 4: AuthRequestPacket (deprecated)

```java
package com.spaiowenta.commons.packets.auth;

@Deprecated
public class AuthRequestPacket {
    public String login;
    public String password;
    public String lang;
    public int[] clientV;
    public int platformId;
}
```

---

## 6. Шаг 5: Получение ответов

### ApiResponsePacket (для token-login и signin)

```java
package com.spaiowenta.commons.api;

public class ApiResponsePacket {
    private int requestId;
    private String uri;
    private String responseInfoJson;
    private String responseDataJson;
}
```

#### Пример успешного ответа

```
responseInfo = {"status":"DELAYED","message":""}
responseData = {"netStatus":"SUCCESSFUL"}
```

### AuthAnswerPacket (для устаревших методов)

```java
package com.spaiowenta.commons.packets.auth;

public class AuthAnswerPacket {
    public boolean success;
    public String ssid;
    public String errorMsg;
}
```

### SignUpResponsePacket

```java
package com.spaiowenta.commons.packets.auth;

public class SignUpResponsePacket {
    public boolean success;
    public String errorMsg;
}
```

### Код обработки ответов (AuthManager.java:67-135)

```java
private void handlePacket(Object packet) {
    if (packet instanceof AuthAnswerPacket authAnswer) {
        handleAuthAnswer(authAnswer);
    } else if (packet instanceof SignUpResponsePacket signUpResponse) {
        handleSignUpResponse(signUpResponse);
    } else if (packet instanceof ApiResponsePacket apiResponse) {
        handleApiResponse(apiResponse);
    } else if (packet instanceof MapConnectAnswerPacket mapAnswer) {
        handleMapConnectAnswer(mapAnswer);
    }
}

private void handleAuthAnswer(AuthAnswerPacket packet) {
    log.debug("Received AuthAnswerPacket: success={}", packet.success);

    if (packet.success) {
        this.sessionId = packet.ssid;
        this.authToken = packet.ssid; // Token is in ssid field
        this.authenticated = true;
        log.info("Authentication successful! Session ID: {}", sessionId);
    } else {
        log.error("Authentication failed: {}", packet.errorMsg);
    }

    if (authFuture != null && !authFuture.isDone()) {
        authFuture.complete(packet);
    }
}

private void handleApiResponse(ApiResponsePacket packet) {
    log.debug("Received ApiResponsePacket: uri={}, requestId={}", packet.getUri(), packet.getRequestId());

    if (AUTH_TOKEN_LOGIN_URI.equals(packet.getUri()) || AUTH_SIGNIN_URI.equals(packet.getUri())) {
        String responseInfo = packet.getResponseInfoJson();
        String responseData = packet.getResponseDataJson();

        log.debug("Auth API response: info={}, data={}", responseInfo, responseData);

        // Check if successful - netStatus is in responseData!
        boolean success = (responseData != null && responseData.contains("SUCCESSFUL")) ||
                          (responseInfo != null && responseInfo.contains("SUCCESSFUL"));

        if (success) {
            this.authenticated = true;
            log.info("Token authentication successful!");
        } else {
            log.error("Token authentication failed: info={}, data={}", responseInfo, responseData);
        }
    }

    if (apiResponseFuture != null && !apiResponseFuture.isDone()) {
        apiResponseFuture.complete(packet);
    }
}
```

---

## 7. Шаг 6: Вход на карту

### MapConnectRequestPacket

```java
package com.spaiowenta.commons.packets.auth;

public class MapConnectRequestPacket {
    public String ssid;
    public String lang;
}
```

### MapConnectAnswerPacket

```java
package com.spaiowenta.commons.packets.auth;

public class MapConnectAnswerPacket {
    public boolean success;
}
```

### Код подключения к карте (AuthManager.java:356-383)

```java
public CompletableFuture<Boolean> connectToMap(String language) {
    if (!authenticated || sessionId == null) {
        log.error("Cannot connect to map - not authenticated");
        return CompletableFuture.completedFuture(false);
    }

    log.info("Connecting to map...");

    mapConnectFuture = new CompletableFuture<>();

    MapConnectRequestPacket request = new MapConnectRequestPacket();
    request.ssid = sessionId;
    request.lang = language;

    client.send(request);

    return mapConnectFuture
            .orTimeout(AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .thenApply(response -> response.success)
            .exceptionally(e -> {
                if (e.getCause() instanceof TimeoutException) {
                    log.error("Map connection timeout");
                } else {
                    log.error("Map connection error: {}", e.getMessage());
                }
                return false;
            });
}
```

**ВАЖНО**: После token-based авторизации (auth/token-login) подключение к карте происходит автоматически, сервер сразу присылает `GameStateResponsePacket`.

---

## 8. Возможные ошибки

### HTTP авторизация

| Код | Описание |
|-----|----------|
| 200 | Успешно |
| 401 | Неверный логин/пароль |
| 403 | Аккаунт заблокирован |
| 429 | Слишком много запросов |
| 500 | Ошибка сервера |

### TCP авторизация

| Ошибка | Описание |
|--------|----------|
| `Connection timeout` | Сервер не отвечает |
| `Authentication timeout` | Сервер не прислал ответ на auth |
| `errorMsg: "Invalid credentials"` | Неверный токен |
| `errorMsg: "Account banned"` | Аккаунт заблокирован |

### ClientHash

```java
// clientHash = MD5 hash of WarUniverse.exe
// Version 1.233.0: "269980fe6e943c59e8ff10338f719870"
public static final String HASH_1_233_0 = "269980fe6e943c59e8ff10338f719870";
```

Если clientHash неверный, сервер может отклонить подключение.

---

## 9. Полный цикл авторизации

```
┌─────────────────────────────────────────────────────────────────┐
│                    ПОЛНЫЙ ЦИКЛ АВТОРИЗАЦИИ                       │
└─────────────────────────────────────────────────────────────────┘

1. ЗАГРУЗКА КОНФИГУРАЦИИ
   │
   ├── Читаем config.json
   ├── Читаем ~/.prefs/WarUniverse (опционально)
   └── Определяем: есть token или нужен логин/пароль

2. ПОДГОТОВКА ClientInfo
   │
   ├── uid: UUID устройства (из config или сгенерированный)
   ├── version: [1, 233, 0]
   ├── platform: "Desktop"
   ├── systemLocale: "ru_MD"
   ├── preferredLocale: "ru"
   └── clientHash: "269980fe6e943c59e8ff10338f719870"

3. ПОЛУЧЕНИЕ ТОКЕНА (если нет сохранённого)
   │
   ├── GET https://eu.api.waruniverse.space/auth-api/v4/get-token/login
   │     ?login=USERNAME&password=PASSWORD
   │
   └── Ответ: {"token":"UUID","tokenId":NUMBER}
             → fullToken = "NUMBER:UUID"

4. TCP ПОДКЛЮЧЕНИЕ
   │
   ├── Host: 162.19.232.126
   ├── Port: 43431
   ├── Protocol: KryoNet (TCP)
   └── Регистрация пакетов перед подключением

5. ОТПРАВКА АВТОРИЗАЦИИ
   │
   ├── ApiRequestPacket {
   │     requestId: 1,
   │     uri: "auth/token-login",
   │     requestDataJson: {
   │       "token": "30337147:4cce0299-...",
   │       "clientInfo": { ... }
   │     }
   │   }
   │
   └── Ожидание ApiResponsePacket

6. ПРОВЕРКА ОТВЕТА
   │
   ├── ApiResponsePacket {
   │     responseData: {"netStatus":"SUCCESSFUL"}
   │   }
   │
   └── authenticated = true

7. ОЖИДАНИЕ GAME STATE
   │
   └── GameStateResponsePacket (приходит автоматически)
       ├── Информация о игроке
       ├── Информация о карте
       ├── Список сущностей
       └── initialized = true

ГОТОВО К ИГРЕ!
```

### Последовательность в коде (WuBot.java:67-188)

```java
public void start() {
    log.info("=== WarUniverse Bot v{} ===", VERSION);

    // Get connection settings
    Map<String, Object> connection = (Map<String, Object>) config.get("connection");
    String host = (String) connection.get("serverHost");
    int port = ((Number) connection.get("serverPort")).intValue();

    // Check if token is available first
    String configToken = (String) connection.get("token");
    String savedToken = accountManager.getSessionToken();
    String tokenToUse = (configToken != null && !configToken.isEmpty()) ? configToken : savedToken;
    boolean useTokenAuth = tokenToUse != null && !tokenToUse.isEmpty();

    // Get credentials only if no token available
    String username = null;
    String password = null;
    if (!useTokenAuth) {
        String[] credentials = getCredentials(connection);
        if (credentials == null) {
            log.error("No credentials provided and no token available");
            return;
        }
        username = credentials[0];
        password = credentials[1];
    }

    // Setup ClientInfo with clientHash
    setupClientInfo();

    log.info("Connecting to {}:{}...", host, port);

    // Add packet handler BEFORE connecting
    client.addPacketHandler(this::handlePacket);

    // Connect to server
    if (!client.connect(host, port)) {
        log.error("Failed to connect to server");
        return;
    }

    // Authenticate
    try {
        boolean loginSuccess;

        if (useTokenAuth) {
            // Token-based auth
            log.info("Using token-based authentication (auth/token-login)");
            loginSuccess = authManager.loginWithToken(tokenToUse).get();
        } else {
            // STEP 1: Get token via HTTPS API
            log.info("Getting auth token via HTTPS API...");
            HttpAuthClient httpAuth = new HttpAuthClient(authManager.getClientInfo());
            String httpsToken = httpAuth.authenticate(username, password);

            if (httpsToken != null) {
                // STEP 2: Use token for TCP authentication
                log.info("Got token from HTTPS, logging in via TCP...");
                loginSuccess = authManager.loginWithToken(httpsToken).get();
            } else {
                // Fallback: Try TCP API auth/signin
                log.warn("HTTPS auth failed, trying TCP API auth/signin...");
                loginSuccess = authManager.loginWithApi(username, password).get();

                if (!loginSuccess) {
                    // Last resort: deprecated AuthRequestPacket
                    log.warn("auth/signin failed, trying deprecated AuthRequestPacket...");
                    loginSuccess = authManager.authenticate(username, password, "en").get();
                }
            }
        }

        if (!loginSuccess) {
            log.error("Login failed!");
            client.disconnect();
            return;
        }

        log.info("Login successful!");

        // Wait for GameStateResponsePacket
        log.info("Waiting for game state initialization...");
        Thread.sleep(2000);

        if (gameState.isInitialized()) {
            log.info("Game state initialized! Player ID: {}", gameState.getPlayer().getPlayerId());
        }

    } catch (Exception e) {
        log.error("Error during login: {}", e.getMessage(), e);
        client.disconnect();
    }
}
```

---

## ClientInfo JSON структура

```java
private static class ClientInfoJson {
    private final String uid;
    private final Integer build;
    private final int[] version;
    private final String platform;
    private final String systemLocale;
    private final String preferredLocale;
    private final String clientHash;

    public ClientInfoJson(ClientInfo info) {
        this.uid = info.getUid();
        this.build = info.getBuild();
        this.version = info.getVersion();
        this.platform = info.getPlatform();
        this.systemLocale = info.getSystemLocale() != null ?
                info.getSystemLocale().toString() : Locale.getDefault().toString();
        this.preferredLocale = info.getPreferredLocale() != null ?
                info.getPreferredLocale().getLanguage() : "en";
        this.clientHash = info.getClientHash();
    }
}
```

---

## Ссылки на исходные файлы

| Файл | Описание |
|------|----------|
| `WuBot.java` | Главный класс, точка входа, полный цикл авторизации |
| `HttpAuthClient.java` | HTTPS авторизация для получения токена |
| `AuthManager.java` | TCP авторизация, отправка пакетов |
| `WuClient.java` | KryoNet TCP клиент |
| `ClientInfo.java` | Информация о клиенте для авторизации |
| `ClientHashGenerator.java` | Генерация MD5 хеша клиента |
| `AccountManager.java` | Загрузка сохранённых аккаунтов |
| `PacketRegistry.java` | Регистрация всех пакетов Kryo |
| `ApiRequestPacket.java` | Пакет API запроса |
| `ApiResponsePacket.java` | Пакет API ответа |
| `AuthRequestPacket.java` | Устаревший пакет авторизации |
| `AuthAnswerPacket.java` | Ответ на авторизацию |
| `MapConnectRequestPacket.java` | Запрос подключения к карте |
| `MapConnectAnswerPacket.java` | Ответ на подключение к карте |
