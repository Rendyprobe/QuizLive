package com.live.quiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.live.quiz.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    @Query("SELECT u FROM User u ORDER BY u.totalPoints DESC LIMIT 22")
    List<User> findTop22ByOrderByTotalPointsDesc();
    
    boolean existsByUsername(String username);
}