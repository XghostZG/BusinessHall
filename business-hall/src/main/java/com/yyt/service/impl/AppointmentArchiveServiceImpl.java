package com.yyt.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yyt.entity.AppointmentArchive;
import com.yyt.mapper.AppointmentArchiveMapper;
import com.yyt.service.AppointmentArchiveService;
import org.springframework.stereotype.Service;

@Service
public class AppointmentArchiveServiceImpl extends ServiceImpl<AppointmentArchiveMapper, AppointmentArchive> implements AppointmentArchiveService {
}
