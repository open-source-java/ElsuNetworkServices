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
public interface IQueueEvents {

    public void OnQueueAdded(UUID QueueId, AbstractBulletin packet);

    public void OnQueueRetrieved(UUID QueueId, AbstractBulletin packet);

    public void OnQueuePurged(UUID QueueId, AbstractBulletin packet);

    public void OnQueueError(UUID QueueId, AbstractBulletin packet, Exception e);
}
