package com.Vaish.SpringSentinel.repository;

import com.Vaish.SpringSentinel.model.Bot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotRespository extends JpaRepository<Bot, Long> {
}