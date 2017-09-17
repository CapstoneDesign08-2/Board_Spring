package com.board;

import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.*;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AppConfig.class)
@WebIntegrationTest(randomPort = true)
public class ModifyTest {

    @Value("${local.server.port}")
    private int port;

    static WebDriver driver;

    static Properties pro;
    static String connectionURL;
    static String username;
    static String password;

    static Connection conn;
    static Statement stmt;

    @Before
    public void setUp() throws Exception {
        File path = new File("");
        System.out.println(path.getAbsolutePath());

        pro = new Properties();
        pro.load(new FileInputStream(path.getAbsolutePath() + "/src/main/resources/application.properties"));
        connectionURL = pro.getProperty("spring.datasource.url");
        username = pro.getProperty("spring.datasource.username");
        password = pro.getProperty("spring.datasource.password");

        try {
            conn = DriverManager.getConnection(connectionURL, username, password);
            stmt = conn.createStatement();
        }
        catch (SQLException e) {
            throw new SQLException("$DB가 연결 되지 않았습니다.$");
        }

        Capabilities caps = new DesiredCapabilities();
        ((DesiredCapabilities) caps).setJavascriptEnabled(true);
        ((DesiredCapabilities) caps).setCapability("takesScreenshot", true);
        if (SystemUtils.IS_OS_WINDOWS) {
            ((DesiredCapabilities) caps).setCapability(
                    PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
                    path.getAbsolutePath() + "/src/test/resources/phantomjs-2.1.1-windows/bin/phantomjs.exe"
            );
        } else if (SystemUtils.IS_OS_LINUX) {
            ((DesiredCapabilities) caps).setCapability(
                    PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
                    path.getAbsolutePath() + "/src/test/resources/phantomjs-2.1.1-linux-x86_64/bin/phantomjs"
            );
        }

        driver = new PhantomJSDriver(caps);
    }

    @Test // modify 페이지에서 default 값이 제대로 들어가있는가 (변경하기 전의 값)
    public void modifyDefaultTest() throws Exception {
        String query;
        try {
            query = "Insert Into post(id, nick ,subject, content, date, hit) VALUES (2, 'TEST', 'TESTSUBJECT', 'TESTCONTENT', '2017/01/16', 2);";
            stmt.executeUpdate(query);

            String baseURL = "http://localhost:" + port + "/postview/modify/2";
            driver.get(baseURL);

            WebElement td = driver.findElement(By.name("nick"));
            assertEquals("$닉네임 default 값이 일치하지 않습니다.$", "TEST", td.getAttribute("value"));
            td = driver.findElement(By.name("subject"));
            assertEquals("$글 제목 default 값이 일치하지 않습니다.$", "TESTSUBJECT", td.getAttribute("value"));
            td = driver.findElement(By.name("content"));
            assertEquals("$글 내용 default 값이 일치하지 않습니다.$", "TESTCONTENT", td.getAttribute("value"));
        }
        catch (NoSuchElementException e) {
            throw new NoSuchElementException("$html이 제대로 호출되지 않았습니다.$");
        }
        finally {
            query = "TRUNCATE TABLE post;";
            stmt.executeUpdate(query);
        }
    }

    @Test // 수정한 값대로 제대로 postview에 보여지는가
    public void modifyPostViewTest() throws Exception {
        String query;
        try {
            query = "Insert Into post(id, nick ,subject, content, date, hit) VALUES (2, 'TEST', 'TESTSUBJECT', 'TESTCONTENT', '2017/01/16', 2);";
            stmt.executeUpdate(query);

            String baseURL = "http://localhost:" + port + "/postview/modify/2";
            driver.get(baseURL);

            driver.findElement(By.name("nick")).clear();
            driver.findElement(By.name("subject")).clear();
            driver.findElement(By.name("content")).clear();

            driver.findElement(By.name("nick")).sendKeys("MODIFY_NICK");
            driver.findElement(By.name("subject")).sendKeys("MODIFY_SUBJECT");
            driver.findElement(By.name("content")).sendKeys("MODIFY_CONTENT");
            driver.findElement(By.tagName("form")).submit();

            WebElement td = driver.findElement(By.className("postViewId"));
            assertEquals("$수정된 글 번호가 일치하지 않습니다.$", "2", td.getText());
            td = driver.findElement(By.className("postViewNick"));
            assertEquals("$수정된 닉네임이 일치하지 않습니다.$", "MODIFY_NICK", td.getText());
            td = driver.findElement(By.className("postViewSubject"));
            assertEquals("$수정된 글 제목이 일치하지 않습니다.$", "MODIFY_SUBJECT", td.getText());
            td = driver.findElement(By.className("postViewContent"));
            assertEquals("$수정된 글 내용이 일치하지 않습니다.$", "MODIFY_CONTENT", td.getText());
        }
        catch (NoSuchElementException e) {
            throw new NoSuchElementException("$html이 제대로 호출되지 않았습니다.$");
        }
        finally {
            query = "TRUNCATE TABLE post;";
            stmt.executeUpdate(query);
        }
    }

    @Test // 뒤로가기버튼이 제대로 작동하는가
    public void modifyBackTest() throws Exception {
        String query;
        try {
            query = "Insert Into post(id, nick ,subject, content, date, hit) VALUES (2, 'TEST', 'TESTSUBJECT', 'TESTCONTENT', '2017/01/16', 2);";
            stmt.executeUpdate(query);

            String baseURL = "http://localhost:" + port + "/postview/modify/2";
            driver.get(baseURL);

            driver.findElement(By.className("back")).click();

            assertEquals("$주소가 제대로 호출되지 않았습니다.$", "http://localhost:" + port + "/postview/2", driver.getCurrentUrl());
        }
        catch (NoSuchElementException e) {
            throw new NoSuchElementException("$html이 제대로 호출되지 않았습니다.$");
        }
        finally {
            query = "TRUNCATE TABLE post;";
            stmt.executeUpdate(query);
        }
    }

    @Test // 닉네임에 공백이 들어간채로 등록하면 ErrorPage로 제대로 이동하는가
    public void modifyNickExceptionTest() throws Exception {
        String query;
        try {
            query = "Insert Into post(id, nick ,subject, content, date, hit) VALUES (2, 'TEST', 'TESTSUBJECT', 'TESTCONTENT', '2017/01/16', 2);";
            stmt.executeUpdate(query);

            String baseURL = "http://localhost:" + port + "/postview/modify/2";
            driver.get(baseURL);

            driver.findElement(By.name("nick")).clear();
            driver.findElement(By.tagName("form")).submit();

            assertEquals("$에러페이지가 제대로 호출되지 않았습니다.$", "Error", driver.getTitle());
        }
        catch (NoSuchElementException e) {
            throw new NoSuchElementException("$html이 제대로 호출되지 않았습니다.$");
        }
        finally {
            query = "TRUNCATE TABLE post;";
            stmt.executeUpdate(query);
        }
    }

    @Test // 제목에 공백이 들어간채로 등록하면 ErrorPage로 제대로 이동하는가
    public void modifySubjectExceptionTest() throws Exception {
        String query;
        try {
            query = "Insert Into post(id, nick ,subject, content, date, hit) VALUES (2, 'TEST', 'TESTSUBJECT', 'TESTCONTENT', '2017/01/16', 2);";
            stmt.executeUpdate(query);

            String baseURL = "http://localhost:" + port + "/postview/modify/2";
            driver.get(baseURL);

            driver.findElement(By.name("subject")).clear();
            driver.findElement(By.tagName("form")).submit();

            assertEquals("$에러페이지가 제대로 호출되지 않았습니다.$", "Error", driver.getTitle());
        }
        catch (NoSuchElementException e) {
            throw new NoSuchElementException("$html이 제대로 호출되지 않았습니다.$");
        }
        finally {
            query = "TRUNCATE TABLE post;";
            stmt.executeUpdate(query);
        }
    }

    @After
    public void tearDown() {
        driver.quit();
    }
}
