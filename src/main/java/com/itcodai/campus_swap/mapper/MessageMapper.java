package com.itcodai.campus_swap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcodai.campus_swap.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Update("UPDATE t_message SET is_read=1, updated_at=NOW() " +
            "WHERE conversation_id=#{convId} AND receiver_id=#{userId} AND is_read=0")
    int markReadByConversation(@Param("convId") Long convId, @Param("userId") Long userId);
}
