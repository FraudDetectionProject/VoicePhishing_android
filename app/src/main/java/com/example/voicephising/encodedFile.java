package com.example.voicephising;

public class encodedFile {
    private boolean checkfraud;
    private String file;

    encodedFile(boolean checkfraud, String file){
        this.checkfraud = checkfraud;
        this.file = new String(file);
    }

    public boolean isCheckfraud() {
        return checkfraud;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file){
        this.file = new String(file);
    }
}
