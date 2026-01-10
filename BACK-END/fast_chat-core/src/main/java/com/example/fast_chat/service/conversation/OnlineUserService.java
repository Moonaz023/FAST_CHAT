package com.example.fast_chat.service.conversation;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class OnlineUserService {

  // username -> active session count
  private final Map<String, AtomicInteger> onlineUsers = new ConcurrentHashMap<>();

  public void userConnected(String username) {
    onlineUsers
        .computeIfAbsent(username, u -> new AtomicInteger(0))
        .incrementAndGet();
  }

  public void userDisconnected(String username) {
    AtomicInteger count = onlineUsers.get(username);
    if (count != null && count.decrementAndGet() <= 0) {
      onlineUsers.remove(username);
    }
  }

  public Set<String> getOnlineUsers() {
    return onlineUsers.keySet();
  }
}

