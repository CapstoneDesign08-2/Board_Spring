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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AppConfig.class)
@WebIntegrationTest(randomPort = true)
public class ControllerTest {

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

    @Test // Write 페이지로 이동할수 있는가
    public void moveToWriteTest() throws Exception {
        try {
            String baseURL = "http://localhost:" + port;
            driver.get(baseURL);

            driver.findElement(By.className("writeBtn")).click();

            assertEquals("$주소 '/'에서 주소 '/write'로의 이동이 제대로 수행되지 않았습니다.\n#", "http://localhost:" + port + "/write", driver.getCurrentUrl());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$Write.html이 제대로 호출되지 않았습니다.\n#");
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

            assertEquals("$주소 '/'에서 주소 '/postview/{id}'로의 이동이 제대로 수행되지 않았습니다.\n#", "http://localhost:" + port + "/postview/1", driver.getCurrentUrl());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$PostView.html이 제대로 호출되지 않았습니다.\n#");
        } finally {
            query = "TRUNCATE TABLE post;";
            stmt.executeUpdate(query);
        }
    }

    @Test // post의 객체의 id, 제목, 닉네임, 해당조회수가 내림차순으로 적용되었나
    public void checkDescPostTest() throws Exception {
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
            assertEquals("$게시물의 제목이 제대로 적용되지 않았습니다.\n#", "TESTSUBJECT3", td.getText());
            td = driver.findElement(By.className("homeNick"));
            assertEquals("$게시물의 글쓴이가 제대로 적용되지 않았습니다.\n#", "TEST3", td.getText());
            td = driver.findElement(By.className("homeDate"));
            assertEquals("$게시물의 날짜가 제대로 적용되지 않았습니다.\n#", "2017/01/18", td.getText());
            td = driver.findElement(By.className("homeHit"));
            assertEquals("$게시물의 조회수가 제대로 적용되지 않았습니다.\n#", "30", td.getText());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$Home.html이 제대로 호출되지 않았습니다.\n#");
        } finally {
            query = "TRUNCATE TABLE post;";
            stmt.executeUpdate(query);
        }
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
            assertEquals("$주소 '/postview/modify/{id}'로 이동시 게시물의 닉네임 default 값이 들어가있지 않습니다.\n#", "TEST", td.getAttribute("value"));
            td = driver.findElement(By.name("subject"));
            assertEquals("$주소 '/postview/modify/{id}'로 이동시 게시물의 제목 default 값이 들어가있지 않습니다.\n#", "TESTSUBJECT", td.getAttribute("value"));
            td = driver.findElement(By.name("content"));
            assertEquals("$주소 '/postview/modify/{id}'로 이동시 게시물의 내용 default 값이 들어가있지 않습니다.\n#", "TESTCONTENT", td.getAttribute("value"));
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$Modify.html이 제대로 호출되지 않았습니다.\n#");
        } finally {
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
            assertEquals("$주소 '/postview/modify/{id}'에서 수정된 게시물의 번호가 적용되지 않았습니다.\n#", "2", td.getText());
            td = driver.findElement(By.className("postViewNick"));
            assertEquals("$주소 '/postview/modify/{id}'에서 수정된 게시물의 닉네임이 적용되지 않았습니다.\n#", "MODIFY_NICK", td.getText());
            td = driver.findElement(By.className("postViewSubject"));
            assertEquals("$주소 '/postview/modify/{id}'에서 수정된 게시물의 제목이 적용되지 않았습니다.\n#", "MODIFY_SUBJECT", td.getText());
            td = driver.findElement(By.className("postViewContent"));
            assertEquals("$주소 '/postview/modify/{id}'에서 수정된 게시물의 내용이 적용되지 않았습니다.\n#", "MODIFY_CONTENT", td.getText());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$Modify.html이 제대로 호출되지 않았습니다.\n#");
        } finally {
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

            assertEquals("$주소 '/postview/modify/{id}'가 주소 '/postview/{id}'로 뒤로가기 버튼이 제대로 수행되지 않았습니다.\n#", "http://localhost:" + port + "/postview/2", driver.getCurrentUrl());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$Modify.html이 제대로 호출되지 않았습니다.\n#");
        } finally {
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

            assertEquals("$주소 '/postview/modify/{id}'에서 게시물의 닉네임에 공백이 들어간채로 작성시 'ErrorPage.html'이 제대로 호출되지 않았습니다.\n#", "Error", driver.getTitle());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$Modify.html이 제대로 호출되지 않았습니다.\n#");
        } finally {
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

            assertEquals("$주소 '/postview/modify/{id}'에서 게시물의 제목에 공백이 들어간채로 작성시 'ErrorPage.html'이 제대로 호출되지 않았습니다.\n#", "Error", driver.getTitle());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$Modify.html이 제대로 호출되지 않았습니다.\n#");
        } finally {
            query = "TRUNCATE TABLE post;";
            stmt.executeUpdate(query);
        }
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
            assertEquals("$게시물 작성후 주소'/postview/{id}'로 이동시 게시물의 번호가 제대로 적용되지 않았습니다.\n#", "1", td.getText());
            td = driver.findElement(By.className("postViewNick"));
            assertEquals("$게시물 작성후 주소'/postview/{id}'로 이동시 게시물의 닉네임가 제대로 적용되지 않았습니다.\n#", "NICK", td.getText());
            td = driver.findElement(By.className("postViewHit"));
            assertEquals("$게시물 작성후 주소'/postview/{id}'로 이동시 게시물의 조회수가 1로 제대로 초기화되지 않았습니다.\n#", "1", td.getText());
            td = driver.findElement(By.className("postViewSubject"));
            assertEquals("$게시물 작성후 주소'/postview/{id}'로 이동시 게시물의 제목이 제대로 적용되지 않았습니다.\n#", "SUBJECT", td.getText());
            td = driver.findElement(By.className("postViewContent"));
            assertEquals("$게시물 작성후 주소'/postview/{id}'로 이동시 게시물의 내용이 제대로 적용되지 않았습니다.\n#", "CONTENT", td.getText());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$Write.html이 제대로 호출되지 않았습니다.\n#");
        } finally {
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

            assertEquals("$주소 '/write'에서 주소'/'로의 뒤로가기 버튼이 제대로 수행되지 않았습니다.\n#", "http://localhost:" + port + "/", driver.getCurrentUrl());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$Write.html이 제대로 호출되지 않았습니다.\n#");
        }
    }

    @Test // write에서 post객체를 생성하면 date가 오늘날짜로 설정되는가
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
            assertEquals("$게시물 작성시 날짜가 현재 시스템 날짜로 설정되지 않았습니다.\n#", today.format(d), td.getText());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$Write.html이 제대로 호출되지 않았습니다.\n#");
        } finally {
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

            assertEquals("$주소'/write'에서 값을 모두 입력하고 작성시 주소'/postview/{id}'로의 이동이 제대로 수행되지 않았습니다.\n#", "http://localhost:" + port + "/postview/1", driver.getCurrentUrl());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$Write.html이 제대로 호출되지 않았습니다.\n#");
        } finally {
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

            assertEquals("$주소 '/write'에서 게시물의 닉네임에 공백이 들어간채로 작성시 'ErrorPage.html'이 제대로 호출되지 않았습니다.\n#", "Error", driver.getTitle());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$Write.html이 제대로 호출되지 않았습니다.\n#");
        } finally {
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

            assertEquals("$주소 '/write'에서 게시물의 제목에 공백이 들어간채로 작성시 'ErrorPage.html'이 제대로 호출되지 않았습니다.\n#", "Error", driver.getTitle());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("$Write.html이 제대로 호출되지 않았습니다.\n#");
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