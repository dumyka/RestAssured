package tests;

import io.qameta.allure.Attachment;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import listener.CustomTpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import services.FileService;

import java.io.File;

import static assertions.Conditions.hasMessage;
import static assertions.Conditions.hasStatusCode;

public class FileTestOld {
    private static FileService fileService;

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = "http://85.192.34.140:8080/api";
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter(),
                CustomTpl.customLogFilter().withCustomTemplates());
        fileService = new FileService();
    }

    @Attachment(value = "downloaded", type = "image/png")
    private byte[] attachFile(byte[] bytes) {
        return bytes;
    }

    @Test
    @DisplayName("Загрузка файла и проверка размера")
    public void positiveDownloadTest() {
        byte[] file = fileService.downloadBaseImage().asResponse().asByteArray();
        attachFile(file);
        File expectedFile = new File("src/test/resources/threadqa.jpeg");
        Assertions.assertEquals(expectedFile.length(), file.length);
    }

    @Test
    @DisplayName("Загрузка  файла и валидация статуса")
    public void positiveUploadTest() {
        File expectedFile = new File("src/test/resources/threadqa.jpeg");
        fileService.uploadFile(expectedFile)
                .should(hasStatusCode(200))
                .should(hasMessage("file uploaded to server"));

        byte[] actualFile = fileService.downloadLastFile().asResponse().asByteArray();
        Assertions.assertTrue(actualFile.length != 0);
        Assertions.assertEquals(expectedFile.length(), actualFile.length);
    }
}
