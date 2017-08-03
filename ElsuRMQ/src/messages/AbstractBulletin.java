/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package messages;

import java.util.*;

/**
 *
 * @author dhaliwal-admin
 */
public abstract class AbstractBulletin {

    UUID _messageId = null;
    String _className = "";
    String _article = "";
    String _packet = "";
    java.util.Date _date = null;
    long _expiration = 0L;

    public AbstractBulletin() {
        setClassName();
        setMessageId();
        setDate();
    }
    
    public AbstractBulletin(String article, String packet) throws Exception {
        setClassName();
        setMessageId();
        setDate();

        setArticle(article);
        setPacket(packet);
    }
    
    public AbstractBulletin(String article, String packet, java.util.Date date) throws Exception {
        setClassName();
        setMessageId();

        setArticle(article);
        setPacket(packet);
        setDate(date);
    }
    
    public AbstractBulletin(String article, String packet, java.util.Date date, long expiration) throws Exception {
        setClassName();
        setMessageId();

        setArticle(article);
        setPacket(packet);
        setDate(date);
        setExpiration(expiration);
    }
    
    public AbstractBulletin(String article, String packet, long expiration) throws Exception {
        setClassName();
        setMessageId();
        setDate();

        setArticle(article);
        setPacket(packet);
        setExpiration(expiration);
    }
    
    public AbstractBulletin(UUID messageId, String article, String packet) throws Exception {
        setClassName();
        setDate();

        setMessageId(messageId);
        setArticle(article);
        setPacket(packet);
    }

    public AbstractBulletin(UUID messageId, String article, String packet, java.util.Date date) throws Exception {
        setClassName();

        setMessageId(messageId);
        setArticle(article);
        setPacket(packet);
        setDate(date);
    }

    public AbstractBulletin(UUID messageId, String article, String packet, java.util.Date date, long expiration) throws Exception {
        setClassName();

        setMessageId(messageId);
        setArticle(article);
        setPacket(packet);
        setDate(date);
        setExpiration(expiration);
    }

    public AbstractBulletin(UUID messageId, String article, String packet, long expiration) throws Exception {
        setClassName();
        setDate();

        setMessageId(messageId);
        setArticle(article);
        setPacket(packet);
        setExpiration(expiration);
    }
    
    public UUID getMessageId() {
        return this._messageId;
    }
    public void setMessageId() {
        this._messageId = UUID.randomUUID();
    }
    private void setMessageId(UUID messageId) {
        this._messageId = messageId;
    }
    
    public String getClassName() {
        return this._className;
    }
    private void setClassName() {
        this._className = getClass().toString().replace("class ", "");
    }
    
    public String getArticle() {
        return this._article;
    }
    public void setArticle(String article) throws Exception {
        if (article == null) {
            throw new Exception(this.getClass().toString() + ", " + "article is null");
        }
        
        this._article = article;
    }
    
    public String getPacket() {
        return this._packet;
    }
    public void setPacket(String packet) {
        this._packet = packet;
    }
    
    public java.util.Date getDate() {
        return this._date;
    }
    public void setDate() {
        this._date = new Date();
    }
    public void setDate(java.util.Date date) {
        if (date == null) {
            date = new Date();
        }
        
        this._date = date;
    }
    
    public long getExpiration() {
        return this._expiration;
    }
    public void setExpiration(long expiration) {
        this._expiration = expiration;
    }
}
