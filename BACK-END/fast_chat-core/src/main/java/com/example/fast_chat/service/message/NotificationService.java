package com.example.fast_chat.service.message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {


  @Autowired
  private SimpMessagingTemplate messagingTemplate;


  public void sendDirectNotification(String msg) {

     //messagingTemplate.convertAndSend("/topic/me"  + "/notify", 1L);

    String username = "mdmoon@bu.ac.bd";
    messagingTemplate.convertAndSendToUser(username, "/queue/notify", 1L);

  }




}
