package com.yocaihua.wms.mapper;

import com.yocaihua.wms.entity.AiRecognitionItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiRecognitionItemMapper {

    int insert(AiRecognitionItem item);

    int batchInsert(@Param("itemList") List<AiRecognitionItem> itemList);

    List<AiRecognitionItem> selectByRecordId(@Param("recordId") Long recordId);

    int deleteByRecordId(@Param("recordId") Long recordId);
}