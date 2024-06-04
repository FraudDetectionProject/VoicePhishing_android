package com.example.voicephising;

public class Result {
    private boolean resultCode;
    private int fraudCode;
    private Double probability;
    private int reasoncode;
    private data data;

    public void setResultCode(boolean resultCode) {
        this.resultCode = resultCode;
    }

    public boolean getResultCode(){
        return this.resultCode;
    }

    public void setFraudCode(int fraudCode) {
        this.fraudCode = fraudCode;
    }

    public int getFraudCode(){
        return this.fraudCode;
    }

    public String getData() {
        return data.getTranscript();
    }

    public void setReasoncode(int reason) {
        this.reasoncode = reason;
    }

    public int getReasoncode(){ return this.reasoncode; }

    public Double getProbability() {
        return probability;
    }

    public void setProbability(Double probability) {
        this.probability = probability;
    }

    private class data{
        private String transcript;

        public String getTranscript(){
            return this.transcript;
        }

        public void setTranscript(String tr){
            this.transcript = new String(tr);
        }
    }
}