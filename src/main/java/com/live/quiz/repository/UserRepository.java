package com.live.quiz.repository;

import com.live.quiz.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    @Query("SELECT u FROM User u ORDER BY u.totalPoints DESC LIMIT 5")
    List<User> findTop5ByOrderByTotalPointsDesc();
    
    boolean existsByUsername(String username);
}