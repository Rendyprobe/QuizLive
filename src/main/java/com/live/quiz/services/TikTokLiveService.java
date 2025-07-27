package com.live.quiz.services;

import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.live.LiveClient;
import io.github.jwdeveloper.tiktok.data.events.TikTokCommentEvent;
import io.github.jwdeveloper.tiktok.data.events.TikTokConnectedEvent;
import io.github.jwdeveloper.tiktok.data.events.TikTokDisconnectedEvent;
import io.github.jwdeveloper.tiktok.data.events.TikTokErrorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class TikTokLiveService {
    
    private static final Logger logger = LoggerFactory.getLogger(TikTokLiveService.class);
    
    @Autowired
    private QuizService quizService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Value("${tiktok.username:@yourusername}")
    private String tikTokUsername;
    
    @Value("${tiktok.auto.connect:false}")
    private boolean autoConnect;
    
    private LiveClient liveClient;
    private boolean connected = false;
    
    @PostConstruct
    public void init() {
        if (autoConnect) {
            connectToTikTokLive();
        }
    }
    
    public void connectToTikTokLive() {
        try {
            logger.info("Connecting to TikTok Live: {}", tikTokUsername);
            
            liveClient = TikTokLive.newClient(tikTokUsername)
                .onComment((liveClient, event) -> handleComment(liveClient, event))
                .onConnected((liveClient, event) -> handleConnected(liveClient, event))
                .onDisconnected((liveClient, event) -> handleDisconnected(liveClient, event))
                .onError((liveClient, event) -> handleError(liveClient, event))
                .buildAndConnect();
                
        } catch (Exception e) {
            logger.error("Failed to connect to TikTok Live: {}", e.getMessage());
            connected = false;
        }
    }
    
    private void handleComment(LiveClient liveClient, TikTokCommentEvent event) {
        try {
            String username = event.getUser().getProfileName();
            String comment = event.getText();
            
            logger.info("Comment from {}: {}", username, comment);
            
            // Process jawaban
            boolean isCorrect = quizService.processAnswer(username, comment);
            
            if (isCorrect) {
                logger.info("Correct answer from {}: {}", username, comment);
                
                // Kirim update ke frontend via WebSocket
                Map<String, Object> update = new HashMap<>();
                update.put("type", "correct_answer");
                update.put("username", username);
                update.put("answer", comment);
                update.put("timestamp", System.currentTimeMillis());
                
                messagingTemplate.convertAndSend("/topic/quiz-updates", update);
            }
            
        } catch (Exception e) {
            logger.error("Error processing comment: {}", e.getMessage());
        }
    }
    
    private void handleConnected(LiveClient liveClient, TikTokConnectedEvent event) {
        connected = true;
        logger.info("Connected to TikTok Live room");
        
        // Kirim status connection ke frontend
        Map<String, Object> status = new HashMap<>();
        status.put("type", "connection_status");
        status.put("connected", true);
        status.put("roomTitle", "Connected to TikTok Live");
        
        messagingTemplate.convertAndSend("/topic/quiz-updates", status);
    }
    
    private void handleDisconnected(LiveClient liveClient, TikTokDisconnectedEvent event) {
        connected = false;
        logger.info("Disconnected from TikTok Live");
        
        // Kirim status disconnection ke frontend
        Map<String, Object> status = new HashMap<>();
        status.put("type", "connection_status");
        status.put("connected", false);
        
        messagingTemplate.convertAndSend("/topic/quiz-updates", status);
    }
    
    private void handleError(LiveClient liveClient, TikTokErrorEvent event) {
        connected = false;
        logger.error("TikTok Live error: {}", event.getException().getMessage());
        
        // Kirim error status ke frontend
        Map<String, Object> status = new HashMap<>();
        status.put("type", "connection_error");
        status.put("error", event.getException().getMessage());
        
        messagingTemplate.convertAndSend("/topic/quiz-updates", status);
    }
    
    public void disconnect() {
        if (liveClient != null) {
            try {
                liveClient.disconnect();
                connected = false;
                logger.info("Disconnected from TikTok Live");
            } catch (Exception e) {
                logger.error("Error disconnecting from TikTok Live: {}", e.getMessage());
            }
        }
    }
    
    public boolean isConnected() {
        return connected;
    }
}