/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interfaces;

import java.util.*;
import messages.*;

/**
 *
 * @author dhaliwal-admin
 */
public interface IQueue {

    public AbstractBulletin getBulletin();

    public AbstractBulletin getBulletin(long Expiration);

    public AbstractBulletin getBulletin(UUID BulletinId);

    public AbstractBulletin getBulletin(UUID BulletinId, long Expiration);

    public AbstractBulletin getBulletin(String Article);

    public List<AbstractBulletin> getBulletinsByCategory(String Category);

    public UUID addBulletin(AbstractBulletin packet);

    public UUID addBulletin(String article, String category, String packet, java.util.Date date, long expiration, String client);

    public boolean purgeQueue();

    public long removeBulletin(UUID BulletinId);

    public long removeBulletin(String Article);

    public long removeBulletinByCategory(String Category);
    
}
