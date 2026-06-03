package com.campusdoc.user.mapper;

import com.campusdoc.user.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    UserEntity findByUsername(@Param("username") String username);

    UserEntity findById(@Param("id") Long id);

    int insert(UserEntity user);

    int countByUsername(@Param("username") String username);

    int updateAvatar(@Param("id") Long id, @Param("avatarUrl") String avatarUrl);

    int updateProfile(UserEntity user);
}
