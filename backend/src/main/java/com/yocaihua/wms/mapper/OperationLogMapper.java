package com.yocaihua.wms.mapper;

import com.yocaihua.wms.entity.OperationLog;
import com.yocaihua.wms.vo.OperationLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OperationLogMapper {

    int insert(OperationLog log);

    Long countLog(@Param("actionType") String actionType,
                  @Param("moduleName") String moduleName,
                  @Param("operatorName") String operatorName,
                  @Param("resultStatus") String resultStatus,
                  @Param("bizNo") String bizNo);

    List<OperationLogVO> selectLogPage(@Param("actionType") String actionType,
                                       @Param("moduleName") String moduleName,
                                       @Param("operatorName") String operatorName,
                                       @Param("resultStatus") String resultStatus,
                                       @Param("bizNo") String bizNo,
                                       @Param("offset") Integer offset,
                                       @Param("pageSize") Integer pageSize);
}

