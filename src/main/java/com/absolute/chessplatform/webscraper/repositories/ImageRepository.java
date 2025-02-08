package com.absolute.chessplatform.webscraper.repositories;

import com.absolute.chessplatform.webscraper.entities.ImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageRepository extends JpaRepository<ImageEntity, Integer> {
}
