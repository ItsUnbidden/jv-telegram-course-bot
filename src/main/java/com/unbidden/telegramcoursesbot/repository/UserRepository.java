package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    @Query("""
        from UserEntity u
        where exists(
            select 1
            from BotRole br
            left join br.role r
            where br.bot.id = :botId and br.user = u and r.type = :type
        )        
    """)
    Page<UserEntity> findByRoleType(Long botId, RoleType type, Pageable pageable);

    @Query("""
        select u.id
        from UserEntity u
        where exists(
            select 1
            from BotRole br
            left join br.role r
            where br.bot.id = :botId and br.user = u and r.type in :types
        )
    """)
    List<Long> findAllIdsByBotIdAndRoleTypeIn(Long botId, List<RoleType> types);

    @Query("""
        from UserEntity u
        where exists(
            select 1
            from BotRole br
            left join br.role r
            where br.bot.id = :botId and br.user = u and r.type = 'DIRECTOR' and r.type = 'CREATOR'
                    and r.type = 'MENTOR' and r.type = 'SUPPORT' 
        )        
    """)
    List<UserEntity> findAllStaffMembers(Long botId);

    @Query("""
        from UserEntity u
        where exists(
            select 1
            from BotRole br
            left join br.role r
            where br.bot.id = :botId and br.user = u and br.isReceivingHomework
                and (r.type = 'DIRECTOR' or r.type = 'CREATOR' or r.type = 'MENTOR')
        )
    """)
    List<UserEntity> findByReceivingHomeworkInBot(Long botId);

    @Query("""
        from UserEntity u
        where exists(
            select 1
            from CourseOwnership co
            where co.course.id = :courseId and co.user = u and co.status = 'ACTIVE'
        )
    """)
    Page<UserEntity> findByActiveCourseOwnership(Long courseId, Pageable pageable);

    @Query("""
        from UserEntity u
        where exists(
            select 1
            from CourseProgress cp
            where cp.course.id = :courseId and cp.user = u and cp.numberOfTimesCompleted > 0
        )
    """)
    Page<UserEntity> findAllCompletedCourse(Long courseId, Pageable pageable);

    @Query("""
        from UserEntity u
        where exists(
            select 1
            from CourseProgress cp
            where cp.course.id = :courseId and cp.user = u and cp.numberOfTimesCompleted = 0 and cp.stage = :stage
        )
    """)
    Page<UserEntity> findAllAtCourseStage(Long courseId, Integer stage, Pageable pageable);
}
