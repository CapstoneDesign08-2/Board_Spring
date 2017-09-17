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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AppConfig.class)
@WebIntegrationTest(randomPort = true)
public class WriteTest {

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
        } else if(SystemUtils.IS_OS_LINUX) {
            ((DesiredCapabilities) caps).setCapability(
                    PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
                    path.getAbsolutePath() + "/src/test/resources/phantomjs-2.1.1-linux-x86_64/bin/phantomjs"
            );
        }

        driver = new PhantomJSDriver(caps);
    }

    @Test // postview 페이지로 넘어갔을때 입력한 값이 제대로 들어갔는가
    public void writePostViewTest() throws Exception {
        String query;
        try {
            String baseURL = "http://localhost:" + port + "/write";
            driver.get(baseURL);

            driver.findElement(By.name("nick")).sendKeys("NICK");
            driver.findElement(By.name("subject")).sendKeys("SUBJECT");
            driver.findElement(By.name("content")).sendKeys("CONTENT");
            driver.findElement(By.tagName("form")).submit();

            WebElement td = driver.findElement(By.className("postViewId"));
            assertEquals("$글 번호가 제대로 넘어가지 않았습니다.$", "1", td.getText());
            td = driver.findElement(By.className("postViewNick"));
            assertEquals("$닉네임이 제대로 넘어가지 않았습니다.$", "NICK", td.getText());
            td = driver.findElement(By.className("postViewHit"));
            assertEquals("$조회수의 초기 값을 1로 해주세요.$", "1", td.getText());
            td = driver.findElement(By.className("postViewSubject"));
            assertEquals("$글 제목이 제대로 넘어가지 않았습니다.$", "SUBJECT", td.getText());
            td = driver.findElement(By.className("postViewContent"));
            assertEquals("$글 내용이 제대로 넘어가지 않았습니다.$", "CONTENT", td.getText());
        }
        catch (NoSuchElementException e) {
            throw new NoSuchElementException("$html이 제대로 호출되지 않았습니다.$");
        }
        finally {
            query = "TRUNCATE TABLE post;";
            stmt.executeUpdate(query);
        }
    }

    @Test // 작성중 뒤로가기 버튼이 제대로 작동하는가
    public void writeBackTest() throws Exception {
        try {
            String baseURL = "http://localhost:" + port + "/write";
            driver.get(baseURL);

            driver.findElement(By.className("back")).click();

            assertEquals("$주소가 제대로 호출되지 않았습니다.$", "http://localhost:" + port + "/", driver.getCurrentUrl());
        }
        catch (NoSuchElementException e) {
            throw new NoSuchElementException("$html이 제대로 호출되지 않았습니다.$");
        }
    }

    @Test //write에서 post객체를 생성하면 date가 오늘날짜로 설정되는가
    public void writeDateTest() throws Exception {
        String query;
        try {
            String baseURL = "http://localhost:" + port + "/write";
            driver.get(baseURL);

            driver.findElement(By.name("nick")).sendKeys("NICK");
            driver.findElement(By.name("subject")).sendKeys("SUBJECT");
            driver.findElement(By.name("content")).sendKeys("CONTENT");
            driver.findElement(By.tagName("form")).submit();

            Date d = new Date();
            SimpleDateFormat today = new SimpleDateFormat("yyyy/MM/dd");

            WebElement td = driver.findElement(By.className("postViewDate"));
            assertEquals("$등록 날짜가 다릅니다.$", today.format(d), td.getText());
        }
        catch (NoSuchElementException e) {
            throw new NoSuchElementException("$html이 제대로 호출되지 않았습니다.$");
        }
        finally {
            query = "TRUNCATE TABLE post;";
            stmt.executeUpdate(query);
        }
    }

    @Test       // 값을 모두 입력하면 해당객체의 postview페이지로 넘어가는가
    public void writePostTest() throws Exception {
        String query;
        try {
            String baseURL = "http://localhost:" + port + "/write";
            driver.get(baseURL);

            driver.findElement(By.name("nick")).sendKeys("NICK");
            driver.findElement(By.name("subject")).sendKeys("SUBJECT");
            driver.findElement(By.name("content")).sendKeys("CONTENT");
            driver.findElement(By.tagName("form")).submit();

            assertEquals("$주소가 제대로 호출되지 않았습니다.$", "http://localhost:" + port + "/postview/1", driver.getCurrentUrl());
        }
        catch (NoSuchElementException e) {
            throw new NoSuchElementException("$html이 제대로 호출되지 않았습니다.$");
        }
        finally {
            query = "TRUNCATE TABLE post;";
            stmt.executeUpdate(query);
        }
    }

    @Test // 닉네임에 공백이 들어간채로 등록하면 Error 페이지로 제대로 이동하는가
    public void writeNickExceptionTest() throws Exception {
        String query;
        try {
            String baseURL = "http://localhost:" + port + "/write";
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

    @Test // 제목에 공백이 들어간채로 등록하면 Error 페이지로 제대로 이동하는가
    public void writeSubjectExceptionTest() throws Exception {
        String query;
        try {
            String baseURL = "http://localhost:" + port + "/write";
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