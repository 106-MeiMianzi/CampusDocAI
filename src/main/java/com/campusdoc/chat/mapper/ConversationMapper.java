package com.campusdoc.chat.mapper;

import com.campusdoc.chat.entity.ConversationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ConversationMapper {

    int insert(ConversationEntity conversation);

    ConversationEntity findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<ConversationEntity> listByUserId(@Param("userId") Long userId);

    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
