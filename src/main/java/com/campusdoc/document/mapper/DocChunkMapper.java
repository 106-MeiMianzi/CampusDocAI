package com.campusdoc.document.mapper;

import com.campusdoc.document.entity.DocChunkEntity;
import com.campusdoc.document.entity.DocChunkVectorRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocChunkMapper {

    int batchInsert(@Param("chunks") List<DocChunkEntity> chunks);

    int deleteByDocumentId(@Param("documentId") Long documentId);

    List<DocChunkEntity> findByDocumentId(@Param("documentId") Long documentId);

    List<DocChunkEntity> keywordSearch(@Param("userId") Long userId,
                                       @Param("documentId") Long documentId,
                                       @Param("keyword") String keyword,
                                       @Param("limit") int limit);

    List<DocChunkEntity> findByIds(@Param("ids") List<Long> ids);

    List<DocChunkVectorRow> listForVectorIndex();
}
