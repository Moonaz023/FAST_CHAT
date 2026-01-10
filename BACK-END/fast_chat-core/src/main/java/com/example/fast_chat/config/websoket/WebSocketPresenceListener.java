package com.example.fast_chat.config.websoket;

import com.example.fast_chat.service.conversation.OnlineUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
@RequiredArgsConstructor
public class WebSocketPresenceListener {

  private final OnlineUserService onlineUserService;
  private final SimpMessagingTemplate messagingTemplate;


  @EventListener
  public void handleConnect(SessionConnectedEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    if (accessor.getUser() == null) return;

    String username = accessor.getUser().getName();
    onlineUserService.userConnected(username);

    // 🔁 broadcast only (NO snapshot here)
    messagingTemplate.convertAndSend(
        "/topic/online-users",
        onlineUserService.getOnlineUsers()
    );
  }

  @EventListener
  public void handleSubscribe(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    if (accessor.getUser() == null) return;

    String destination = accessor.getDestination();
    if (!"/user/queue/online-users".equals(destination)) return;

    String username = accessor.getUser().getName();

    // 🔥 SEND SNAPSHOT ONLY AFTER SUBSCRIBE
    messagingTemplate.convertAndSendToUser(
        username,
        "/queue/online-users",
        onlineUserService.getOnlineUsers()
    );
  }

  @EventListener
  public void handleDisconnect(SessionDisconnectEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    if (accessor.getUser() == null) return;

    String username = accessor.getUser().getName();
    onlineUserService.userDisconnected(username);

    messagingTemplate.convertAndSend(
        "/topic/online-users",
        onlineUserService.getOnlineUsers()
    );
  }
}


