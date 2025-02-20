package tests;

import assertions.AssertableResponse;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.filter.log.*;
import io.restassured.http.ContentType;
import listener.CustomTpl;
import models.FullUser;
import models.Info;
import models.JwtAuthData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static assertions.Conditions.hasMessage;
import static assertions.Conditions.hasStatusCode;
import static io.restassured.RestAssured.given;

public class UserApiTests {
    private static Random random;

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = "http://85.192.34.140:8080/";
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter(),
                CustomTpl.customLogFilter().withCustomTemplates()); //логируем
        random = new Random();
    }

    @Test
    @DisplayName("Регистрация нового пользователя")
    public void positiveRegisterTest(){
        int randomNumber = Math.abs(random.nextInt());
        FullUser user = FullUser.builder()
                .login("dumyka" + randomNumber)
                .pass("threadqa")
                .build();

        Info info = given()
                .contentType(ContentType.JSON)
                .body(user)
                .post("/api/signup")
                .then().statusCode(201)
                .extract().jsonPath().getObject("info", Info.class);

        Assertions.assertEquals("User created", info.getMessage());
    }

    @Test
    @DisplayName("Регистрация пользователя с существующим логином")
    public void negativeRegisterLoginExistsTest() {
        int randomNumber = Math.abs(random.nextInt());

        FullUser user = FullUser.builder()
                .login("dumyka" + randomNumber)
                .pass("threadqa")
                .build();

        Info info = given().contentType(ContentType.JSON)
                .body(user)
                .post("/api/signup")
                .then().statusCode(201)
                .extract().jsonPath().getObject("info", Info.class);

        Assertions.assertEquals("User created", info.getMessage());

        new AssertableResponse(given().contentType(ContentType.JSON)
                .body(user)
                .post("/api/signup")
                .then())
                .should(hasMessage("Login already exist"))
                .should(hasStatusCode(400));

    }

    @Test
    @DisplayName("Регистрация пользователя без пароля")
    public void negativeRegisterNoPasswordTest() {
        int randomNumber = Math.abs(random.nextInt());
        FullUser user = FullUser.builder()
                .login("threadQATestUser" + randomNumber)
                .build();

        Info info = given().contentType(ContentType.JSON)
                .body(user)
                .post("/api/signup")
                .then().statusCode(400)
                .extract().jsonPath().getObject("info", Info.class);

        Assertions.assertEquals("Missing login or password", info.getMessage());
    }

    @Test
    @DisplayName("Авторизация администратора")
    public void positiveAdminAuthTest() {
        JwtAuthData authData = new JwtAuthData("admin", "admin");

        String token = given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("token");

        Assertions.assertNotNull(token);
    }

    @Test
    @DisplayName("Авторизация нового пользователя")
    public void positiveNewUserAuthTest() {
        int randomNumber = Math.abs(random.nextInt());
        FullUser user = FullUser.builder()
                .login("loginTestUser" + randomNumber)
                .pass("passwordCOOL")
                .build();

        Info info = given().contentType(ContentType.JSON)
                .body(user)
                .post("/api/signup")
                .then().statusCode(201)
                .extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("User created", info.getMessage());

        JwtAuthData authData = new JwtAuthData(user.getLogin(), user.getPass());

        String token = given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("token");

        Assertions.assertNotNull(token);
    }

    @Test
    @DisplayName("Авторизация с неверными учетными данными")
    public void negativeAuthTest() {
        JwtAuthData authData = new JwtAuthData("asodn9121", "asd129j9zx");

        given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("Получение информации о пользователе")
    public void positiveGetUserInfoTest() {
        JwtAuthData authData = new JwtAuthData("admin", "admin");

        String token = given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("token");

        Assertions.assertNotNull(token);

        given().auth().oauth2(token)
                .get("/api/user")
                .then().statusCode(200);
    }

    @Test
    @DisplayName("Получение информации о пользователе с недействительным JWT")
    public void negativeGetUserInfoInvalidJwtTest() {
        given().auth().oauth2("some values")
                .get("/api/user")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("Получение информации о пользователе без токена")
    public void negativeGetUserInfoWithoutJwtTest() {
        given()
                .get("/api/user")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("Изменение пароля пользователя")
    public void positiveChangeUserPassTest() {
        int randomNumber = Math.abs(random.nextInt());
        FullUser user = FullUser.builder()
                .login("threadQATestUser" + randomNumber)
                .pass("passwordCOOL")
                .build();

        Info info = given().contentType(ContentType.JSON)
                .body(user)
                .post("/api/signup")
                .then().statusCode(201)
                .extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("User created", info.getMessage());

        JwtAuthData authData = new JwtAuthData(user.getLogin(), user.getPass());

        String token = given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("token");

        Map<String, String> password = new HashMap<>();
        String updatedPassValue = "newpassUpdated";
        password.put("password", updatedPassValue);

        Info updatePassInfo = given().contentType(ContentType.JSON)
                .auth().oauth2(token)
                .body(password)
                .put("/api/user")
                .then().extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("User password successfully changed", updatePassInfo.getMessage());

        authData.setPassword(updatedPassValue);
        token = given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("token");

        FullUser updatedUser = given().auth().oauth2(token)
                .get("/api/user")
                .then().statusCode(200)
                .extract().as(FullUser.class);
        Assertions.assertNotEquals(user.getPass(), updatedUser.getPass());
    }

    @Test
    @DisplayName("Попытка сменить пароль администратора")
    public void negativeChangeAdminPasswordTest() {
        JwtAuthData authData = new JwtAuthData("admin", "admin");

        String token = given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("token");

        Map<String, String> password = new HashMap<>();
        String updatedPassValue = "newpassUpdated";
        password.put("password", updatedPassValue);

        Info updatePassInfo = given().contentType(ContentType.JSON)
                .auth().oauth2(token)
                .body(password)
                .put("/api/user")
                .then().statusCode(400)
                .extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("Cant update base users", updatePassInfo.getMessage());
    }

    @Test
    @DisplayName("Попытка удалить администратора")
    public void negativeDeleteAdminTest() {
        JwtAuthData authData = new JwtAuthData("admin", "admin");

        String token = given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("token");

        Info info = given().auth().oauth2(token)
                .delete("/api/user")
                .then().statusCode(400)
                .extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("Cant delete base users", info.getMessage());
    }

    @Test
    @DisplayName("Удаление нового пользователя")
    public void positiveDeleteNewUserTest() {
        int randomNumber = Math.abs(random.nextInt());
        FullUser user = FullUser.builder()
                .login("threadQATestUser" + randomNumber)
                .pass("passwordCOOL")
                .build();

        Info info = given().contentType(ContentType.JSON)
                .body(user)
                .post("/api/signup")
                .then().statusCode(201)
                .extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("User created", info.getMessage());

        JwtAuthData authData = new JwtAuthData(user.getLogin(), user.getPass());

        String token = given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("token");

        Info infoDelete = given().auth().oauth2(token)
                .delete("/api/user")
                .then().statusCode(200)
                .extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("User successfully deleted", infoDelete.getMessage());
    }

    @Test
    @DisplayName("Получение списка всех пользователей")
    public void positiveGetAllUsersTest() {
        List<String> users = given()
                .get("/api/users")
                .then()
                .statusCode(200)
                .extract().as(new TypeRef<List<String>>() {});
        Assertions.assertTrue(users.size() >= 3);
    }


}
