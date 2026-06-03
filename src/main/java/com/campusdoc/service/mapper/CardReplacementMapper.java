package com.campusdoc.service.mapper;

import com.campusdoc.service.entity.CardReplacementRequestEntity;
import com.campusdoc.service.entity.CardReplacementStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CardReplacementMapper {

    int insert(CardReplacementRequestEntity entity);

    CardReplacementRequestEntity findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int updateStatus(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("status") CardReplacementStatus status);

    List<CardReplacementRequestEntity> listByUser(@Param("userId") Long userId,
                                                  @Param("status") CardReplacementStatus status,
                                                  @Param("offset") int offset,
                                                  @Param("limit") int limit);

    long countByUser(@Param("userId") Long userId, @Param("status") CardReplacementStatus status);
}
