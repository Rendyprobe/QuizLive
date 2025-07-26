package com.live.quiz.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "answer_history")
public class AnswerHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String username;
    
    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;
    
    @Column(nullable = false)
    private String answer;
    
    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;
    
    @Column(name = "points_earned")
    private Integer pointsEarned = 0;
    
    @Column(name = "answered_at")
    private LocalDateTime answeredAt = LocalDateTime.now();
    
    // Constructors
    public AnswerHistory() {}
    
    public AnswerHistory(String username, Integer questionNumber, String answer, Boolean isCorrect, Integer pointsEarned) {
        this.username = username;
        this.questionNumber = questionNumber;
        this.answer = answer;
        this.isCorrect = isCorrect;
        this.pointsEarned = pointsEarned;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public Integer getQuestionNumber() {
        return questionNumber;
    }
    
    public void setQuestionNumber(Integer questionNumber) {
        this.questionNumber = questionNumber;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    public void setAnswer(String answer) {
        this.answer = answer;
    }
    
    public Boolean getIsCorrect() {
        return isCorrect;
    }
    
    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
    
    public Integer getPointsEarned() {
        return pointsEarned;
    }
    
    public void setPointsEarned(Integer pointsEarned) {
        this.pointsEarned = pointsEarned;
    }
    
    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }
    
    public void setAnsweredAt(LocalDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }
}