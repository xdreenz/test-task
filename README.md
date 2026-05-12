# Инструкция по запуску `Е2Е-теста`

Тест полностью самодостаточный: WireMock стартует на случайном порту внутри JVM, HtmlUnitDriver работает in-process. Реальный сервер, Chrome и chromedriver не нужны. UserApiClient был доработан для сохранения последнего запроса в артефакты CI, а BaseUiTest - для сохранения скриншота при падении теста.

## Запуск из командной строки

### Все тесты

```bash
mvn test
```

### Только сценарий

```bash
mvn test -Dtest=UserLifecycleE2ETest
```

### Только один метод

```bash
mvn test -Dtest=UserLifecycleE2ETest#createdUserAppearsInAdminUi_andDisappearsAfterDeletion
```

### С подробным выводом

```bash
mvn test -Dtest=UserLifecycleE2ETest -X
```

## Запуск из IDE

### IntelliJ IDEA

1.  Открыть `UserLifecycleE2ETest.java`.
2.  Кликнуть на зелёный треугольник слева от имени класса или метода - **Run**.
3.  Если в Run Configuration спросит JDK - выбрать 17+.

### Eclipse / VS Code

Right-click на файле - **Run As - JUnit Test**.

* * *

&nbsp;

## У задания 4 выполнил вариант A. Решение находится в файле run-tests.yml.