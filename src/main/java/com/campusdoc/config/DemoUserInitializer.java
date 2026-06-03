package com.campusdoc.config;

import com.campusdoc.user.entity.UserEntity;
import com.campusdoc.user.entity.UserRole;
import com.campusdoc.user.mapper.UserMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class DemoUserInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DemoUserInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        upsertTeacher("123456789", "123456", "小希", "副教授",
                "信息科学与工程学院", "xiaoxi@univ.edu.cn");
        upsertStudent("987654321", "654321", "小泉",
                "信息学院", "大一", "计算机科学与技术", "计算机1班", "13800138000");
    }

    private void upsertTeacher(String username, String password, String name, String jobTitle,
                               String college, String email) {
        UserEntity user = userMapper.findByUsername(username);
        if (user == null) {
            user = new UserEntity();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(UserRole.TEACHER);
            userMapper.insert(user);
            user = userMapper.findByUsername(username);
        }
        user.setRole(UserRole.TEACHER);
        user.setDisplayName(name);
        user.setCollege(college);
        user.setEmail(email);
        user.setJobTitle(jobTitle);
        userMapper.updateProfile(user);
    }

    private void upsertStudent(String username, String password, String name, String college,
                               String grade, String major, String className, String phone) {
        UserEntity user = userMapper.findByUsername(username);
        if (user == null) {
            user = new UserEntity();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(UserRole.STUDENT);
            userMapper.insert(user);
            user = userMapper.findByUsername(username);
        }
        user.setRole(UserRole.STUDENT);
        user.setDisplayName(name);
        user.setCollege(college);
        user.setGrade(grade);
        user.setMajor(major);
        user.setClassName(className);
        user.setPhone(phone);
        userMapper.updateProfile(user);
    }
}
