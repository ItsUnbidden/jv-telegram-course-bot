package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Video;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, String> {

}
