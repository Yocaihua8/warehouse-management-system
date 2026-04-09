package com.yocaihua.wms.mapper;

import com.yocaihua.wms.entity.AiRecognitionRecord;
import com.yocaihua.wms.vo.AiRecognitionRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiRecognitionRecordMapper {

    int insert(AiRecognitionRecord record);

    AiRecognitionRecord selectById(@Param("id") Long id);

    AiRecognitionRecord selectByIdForUpdate(@Param("id") Long id);

    AiRecognitionRecord selectByTaskNo(@Param("taskNo") String taskNo);

    int updateStatus(@Param("id") Long id,
                     @Param("recognitionStatus") String recognitionStatus,
                     @Param("errorMessage") String errorMessage,
                     @Param("supplierName") String supplierName,
                     @Param("rawText") String rawText,
                     @Param("warningsJson") String warningsJson,
                     @Param("resultJson") String resultJson);

    int updateConfirmedOrderId(@Param("id") Long id,
                               @Param("confirmedOrderId") Long confirmedOrderId,
                               @Param("recognitionStatus") String recognitionStatus);

    List<AiRecognitionRecordVO> selectInboundRecordList();

    List<AiRecognitionRecordVO> selectOutboundRecordList();
}
