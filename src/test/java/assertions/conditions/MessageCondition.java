package assertions.conditions;


import assertions.Condition;
import io.restassured.response.ValidatableResponse;
import lombok.RequiredArgsConstructor;
import models.Info;
import org.junit.jupiter.api.Assertions;

@RequiredArgsConstructor
public class MessageCondition implements Condition {
    private final String expectedMessage;
    @Override
    public void check(ValidatableResponse response) {
        Info info = response.extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals(expectedMessage, info.getMessage());
    }
}
