package com.board;


import org.apache.commons.lang3.SystemUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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
public class PostViewTest {

    @Value("${local.server.port}")
    private int port;

    static WebDriver driver;

    static Properties pro;
    static String connectionURL;
    static String username;
    static String password;

    static Connection conn;
    static Statement stmt;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
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
        } catch (SQLException e) {
            throw new SQLException("$DB가 연결 되지 않았습니다.\n#");
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


    @Test // post객체 하나 클릭시 입력한 값이 제대로 들어가는가
    public void checkPostViewTest() throws Exception {
        String query;
        try {
            query = "Insert Into post(id, nick ,subject, content, date, hit) VALUES (2, 'TEST', 'TESTSUBJECT', 'TESTCONTENT', '2017/01/16', 20);";
            stmt.executeUpdate(query);

            String baseURL = "http://localhost:" + port + "/postview/2";
            driver.get(baseURL);

            WebElement td = driver.findElement(By.className("postViewId"));
            assertEquals("$주소 '/'에서 게시물의 제목 클릭시 주소 '/postview/{id}'에서 게시물의 번호가 제대로 반영되지 않았습니다.\n#", "2", td.getText());
            td = driver.findElement(By.className("postViewNick"));
            assertEquals("$주소 '/'에서 게시물의 제목 클릭시 주소 '/postview/{id}'에서 게시물의 닉네임이 제대로 반영되지 않았습니다.\n#", "TEST", td.getText());
            td = driver.findElement(By.className("postViewContent"));
            assertEquals("$주소 '/'에서 게시물의 제목 클릭시 주소 '/postview/{id}'에서 게시물의 내용이 제대로 반영되지 않았습니다.\n#", "TESTCONTENT", td.getText());
            td = driver.findElement(By.className("postViewDate"));
            assertEquals("$주소 '/'에서 게시물의 제목 클릭시 주소 '/postview/{id}'에서 게시물의 날짜가 제대로 반영되지 않았습니다.\n#", "2017/01/16", td.getText());
            td = driver.findElement(By.className("postViewSubject"));
            assertEquals("$주소 '/'에서 게시물의 제목 클릭시 주소 '/postview/{id}'에서 게시물의 제목이 제대로 반영되지 않았습니다.\n#", "TESTSUBJECT", td.getText());

        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$PostView.html이 제대로 호출되지 않았습니다.\n#");
        } finally {
            query = "TRUNCATE TABLE post;";
            stmt.executeUpdate(query);
        }
    }

    @Test // postview로 들어오면 조회수가 제대로 적용되는가
    public void checkPostViewHitTest() throws Exception {
        String query;
        try {
            query = "Insert Into post(id, nick ,subject, content, date, hit) VALUES (2, 'TEST', 'TESTSUBJECT', 'TESTCONTENT', '2017/01/16', 20);";
            stmt.executeUpdate(query);

            String baseURL = "http://localhost:" + port;
            driver.get(baseURL);

            WebElement td = driver.findElement(By.className("homeHit"));
            int expected_hit = Integer.parseInt(td.getText()) + 1;

            baseURL = "http://localhost:" + port + "/postview/2";
            driver.get(baseURL);

            td = driver.findElement(By.className("postViewHit"));
            assertEquals("$주소 '/postview/{id}'로 이동시 게시물의 조회수 증가 제대로 수행되지 않았습니다.\n#", Integer.toString(expected_hit), td.getText());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$PostView.html이 제대로 호출되지 않았습니다.\n#");
        } finally {
            query = "TRUNCATE TABLE post;";
            stmt.executeUpdate(query);
        }
    }

    @Test // 뒤로가기 버튼이 제대로 작동하는가
    public void backPostViewTest() throws Exception {
        String query;
        try {
            query = "Insert Into post(id, nick ,subject, content, date, hit) VALUES (2, 'TEST', 'TESTSUBJECT', 'TESTCONTENT', '2017/01/16', 2);";
            stmt.executeUpdate(query);

            String baseURL = "http://localhost:" + port + "/postview/2";
            driver.get(baseURL);

            driver.findElement(By.className("back")).click();

            assertEquals("$주소 '/postview/{id}'에서 주소 '/'로 뒤로가기 버튼이 제대로 수행되지 않았습니다.\n#", "http://localhost:" + port + "/", driver.getCurrentUrl());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$PostView.html이 제대로 호출되지 않았습니다.\n#");
        } finally {
            query = "TRUNCATE TABLE post;";
            stmt.executeUpdate(query);
        }
    }

    @Test // 삭제 클릭시 post객체 데이터가 제대로 삭제가 되는가
    public void deletePostTest() throws Exception {
        String query;
        try {
            query = "Insert Into post(id, nick ,subject, content, date, hit) VALUES (2, 'TEST', 'TESTSUBJECT', 'TESTCONTENT', '2017/01/16', 2);";
            stmt.executeUpdate(query);

            String baseURL = "http://localhost:" + port;
            driver.get(baseURL);

            List<WebElement> div = driver.findElements(By.className("postList"));
            int expected_size = div.size() - 1;

            baseURL = "http://localhost:" + port + "/postview/2";
            driver.get(baseURL);

            driver.findElement(By.className("del")).click();

            div = driver.findElements(By.cssSelector("div.postList"));
            assertEquals("$주소 '/postview/{id}'에서 삭제 버튼 클릭시 게시물 삭제가 제대로 수행되지 않았습니다.\n#", expected_size, div.size());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$PostView.html이 제대로 호출되지 않았습니다.\n#");
        } finally {
            query = "TRUNCATE TABLE post;";
            stmt.executeUpdate(query);
        }
    }

    @Test // modify 페이지로 제대로 넘어가는가
    public void moveToModifyTest() throws Exception {
        String query;
        try {
            query = "Insert Into post(id, nick ,subject, content, date, hit) VALUES (2, 'TEST', 'TESTSUBJECT', 'TESTCONTENT', '2017/01/16', 2);";
            stmt.executeUpdate(query);

            String baseURL = "http://localhost:" + port + "/postview/2";
            driver.get(baseURL);

            driver.findElement(By.className("modify")).click();

            assertEquals("$주소 '/postview/{id}'에서 주소 '/postview/modify/{id}'로의 이동이 제대로 수행되지 않았습니다.\n#", "http://localhost:" + port + "/postview/modify/2", driver.getCurrentUrl());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$PostView.html이 제대로 호출되지 않았습니다.\n#");
        } finally {
            query = "TRUNCATE TABLE post;";
            stmt.executeUpdate(query);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() {
        driver.quit();
    }
}