package com.live.quiz.services;

import com.live.quiz.entity.AnswerHistory;
import com.live.quiz.entity.User;
import com.live.quiz.repository.AnswerHistoryRepository;
import com.live.quiz.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class QuizService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AnswerHistoryRepository answerHistoryRepository;
    
    // Data soal matematika (soal, jawaban, poin)
    private static final String[][] QUESTIONS = {
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
                    
                    return true;
                }
            }
        }
        return false;
    }
    
    public List<User> getTopUsers() {
        return userRepository.findTop5ByOrderByTotalPointsDesc();
    }
    
    public List<AnswerHistory> getCorrectAnswers() {
        return answerHistoryRepository.findCorrectAnswersOrderByDate();
    }
    
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}