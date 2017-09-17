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
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AppConfig.class)
@WebIntegrationTest(randomPort = true)
public class HomeTest {

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

    @Test // Write 페이지로 이동할수 있는가
    public void moveToWriteTest() throws Exception {
        try {
            String baseURL = "http://localhost:" + port;
            driver.get(baseURL);

            driver.findElement(By.className("writeBtn")).click();

            assertEquals("$주소가 제대로 호출되지 않았습니다.$", "http://localhost:" + port + "/write", driver.getCurrentUrl());
        }
        catch (NoSuchElementException e) {
            throw new NoSuchElementException("$html이 제대로 호출되지 않았습니다.$");
        }
    }

    @Test // PostView 페이지로 이동할 수 있는가
    public void moveToPostViewTest() throws Exception {
        String query;
        try {
            query = "Insert Into post(id, nick ,subject, content, date, hit) VALUES (1, 'TEST1', 'TESTSUBJECT1', 'TESTCONTENT1', '2017/01/16', 1);";
            stmt.executeUpdate(query);

            String baseURL = "http://localhost:" + port;
            driver.get(baseURL);

            driver.findElement(By.className("subjectBtn")).click();

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

    @Test // post의 객체의 id, 제목, 닉네임, 해당조회수가 내림차순으로 적용되었나
    public void homePostTest() throws Exception {
        String query;
        try {
            query = "Insert Into post(id, nick ,subject, content, date, hit) VALUES (1, 'TEST1', 'TESTSUBJECT1', 'TESTCONTENT1', '2017/01/16', 10);";
            stmt.executeUpdate(query);
            query = "Insert Into post(id, nick ,subject, content, date, hit) VALUES (3, 'TEST3', 'TESTSUBJECT3', 'TESTCONTENT3', '2017/01/18', 30);";
            stmt.executeUpdate(query);
            query = "Insert Into post(id, nick ,subject, content, date, hit) VALUES (2, 'TEST2', 'TESTSUBJECT2', 'TESTCONTENT2', '2017/01/17', 20);";
            stmt.executeUpdate(query);

            String baseURL = "http://localhost:" + port;
            driver.get(baseURL);

            List<WebElement> div = driver.findElements(By.className("postList"));
            assertEquals("", 3, div.size());
            WebElement td = driver.findElement(By.className("homeId"));
            assertEquals("", "3", td.getText());
            td = driver.findElement(By.className("homeSubject"));
            assertEquals("$글 제목이 일치하지 않습니다.$", "TESTSUBJECT3", td.getText());
            td = driver.findElement(By.className("homeNick"));
            assertEquals("$글쓴이가 일치하지 않습니다.$", "TEST3", td.getText());
            td = driver.findElement(By.className("homeDate"));
            assertEquals("$날짜가 일치하지 않습니다.$", "2017/01/18", td.getText());
            td = driver.findElement(By.className("homeHit"));
            assertEquals("$조회수가 일치하지 않습니다.$", "30", td.getText());
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
