package com.aksh.rand.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
@Data
public class TradeInfo {
    private long id;
    private String sybmol;
    private Double shares;
    private Double price;
    private long traderId;
    private String status;
    private Date date;

    public TradeInfo(long id, String sybmol, Double shares, Double price, long traderId, String status, Date date) {
        this.id = id;
        this.sybmol = sybmol;
        this.shares = shares;
        this.price = price;
        this.traderId = traderId;
        this.status = status;
        this.date = date;
    }
}
