package com.live.quiz.services;

import com.live.quiz.entity.AnswerHistory;
import com.live.quiz.entity.User;
import com.live.quiz.repository.AnswerHistoryRepository;
import com.live.quiz.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class QuizService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AnswerHistoryRepository answerHistoryRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    // Data soal matematika yang bisa diganti otomatis
    private static String[][] QUESTIONS = {
        {"15 + 27", "42", "10"},
        {"8 × 7", "56", "10"},
        {"144 ÷ 12", "12", "15"},
        {"25²", "625", "20"},
        {"√81", "9", "15"},
        {"2³ + 5", "13", "15"},
        {"100 - 37", "63", "10"},
        {"12 × 8 ÷ 4", "24", "15"},
        {"3⁴", "81", "20"},
        {"45 + 78 - 23", "100", "10"}
    };
    
    // Pool soal tambahan untuk mengganti soal yang sudah dijawab
    private static final String[][] ADDITIONAL_QUESTIONS = {
        {"99 - 45", "54", "10"},
        {"7 × 9", "63", "10"},
        {"150 ÷ 6", "25", "15"},
        {"4³", "64", "20"},
        {"√64", "8", "15"},
        {"5² - 10", "15", "15"},
        {"85 + 29", "114", "10"},
        {"20 × 3 ÷ 4", "15", "15"},
        {"6²", "36", "20"},
        {"200 - 89", "111", "10"},
        {"11 × 6", "66", "10"},
        {"96 ÷ 8", "12", "15"},
        {"3⁵", "243", "25"},
        {"√49", "7", "15"},
        {"12² - 100", "44", "20"},
        {"75 + 38", "113", "10"},
        {"18 × 4 ÷ 6", "12", "15"},
        {"9²", "81", "20"},
        {"300 - 156", "144", "10"}
    };
    
    private int additionalQuestionIndex = 0;
    
    public String[][] getQuestions() {
        return QUESTIONS;
    }
    
    @Transactional
    public boolean processAnswer(String username, String comment) {
        // Cek setiap soal untuk melihat apakah comment adalah jawaban yang benar
        for (int i = 0; i < QUESTIONS.length; i++) {
            String correctAnswer = QUESTIONS[i][1];
            if (comment.trim().equals(correctAnswer)) {
                // Cek apakah user sudah menjawab soal ini sebelumnya
                Optional<AnswerHistory> existingAnswer = answerHistoryRepository
                    .findByUsernameAndQuestionNumber(username, i + 1);
                
                if (existingAnswer.isEmpty()) {
                    int points = Integer.parseInt(QUESTIONS[i][2]);
                    
                    // Simpan ke answer history
                    AnswerHistory history = new AnswerHistory(username, i + 1, comment, true, points);
                    answerHistoryRepository.save(history);
                    
                    // Update atau buat user baru
                    User user = userRepository.findByUsername(username)
                        .orElse(new User(username));
                    user.addPoints(points);
                    userRepository.save(user);
                    
                    // Kirim notifikasi jawaban benar ke frontend
                    sendCorrectAnswerNotification(i, username, comment, points);
                    
                    // Ganti soal dengan soal baru secara otomatis
                    changeQuestion(i);
                    
                    return true;
                }
            }
        }
        return false;
    }
    
    private void sendCorrectAnswerNotification(int questionIndex, String username, String answer, int points) {
        Map<String, Object> update = new HashMap<>();
        update.put("type", "correct_answer");
        update.put("questionIndex", questionIndex);
        update.put("username", username);
        update.put("answer", answer);
        update.put("points", points);
        update.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/quiz-updates", update);
    }
    
    private void changeQuestion(int questionIndex) {
        // Ambil soal baru dari pool tambahan
        if (additionalQuestionIndex < ADDITIONAL_QUESTIONS.length) {
            String[] newQuestion = ADDITIONAL_QUESTIONS[additionalQuestionIndex];
            
            // Ganti soal di array
            QUESTIONS[questionIndex] = newQuestion.clone();
            additionalQuestionIndex++;
            
            // Kirim update soal baru ke frontend
            Map<String, Object> update = new HashMap<>();
            update.put("type", "question_changed");
            update.put("questionIndex", questionIndex);
            update.put("newQuestion", newQuestion[0]);
            update.put("newAnswer", newQuestion[1]);
            update.put("newPoints", newQuestion[2]);
            update.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/quiz-updates", update);
            
            // Log perubahan soal
            System.out.println("Soal " + (questionIndex + 1) + " diganti dengan: " + newQuestion[0] + " = " + newQuestion[1]);
        }
    }
    
    public List<User> getTopUsers() {
        return userRepository.findTop20ByOrderByTotalPointsDesc();
    }
    
    public List<AnswerHistory> getCorrectAnswers() {
        return answerHistoryRepository.findCorrectAnswersOrderByDate();
    }
    
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    // Method untuk reset quiz (opsional)
    public void resetQuiz() {
        // Reset ke soal awal
        QUESTIONS = new String[][]{
            {"15 + 27", "42", "10"},
            {"8 × 7", "56", "10"},
            {"144 ÷ 12", "12", "15"},
            {"25²", "625", "20"},
            {"√81", "9", "15"},
            {"2³ + 5", "13", "15"},
            {"100 - 37", "63", "10"},
            {"12 × 8 ÷ 4", "24", "15"},
            {"3⁴", "81", "20"},
            {"45 + 78 - 23", "100", "10"}
        };
        
        additionalQuestionIndex = 0;
        
        // Kirim notifikasi reset ke frontend
        Map<String, Object> update = new HashMap<>();
        update.put("type", "quiz_reset");
        update.put("questions", QUESTIONS);
        update.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/quiz-updates", update);
    }
}