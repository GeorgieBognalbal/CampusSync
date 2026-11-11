package com.example.campussync.data.repository;

import com.example.campussync.data.local.AnnouncementDao;
import com.example.campussync.data.local.AnnouncementEntity;

import java.util.List;

public class AnnouncementRepository {

    private final AnnouncementDao announcementDao;

    public AnnouncementRepository(AnnouncementDao announcementDao) {
        this.announcementDao = announcementDao;
    }

    public List<AnnouncementEntity> getAll() {
        return announcementDao.getAll();
    }

    public List<AnnouncementEntity> search(String keyword) {
        return announcementDao.search(keyword);
    }

    public void insertAll(List<AnnouncementEntity> announcements) {
        announcementDao.insertAll(announcements);
    }

    public void deleteAll() {
        announcementDao.deleteAll();
    }

    public int count() {
        return announcementDao.count();
    }
}
