package com.yocaihua.wms.mapper;

import com.yocaihua.wms.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User selectByUsername(@Param("username") String username);

    List<User> selectAll();

    void updatePasswordById(@Param("id") Long id, @Param("password") String password);

    List<User> selectPage(@Param("username") String username,
                          @Param("nickname") String nickname,
                          @Param("offset") Integer offset,
                          @Param("pageSize") Integer pageSize);

    Long count(@Param("username") String username,
               @Param("nickname") String nickname);

    User selectById(@Param("id") Long id);

    int insert(User user);

    int updateById(User user);

    int deleteById(@Param("id") Long id);
}
