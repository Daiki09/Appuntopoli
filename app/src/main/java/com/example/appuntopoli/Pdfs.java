package com.example.appuntopoli;

public class Pdfs {

    public String uid, time, date, postPdf, title, fullname;

    public Pdfs() {}

    public Pdfs(String uid, String time, String date, String postPdf, String title, String fullname){

        this.uid = uid;
        this.time = time;
        this.date = date;
        this.postPdf = postPdf;
        this.title = title;
        this.fullname = fullname;

    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPostPdf() {
        return postPdf;
    }

    public void setPostPdf(String postImage) {
        this.postPdf = postPdf;
    }

    public String getPdfTitle() {
        return title;
    }

    public void setPdfTitle(String title) {
        this.title = title;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

}
