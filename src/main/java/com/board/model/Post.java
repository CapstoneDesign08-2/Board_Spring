package com.board.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id ;         // 번호
    private String nick;        // 글쓴이
    private String subject;     // 제목
    private String content;     // 내용
    private String date;          // 날짜
    private int hit;            // 조회

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
