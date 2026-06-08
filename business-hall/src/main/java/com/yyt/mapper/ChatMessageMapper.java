package com.yyt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yyt.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
