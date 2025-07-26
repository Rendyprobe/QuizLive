package com.live.quiz.repository;

import com.live.quiz.entity.AnswerHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerHistoryRepository extends JpaRepository<AnswerHistory, Long> {
    
    List<AnswerHistory> findByUsername(String username);
    
    List<AnswerHistory> findByQuestionNumber(Integer questionNumber);
    
    @Query("SELECT ah FROM AnswerHistory ah WHERE ah.username = :username AND ah.questionNumber = :questionNumber")
    Optional<AnswerHistory> findByUsernameAndQuestionNumber(@Param("username") String username, @Param("questionNumber") Integer questionNumber);
    
    @Query("SELECT ah FROM AnswerHistory ah WHERE ah.isCorrect = true ORDER BY ah.answeredAt DESC")
    List<AnswerHistory> findCorrectAnswersOrderByDate();
}