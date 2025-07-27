package com.live.quiz.controller;

import com.live.quiz.entity.User;
import com.live.quiz.services.QuizService;
import com.live.quiz.services.TikTokLiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class QuizController {
    
    @Autowired
    private QuizService quizService;
    
    @Autowired
    private TikTokLiveService tikTokLiveService;
    
    @GetMapping("/")
    public String display(Model model) {
        // Ambil data soal
        String[][] questions = quizService.getQuestions();
        model.addAttribute("questions", questions);
        
        // Ambil top 5 users
        List<User> topUsers = quizService.getTopUsers();
        model.addAttribute("topUsers", topUsers);
        
        return "display";
    }
    
    @GetMapping("/api/leaderboard")
    @ResponseBody
    public List<User> getLeaderboard() {
        return quizService.getTopUsers();
    }
    
    @PostMapping("/api/connect-tiktok")
    @ResponseBody
    public ResponseEntity<String> connectTikTok() {
        try {
            tikTokLiveService.connectToTikTokLive();
            return ResponseEntity.ok("Connected to TikTok Live");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to connect: " + e.getMessage());
        }
    }
    
    @GetMapping("/api/connection-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getConnectionStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("connected", tikTokLiveService.isConnected());
        return ResponseEntity.ok(status);
    }
    
    @PostMapping("/api/disconnect-tiktok")
    @ResponseBody
    public ResponseEntity<String> disconnectTikTok() {
        try {
            tikTokLiveService.disconnect();
            return ResponseEntity.ok("Disconnected from TikTok Live");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to disconnect: " + e.getMessage());
        }
    }

    @GetMapping("/display")
    public String showQuizPage() {
        return "display"; // akan menampilkan display.html di folder templates
    @GetMapping("/test-tiktok")
@ResponseBody
public String testTikTok() {
    try {
        tikTokLiveService.connectToTikTokLive();
        return "TikTok connection test started. Check logs.";
    } catch (Exception e) {
        return "Error: " + e.getMessage();
    }
}

    
}