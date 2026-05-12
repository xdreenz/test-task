# Code Review: UserApiClient и BaseTest

## UserApiClient

### 1\. Поле timeout объявлено, но не используется

Это мёртвый код, который надо удалить. Кто-то может подумать, что таймаут реально работает.

### 2\. Хардкод версии API

`/api/v1/users` зашито в трёх местах. При смене версии можно будет забыть где-то её исправить.

### 3\. Дублирование `given().baseUri(...)`

Спецификация в каждом методе строится руками. Это засоряет код и мешает централизованно добавить логирование, аутентификацию, заголовки и т.д. через `RequestSpecification`.

### 4\. Синглтон с параметром

`baseUrl` используется только при первом вызове. Все последующие вызовы с другим `baseUrl` вернут инстанс со старым адресом. И как только понадобятся параллельные тесты в разных окружениях (dev/stage), либо интеграция с моком на другом порту, то клиент будет стучаться по неправильному адресу, и тесты упадут.

### 5\. Нет проверки статус-кодов

Все методы возвращают `Response` или void без какой-либо валидации.

### 6\. Небезопасное использование параметра `userId` в методе `deleteUser`

- `userId` там подставляется склейкой строк вместо использования параметра пути, как в методе `getUserById`. Если в `userId` попадёт пробел или спецсимвол - получится невалидный URL.

* * *

## BaseTest

### 1\. Хардкод пути к chromedriver

```java
System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");
```

Не запустится на другой машине. К тому же Selenium 4 сам умеет скачивать драйвер, так что эту строку вообще надо удалить.

### 2\. Хардкод baseUrl `http://localhost:8080`

Не запустится на другой машине или в CI/CD с другим путём. Такие вещи должны читаться из конфига/переменных окружения/system property.

### 3\. `driver.quit()` в tearDown без null-check

Если `setUp` упал на этапе создания драйвера, то `driver` будет null, и tearDown выбросит NullPointerException, замаскировав исходную ошибку.

### 4\. setUp создаёт WebDriver всегда, даже для чистых API-тестов

Если унаследовать `BaseTest` для чистых API-тестов, то Chrome будет запускаться впустую, что замедляет процесс и тратит ресурсы.

### 5\. Конфликт `@ExtendWith(SeleniumExtension.class)` и ручного создания драйвера

Если `SeleniumExtension` инжектит драйвер, то его ручное создание в `@BeforeEach` либо дублирует код, либо конфликтует с extension’ом. Надо выбрать что-то одно.

### 6\. implicitlyWait(10с) - антипаттерн

Если потом будут использоваться явные ожидания, то поведение тестов станет неопределённым. Поэтому документация Selenium прямо запрещает смешивать явные и неявные ожидания.

### 7\. Утечка состояния между тестами

`apiClient` - синглтон без подчистки данных. Если один тест создал пользователя и упал до удаления, то следующий тест будет работать с загрязнённой БД.

* * *

## На обсуждение с командой

- **Главный вопрос: должен ли `UserApiClient` вообще быть синглтоном, и должен ли `BaseTest` быть один и на UI, и на API?**  
    Я бы это вынес на обсуждение, потому что это архитектурное решение. Если команда планирует параллельный запуск, мультиокружение или моки, то синглтон надо убрать. Если все тесты крутятся в один поток и на одном стенде, то текущий подход допустим. Это зависит от стратегии тестирования, которую я в одиночку определять не должен.
    
- Один `BaseTest` или иерархия (`BaseApiTest` / `BaseUiTest` / `BaseE2ETest`)?  
    Сейчас классы смешаны, осознанный ли это выбор? Разделение повлияет на скорость CI и шаблоны написания тестов, так что это командный вопрос.
    
- Где живёт конфиг - в properties, в переменных окружения или в отдельном провайдере?  
    Также решать не мне, а команде.
    

* * *

## Предложения по рефакторингу

### UserApiClient

```java
public class UserApiClient {
    private final RequestSpecification spec;

    public UserApiClient(ApiConfig config) {
        this.spec = new RequestSpecBuilder()
            .setBaseUri(config.baseUrl())
            .setContentType(ContentType.JSON)
            .addFilter(new AllureRestAssured())
            .setConfig(RestAssured.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", config.timeoutMs())
                    .setParam("http.socket.timeout", config.timeoutMs())))
            .build();
    }

    public Response createUser(CreateUserRequest payload) {
        return given(spec).body(payload).post(Endpoints.USERS);
    }

    public Response getUserById(String userId) {
        return given(spec).pathParam("id", userId).get(Endpoints.USER_BY_ID);
    }

    public Response deleteUser(String userId) {
        return given(spec).pathParam("id", userId).delete(Endpoints.USER_BY_ID);
    }
}
```

Что изменил:

- Убрал синглтон, клиент создаётся через фабрику, можно иметь несколько.
- `RequestSpecification` собран один раз и переиспользуется.
- DTO вместо `Map`.
- Эндпоинты вынесены в константы.
- `deleteUser` возвращает `Response`, вызывающий код может проверить статус.
- Таймаут реально применяется.

### BaseTest

Разделил бы на два:

```java
public abstract class BaseApiTest {
    protected UserApiClient apiClient;

    @BeforeEach
    void setUpApi() {
        apiClient = new UserApiClient(ConfigProvider.api());
    }
}

public abstract class BaseUiTest {
    protected WebDriver driver;

    @BeforeEach
    void setUpUi() {
        driver = WebDriverFactory.create(ConfigProvider.ui());
    }

    @AfterEach
    void tearDownUi() {
        if (driver != null) driver.quit();
    }
}
```