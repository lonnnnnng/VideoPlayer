package com.zy.player.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zy.player.data.local.dao.HistoryDao
import com.zy.player.data.local.dao.LiveSourceDao
import com.zy.player.data.local.dao.VideoSiteDao
import com.zy.player.data.local.entity.HistoryEntity
import com.zy.player.data.local.entity.LiveSourceEntity
import com.zy.player.data.local.entity.VideoSiteEntity

@Database(
    entities = [
        VideoSiteEntity::class,
        HistoryEntity::class,
        LiveSourceEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoSiteDao(): VideoSiteDao
    abstract fun historyDao(): HistoryDao
    abstract fun liveSourceDao(): LiveSourceDao

    class Callback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // 插入默认视频源
            db.execSQL("""
                INSERT INTO video_sites (name, apiUrl, enabled, sortOrder, lastCheckStatus, lastCheckTime, lastLatencyMs, isDefault)
                VALUES
                ('无尽资源', 'https://api.wujinapi.me/api.php/provide/vod/', 1, 1, '可播放', 0, 0, 1),
                ('量子资源', 'https://cj.lziapi.com/api.php/provide/vod/', 1, 2, '未检测', 0, 0, 0),
                ('非凡资源', 'https://cj.ffzyapi.com/api.php/provide/vod/', 1, 3, '未检测', 0, 0, 0)
            """)

            // 插入默认直播源
            db.execSQL("""
                INSERT INTO live_sources (name, url, enabled, sortOrder, lastCheckStatus, lastCheckTime)
                VALUES
                ('IPTV直播源', '${DefaultSources.DEFAULT_LIVE_SOURCE_URL}', 1, 1, '未检测', 0),
                ('播放测试源', '${DefaultSources.PLAYBACK_TEST_LIVE_SOURCE_URL}', 1, 2, '可播放', 0)
            """)
        }
    }

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    UPDATE video_sites
                    SET sortOrder = sortOrder + 1
                    WHERE apiUrl != 'https://api.wujinapi.me/api.php/provide/vod/'
                      AND sortOrder >= 1
                """)
                db.execSQL("""
                    INSERT INTO video_sites (name, apiUrl, enabled, sortOrder, lastCheckStatus, lastCheckTime)
                    SELECT '无尽资源', 'https://api.wujinapi.me/api.php/provide/vod/', 1, 1, '可播放', 0
                    WHERE NOT EXISTS (
                        SELECT 1 FROM video_sites
                        WHERE apiUrl = 'https://api.wujinapi.me/api.php/provide/vod/'
                    )
                """)
                db.execSQL("""
                    UPDATE video_sites
                    SET enabled = 1, sortOrder = 1, lastCheckStatus = '可播放'
                    WHERE apiUrl = 'https://api.wujinapi.me/api.php/provide/vod/'
                """)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    UPDATE live_sources
                    SET url = '${DefaultSources.DEFAULT_LIVE_SOURCE_URL}', lastCheckStatus = '未检测'
                    WHERE url = '${DefaultSources.LEGACY_IPV6_LIVE_SOURCE_URL}'
                """)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    INSERT INTO live_sources (name, url, enabled, sortOrder, lastCheckStatus, lastCheckTime)
                    SELECT '播放测试源', '${DefaultSources.PLAYBACK_TEST_LIVE_SOURCE_URL}', 1, 2, '可播放', 0
                    WHERE NOT EXISTS (
                        SELECT 1 FROM live_sources
                        WHERE url = '${DefaultSources.PLAYBACK_TEST_LIVE_SOURCE_URL}'
                    )
                """)
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    UPDATE live_sources
                    SET url = '${DefaultSources.DEFAULT_LIVE_SOURCE_URL}', lastCheckStatus = '未检测'
                    WHERE name = 'IPTV直播源'
                      AND url = '${DefaultSources.LEGACY_DEFAULT_LIVE_SOURCE_URL}'
                """)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE video_sites ADD COLUMN lastLatencyMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE video_sites ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    UPDATE video_sites
                    SET isDefault = 1
                    WHERE id = (
                        SELECT id
                        FROM video_sites
                        WHERE enabled = 1
                        ORDER BY sortOrder ASC, id ASC
                        LIMIT 1
                    )
                """)
            }
        }
    }
}
