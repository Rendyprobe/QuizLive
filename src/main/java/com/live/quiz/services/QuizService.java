package com.live.quiz.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.live.quiz.entity.AnswerHistory;
import com.live.quiz.entity.User;
import com.live.quiz.repository.AnswerHistoryRepository;
import com.live.quiz.repository.UserRepository;

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
        {"45 + 78 - 23", "100", "10"},
        {"18 × 6", "108", "10"},    
        {"225 ÷ 15", "15", "15"},    
        {"7² - 10", "39", "15"},    
        {"√169", "13", "20"}        
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
        {"300 - 156", "144", "10"},
        {"13 × 8", "104", "10"},    
        {"196 ÷ 14", "14", "15"},
        {"8² + 15", "79", "15"},
        {"√121", "11", "20"},
        {"250 - 67", "183", "10"},
        {"15 × 9", "135", "10"},
        {"289 ÷ 17", "17", "15"},
        {"6³", "216", "25"}
    };
    
    private int additionalQuestionIndex = 0;
    
    // Track soal mana yang sudah dijawab untuk setiap user
    private Set<String> answeredQuestions = new HashSet<>();
    
    public String[][] getQuestions() {
        return QUESTIONS;
    }
    
    @Transactional
    public boolean processAnswer(String username, String comment) {
        // Cek setiap soal untuk melihat apakah comment adalah jawaban yang benar
        for (int i = 0; i < QUESTIONS.length; i++) {
            String correctAnswer = QUESTIONS[i][1];
            if (comment.trim().equals(correctAnswer)) {
                
                // Generate unique key untuk soal ini berdasarkan konten soal, bukan indeks
                String questionKey = QUESTIONS[i][0] + "_" + QUESTIONS[i][1];
                String userQuestionKey = username + "_" + questionKey;
                
                // Cek apakah user sudah menjawab soal dengan konten yang sama
                if (!answeredQuestions.contains(userQuestionKey)) {
                    
                    // Cek di database juga untuk memastikan
                    Optional<AnswerHistory> existingAnswer = answerHistoryRepository
                        .findByUsernameAndQuestionNumber(username, i + 1);
                    
                    // Jika belum ada di database, atau jika ada tapi soalnya sudah berbeda
                    boolean canAnswer = true;
                    if (existingAnswer.isPresent()) {
                        // Cek apakah jawaban di database untuk soal yang sama
                        AnswerHistory history = existingAnswer.get();
                        // Jika jawaban sebelumnya untuk soal yang berbeda, maka boleh jawab
                        if (history.getAnswer().equals(correctAnswer)) {
                            canAnswer = false; // Sudah pernah jawab soal dengan jawaban yang sama
                        }
                    }
                    
                    if (canAnswer) {
                        int points = Integer.parseInt(QUESTIONS[i][2]);
                        
                        // Simpan ke answer history
                        AnswerHistory history = new AnswerHistory(username, i + 1, comment, true, points);
                        answerHistoryRepository.save(history);
                        
                        // Update atau buat user baru
                        User user = userRepository.findByUsername(username)
                            .orElse(new User(username));
                        user.addPoints(points);
                        userRepository.save(user);
                        
                        // Mark soal ini sudah dijawab oleh user ini
                        answeredQuestions.add(userQuestionKey);
                        
                        // Kirim notifikasi jawaban benar ke frontend
                        sendCorrectAnswerNotification(i, username, comment, points);
                        
                        // Ganti soal dengan soal baru secara otomatis
                        changeQuestion(i);
                        
                        System.out.println("User " + username + " berhasil menjawab soal " + (i+1) + ": " + comment);
                        
                        return true;
                    }
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
        } else {
            // Jika habis soal tambahan, reset ke pool awal tapi dengan soal random
            resetToRandomQuestion(questionIndex);
        }
    }
    
    private void resetToRandomQuestion(int questionIndex) {
        // Buat soal random sederhana
        Random random = new Random();
        int a = random.nextInt(50) + 10; // 10-59
        int b = random.nextInt(30) + 5;  // 5-34
        
        String[] operations = {"+", "-", "×"};
        String operation = operations[random.nextInt(operations.length)];
        
        int answer;
        String questionText;
        
        switch (operation) {
            case "+":
                answer = a + b;
                questionText = a + " + " + b;
                break;
            case "-":
                if (a < b) {
                    int temp = a;
                    a = b;
                    b = temp;
                }
                answer = a - b;
                questionText = a + " - " + b;
                break;
            case "×":
                answer = a * b;
                questionText = a + " × " + b;
                break;
            default:
                answer = a + b;
                questionText = a + " + " + b;
        }
        
        String[] newQuestion = {questionText, String.valueOf(answer), "10"};
        
        // Ganti soal di array
        QUESTIONS[questionIndex] = newQuestion.clone();
        
        // Kirim update soal baru ke frontend
        Map<String, Object> update = new HashMap<>();
        update.put("type", "question_changed");
        update.put("questionIndex", questionIndex);
        update.put("newQuestion", newQuestion[0]);
        update.put("newAnswer", newQuestion[1]);
        update.put("newPoints", newQuestion[2]);
        update.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/quiz-updates", update);
        
        System.out.println("Soal " + (questionIndex + 1) + " diganti dengan soal random: " + questionText + " = " + answer);
    }
    
    public List<User> getTopUsers() {
        return userRepository.findTop22ByOrderByTotalPointsDesc();
    }
    
    public List<AnswerHistory> getCorrectAnswers() {
        return answerHistoryRepository.findCorrectAnswersOrderByDate();
    }
    
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    // Method untuk reset quiz (opsional)
    public void resetQuiz() {
        // Reset ke soal awal dengan 14 soal
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
            {"45 + 78 - 23", "100", "10"},
            {"18 × 6", "108", "10"},
            {"225 ÷ 15", "15", "15"},
            {"7² - 10", "39", "15"},
            {"√169", "13", "20"}
        };
        
        additionalQuestionIndex = 0;
        answeredQuestions.clear(); // Clear tracking
        
        // Kirim notifikasi reset ke frontend
        Map<String, Object> update = new HashMap<>();
        update.put("type", "quiz_reset");
        update.put("questions", QUESTIONS);
        update.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/quiz-updates", update);
        
        System.out.println("Quiz berhasil direset");
    }
}