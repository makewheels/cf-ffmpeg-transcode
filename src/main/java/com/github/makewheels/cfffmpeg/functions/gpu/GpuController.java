package com.github.makewheels.cfffmpeg.functions.gpu;

import cn.hutool.core.util.IdUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GpuController {
    @RequestMapping("/")
    public String hello() {
        return "this is gpu controller " + IdUtil.simpleUUID();
    }
}
