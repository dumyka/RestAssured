package tests;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import listener.AdminUser;
import listener.AdminUserResolver;
import listener.CustomTpl;
import models.FullUser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import services.UserService;

import java.util.List;

import static assertions.Conditions.hasMessage;
import static assertions.Conditions.hasStatusCode;
import static utils.RandomTestData.*;

@ExtendWith(AdminUserResolver.class)
public class UserNewTests {
    private FullUser user;
    private static UserService userService;

    @BeforeEach
    public void initTestUser() {
        user = getRandomUser();
    }

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = "http://85.192.34.140:8080/api/";
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter(),
                CustomTpl.customLogFilter().withCustomTemplates()); //логируем
        userService = new UserService();
    }

    @Test
    @DisplayName("Регистрация нового пользователя")
    public void positiveRegisterTest() {
        userService.register(user)
                .should(hasStatusCode(201))
                .should(hasMessage("User created"));
    }

    @Test
    @DisplayName("Регистрация нового пользователя с играми")
    public void positiveRegisterWithGamesTest() {
        FullUser user = getRandomUserWithGames();
        userService.register(user)
                .should(hasStatusCode(201))
                .should(hasMessage("User created"));

    }

    @Test
    @DisplayName("Регистрация пользователя с существующим логином")
    public void negativeRegisterLoginExistsTest() {
        userService.register(user);
        userService.register(user)
                .should(hasStatusCode(400))
                .should(hasMessage("Login already exist"));

    }

    @Test
    @DisplayName("Регистрация пользователя без пароля")
    public void negativeRegisterNoPasswordTest() {
        user.setPass(null);
        userService.register(user)
                .should(hasStatusCode(400))
                .should(hasMessage("Missing login or password"));

    }

    @Test
    @DisplayName("Авторизация администратора")
    public void positiveAdminAuthTest(@AdminUser FullUser admin) {

       String token = userService.auth(admin)
               .should(hasStatusCode(200))
               .asJwt();

        Assertions.assertNotNull(token);
    }

    @Test
    @DisplayName("Авторизация нового пользователя")
    public void positiveNewUserAuthTest() {
        userService.register(user);
        String token = userService.auth(user)
                .should(hasStatusCode(200)).asJwt();

        Assertions.assertNotNull(token);
    }

    @Test
    @DisplayName("Авторизация с неверными учетными данными")
    public void negativeAuthTest() {
      userService.auth(user).should(hasStatusCode(401));
    }

    @Test
    @DisplayName("Получение информации о пользователе")
    public void positiveGetUserInfoTest() {
        FullUser user = getAdminUser();
        String token = userService.auth(user).asJwt();
        userService.getUserInfo(token)
                .should(hasStatusCode(200));
    }

    @Test
    @DisplayName("Получение информации о пользователе с недействительным JWT")
    public void negativeGetUserInfoInvalidJwtTest() {
        userService.getUserInfo("fake jwt").should(hasStatusCode(401));
    }

    @Test
    @DisplayName("Получение информации о пользователе без токена")
    public void negativeGetUserInfoWithoutJwtTest() {
        userService.getUserInfo("").should(hasStatusCode(401));
    }

    @Test
    @DisplayName("Изменение пароля пользователя")
    public void positiveChangeUserPassTest() {
        String oldPassword = user.getPass();

        userService.register(user).
                should(hasStatusCode(201)).
                should(hasMessage("User created"));

        String token = userService.auth(user).should(hasStatusCode(200)).asJwt();
        String updatedPassValue = "newUpdatePass";

        userService.updatePass(updatedPassValue, token).
                should(hasStatusCode(200)).
                should(hasMessage("User password successfully changed"));

        user.setPass(updatedPassValue);

        token = userService.auth(user).should(hasStatusCode(200)).asJwt();

        FullUser updatedUser = userService.getUserInfo(token).as(FullUser.class);

        Assertions.assertNotEquals(oldPassword, updatedUser.getPass());

    }

    @Test
    @DisplayName("Попытка сменить пароль администратора")
    public void negativeChangeAdminPasswordTest() {
        FullUser admin = getAdminUser();

        String token = userService.auth(admin).asJwt();

        String updatedPassValue = "newPassUpdated";
        userService.updatePass(updatedPassValue, token)
                .should(hasStatusCode(400))
                .should(hasMessage("Cant update base users"));
    }

    @Test
    @DisplayName("Попытка удалить администратора")
    public void negativeDeleteAdminTest() {
        FullUser admin = getAdminUser();

        String token = userService.auth(admin).asJwt();
        userService.deleteUser(token)
                .should(hasStatusCode(400))
                .should(hasMessage("Cant delete base users"));
    }

    @Test
    @DisplayName("Удаление нового пользователя")
    public void positiveDeleteNewUserTest() {
        String token = userService.auth(user).asJwt();
        userService.deleteUser(token)
                .should(hasStatusCode(200))
                .should(hasMessage("User successfully deleted"));
    }

    @Test
    @DisplayName("Получение списка всех пользователей")
    public void positiveGetAllUsersTest() {
        List<String> users = userService.getAllUsers().asList(String.class);
        Assertions.assertTrue(users.size() >= 3);
    }
}
