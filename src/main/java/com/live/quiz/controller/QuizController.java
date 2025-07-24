package com.live.quiz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class QuizController {

    @GetMapping("/display")
    public String showQuizPage() {
        return "display"; // akan menampilkan display.html di folder templates
    }
}
