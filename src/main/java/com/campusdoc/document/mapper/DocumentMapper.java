package com.campusdoc.document.mapper;

import com.campusdoc.document.entity.DocumentEntity;
import com.campusdoc.document.entity.DocumentStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentMapper {

    int insert(DocumentEntity document);

    DocumentEntity findById(@Param("id") Long id);

    DocumentEntity findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int updateStatus(@Param("id") Long id,
                     @Param("status") DocumentStatus status,
                     @Param("errorMsg") String errorMsg);

    List<DocumentEntity> listByUser(@Param("userId") Long userId,
                                    @Param("status") DocumentStatus status,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    long countByUser(@Param("userId") Long userId, @Param("status") DocumentStatus status);

    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int deleteById(@Param("id") Long id);

    DocumentEntity findAccessibleById(@Param("id") Long id, @Param("userId") Long userId);

    List<DocumentEntity> listAccessible(@Param("userId") Long userId,
                                        @Param("status") DocumentStatus status,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    long countAccessible(@Param("userId") Long userId, @Param("status") DocumentStatus status);
}
