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
public class Bulletin extends AbstractBulletin {
    String _article = "";
    String _source = "";
    UUID _bulletinId = null;

    public Bulletin() {
        super();

        setBulletinId();
    }
    
    public Bulletin(String article, String packet) throws Exception {
        super(article, packet);

        setBulletinId();
    }
    
    public Bulletin(String article, String packet, java.util.Date date) throws Exception {
        super(article, packet, date);

        setBulletinId();
    }
        
    public Bulletin(String article, String packet, java.util.Date date, long expiration) throws Exception {
        super(article, packet, date, expiration);

        setBulletinId();
    }
        
    public Bulletin(String article, String packet, long expiration) throws Exception {
        super(article, packet, expiration);

        setBulletinId();
    }

    public Bulletin(UUID bulletinId, String article, String packet, java.util.Date date) throws Exception {
        super(article, packet, date);

        setBulletinId(bulletinId);
    }

    public Bulletin(UUID bulletinId, String article, String packet, java.util.Date date, long expiration) throws Exception {
        super(article, packet, date, expiration);

        setBulletinId(bulletinId);
    }
    
    public Bulletin(UUID bulletinId, String article, String packet, long expiration) throws Exception {
        super(article, packet, expiration);

        setBulletinId(bulletinId);
    }
    
    public Bulletin(UUID bulletinId, String article, String packet, java.util.Date date, String source) throws Exception {
        super(article, packet, date);
        
        setBulletinId(bulletinId);
        setSource(source);
    }
    
    public Bulletin(UUID bulletinId, String article, String packet, java.util.Date date, String source, long expiration) throws Exception {
        super(article, packet, date, expiration);

        setBulletinId(bulletinId);
        setSource(source);
    }
    
    public Bulletin(UUID bulletinId, String article, String packet, String source, long expiration) throws Exception {
        super(article, packet, expiration);

        setBulletinId(bulletinId);
        setSource(source);
    }
    
    public Bulletin(UUID bulletinId, UUID messageUUID, String article, String packet) throws Exception {
        super(messageUUID, article, packet);
        
        setBulletinId(bulletinId);
    }

    public Bulletin(UUID bulletinId, UUID messageUUID, String article, String packet, java.util.Date date) throws Exception {
        super(messageUUID, article, packet, date);
        
        setBulletinId(bulletinId);
    }

    public Bulletin(UUID bulletinId, UUID messageUUID, String article, String packet, java.util.Date date, long expiration) throws Exception {
        super(messageUUID, article, packet, date, expiration);
        
        setBulletinId(bulletinId);
    }

    public Bulletin(UUID bulletinId, UUID messageUUID, String article, String packet, java.util.Date date, String source) throws Exception {
        super(messageUUID, article, packet, date);
        
        setBulletinId(bulletinId);
        setSource(source);
    }

    public Bulletin(UUID bulletinId, UUID messageUUID, String article, String packet, java.util.Date date, String source, long expiration) throws Exception {
        super(messageUUID, article, packet, date, expiration);
        
        setBulletinId(bulletinId);
        setSource(source);
    }

    public Bulletin(UUID bulletinId, UUID messageUUID, String article, String packet, String source, long expiration) throws Exception {
        super(messageUUID, article, packet, expiration);
        
        setBulletinId(bulletinId);
        setSource(source);
    }
    
    public String getSource() {
        return this._source;
    }
    public void setSource(String source) {
        this._source = source;
    }
    
    public UUID getBulletinId() {
        return this._bulletinId;
    }
    public void setBulletinId() {
        this._bulletinId = UUID.randomUUID();
    }
    public void setBulletinId(UUID bulletinId) throws Exception {
        if (bulletinId == null) {
            throw new Exception(this.getClass().toString() + ", " + "bulletinId is null");
        }
        
        this._bulletinId = bulletinId;
    }

}
